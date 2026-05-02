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
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.example.entities.ForumComment;
import org.example.entities.Post;
import org.example.entities.ForumReaction;
import org.example.services.*;
import org.example.utils.EmojiUtil;
import org.example.utils.TranslationUtil;
import org.example.utils.CohereSummarizer;


import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

public class ForumController implements Initializable {

    // FXML Elements
    @FXML private TextField searchField;
    @FXML private ComboBox<String> categoriesFilterCombo;
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
    private GamificationService gamificationService;
    private RecommendationService recommendationService;
    private PostLikeService postLikeService;

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
    private String selectedSort = "Plus récent";
    private String attachedFilePath = null;
    private javafx.stage.Popup emojiPopup;

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
            gamificationService = new GamificationService();
            recommendationService = new RecommendationService();
            postLikeService = new PostLikeService();

            // Get current user from session
            SessionContext context = SessionContext.getInstance();
            currentUsername = context.getDisplayName();
            currentUserId = context.getUserId();

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
        categoriesFilterCombo.getItems().addAll(categories);
        categoriesFilterCombo.setValue("Tous les posts");

        categoriesFilterCombo.setOnAction(e -> {
            selectedCategory = categoriesFilterCombo.getValue();
            filterAndDisplayPosts();
        });
    }

    private void setupSortCombo() {
        sortComboBox.getItems().addAll(
                "Popularité (likes)",
                "Plus récent",
                "Plus ancien"
        );
        sortComboBox.setValue("Plus récent");

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
        gridViewBtn.getStyleClass().add("selected");

        gridViewBtn.setOnAction(e -> {
            isGridView = true;
            gridViewBtn.getStyleClass().add("selected");
            listViewBtn.getStyleClass().remove("selected");
            displayPosts();
        });

        listViewBtn.setOnAction(e -> {
            isGridView = false;
            listViewBtn.getStyleClass().add("selected");
            gridViewBtn.getStyleClass().remove("selected");
            displayPosts();
        });
    }

    private void loadAllPosts() {
        try {
            allPosts = postService.afficher();
            filterAndDisplayPosts();
        } catch (SQLException e) {
            showError("Error loading posts: " + e.getMessage());
        }
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
            emptyLabel.getStyleClass().add("empty-state-label");
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
            emptyLabel.getStyleClass().add("empty-state-label");
            postsContainer.getChildren().add(emptyLabel);
            return;
        }

        if (isGridView) {
            // Grid layout - 2 columns using FlowPane that fills width
            FlowPane gridPane = new FlowPane();
            gridPane.setHgap(20);
            gridPane.setVgap(20);
            gridPane.setPrefWrapLength(Double.MAX_VALUE);
            // Bind wrap length to container width so 2 cards fill each row
            gridPane.prefWrapLengthProperty().bind(postsContainer.widthProperty().subtract(24));

            for (Post post : filtered) {
                VBox postCard = (VBox) createModernPostCard(post);
                // Each card takes ~half the available width minus the gap
                postCard.prefWidthProperty().bind(postsContainer.widthProperty().subtract(44).divide(2));
                postCard.setMaxWidth(Double.MAX_VALUE);
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
        // Width is set externally for grid view; for list view fill the container
        if (!isGridView) {
            card.setPrefWidth(Double.MAX_VALUE);
            card.setMaxWidth(Double.MAX_VALUE);
        }

        // Header with user info and badge
        HBox headerBox = new HBox(12);
        headerBox.getStyleClass().add("post-header");
        headerBox.setPadding(new Insets(20, 20, 12, 20));

        String initials = getInitialsForAuthor(post.getId_author_post());
        Label avatarLabel = new Label(initials);
        avatarLabel.getStyleClass().addAll("post-avatar", "standard-avatar");

        VBox userInfoBox = new VBox(4);
        userInfoBox.getStyleClass().add("post-user-info");
        String fullName = getFullNameForAuthor(post.getId_author_post());
        
        // Username with badges
        HBox usernameWithBadges = new HBox(6);
        usernameWithBadges.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        
        Label usernameLabel = new Label(fullName);
        usernameLabel.getStyleClass().add("post-username");
        usernameWithBadges.getChildren().add(usernameLabel);
        
        // Fetch and display only the highest priority badge
        try {
            org.example.entities.UserGamification gamification = gamificationService.getGamificationByUserId(post.getId_author_post());
            if (gamification != null && !gamification.getBadges().isEmpty()) {
                String highestPriorityBadge = gamificationService.getHighestPriorityBadge(gamification.getBadges());
                if (highestPriorityBadge != null) {
                    Label badgeLabel = createBadgeLabel(highestPriorityBadge);
                    usernameWithBadges.getChildren().add(badgeLabel);
                }
            }
        } catch (SQLException e) {
            // Silently fail if gamification fetch fails - don't break post display
            System.err.println("Error fetching gamification for user " + post.getId_author_post() + ": " + e.getMessage());
        }
        
        userInfoBox.getChildren().add(usernameWithBadges);

        Label dateLabel = new Label(post.getCreated_at_post());
        dateLabel.getStyleClass().add("post-date");

        userInfoBox.getChildren().add(dateLabel);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Category Badge
        Label categoryBadge = new Label(post.getCategory_post().toUpperCase());
        categoryBadge.getStyleClass().add("post-category-badge");
        // Apply category-specific style class if matches
        String cat = post.getCategory_post().toLowerCase();
        if (cat.contains("nutrition")) {
            categoryBadge.getStyleClass().add("cat-nutrition");
        } else if (cat.contains("motivation")) {
            categoryBadge.getStyleClass().add("cat-motivation");
        } else if (cat.contains("question")) {
            categoryBadge.getStyleClass().add("cat-questions");
        } else if (cat.contains("réussite") || cat.contains("reussite")) {
            categoryBadge.getStyleClass().add("cat-reussites");
        } else if (cat.contains("équipement") || cat.contains("equipement")) {
            categoryBadge.getStyleClass().add("cat-equipement");
        } else if (cat.contains("conseil")) {
            categoryBadge.getStyleClass().add("cat-conseils");
        } else {
            categoryBadge.getStyleClass().add("cat-general"); // Default for "Général" or unknown
        }

        // Recommended indicator
        HBox categoryWithRecommendation = new HBox(8);
        categoryWithRecommendation.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
        categoryWithRecommendation.getChildren().add(categoryBadge);
        
        try {
            List<String> recommendedCategories = recommendationService.getRecommendedCategories(currentUserId, 5);
            if (recommendedCategories.contains(post.getCategory_post())) {
                Label recommendedLabel = new Label("✨ Recommended");
                recommendedLabel.getStyleClass().add("recommended-badge");
                categoryWithRecommendation.getChildren().add(recommendedLabel);
            }
        } catch (SQLException e) {
            // Silently fail if recommendation fetch fails
            System.err.println("Error checking recommendations: " + e.getMessage());
        }

        // Owner actions box (will be populated later if isOwner)
        HBox ownerActionsBox = new HBox(8);
        ownerActionsBox.setAlignment(javafx.geometry.Pos.TOP_RIGHT);
        ownerActionsBox.setPadding(new Insets(0, 4, 0, 0)); // a little padding to align perfectly

        headerBox.getChildren().addAll(avatarLabel, userInfoBox, spacer, ownerActionsBox, categoryWithRecommendation);

        // Content - Editable container
        VBox contentContainer = new VBox();
        contentContainer.setPadding(new Insets(0, 20, 14, 20));

        // View mode: TextFlow for colored emojis
        javafx.scene.text.TextFlow contentFlow = new javafx.scene.text.TextFlow();
        javafx.scene.text.Text contentText = new javafx.scene.text.Text(post.getContent_post());
        contentText.getStyleClass().add("post-content-text");
        contentFlow.getChildren().add(contentText);
        contentFlow.getStyleClass().add("post-content");

        // Edit mode: TextArea (initially hidden)
        TextArea contentEditor = new TextArea(post.getContent_post());
        contentEditor.getStyleClass().add("post-content-editor");
        contentEditor.setWrapText(true);
        contentEditor.setPrefRowCount(3);
        contentEditor.setVisible(false);
        contentEditor.setManaged(false);

        // Edit action buttons (initially hidden)
        HBox editActions = new HBox(8);
        editActions.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
        editActions.setVisible(false);
        editActions.setManaged(false);

        Button saveBtn = new Button("✓ Save");
        saveBtn.getStyleClass().addAll("action-button", "save-btn");
        Button cancelBtn = new Button("✕ Cancel");
        cancelBtn.getStyleClass().addAll("action-button", "cancel-btn");
        editActions.getChildren().addAll(cancelBtn, saveBtn);

        contentContainer.getChildren().addAll(contentFlow, contentEditor, editActions);

        // Media support - check for file path or BLOB
        VBox mediaBox = new VBox();
        String mediaUrl = post.getMedia_url_post();
        String mediaType = post.getMedia_type_post();

        System.out.println("Post ID: " + post.getId_post() + " - URL: " + mediaUrl + " - Type: " + mediaType);

        if (mediaUrl != null && !mediaUrl.isEmpty()) {
            // Load from URL (or local file path if legacy)
            if (mediaType != null && mediaType.toLowerCase().contains("image")) {
                ImageView imageView = createImageViewFromPath(mediaUrl);
                if (imageView != null) {
                    imageView.getStyleClass().add("post-image-view");
                    imageView.setPreserveRatio(true);
                    imageView.setFitWidth(isGridView ? 360 : 600);
                    imageView.setFitHeight(350);
                    mediaBox.getChildren().add(imageView);
                    mediaBox.setPadding(new Insets(0, 20, 14, 20));
                }
            } else if (mediaType != null && mediaType.toLowerCase().contains("video")) {
                // Modern video player
                VBox videoContainer = createVideoPlayer(mediaUrl);
                if (videoContainer != null) {
                    mediaBox.getChildren().add(videoContainer);
                    mediaBox.setPadding(new Insets(0, 20, 14, 20));
                }
            } else if (mediaType != null && mediaType.toLowerCase().contains("audio")) {
                // Modern audio player
                VBox audioContainer = createAudioPlayer(mediaUrl);
                if (audioContainer != null) {
                    mediaBox.getChildren().add(audioContainer);
                    mediaBox.setPadding(new Insets(0, 20, 14, 20));
                }
            }
        }

        // Actions and stats
        HBox actionsBox = new HBox(10);
        actionsBox.getStyleClass().add("post-actions");
        actionsBox.setPadding(new Insets(14, 20, 16, 20));
        actionsBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        // Like button with count — modern pill style
        // Check if current user has already liked this post
        boolean isLiked = false;
        try {
            isLiked = postLikeService.hasUserLikedPost(currentUserId, post.getId_post());
        } catch (SQLException e) {
            System.err.println("Error checking like status: " + e.getMessage());
        }

        Button likeBtn = new Button((isLiked ? "❤️  Liked  " : "❤  J'aime  ") + post.getLike_count_post());
        likeBtn.getStyleClass().addAll("action-button", "like-action-btn");
        if (isLiked) {
            likeBtn.getStyleClass().add("liked");
        }
        likeBtn.setOnAction(e -> likePost(post, likeBtn));

        // Comments section (expandable, initially hidden)
        VBox commentsSection = new VBox(0);
        commentsSection.getStyleClass().add("comments-section");
        commentsSection.setVisible(false);
        commentsSection.setManaged(false);

        // Comment button — modern pill style
        int commentCount = getCommentCount(post.getId_post());
        Button commentBtn = new Button("💬  Commenter  " + (commentCount > 0 ? commentCount : "0"));
        commentBtn.getStyleClass().addAll("action-button", "comment-action-btn");
        commentBtn.setOnAction(e -> toggleCommentsSection(commentsSection, post, commentBtn));

        // Translate button — modern pill style
        Button translateBtn = new Button("🌐  Translate");
        translateBtn.getStyleClass().addAll("action-button", "translate-action-btn");
        translateBtn.setOnAction(e -> handleTranslatePost(post, contentText, translateBtn));

        // Summarize button — only show for long content (>200 characters)
        Button summarizeBtn = null;
        String originalContent = post.getContent_post();
        if (originalContent != null && originalContent.length() > 200) {
            summarizeBtn = new Button("✨  Summarize");
            summarizeBtn.getStyleClass().addAll("action-button", "summarize-action-btn");
            final Button finalSummarizeBtn = summarizeBtn;
            final javafx.scene.text.Text finalContentText = contentText;
            summarizeBtn.setOnAction(e -> handleSummarizePost(post, finalContentText, finalSummarizeBtn));
        }

        // Spacer to push owner buttons right
        Region actionSpacer = new Region();
        HBox.setHgrow(actionSpacer, Priority.ALWAYS);

        // Add action buttons to the actions box
        if (summarizeBtn != null) {
            actionsBox.getChildren().addAll(likeBtn, commentBtn, translateBtn, summarizeBtn, actionSpacer);
        } else {
            actionsBox.getChildren().addAll(likeBtn, commentBtn, translateBtn, actionSpacer);
        }



        // Owner actions — inline in the actions bar
        boolean isOwner = post.getId_author_post() == currentUserId;
        if (isOwner) {
            // Modern icon edit button
            Button editBtn = new Button("✎");
            editBtn.getStyleClass().addAll("icon-action-btn", "edit-icon-btn");
            editBtn.setTooltip(new Tooltip("Modifier ce post"));
            editBtn.setOnAction(e -> {
                // Switch to edit mode
                contentFlow.setVisible(false);
                contentFlow.setManaged(false);
                contentEditor.setVisible(true);
                contentEditor.setManaged(true);
                editActions.setVisible(true);
                editActions.setManaged(true);
                // Focus the editor
                contentEditor.requestFocus();
                contentEditor.positionCaret(contentEditor.getText().length());
            });

            // Modern icon delete button
            Button deleteBtn = new Button("🗑");
            deleteBtn.getStyleClass().addAll("icon-action-btn", "delete-icon-btn");
            deleteBtn.setTooltip(new Tooltip("Supprimer ce post"));
            deleteBtn.setOnAction(e -> handleDeletePost(post));

            ownerActionsBox.getChildren().addAll(editBtn, deleteBtn);

            // Edit action handlers with validation
            saveBtn.setOnAction(e -> {
                String newContent = contentEditor.getText().trim();
                if (newContent.isEmpty()) {
                    // Show inline error
                    Label errorLabel = new Label("⚠ Content cannot be empty");
                    errorLabel.getStyleClass().add("inline-error-label");
                    errorLabel.setStyle("-fx-text-fill: #EF4444; -fx-font-size: 11px;");
                    if (!editActions.getChildren().contains(errorLabel)) {
                        editActions.getChildren().add(0, errorLabel);
                    }
                    return;
                }
                // Remove error if exists
                editActions.getChildren().removeIf(node -> node instanceof Label && ((Label)node).getText().contains("empty"));
                handleUpdatePost(post, newContent);
                // Switch back to view mode
                contentText.setText(newContent);
                contentFlow.setVisible(true);
                contentFlow.setManaged(true);
                contentEditor.setVisible(false);
                contentEditor.setManaged(false);
                editActions.setVisible(false);
                editActions.setManaged(false);
            });

            cancelBtn.setOnAction(e -> {
                contentEditor.setText(post.getContent_post());
                // Remove any error labels
                editActions.getChildren().removeIf(node -> node instanceof Label && ((Label)node).getText().contains("empty"));
                contentFlow.setVisible(true);
                contentFlow.setManaged(true);
                contentEditor.setVisible(false);
                contentEditor.setManaged(false);
                editActions.setVisible(false);
                editActions.setManaged(false);
            });
        }

        // Build card
        card.getChildren().addAll(headerBox, contentContainer);
        if (!mediaBox.getChildren().isEmpty()) {
            card.getChildren().add(mediaBox);
        }
        card.getChildren().add(actionsBox);
        card.getChildren().add(commentsSection);

        return card;
    }

    private void likePost(Post post, Button likeBtn) {
        try {
            // Toggle like/unlike using PostLikeService
            boolean isNowLiked = postLikeService.toggleLike(currentUserId, post.getId_post());

            // Update the like count in the posts table
            postLikeService.updatePostLikeCount(post.getId_post());

            // Update the post object with the new count
            int newCount = postLikeService.getLikeCount(post.getId_post());
            post.setLike_count_post(newCount);

            // Update button appearance
            if (isNowLiked) {
                likeBtn.setText("❤️  Liked  " + newCount);
                likeBtn.getStyleClass().add("liked");

                // Update gamification for the post author receiving a like (+1 point)
                try {
                    gamificationService.handleUserAction(post.getId_author_post(), GamificationService.ActionType.RECEIVE_LIKE);
                } catch (SQLException gamifEx) {
                    System.err.println("Error updating gamification: " + gamifEx.getMessage());
                }

                // Update recommendation scores
                try {
                    recommendationService.handleUserAction(currentUserId, post.getCategory_post(), RecommendationService.ActionType.LIKE_POST);
                } catch (SQLException recEx) {
                    System.err.println("Error updating recommendations: " + recEx.getMessage());
                }
            } else {
                likeBtn.setText("❤  J'aime  " + newCount);
                likeBtn.getStyleClass().remove("liked");
            }

        } catch (SQLException e) {
            showError("Error toggling like: " + e.getMessage());
        }
    }

    private void handleTranslatePost(Post post, javafx.scene.text.Text contentText, Button translateBtn) {
        String originalContent = post.getContent_post();
        String currentText = contentText.getText();

        // Check if currently showing translated content
        if (translateBtn.getUserData() != null && translateBtn.getUserData().equals("translated")) {
            // Revert to original
            contentText.setText(originalContent);
            translateBtn.setText("🌐  Translate");
            translateBtn.setUserData("original");
            return;
        }

        // Detect language and translate
        String detectedLang = TranslationUtil.detectLanguage(originalContent);

        // If already English, show a message
        if ("en".equals(detectedLang)) {
            showSuccess("This post is already in English");
            return;
        }

        // Show loading state
        String originalBtnText = translateBtn.getText();
        translateBtn.setText("⏳  Translating...");
        translateBtn.setDisable(true);

        // Perform translation in background thread
        new Thread(() -> {
            try {
                String translatedText = TranslationUtil.translateToEnglish(originalContent);

                // Update UI on JavaFX thread
                Platform.runLater(() -> {
                    contentText.setText(translatedText);
                    translateBtn.setText("🔄  Original");
                    translateBtn.setUserData("translated");
                    translateBtn.setDisable(false);

                    // Show success message with language info
                    String langName = TranslationUtil.getLanguageName(detectedLang);
                    showSuccess("Translated from " + langName + " to English");
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    translateBtn.setText(originalBtnText);
                    translateBtn.setDisable(false);
                    showError("Translation failed: " + e.getMessage());
                });
            }
        }).start();
    }

    private void handleSummarizePost(Post post, javafx.scene.text.Text contentText, Button summarizeBtn) {
        String originalContent = post.getContent_post();

        // Check if currently showing summary
        if (summarizeBtn.getUserData() != null && summarizeBtn.getUserData().equals("summarized")) {
            // Revert to original
            contentText.setText(originalContent);
            summarizeBtn.setText("✨  Summarize");
            summarizeBtn.setUserData("original");
            return;
        }

        // Show loading state
        String originalBtnText = summarizeBtn.getText();
        summarizeBtn.setText("⏳  Summarizing...");
        summarizeBtn.setDisable(true);

        // Perform summarization in background thread
        new Thread(() -> {
            try {
                String summary = CohereSummarizer.summarize(originalContent);

                // Update UI on JavaFX thread
                Platform.runLater(() -> {
                    contentText.setText(summary);
                    summarizeBtn.setText("🔄  Original");
                    summarizeBtn.setUserData("summarized");
                    summarizeBtn.setDisable(false);
                    showSuccess("Post summarized successfully");
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    summarizeBtn.setText(originalBtnText);
                    summarizeBtn.setDisable(false);
                    showError("Summarization failed: " + e.getMessage());
                });
            }
        }).start();
    }


    private void handleDeletePost(Post post) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete Post");
        alert.setHeaderText("Are you sure you want to delete this post?");
        alert.setContentText("This action cannot be undone.");

        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    postService.supprimer(post.getId_post());
                    displayPosts();
                } catch (SQLException e) {
                    showError("Error deleting post: " + e.getMessage());
                }
            }
        });
    }

    private void handleUpdatePost(Post post, String newContent) {
        try {
            post.setContent_post(newContent);
            String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
            post.setUpdated_at_post(timestamp);
            postService.updatePost(post.getId_post(), post);
        } catch (SQLException e) {
            showError("Error updating post: " + e.getMessage());
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
            
            String mediaUrl = "";
            if (attachedFilePath != null && !attachedFilePath.isEmpty()) {
                System.out.println("Uploading media to Cloudinary...");
                String uploadedUrl = org.example.utils.CloudinaryUtil.uploadMedia(attachedFilePath);
                if (uploadedUrl != null) {
                    mediaUrl = uploadedUrl;
                } else {
                    showError("Erreur lors de l'upload du fichier vers Cloudinary.");
                    return;
                }
            }

            Post post = new Post(
                    content,
                    mediaUrl,
                    mediaType,
                    "public",
                    timestamp,
                    timestamp,
                    0,
                    category,
                    currentUserId
            );

            postService.ajouter(post);
            
            // Update gamification for creating a post (+5 points)
            try {
                gamificationService.handleUserAction(currentUserId, GamificationService.ActionType.CREATE_POST);
            } catch (SQLException gamifEx) {
                System.err.println("Error updating gamification: " + gamifEx.getMessage());
                // Don't fail the post creation if gamification fails
            }
            
            // Update recommendation scores for creating a post in this category
            try {
                recommendationService.handleUserAction(currentUserId, category, RecommendationService.ActionType.CREATE_POST);
            } catch (SQLException recEx) {
                System.err.println("Error updating recommendations: " + recEx.getMessage());
                // Don't fail the post creation if recommendation fails
            }
            
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
        
        // Add filters - All files first so it's selected by default
        FileChooser.ExtensionFilter allFiles = new FileChooser.ExtensionFilter("Tous les fichiers", "*.*");
        FileChooser.ExtensionFilter images = new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.webp", "*.bmp");
        FileChooser.ExtensionFilter videos = new FileChooser.ExtensionFilter("Vidéos", "*.mp4", "*.avi", "*.mov", "*.mkv", "*.flv", "*.wmv", "*.webm", "*.m4v");
        FileChooser.ExtensionFilter audio = new FileChooser.ExtensionFilter("Audio", "*.mp3", "*.wav", "*.flac", "*.aac", "*.ogg", "*.m4a", "*.wma");
        
        fileChooser.getExtensionFilters().addAll(allFiles, images, videos, audio);
        fileChooser.setSelectedExtensionFilter(allFiles); // Default to all files

        Stage stage = new Stage();
        File file = fileChooser.showOpenDialog(stage);
        if (file != null) {
            attachedFilePath = file.getAbsolutePath();
            attachmentLabel.setText("✓ " + file.getName());
        }
    }

    private void handleEmojiPicker() {
        if (emojiPopup != null && emojiPopup.isShowing()) {
            emojiPopup.hide();
            return;
        }

        if (emojiPopup == null) {
            emojiPopup = new javafx.stage.Popup();
            emojiPopup.setAutoHide(true);
            
            TabPane tabPane = new TabPane();
            tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
            tabPane.getStyleClass().add("emoji-tab-pane");

            Map<String, String[]> categories = EmojiUtil.getEmojisPerCategory();
            for (String category : categories.keySet()) {
                Tab tab = new Tab();
                tab.setText(category);

                FlowPane flowPane = new FlowPane();
                flowPane.setPrefWrapLength(380);
                flowPane.setHgap(8);
                flowPane.setVgap(8);
                flowPane.setPadding(new Insets(12));
                flowPane.getStyleClass().add("emoji-flow-pane");

                for (String emoji : categories.get(category)) {
                    javafx.scene.text.Text emojiText = new javafx.scene.text.Text(emoji);
                    emojiText.setStyle("-fx-font-family: 'Segoe UI Emoji'; -fx-font-size: 24px;"); // removed -fx-fill completely
                    Button emojiBtn = new Button("", emojiText);
                    emojiBtn.getStyleClass().add("emoji-button");
                    emojiBtn.setOnAction(e -> {
                        if (postContentArea != null) {
                            postContentArea.appendText(emoji);
                            postContentArea.requestFocus(); // Bring focus back to text area but keep emoji picker open
                            postContentArea.positionCaret(postContentArea.getText().length());
                        }
                    });
                    flowPane.getChildren().add(emojiBtn);
                }

                ScrollPane scrollPane = new ScrollPane(flowPane);
                scrollPane.setFitToWidth(true);
                scrollPane.setPrefHeight(250);
                scrollPane.getStyleClass().add("emoji-scroll-pane");
                tab.setContent(scrollPane);
                tabPane.getTabs().add(tab);
            }

            VBox root = new VBox(tabPane);
            root.getStyleClass().add("emoji-popup-root");
            root.setPrefWidth(420);
            root.setPrefHeight(320);
            VBox.setVgrow(tabPane, Priority.ALWAYS);
            
            // Add stylesheet to the Popup's scene (we have to do this via the popup's scene root or scene once shown, 
            // but Popup doesn't have a Scene accessible the same way. We apply it to the root node.)
            root.getStylesheets().add(getClass().getResource("/css/forum.css").toExternalForm());

            emojiPopup.getContent().add(root);
        }

        // Calculate position exactly under the emoji button
        javafx.geometry.Point2D point = emojiButton.localToScreen(0.0, emojiButton.getHeight());
        if (point != null) {
            emojiPopup.show(emojiButton.getScene().getWindow(), point.getX(), point.getY() + 5);
        } else {
            emojiPopup.show(emojiButton.getScene().getWindow());
        }
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
            headerBox.setPadding(new Insets(10, 16, 6, 16));

            Label commentsTitle = new Label("💬 Commentaires (" + comments.size() + ")");
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
            inputSection.setPadding(new Insets(10, 16, 12, 16));

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
            commentsList.setPadding(new Insets(6, 16, 10, 16));

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

        // Comment content container (for inline editing)
        VBox contentContainer = new VBox(4);

        // View mode: Label
        Label contentLabel = new Label(comment.getContent_comment());
        contentLabel.getStyleClass().add("comment-content");
        contentLabel.setWrapText(true);

        // Edit mode: TextArea (initially hidden)
        TextArea contentEditor = new TextArea(comment.getContent_comment());
        contentEditor.getStyleClass().add("comment-content-editor");
        contentEditor.setWrapText(true);
        contentEditor.setPrefRowCount(2);
        contentEditor.setVisible(false);
        contentEditor.setManaged(false);

        // Edit action buttons (initially hidden)
        HBox editActions = new HBox(8);
        editActions.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
        editActions.setVisible(false);
        editActions.setManaged(false);

        Button saveBtn = new Button("✓ Enregistrer");
        saveBtn.getStyleClass().add("save-btn");
        Button cancelBtn = new Button("✕ Annuler");
        cancelBtn.getStyleClass().add("cancel-btn");
        editActions.getChildren().addAll(cancelBtn, saveBtn);

        contentContainer.getChildren().addAll(contentLabel, contentEditor, editActions);

        // Comment actions — modern pill style
        HBox actionsBox = new HBox(8);
        actionsBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        actionsBox.setPadding(new Insets(4, 0, 0, 0));

        Button replyBtn = new Button("↩  Répondre");
        replyBtn.getStyleClass().add("comment-reply-btn");
        replyBtn.setOnAction(e -> showReplyInput(comment, post, commentsSection, commentBtn));

        actionsBox.getChildren().add(replyBtn);

        // Owner actions (edit/delete) - only for comment author
        boolean isCommentOwner = comment.getId_author_comment() == currentUserId;
        if (isCommentOwner) {
            Region spacer2 = new Region();
            HBox.setHgrow(spacer2, Priority.ALWAYS);

            Button editBtn = new Button("✎  Modifier");
            editBtn.getStyleClass().addAll("owner-pill-btn", "edit-pill-btn");
            editBtn.setTooltip(new Tooltip("Modifier ce commentaire"));
            editBtn.setOnAction(e -> {
                // Switch to edit mode
                contentLabel.setVisible(false);
                contentLabel.setManaged(false);
                contentEditor.setVisible(true);
                contentEditor.setManaged(true);
                editActions.setVisible(true);
                editActions.setManaged(true);
                // Focus the editor
                contentEditor.requestFocus();
                contentEditor.positionCaret(contentEditor.getText().length());
            });

            Button deleteBtn = new Button("🗑  Supprimer");
            deleteBtn.getStyleClass().addAll("owner-pill-btn", "delete-pill-btn");
            deleteBtn.setTooltip(new Tooltip("Supprimer ce commentaire"));
            deleteBtn.setOnAction(e -> handleDeleteComment(comment, post, commentsSection, commentBtn));

            actionsBox.getChildren().addAll(spacer2, editBtn, deleteBtn);

            // Edit action handlers with validation
            saveBtn.setOnAction(e -> {
                String newContent = contentEditor.getText().trim();
                if (newContent.isEmpty()) {
                    // Show inline error
                    Label errorLabel = new Label("⚠ Comment cannot be empty");
                    errorLabel.setStyle("-fx-text-fill: #EF4444; -fx-font-size: 11px;");
                    if (!editActions.getChildren().contains(errorLabel)) {
                        editActions.getChildren().add(0, errorLabel);
                    }
                    return;
                }
                // Remove error if exists
                editActions.getChildren().removeIf(node -> node instanceof Label && ((Label)node).getText().contains("empty"));
                handleUpdateComment(comment, newContent);
                // Reload comments to show updated content
                loadCommentsIntoSection(commentsSection, post, commentBtn);
            });

            cancelBtn.setOnAction(e -> {
                contentEditor.setText(comment.getContent_comment());
                contentLabel.setVisible(true);
                contentLabel.setManaged(true);
                contentEditor.setVisible(false);
                contentEditor.setManaged(false);
                editActions.setVisible(false);
                editActions.setManaged(false);
            });
        }

        commentBox.getChildren().addAll(headerBox, contentContainer, actionsBox);

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
        // Remove any existing reply input box to prevent duplicates
        commentsSection.getChildren().removeIf(node ->
                node instanceof VBox && ((VBox) node).getStyleClass().contains("reply-input-box"));

        // Create new reply input below the comment
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
            
            // Update gamification for adding a comment (+2 points)
            try {
                gamificationService.handleUserAction(currentUserId, GamificationService.ActionType.ADD_COMMENT);
            } catch (SQLException gamifEx) {
                System.err.println("Error updating gamification: " + gamifEx.getMessage());
                // Don't fail the comment creation if gamification fails
            }
            
            // Update recommendation scores for commenting on a post in this category
            try {
                recommendationService.handleUserAction(currentUserId, post.getCategory_post(), RecommendationService.ActionType.ADD_COMMENT);
            } catch (SQLException recEx) {
                System.err.println("Error updating recommendations: " + recEx.getMessage());
                // Don't fail the comment creation if recommendation fails
            }
            
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
            
            // Update gamification for adding a comment/reply (+2 points)
            try {
                gamificationService.handleUserAction(currentUserId, GamificationService.ActionType.ADD_COMMENT);
            } catch (SQLException gamifEx) {
                System.err.println("Error updating gamification: " + gamifEx.getMessage());
                // Don't fail the reply creation if gamification fails
            }
            
            // Update recommendation scores for replying to a post in this category
            try {
                recommendationService.handleUserAction(currentUserId, post.getCategory_post(), RecommendationService.ActionType.ADD_COMMENT);
            } catch (SQLException recEx) {
                System.err.println("Error updating recommendations: " + recEx.getMessage());
                // Don't fail the reply creation if recommendation fails
            }
            
            loadCommentsIntoSection(commentsSection, post, commentBtn);
        } catch (SQLException e) {
            showError("Erreur lors de la réponse: " + e.getMessage());
        }
    }

    private void handleDeleteComment(ForumComment comment, Post post, VBox commentsSection, Button commentBtn) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete Comment");
        alert.setHeaderText("Are you sure you want to delete this comment?");
        alert.setContentText("This action cannot be undone.");

        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    commentService.supprimer(comment.getId_comment());
                    loadCommentsIntoSection(commentsSection, post, commentBtn);
                } catch (SQLException e) {
                    showError("Error deleting comment: " + e.getMessage());
                }
            }
        });
    }

    private void handleUpdateComment(ForumComment comment, String newContent) {
        try {
            commentService.updateComment(comment.getId_comment(), newContent);
        } catch (SQLException e) {
            showError("Error updating comment: " + e.getMessage());
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
        if (authorId == currentUserId && currentUsername != null && !currentUsername.trim().isEmpty()) {
            String[] parts = currentUsername.trim().split("\\s+");
            if (parts.length >= 2) {
                return (parts[0].charAt(0) + "" + parts[1].charAt(0)).toUpperCase();
            } else {
                return currentUsername.substring(0, Math.min(2, currentUsername.length())).toUpperCase();
            }
        }
        // Use author ID to deterministically pick a name
        int index = Math.abs(authorId) % RANDOM_NAMES.length;
        String firstName = RANDOM_NAMES[index][0];
        String lastName = RANDOM_NAMES[index][1];
        return (firstName.charAt(0) + "" + lastName.charAt(0)).toUpperCase();
    }

    private String getFullNameForAuthor(int authorId) {
        if (authorId == currentUserId && currentUsername != null && !currentUsername.trim().isEmpty()) {
            return currentUsername;
        }
        int index = Math.abs(authorId) % RANDOM_NAMES.length;
        return RANDOM_NAMES[index][0] + " " + RANDOM_NAMES[index][1];
    }

    /**
     * Creates a styled badge label for gamification badges.
     * Applies different styles based on the badge type.
     */
    private Label createBadgeLabel(String badge) {
        Label badgeLabel = new Label(badge);
        badgeLabel.getStyleClass().add("user-badge");
        
        // Add specific style class based on badge type
        String badgeLower = badge.toLowerCase();
        if (badgeLower.contains("first post")) {
            badgeLabel.getStyleClass().add("badge-first-post");
        } else if (badgeLower.contains("rising star")) {
            badgeLabel.getStyleClass().add("badge-rising-star");
        } else if (badgeLower.contains("content creator")) {
            badgeLabel.getStyleClass().add("badge-content-creator");
        } else if (badgeLower.contains("chatty")) {
            badgeLabel.getStyleClass().add("badge-chatty");
        } else if (badgeLower.contains("conversation starter")) {
            badgeLabel.getStyleClass().add("badge-conversation-starter");
        } else if (badgeLower.contains("liked")) {
            badgeLabel.getStyleClass().add("badge-liked");
        } else if (badgeLower.contains("beloved")) {
            badgeLabel.getStyleClass().add("badge-beloved");
        } else if (badgeLower.contains("superstar")) {
            badgeLabel.getStyleClass().add("badge-superstar");
        } else {
            badgeLabel.getStyleClass().add("badge-default");
        }
        
        return badgeLabel;
    }

    private VBox createVideoPlayer(String mediaUrl) {
        try {
            // Main container with dark background
            VBox videoContainer = new VBox(0);
            videoContainer.getStyleClass().add("post-video-container");
            videoContainer.setAlignment(javafx.geometry.Pos.CENTER);
            
            // Responsive sizing based on view mode
            double width = isGridView ? 340 : 560;
            double height = 280;
            
            videoContainer.setPrefWidth(width);
            videoContainer.setMaxWidth(width);
            videoContainer.setPrefHeight(height + 60); // Height + controls
            videoContainer.setMaxHeight(height + 60);

            // Create media components
            Media media = new Media(mediaUrl);
            MediaPlayer mediaPlayer = new MediaPlayer(media);
            MediaView mediaView = new MediaView(mediaPlayer);

            // Set media view size
            mediaView.setFitWidth(width);
            mediaView.setFitHeight(height);
            mediaView.setPreserveRatio(true);
            mediaView.getStyleClass().add("post-video-view");
            mediaView.setSmooth(true);

            // Loading placeholder
            Label loadingLabel = new Label("Chargement...");
            loadingLabel.setStyle("-fx-text-fill: #94A3B8; -fx-font-size: 14px;");
            
            // Error handling
            mediaPlayer.setOnError(() -> {
                loadingLabel.setText("Erreur de chargement");
                loadingLabel.setStyle("-fx-text-fill: #EF4444; -fx-font-size: 14px;");
            });
            
            media.setOnError(() -> {
                loadingLabel.setText("Vidéo invalide");
                loadingLabel.setStyle("-fx-text-fill: #EF4444; -fx-font-size: 14px;");
            });

            // StackPane to center video
            javafx.scene.layout.StackPane videoPane = new javafx.scene.layout.StackPane();
            videoPane.setPrefWidth(width);
            videoPane.setPrefHeight(height);
            videoPane.setStyle("-fx-background-color: #0F172A; -fx-background-radius: 16 16 0 0;");
            videoPane.getChildren().addAll(mediaView, loadingLabel);

            // Remove loading text when ready
            mediaPlayer.setOnReady(() -> {
                loadingLabel.setVisible(false);
            });

            // Modern controls container
            HBox controlsBox = new HBox(10);
            controlsBox.getStyleClass().add("video-controls");
            controlsBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            controlsBox.setPadding(new Insets(12, 16, 12, 16));
            controlsBox.setPrefWidth(width);

            // Play/Pause button (circular)
            Button playBtn = new Button("▶");
            playBtn.getStyleClass().addAll("media-control-btn", "play-btn");
            playBtn.setStyle("-fx-background-color: #3B82F6; -fx-text-fill: white; -fx-font-size: 16px; " +
                           "-fx-min-width: 40px; -fx-min-height: 40px; -fx-background-radius: 20px;");
            playBtn.setOnAction(e -> {
                if (mediaPlayer.getStatus() == MediaPlayer.Status.PLAYING) {
                    mediaPlayer.pause();
                    playBtn.setText("▶");
                } else {
                    mediaPlayer.play();
                    playBtn.setText("⏸");
                }
            });

            // Time label
            Label timeLabel = new Label("00:00 / 00:00");
            timeLabel.setStyle("-fx-text-fill: #CBD5E1; -fx-font-size: 12px; -fx-font-family: 'Segoe UI', sans-serif;");
            timeLabel.setPrefWidth(90);

            // Progress slider
            Slider progressSlider = new Slider(0, 100, 0);
            progressSlider.setStyle("-fx-pref-height: 8px;");
            HBox.setHgrow(progressSlider, Priority.ALWAYS);

            // Update progress
            mediaPlayer.currentTimeProperty().addListener((obs, oldVal, newVal) -> {
                if (mediaPlayer.getTotalDuration() != null && mediaPlayer.getTotalDuration().toSeconds() > 0) {
                    double progress = newVal.toSeconds() / mediaPlayer.getTotalDuration().toSeconds() * 100;
                    progressSlider.setValue(progress);
                    
                    String current = String.format("%02d:%02d", (int)newVal.toMinutes(), (int)newVal.toSeconds() % 60);
                    String total = String.format("%02d:%02d", 
                        (int)mediaPlayer.getTotalDuration().toMinutes(), 
                        (int)mediaPlayer.getTotalDuration().toSeconds() % 60);
                    timeLabel.setText(current + " / " + total);
                }
            });

            // Seek on slider change
            progressSlider.setOnMouseReleased(e -> {
                if (mediaPlayer.getTotalDuration() != null) {
                    double seekTime = progressSlider.getValue() / 100 * mediaPlayer.getTotalDuration().toSeconds();
                    mediaPlayer.seek(javafx.util.Duration.seconds(seekTime));
                }
            });

            // Volume button
            Button volumeBtn = new Button("🔊");
            volumeBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #94A3B8; -fx-font-size: 16px; " +
                              "-fx-min-width: 36px; -fx-min-height: 36px; -fx-cursor: hand;");
            
            controlsBox.getChildren().addAll(playBtn, progressSlider, timeLabel, volumeBtn);

            // Assemble container
            videoContainer.getChildren().addAll(videoPane, controlsBox);

            return videoContainer;
        } catch (Exception e) {
            System.err.println("Error creating video player: " + e.getMessage());
            e.printStackTrace();
            
            // Return error placeholder
            VBox errorBox = new VBox(10);
            errorBox.setAlignment(javafx.geometry.Pos.CENTER);
            errorBox.setStyle("-fx-background-color: #1E293B; -fx-padding: 40; -fx-background-radius: 16;");
            errorBox.setPrefSize(340, 200);
            
            Label errorIcon = new Label("🎬");
            errorIcon.setStyle("-fx-font-size: 48px;");
            
            Label errorLabel = new Label("Vidéo non disponible");
            errorLabel.setStyle("-fx-text-fill: #94A3B8; -fx-font-size: 14px;");
            
            errorBox.getChildren().addAll(errorIcon, errorLabel);
            return errorBox;
        }
    }

    private VBox createAudioPlayer(String mediaUrl) {
        try {
            Media media = new Media(mediaUrl);
            MediaPlayer mediaPlayer = new MediaPlayer(media);

            // Modern audio player container
            VBox audioContainer = new VBox(12);
            audioContainer.getStyleClass().add("post-audio-container");
            audioContainer.setPadding(new Insets(20));
            audioContainer.setAlignment(javafx.geometry.Pos.CENTER);

            // Audio icon/waveform visualization placeholder
            HBox waveformBox = new HBox(4);
            waveformBox.getStyleClass().add("audio-waveform");
            waveformBox.setAlignment(javafx.geometry.Pos.CENTER);
            waveformBox.setPadding(new Insets(10, 0, 10, 0));

            // Create animated bars
            for (int i = 0; i < 20; i++) {
                javafx.scene.layout.Region bar = new javafx.scene.layout.Region();
                bar.getStyleClass().add("audio-bar");
                bar.setPrefWidth(4);
                bar.setPrefHeight(20 + Math.random() * 30);
                waveformBox.getChildren().add(bar);
            }

            // Controls
            HBox controlsBox = new HBox(16);
            controlsBox.getStyleClass().add("audio-controls");
            controlsBox.setAlignment(javafx.geometry.Pos.CENTER);

            Button playBtn = new Button("▶");
            playBtn.getStyleClass().addAll("media-control-btn", "audio-play-btn");
            playBtn.setOnAction(e -> {
                if (mediaPlayer.getStatus() == MediaPlayer.Status.PLAYING) {
                    mediaPlayer.pause();
                    playBtn.setText("▶");
                } else {
                    mediaPlayer.play();
                    playBtn.setText("⏸");
                }
            });

            // Progress slider
            Slider progressSlider = new Slider(0, 100, 0);
            progressSlider.getStyleClass().add("media-progress-slider");
            progressSlider.setPrefWidth(300);

            mediaPlayer.currentTimeProperty().addListener((obs, oldVal, newVal) -> {
                if (mediaPlayer.getTotalDuration() != null && mediaPlayer.getTotalDuration().toSeconds() > 0) {
                    progressSlider.setValue(newVal.toSeconds() / mediaPlayer.getTotalDuration().toSeconds() * 100);
                }
            });

            progressSlider.setOnMouseReleased(e -> {
                if (mediaPlayer.getTotalDuration() != null) {
                    mediaPlayer.seek(javafx.util.Duration.seconds(progressSlider.getValue() / 100 * mediaPlayer.getTotalDuration().toSeconds()));
                }
            });

            Label timeLabel = new Label("00:00 / 00:00");
            timeLabel.getStyleClass().add("media-time-label");

            mediaPlayer.currentTimeProperty().addListener((obs, oldVal, newVal) -> {
                String current = String.format("%02d:%02d", (int)newVal.toMinutes(), (int)newVal.toSeconds() % 60);
                String total = mediaPlayer.getTotalDuration() != null ?
                    String.format("%02d:%02d", (int)mediaPlayer.getTotalDuration().toMinutes(), (int)mediaPlayer.getTotalDuration().toSeconds() % 60) : "00:00";
                timeLabel.setText(current + " / " + total);
            });

            Label audioLabel = new Label("🎵 Audio");
            audioLabel.getStyleClass().add("audio-title-label");

            controlsBox.getChildren().addAll(playBtn, progressSlider, timeLabel);
            audioContainer.getChildren().addAll(audioLabel, waveformBox, controlsBox);

            return audioContainer;
        } catch (Exception e) {
            System.err.println("Error creating audio player: " + e.getMessage());
            return null;
        }
    }

}
