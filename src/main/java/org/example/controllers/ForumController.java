package org.example.controllers;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.example.entities.ForumComment;
import org.example.entities.Post;
import org.example.entities.ForumReaction;
import org.example.services.*;
import org.example.utils.EmojiUtil;

import java.io.File;
import java.net.URL;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

public class ForumController implements Initializable {

    // FXML Elements
    @FXML private TextField searchField;
    @FXML private VBox categoriesContainer;
    @FXML private VBox recentPostsContainer;
    @FXML private ComboBox<String> sortComboBox;
    @FXML private ComboBox<String> categoryComboBox;
    @FXML private Button gridViewBtn;
    @FXML private Button listViewBtn;
    @FXML private VBox postsContainer;
    @FXML private TextArea postContentArea;
    @FXML private Button attachmentButton;
    @FXML private Button emojiButton;
    @FXML private Button postButton;
    @FXML private Label attachmentLabel;
    @FXML private HBox attachmentPreviewBox;
    @FXML private Label postContentError;

    // Services
    private ForumPostService postService;
    private ForumCommentService commentService;
    private ForumReactionService reactionService;

    // Current user
    private int currentUserId = 1;
    private String currentUsername = "User";
    private String currentUserAvatar = "👤";

    // Random names for generating initials based on author ID
    private final String[][] RANDOM_NAMES = {
        {"Ahmed", "Ben Salah"}, {"Fatima", "Zahra"}, {"Mohamed", "Ali"}, {"Sarra", "Khalil"},
        {"Omar", "Dridi"}, {"Leila", "Mansour"}, {"Karim", "Guesmi"}, {"Nadia", "Farsi"},
        {"Youssef", "Bouaziz"}, {"Amina", "Cherif"}, {"Hassan", "Miladi"}, {"Rania", "Saidi"},
        {"Tarek", "Jaziri"}, {"Mona", "Haddad"}, {"Sami", "Karray"}, {"Dorra", "Zarrouk"},
        {"Anis", "Mabrouk"}, {"Ines", "Ben Amor"}, {"Wael", "Gharbi"}, {"Sonia", "Trabelsi"}
    };

    // State
    private List<Post> allPosts = new ArrayList<>();
    private boolean isGridView = true;
    private String selectedCategory = "Tous les posts";
    private String selectedSort = "Popularité (likes)";
    private String attachedFilePath = null;

    // Categories
    private final List<String> categories = Arrays.asList(
            "Tous les posts",
            "Conseils d'entraînement",
            "Nutrition",
            "Motivation",
            "Questions",
            "Réussites",
            "Équipement"
    );

    private final Map<String, String> categoryColors = Map.of(
            "Conseils d'entraînement", "#27ae60",
            "Nutrition", "#3498db",
            "Motivation", "#f39c12",
            "Questions", "#9b59b6",
            "Réussites", "#e74c3c",
            "Équipement", "#1142c1"
    );

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        try {
            postService = new ForumPostService();
            commentService = new ForumCommentService();
            reactionService = new ForumReactionService();

            // Get current user from session
            SessionContext context = SessionContext.getInstance();
            currentUsername = context.getDisplayName();
            currentUserId = 1;

            setupEventHandlers();
            // Initialize error labels and simple listeners
            if (postContentError != null) postContentError.setVisible(false);
            if (postContentArea != null) {
                postContentArea.textProperty().addListener((obs, oldVal, newVal) -> {
                    if (newVal != null && !newVal.trim().isEmpty()) postContentError.setVisible(false);
                });
            }
            setupCategories();
            setupSortCombo();
            setupViewToggle();
            loadAllPosts();

        } catch (Exception e) {
            showError("Error initializing forum: " + e.getMessage());
        }
    }

    private void setupEventHandlers() {
        postButton.setOnAction(event -> handlePostSubmit());
        attachmentButton.setOnAction(event -> handleAttachmentSelect());
        emojiButton.setOnAction(event -> handleEmojiPicker());
        searchField.setOnKeyReleased(event -> filterAndDisplayPosts());
        sortComboBox.setOnAction(event -> {
            selectedSort = sortComboBox.getValue();
            displayPosts();
        });
    }

    private void setupCategories() {
        for (String category : categories) {
            Button categoryBtn = new Button(category);
            categoryBtn.setStyle("-fx-padding: 10 12; -fx-font-size: 12px; -fx-text-alignment: left;");
            categoryBtn.setMaxWidth(Double.MAX_VALUE);
            categoryBtn.setUserData(category);

            if (category.equals("Tous les posts")) {
                categoryBtn.setStyle(categoryBtn.getStyle() + "; -fx-background-color: #e8ecf0; -fx-border-color: #1142c1; -fx-border-width: 0 0 0 3;");
            } else {
                categoryBtn.setStyle(categoryBtn.getStyle() + "; -fx-background-color: #f5f7fa;");
            }

            categoryBtn.setOnAction(e -> {
                selectedCategory = category;
                updateCategoryButtons(categoryBtn);
                filterAndDisplayPosts();
            });

            categoriesContainer.getChildren().add(categoryBtn);
        }
    }

    private void updateCategoryButtons(Button selectedBtn) {
        for (Node node : categoriesContainer.getChildren()) {
            if (node instanceof Button) {
                Button btn = (Button) node;
                if (btn == selectedBtn) {
                    btn.setStyle(btn.getStyle().replaceAll("-fx-background-color: [^;]+;", "") + "-fx-background-color: #e8ecf0; -fx-border-color: #1142c1; -fx-border-width: 0 0 0 3;");
                } else {
                    btn.setStyle(btn.getStyle().replaceAll("-fx-background-color: [^;]+;", "") + "-fx-background-color: #f5f7fa;");
                }
            }
        }
    }

    private void setupSortCombo() {
        sortComboBox.getItems().addAll(
                "Popularité (likes)",
                "Plus récent",
                "Plus ancien"
        );
        sortComboBox.setValue("Popularité (likes)");

        // Setup category combo
        categoryComboBox.getItems().addAll(
                "Conseils d'entraînement",
                "Nutrition",
                "Motivation",
                "Questions",
                "Réussites",
                "Équipement",
                "Général"
        );
        categoryComboBox.setValue("Général");
    }

    private void setupViewToggle() {
        gridViewBtn.setStyle("-fx-background-color: white; -fx-border-color: #e0e0e0; -fx-border-width: 0 0 2 0; -fx-text-fill: #1142c1;");
        listViewBtn.setStyle("-fx-background-color: transparent; -fx-border-color: transparent; -fx-text-fill: #666;");

        gridViewBtn.setOnAction(e -> {
            isGridView = true;
            gridViewBtn.setStyle("-fx-background-color: white; -fx-border-color: #e0e0e0; -fx-border-width: 0 0 2 0; -fx-text-fill: #1142c1;");
            listViewBtn.setStyle("-fx-background-color: transparent; -fx-border-color: transparent; -fx-text-fill: #666;");
            displayPosts();
        });

        listViewBtn.setOnAction(e -> {
            isGridView = false;
            listViewBtn.setStyle("-fx-background-color: white; -fx-border-color: #e0e0e0; -fx-border-width: 0 0 2 0; -fx-text-fill: #1142c1;");
            gridViewBtn.setStyle("-fx-background-color: transparent; -fx-border-color: transparent; -fx-text-fill: #666;");
            displayPosts();
        });
    }

    private void loadAllPosts() {
        try {
            allPosts = postService.afficher();
            updateRecentPosts();
            filterAndDisplayPosts();
        } catch (SQLException e) {
            showError("Error loading posts: " + e.getMessage());
        }
    }

    private void updateRecentPosts() {
        recentPostsContainer.getChildren().clear();
        List<Post> recent = allPosts.stream()
                .limit(5)
                .collect(Collectors.toList());

        for (Post post : recent) {
            VBox recentItem = createRecentPostItem(post);
            recentPostsContainer.getChildren().add(recentItem);
        }
    }

    private VBox createRecentPostItem(Post post) {
        VBox item = new VBox(4);
        item.getStyleClass().add("recent-post-item");

        Label usernameLabel = new Label("👤 Author ID: " + post.getId_author_post());
        usernameLabel.getStyleClass().add("recent-post-author");

        Label dateLabel = new Label(post.getCreated_at_post());
        dateLabel.getStyleClass().add("recent-post-date");

        item.getChildren().addAll(usernameLabel, dateLabel);
        item.setOnMouseClicked(e -> scrollToPost(post.getId_post()));

        return item;
    }

    private void scrollToPost(int postId) {
        // Simple implementation - could be enhanced
        filterAndDisplayPosts();
    }

    private void filterAndDisplayPosts() {
        String searchText = searchField.getText().toLowerCase();

        List<Post> filtered = allPosts.stream()
                .filter(post -> selectedCategory.equals("Tous les posts") || post.getCategory_post().equals(selectedCategory))
                .filter(post -> post.getContent_post().toLowerCase().contains(searchText) || searchText.isEmpty())
                .sorted((p1, p2) -> {
                    if (selectedSort.equals("Popularité (likes)")) {
                        return Integer.compare(p2.getLike_count_post(), p1.getLike_count_post());
                    } else if (selectedSort.equals("Plus récent")) {
                        return p2.getCreated_at_post().compareTo(p1.getCreated_at_post());
                    } else {
                        return p1.getCreated_at_post().compareTo(p2.getCreated_at_post());
                    }
                })
                .collect(Collectors.toList());

        displayPostsByView(filtered);
    }

    private void displayPostsByView(List<Post> posts) {
        postsContainer.getChildren().clear();

        if (posts.isEmpty()) {
            Label emptyLabel = new Label("Aucun post trouvé");
            emptyLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #999;");
            postsContainer.getChildren().add(emptyLabel);
            return;
        }

        if (isGridView) {
            displayPosts();
        } else {
            displayPosts();
        }
    }

    private void displayPosts() {
        String searchText = searchField.getText().toLowerCase();

        List<Post> filtered = allPosts.stream()
                .filter(post -> selectedCategory.equals("Tous les posts") || post.getCategory_post().equals(selectedCategory))
                .filter(post -> post.getContent_post().toLowerCase().contains(searchText) || searchText.isEmpty())
                .sorted((p1, p2) -> {
                    if (selectedSort.equals("Popularité (likes)")) {
                        return Integer.compare(p2.getLike_count_post(), p1.getLike_count_post());
                    } else if (selectedSort.equals("Plus récent")) {
                        return p2.getCreated_at_post().compareTo(p1.getCreated_at_post());
                    } else {
                        return p1.getCreated_at_post().compareTo(p2.getCreated_at_post());
                    }
                })
                .collect(Collectors.toList());

        postsContainer.getChildren().clear();

        if (filtered.isEmpty()) {
            Label emptyLabel = new Label("Aucun post trouvé");
            emptyLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #999; -fx-padding: 32;");
            postsContainer.getChildren().add(emptyLabel);
            return;
        }

        if (isGridView) {
            // Grid layout - 2 columns
            FlowPane gridPane = new FlowPane();
            gridPane.setHgap(16);
            gridPane.setVgap(16);
            gridPane.setPrefWrapLength(900);

            for (Post post : filtered) {
                Node postCard = createModernPostCard(post);
                gridPane.getChildren().add(postCard);
            }

            postsContainer.getChildren().add(gridPane);
        } else {
            // List layout
            for (Post post : filtered) {
                Node postCard = createModernPostCard(post);
                postsContainer.getChildren().add(postCard);
            }
        }
    }

    private Node createModernPostCard(Post post) {
        VBox card = new VBox(0);
        card.getStyleClass().add("post-card");
        card.setPrefWidth(isGridView ? 400 : Double.MAX_VALUE);
        // Removed fixed minHeight to allow flexible resizing when comments are toggled

        // Header with user info and badge
        HBox headerBox = new HBox(12);
        headerBox.getStyleClass().add("post-header");
        headerBox.setPadding(new Insets(16));

        String initials = getInitialsForAuthor(post.getId_author_post());
        Label avatarLabel = new Label(initials);
        avatarLabel.getStyleClass().addAll("post-avatar", "standard-avatar");

        VBox userInfoBox = new VBox(4);
        userInfoBox.getStyleClass().add("post-user-info");
        String fullName = getFullNameForAuthor(post.getId_author_post());
        Label usernameLabel = new Label(fullName);
        usernameLabel.getStyleClass().add("post-username");

        Label dateLabel = new Label(post.getCreated_at_post());
        dateLabel.getStyleClass().add("post-date");

        userInfoBox.getChildren().addAll(usernameLabel, dateLabel);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Category Badge
        Label categoryBadge = new Label(post.getCategory_post().toUpperCase());
        categoryBadge.getStyleClass().add("post-category-badge");
        // Apply category-specific style class if matches
        String cat = post.getCategory_post().toLowerCase().replace(" ", "-");
        if (cat.contains("wellness") || cat.contains("nutrition")) {
            categoryBadge.getStyleClass().add("wellness");
        } else if (cat.contains("cardio") || cat.contains("force")) {
            categoryBadge.getStyleClass().add("cardio");
        } else if (cat.contains("workout") || cat.contains("conseil")) {
            categoryBadge.getStyleClass().add("workout-tips");
        } else if (cat.contains("trend") || cat.contains("tendance")) {
            categoryBadge.getStyleClass().add("trending");
        }

        headerBox.getChildren().addAll(avatarLabel, userInfoBox, spacer, categoryBadge);

        // Content
        Label contentLabel = new Label(post.getContent_post());
        contentLabel.setWrapText(true);
        contentLabel.getStyleClass().add("post-content");
        contentLabel.setPadding(new Insets(0, 16, 12, 16));

        // Media support - check for file path or BLOB
        VBox mediaBox = new VBox();
        String mediaUrl = post.getMedia_url_post();
        byte[] mediaBlob = post.getMedia_blob_post();
        String mediaType = post.getMedia_type_post();

        System.out.println("Post ID: " + post.getId_post() + " - URL: " + mediaUrl + " - BLOB size: " + (mediaBlob != null ? mediaBlob.length : 0) + " - Type: " + mediaType);

        if (mediaUrl != null && !mediaUrl.isEmpty()) {
            // Load from file path
            if (mediaType != null && mediaType.toLowerCase().contains("image")) {
                ImageView imageView = createImageViewFromPath(mediaUrl);
                if (imageView != null) {
                    imageView.getStyleClass().add("post-image-view");
                    imageView.setPreserveRatio(true);
                    imageView.setFitWidth(isGridView ? 360 : 600);
                    imageView.setFitHeight(350);
                    mediaBox.getChildren().add(imageView);
                    mediaBox.setPadding(new Insets(0, 16, 12, 16));
                }
            } else if (mediaType != null && mediaType.toLowerCase().contains("video")) {
                // Video placeholder
                VBox videoContainer = new VBox(8);
                videoContainer.getStyleClass().add("post-video-container");
                videoContainer.setAlignment(javafx.geometry.Pos.CENTER);
                videoContainer.setPadding(new Insets(16));
                videoContainer.setStyle("-fx-background-color: rgba(15, 23, 42, 0.9); -fx-background-radius: 12;");

                Label videoIcon = new Label("▶");
                videoIcon.setStyle("-fx-font-size: 48px; -fx-text-fill: #3B82F6;");

                Label videoLabel = new Label("Vidéo");
                videoLabel.getStyleClass().add("post-media-label");
                videoLabel.setStyle("-fx-text-fill: white;");

                videoContainer.getChildren().addAll(videoIcon, videoLabel);
                mediaBox.getChildren().add(videoContainer);
                mediaBox.setPadding(new Insets(0, 16, 12, 16));
            }
        } else if (mediaBlob != null && mediaBlob.length > 0) {
            // Load from BLOB
            ImageView imageView = createImageViewFromBlob(mediaBlob);
            if (imageView != null) {
                imageView.getStyleClass().add("post-image-view");
                imageView.setPreserveRatio(true);
                imageView.setFitWidth(isGridView ? 360 : 600);
                imageView.setFitHeight(350);
                mediaBox.getChildren().add(imageView);
                mediaBox.setPadding(new Insets(0, 16, 12, 16));
            }
        }

        // Actions and stats
        HBox actionsBox = new HBox(16);
        actionsBox.getStyleClass().add("post-actions");
        actionsBox.setPadding(new Insets(12, 16, 16, 16));
        actionsBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        // Like button with count
        Button likeBtn = new Button("❤ " + post.getLike_count_post());
        likeBtn.getStyleClass().add("action-button");
        likeBtn.setOnAction(e -> likePost(post));

        // Comments section (expandable, initially hidden)
        VBox commentsSection = new VBox(0);
        commentsSection.getStyleClass().add("comments-section");
        commentsSection.setVisible(false);
        commentsSection.setManaged(false);

        // Comment count label for button
        int commentCount = getCommentCount(post.getId_post());
        Button commentBtn = new Button("💬 " + (commentCount > 0 ? commentCount : "Commenter"));
        commentBtn.getStyleClass().add("action-button");
        commentBtn.setOnAction(e -> toggleCommentsSection(commentsSection, post, commentBtn));

        actionsBox.getChildren().addAll(likeBtn, commentBtn);

        // Build card
        card.getChildren().addAll(headerBox, contentLabel);
        if (!mediaBox.getChildren().isEmpty()) {
            card.getChildren().add(mediaBox);
        }
        card.getChildren().addAll(actionsBox, commentsSection);

        return card;
    }

    private void likePost(Post post) {
        try {
            post.setLike_count_post(post.getLike_count_post() + 1);
            postService.updateLikeCount(post.getId_post());
            displayPosts();
        } catch (SQLException e) {
            showError("Error liking post: " + e.getMessage());
        }
    }

    private void handlePostSubmit() {
        String content = postContentArea != null ? postContentArea.getText().trim() : "";

        boolean hasError = false;
        if (content.isEmpty()) {
            if (postContentError != null) {
                postContentError.setText("Le contenu ne peut pas être vide");
                postContentError.setVisible(true);
            }
            hasError = true;
        }

        if (hasError) return;

        try {
            String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
            String mediaType = (attachedFilePath != null) ? getMediaType(attachedFilePath) : "";
            String category = categoryComboBox.getValue() != null ? categoryComboBox.getValue() : "Général";

            Post post = new Post(
                    content,
                    attachedFilePath != null ? attachedFilePath : "",
                    mediaType,
                    "public",
                    timestamp,
                    timestamp,
                    0,
                    category,
                    currentUserId
            );

            postService.ajouter(post);
            if (postContentArea != null) postContentArea.clear();
            if (attachmentLabel != null) attachmentLabel.setText("Aucun fichier sélectionné");
            if (attachmentPreviewBox != null) attachmentPreviewBox.getChildren().clear();
            attachedFilePath = null;
            if (postContentError != null) postContentError.setVisible(false);
            loadAllPosts();
            showSuccess("Post créé avec succès!");
        } catch (SQLException e) {
            showError("Erreur lors de la création du post: " + e.getMessage());
        }
    }

    private void handleAttachmentSelect() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Sélectionner une pièce jointe");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif"),
                new FileChooser.ExtensionFilter("Vidéos", "*.mp4", "*.avi", "*.mov"),
                new FileChooser.ExtensionFilter("Tous les fichiers", "*.*")
        );

        Stage stage = new Stage();
        File file = fileChooser.showOpenDialog(stage);
        if (file != null) {
            attachedFilePath = file.getAbsolutePath();
            attachmentLabel.setText("✓ " + file.getName());
        }
    }

    private void handleEmojiPicker() {
        Stage emojiStage = new Stage();
        emojiStage.setTitle("Sélectionner un emoji");
        emojiStage.setWidth(600);
        emojiStage.setHeight(400);

        TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        Map<String, String[]> categories = EmojiUtil.getEmojisPerCategory();
        for (String category : categories.keySet()) {
            Tab tab = new Tab();
            tab.setText(category);
            tab.setStyle("-fx-font-size: 12px;");

            FlowPane flowPane = new FlowPane();
            flowPane.setPrefWrapLength(500);
            flowPane.setHgap(8);
            flowPane.setVgap(8);
            flowPane.setPadding(new Insets(12));
            flowPane.setStyle("-fx-font-size: 20px;");

            for (String emoji : categories.get(category)) {
                Button emojiBtn = new Button(emoji);
                emojiBtn.setPrefSize(40, 40);
                emojiBtn.setStyle("-fx-font-size: 20px; -fx-padding: 5;");
                emojiBtn.setOnAction(e -> {
                    postContentArea.appendText(emoji);
                    emojiStage.close();
                });
                flowPane.getChildren().add(emojiBtn);
            }

            ScrollPane scrollPane = new ScrollPane(flowPane);
            scrollPane.setFitToWidth(true);
            tab.setContent(scrollPane);
            tabPane.getTabs().add(tab);
        }

        emojiStage.setScene(new javafx.scene.Scene(tabPane));
        emojiStage.show();
    }

    private int getCommentCount(int postId) {
        try {
            return commentService.getCommentsByPostId(postId).size();
        } catch (SQLException e) {
            return 0;
        }
    }

    private void toggleCommentsSection(VBox commentsSection, Post post, Button commentBtn) {
        boolean isVisible = commentsSection.isVisible();

        if (isVisible) {
            // Hide comments
            commentsSection.setVisible(false);
            commentsSection.setManaged(false);
            int count = getCommentCount(post.getId_post());
            commentBtn.setText("💬 " + (count > 0 ? count : "Commenter"));
        } else {
            // Show and load comments
            commentsSection.setVisible(true);
            commentsSection.setManaged(true);
            loadCommentsIntoSection(commentsSection, post, commentBtn);
            commentBtn.setText("💬 Masquer");
        }
    }

    private void loadCommentsIntoSection(VBox commentsSection, Post post, Button commentBtn) {
        commentsSection.getChildren().clear();

        try {
            List<ForumComment> comments = commentService.getCommentsByPostId(post.getId_post());

            // Comments header with count
            HBox headerBox = new HBox(8);
            headerBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            headerBox.setPadding(new Insets(12, 16, 8, 16));

            Label commentsTitle = new Label("Commentaires (" + comments.size() + ")");
            commentsTitle.getStyleClass().add("comments-header-title");

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            Button closeBtn = new Button("✕");
            closeBtn.getStyleClass().add("comments-close-btn");
            closeBtn.setOnAction(e -> toggleCommentsSection(commentsSection, post, commentBtn));

            headerBox.getChildren().addAll(commentsTitle, spacer, closeBtn);
            commentsSection.getChildren().add(headerBox);

            // Separator
            Separator separator = new Separator();
            separator.getStyleClass().add("comments-separator");
            commentsSection.getChildren().add(separator);

            // Add comment input section (NOW FIRST - before comments list)
            VBox inputSection = new VBox(8);
            inputSection.getStyleClass().add("comment-input-section");
            inputSection.setPadding(new Insets(12, 16, 16, 16));

            HBox inputBox = new HBox(10);
            inputBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

            // Current user avatar
            String initials = getInitialsForAuthor(currentUserId);
            Label userAvatar = new Label(initials);
            userAvatar.getStyleClass().addAll("comment-avatar", "standard-avatar");

            // Text input
            TextArea commentInput = new TextArea();
            commentInput.setPromptText("Écrivez un commentaire...");
            commentInput.getStyleClass().add("comment-input");
            commentInput.setWrapText(true);
            commentInput.setPrefRowCount(2);
            HBox.setHgrow(commentInput, Priority.ALWAYS);

            inputBox.getChildren().addAll(userAvatar, commentInput);

            // Action buttons
            HBox actionBox = new HBox(10);
            actionBox.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);

            Button cancelBtn = new Button("Annuler");
            cancelBtn.getStyleClass().add("comment-cancel-btn");
            cancelBtn.setOnAction(e -> commentInput.clear());

            Button submitBtn = new Button("Commenter");
            submitBtn.getStyleClass().add("comment-submit-btn");
            submitBtn.setOnAction(e -> {
                String text = commentInput.getText().trim();
                if (!text.isEmpty()) {
                    addComment(post, text, commentsSection, commentBtn);
                    commentInput.clear();
                }
            });

            // Error label for validation (initially hidden)
            Label errorLabel = new Label("");
            errorLabel.getStyleClass().add("comment-error-label");
            errorLabel.setVisible(false);
            errorLabel.setManaged(false);

            actionBox.getChildren().addAll(cancelBtn, submitBtn, errorLabel);

            // Update submit action to validate
            submitBtn.setOnAction(e -> {
                String text = commentInput.getText().trim();
                if (text.isEmpty()) {
                    errorLabel.setText("⚠ Le commentaire ne peut pas être vide");
                    errorLabel.setVisible(true);
                    errorLabel.setManaged(true);
                } else {
                    errorLabel.setVisible(false);
                    errorLabel.setManaged(false);
                    addComment(post, text, commentsSection, commentBtn);
                    commentInput.clear();
                }
            });

            inputSection.getChildren().addAll(inputBox, actionBox);
            commentsSection.getChildren().add(inputSection);

            // Second separator before comments list
            Separator separator2 = new Separator();
            separator2.getStyleClass().add("comments-separator");
            commentsSection.getChildren().add(separator2);

            // Comments list (NOW AFTER input section)
            VBox commentsList = new VBox(8);
            commentsList.getStyleClass().add("comments-list");
            commentsList.setPadding(new Insets(8, 16, 8, 16));

            if (comments.isEmpty()) {
                Label noCommentsLabel = new Label("Aucun commentaire. Soyez le premier à commenter !");
                noCommentsLabel.getStyleClass().add("no-comments-label");
                commentsList.getChildren().add(noCommentsLabel);
            } else {
                for (ForumComment comment : comments) {
                    VBox commentBox = createCommentBox(comment, post, commentsSection, commentBtn);
                    commentsList.getChildren().add(commentBox);
                }
            }

            commentsSection.getChildren().add(commentsList);

        } catch (SQLException e) {
            e.printStackTrace();
            Label errorLabel = new Label("Erreur: " + e.getMessage());
            errorLabel.getStyleClass().add("error-label");
            errorLabel.setWrapText(true);
            commentsSection.getChildren().add(errorLabel);
        }
    }

    private VBox createCommentBox(ForumComment comment, Post post, VBox commentsSection, Button commentBtn) {
        VBox commentBox = new VBox(6);
        commentBox.getStyleClass().add("comment-box");

        // Comment header with avatar and user info
        HBox headerBox = new HBox(10);
        headerBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        String initials = getInitialsForAuthor(comment.getId_author_comment());
        Label avatarLabel = new Label(initials);
        avatarLabel.getStyleClass().addAll("comment-avatar", "standard-avatar");
        avatarLabel.setStyle("-fx-min-width: 32; -fx-max-width: 32; -fx-min-height: 32; -fx-max-height: 32; -fx-font-size: 12px;");

        VBox userInfoBox = new VBox(2);
        String fullName = getFullNameForAuthor(comment.getId_author_comment());
        Label usernameLabel = new Label(fullName);
        usernameLabel.getStyleClass().add("comment-username");

        // Date label
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy HH:mm");
        String dateStr = comment.getCreated_at_comment() != null ?
                sdf.format(comment.getCreated_at_comment()) : "";
        if (comment.isIs_edited()) {
            dateStr += " (modifié)";
        }
        Label dateLabel = new Label(dateStr);
        dateLabel.getStyleClass().add("comment-date");

        userInfoBox.getChildren().addAll(usernameLabel, dateLabel);
        headerBox.getChildren().addAll(avatarLabel, userInfoBox);

        // Comment content
        Label contentLabel = new Label(comment.getContent_comment());
        contentLabel.getStyleClass().add("comment-content");
        contentLabel.setWrapText(true);

        // Comment actions
        HBox actionsBox = new HBox(16);
        actionsBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        Button likeBtn = new Button("❤ " + (comment.getLike_count() > 0 ? comment.getLike_count() : "J'aime"));
        likeBtn.getStyleClass().add("comment-like-btn");
        likeBtn.setOnAction(e -> {
            try {
                commentService.likeComment(comment.getId_comment());
                loadCommentsIntoSection(commentsSection, post, commentBtn);
            } catch (SQLException ex) {
                showError("Erreur: " + ex.getMessage());
            }
        });

        Button replyBtn = new Button("↩ Répondre");
        replyBtn.getStyleClass().add("comment-reply-btn");
        replyBtn.setOnAction(e -> showReplyInput(comment, post, commentsSection, commentBtn));

        actionsBox.getChildren().addAll(likeBtn, replyBtn);

        commentBox.getChildren().addAll(headerBox, contentLabel, actionsBox);

        // Load replies if any
        try {
            List<ForumComment> replies = commentService.getRepliesByCommentId(comment.getId_comment());
            if (!replies.isEmpty()) {
                VBox repliesBox = new VBox(6);
                repliesBox.getStyleClass().add("replies-box");
                repliesBox.setPadding(new Insets(8, 0, 0, 32)); // Indent replies

                for (ForumComment reply : replies) {
                    VBox replyBox = createCommentBox(reply, post, commentsSection, commentBtn);
                    replyBox.getStyleClass().add("reply-box");
                    repliesBox.getChildren().add(replyBox);
                }
                commentBox.getChildren().add(repliesBox);
            }
        } catch (SQLException e) {
            // Ignore reply loading errors
        }

        return commentBox;
    }

    private void showReplyInput(ForumComment parentComment, Post post, VBox commentsSection, Button commentBtn) {
        // Find the comment box and add reply input below it
        VBox replyInputBox = new VBox(8);
        replyInputBox.getStyleClass().add("reply-input-box");
        replyInputBox.setPadding(new Insets(8, 0, 8, 40));

        HBox inputRow = new HBox(10);
        inputRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        TextArea replyInput = new TextArea();
        replyInput.setPromptText("Répondre à ce commentaire...");
        replyInput.getStyleClass().add("comment-input");
        replyInput.setWrapText(true);
        replyInput.setPrefRowCount(2);
        HBox.setHgrow(replyInput, Priority.ALWAYS);

        inputRow.getChildren().add(replyInput);

        HBox actionRow = new HBox(10);
        actionRow.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);

        Button cancelBtn = new Button("Annuler");
        cancelBtn.getStyleClass().add("comment-cancel-btn");
        cancelBtn.setOnAction(e -> commentsSection.getChildren().remove(replyInputBox));

        Button submitBtn = new Button("Répondre");
        submitBtn.getStyleClass().add("comment-submit-btn");
        submitBtn.setOnAction(e -> {
            String text = replyInput.getText().trim();
            if (!text.isEmpty()) {
                addReply(post, parentComment, text, commentsSection, commentBtn);
            }
        });

        actionRow.getChildren().addAll(cancelBtn, submitBtn);
        replyInputBox.getChildren().addAll(inputRow, actionRow);

        // Add to comments section
        commentsSection.getChildren().add(replyInputBox);
        replyInput.requestFocus();
    }

    private void addComment(Post post, String content, VBox commentsSection, Button commentBtn) {
        try {
            ForumComment comment = new ForumComment(content, currentUserId, post.getId_post());
            commentService.ajouter(comment);
            loadCommentsIntoSection(commentsSection, post, commentBtn);
            commentBtn.setText("💬 Masquer");
        } catch (SQLException e) {
            showError("Erreur lors de l'ajout du commentaire: " + e.getMessage());
        }
    }

    private void addReply(Post post, ForumComment parentComment, String content, VBox commentsSection, Button commentBtn) {
        try {
            ForumComment reply = new ForumComment(content, currentUserId, post.getId_post());
            reply.setParent_id(parentComment.getId_comment());
            commentService.ajouter(reply);
            loadCommentsIntoSection(commentsSection, post, commentBtn);
        } catch (SQLException e) {
            showError("Erreur lors de la réponse: " + e.getMessage());
        }
    }

    private void showCreatePostDialog() {
        // This method is no longer needed - create post is now on the main page
    }

    private String getMediaType(String filePath) {
        if (filePath == null || filePath.isEmpty()) return "";
        String extension = filePath.substring(filePath.lastIndexOf(".") + 1).toLowerCase();
        if (extension.matches("png|jpg|jpeg|gif|webp")) return "image";
        if (extension.matches("mp4|avi|mov|mkv")) return "video";
        if (extension.matches("mp3|wav|flac|aac")) return "audio";
        return "file";
    }

    private ImageView createImageViewFromPath(String filePath) {
        try {
            if (filePath == null || filePath.isEmpty()) return null;
            System.out.println("Loading image from path: " + filePath);

            // Check if it's a URL or local file path
            if (filePath.startsWith("http://") || filePath.startsWith("https://")) {
                javafx.scene.image.Image image = new javafx.scene.image.Image(filePath, true);
                if (image.isError()) {
                    System.err.println("Error loading image from URL: " + image.getException().getMessage());
                    return null;
                }
                return new ImageView(image);
            } else {
                // Local file path
                java.io.File file = new java.io.File(filePath);
                if (!file.exists()) {
                    System.err.println("File does not exist: " + filePath);
                    return null;
                }
                javafx.scene.image.Image image = new javafx.scene.image.Image(file.toURI().toString());
                if (image.isError()) {
                    System.err.println("Error loading image from file: " + image.getException().getMessage());
                    return null;
                }
                System.out.println("Image loaded successfully: " + image.getWidth() + "x" + image.getHeight());
                return new ImageView(image);
            }
        } catch (Exception e) {
            System.err.println("Error loading image from path: " + e.getMessage());
            return null;
        }
    }

    private ImageView createImageViewFromBlob(byte[] blobData) {
        try {
            if (blobData == null || blobData.length == 0) return null;
            System.out.println("Loading image from BLOB, size: " + blobData.length + " bytes");
            javafx.scene.image.Image image = new javafx.scene.image.Image(new java.io.ByteArrayInputStream(blobData));
            if (image.isError()) return null;
            return new ImageView(image);
        } catch (Exception e) {
            System.err.println("Error loading image from blob: " + e.getMessage());
            return null;
        }
    }

    private String detectImageFormat(byte[] data) {
        if (data.length < 8) return "unknown";
        // Print first 20 bytes for debugging
        StringBuilder hex = new StringBuilder("First 20 bytes: ");
        StringBuilder ascii = new StringBuilder("ASCII: ");
        for (int i = 0; i < Math.min(20, data.length); i++) {
            hex.append(String.format("%02X ", data[i]));
            char c = (char)(data[i] & 0xFF);
            ascii.append((c >= 32 && c < 127) ? c : '.');
        }
        System.out.println(hex.toString());
        System.out.println(ascii.toString());

        // Check magic numbers
        if (data[0] == (byte)0xFF && data[1] == (byte)0xD8) return "JPEG";
        if (data[0] == (byte)0x89 && data[1] == (byte)0x50) return "PNG";
        if (data[0] == (byte)0x47 && data[1] == (byte)0x49) return "GIF";
        if (data[0] == (byte)0x42 && data[1] == (byte)0x4D) return "BMP";
        if (data[0] == (byte)0x52 && data[1] == (byte)0x49) return "WEBP";
        return "unknown";
    }

    private ImageView loadImageViaBufferedImage(byte[] blobData) {
        try {
            javax.imageio.ImageIO.setUseCache(false);
            java.awt.image.BufferedImage bufferedImage = javax.imageio.ImageIO.read(
                new java.io.ByteArrayInputStream(blobData)
            );
            if (bufferedImage == null) {
                System.err.println("BufferedImage is null - data may not be a valid image");
                return null;
            }
            System.out.println("Loaded via BufferedImage: " + bufferedImage.getWidth() + "x" + bufferedImage.getHeight());

            // Convert to JavaFX Image
            javafx.scene.image.WritableImage writableImage = new javafx.scene.image.WritableImage(
                bufferedImage.getWidth(), bufferedImage.getHeight()
            );
            javafx.scene.image.PixelWriter pixelWriter = writableImage.getPixelWriter();

            for (int x = 0; x < bufferedImage.getWidth(); x++) {
                for (int y = 0; y < bufferedImage.getHeight(); y++) {
                    pixelWriter.setArgb(x, y, bufferedImage.getRGB(x, y));
                }
            }

            return new ImageView(writableImage);
        } catch (Exception e) {
            System.err.println("Error in BufferedImage conversion: " + e.getMessage());
            return null;
        }
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showSuccess(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Success");
        alert.setContentText(message);
        alert.showAndWait();
    }

    private String getInitialsForAuthor(int authorId) {
        // Use author ID to deterministically pick a name
        int index = Math.abs(authorId) % RANDOM_NAMES.length;
        String firstName = RANDOM_NAMES[index][0];
        String lastName = RANDOM_NAMES[index][1];
        return (firstName.charAt(0) + "" + lastName.charAt(0)).toUpperCase();
    }

    private String getFullNameForAuthor(int authorId) {
        int index = Math.abs(authorId) % RANDOM_NAMES.length;
        return RANDOM_NAMES[index][0] + " " + RANDOM_NAMES[index][1];
    }
}
