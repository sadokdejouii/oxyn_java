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
        item.setStyle("-fx-background-color: white; -fx-border-color: #f0f0f0; -fx-border-radius: 4; -fx-padding: 8; -fx-cursor: hand;");

        Label usernameLabel = new Label("👤 Author ID: " + post.getId_author_post());
        usernameLabel.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: #1a2332;");

        Label dateLabel = new Label(post.getCreated_at_post());
        dateLabel.setStyle("-fx-font-size: 9px; -fx-text-fill: #999;");

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
        card.setStyle("-fx-background-color: white; -fx-border-color: #e8ecf0; -fx-border-radius: 8; -fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.06), 4, 0, 0, 1);");
        card.setPrefWidth(isGridView ? 400 : Double.MAX_VALUE);
        card.setMinHeight(300);

        // Header with user info and badge
        HBox headerBox = new HBox(12);
        headerBox.setPadding(new Insets(16));
        headerBox.setStyle("-fx-background-color: white;");

        Label avatarLabel = new Label("👤");
        avatarLabel.setStyle("-fx-font-size: 40px;");

        VBox userInfoBox = new VBox(4);
        Label usernameLabel = new Label("Author ID: " + post.getId_author_post());
        usernameLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #1a2332;");

        Label dateLabel = new Label(post.getCreated_at_post());
        dateLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #999;");

        userInfoBox.getChildren().addAll(usernameLabel, dateLabel);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Category Badge
        Label categoryBadge = new Label(post.getCategory_post().toUpperCase());
        String badgeColor = categoryColors.getOrDefault(post.getCategory_post(), "#1142c1");
        categoryBadge.setStyle("-fx-font-size: 9px; -fx-font-weight: bold; -fx-padding: 4 8; -fx-background-color: " + badgeColor + "; -fx-text-fill: white; -fx-border-radius: 12;");

        headerBox.getChildren().addAll(avatarLabel, userInfoBox, spacer, categoryBadge);

        // Content
        Label contentLabel = new Label(post.getContent_post());
        contentLabel.setWrapText(true);
        contentLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #333; -fx-line-spacing: 1.5;");
        contentLabel.setPadding(new Insets(0, 16, 12, 16));

        // Media
        VBox mediaBox = new VBox();
        if (post.getMedia_url_post() != null && !post.getMedia_url_post().isEmpty()) {
            VBox mediaContainer = new VBox();
            mediaContainer.setStyle("-fx-background-color: #1a1a1a; -fx-border-radius: 8;");
            mediaContainer.setPrefHeight(180);
            mediaContainer.setAlignment(javafx.geometry.Pos.CENTER);
            mediaContainer.setPadding(new Insets(20));

            Label mediaLabel = new Label("Vidéo non disponible");
            mediaLabel.setStyle("-fx-text-fill: white; -fx-font-size: 14px;");

            Hyperlink youtubeLink = new Hyperlink("Regarder sur YouTube");
            youtubeLink.setStyle("-fx-text-fill: white; -fx-font-size: 12px;");

            mediaContainer.getChildren().addAll(mediaLabel, youtubeLink);
            mediaBox.getChildren().add(mediaContainer);
            mediaBox.setPadding(new Insets(0, 16, 12, 16));
        }

        // Actions and stats
        HBox actionsBox = new HBox(16);
        actionsBox.setPadding(new Insets(12, 16, 16, 16));
        actionsBox.setStyle("-fx-border-color: #f0f0f0; -fx-border-width: 1 0 0 0;");
        actionsBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        // Like button with count
        Button likeBtn = new Button("❤ " + post.getLike_count_post());
        likeBtn.setStyle("-fx-background-color: transparent; -fx-border-color: transparent; -fx-font-size: 12px; -fx-text-fill: #666; -fx-cursor: hand;");
        likeBtn.setOnAction(e -> likePost(post));

        // Comment button
        Button commentBtn = new Button("💬 Commenter");
        commentBtn.setStyle("-fx-background-color: transparent; -fx-border-color: transparent; -fx-font-size: 12px; -fx-text-fill: #666; -fx-cursor: hand;");
        commentBtn.setOnAction(e -> showCommentsDialog(post));

        // Share button
        Button shareBtn = new Button("🔊 Écouter");
        shareBtn.setStyle("-fx-background-color: transparent; -fx-border-color: transparent; -fx-font-size: 12px; -fx-text-fill: #666; -fx-cursor: hand;");

        // Translate button
        Button translateBtn = new Button("🗣 Traduire");
        translateBtn.setStyle("-fx-background-color: transparent; -fx-border-color: transparent; -fx-font-size: 12px; -fx-text-fill: #666; -fx-cursor: hand;");

        actionsBox.getChildren().addAll(likeBtn, commentBtn, shareBtn, translateBtn);

        card.getChildren().addAll(headerBox, contentLabel);
        if (!mediaBox.getChildren().isEmpty()) {
            card.getChildren().add(mediaBox);
        }
        card.getChildren().add(actionsBox);

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

    private void showCommentsDialog(Post post) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Commentaires");
        dialog.initModality(Modality.APPLICATION_MODAL);

        VBox content = new VBox(12);
        content.setPadding(new Insets(16));

        try {
            List<ForumComment> comments = commentService.getCommentsByPostId(post.getId_post());

            // Comments list
            VBox commentsListBox = new VBox(8);
            ScrollPane scrollPane = new ScrollPane(commentsListBox);
            scrollPane.setFitToWidth(true);
            scrollPane.setPrefHeight(300);

            for (ForumComment comment : comments) {
                VBox commentItem = new VBox(4);
                commentItem.setStyle("-fx-background-color: #f9fafc; -fx-border-radius: 4; -fx-padding: 8;");

                Label commentUser = new Label(comment.getUsername());
                commentUser.setStyle("-fx-font-size: 11px; -fx-font-weight: bold;");

                Label commentText = new Label(comment.getContent());
                commentText.setWrapText(true);
                commentText.setStyle("-fx-font-size: 12px;");

                commentItem.getChildren().addAll(commentUser, commentText);
                commentsListBox.getChildren().add(commentItem);
            }

            // Add comment input
            HBox inputBox = new HBox(8);
            TextField commentField = new TextField();
            commentField.setPromptText("Ajouter un commentaire...");
            commentField.setStyle("-fx-padding: 8;");
            HBox.setHgrow(commentField, Priority.ALWAYS);

            Button postBtn = new Button("Poster");
            postBtn.setStyle("-fx-background-color: #1142c1; -fx-text-fill: white; -fx-padding: 8 16;");
            postBtn.setOnAction(e -> {
                String text = commentField.getText().trim();
                if (!text.isEmpty()) {
                    try {
                        ForumComment comment = new ForumComment(
                                post.getId_post(),
                                currentUserId,
                                currentUsername,
                                currentUserAvatar,
                                text,
                                new Date()
                        );
                        commentService.ajouter(comment);
                        commentField.clear();
                        showCommentsDialog(post);
                    } catch (SQLException ex) {
                        showError("Error: " + ex.getMessage());
                    }
                }
            });

            inputBox.getChildren().addAll(commentField, postBtn);

            content.getChildren().addAll(scrollPane, inputBox);
            dialog.getDialogPane().setContent(content);
            dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
            dialog.showAndWait();

        } catch (SQLException e) {
            showError("Error loading comments: " + e.getMessage());
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
}
