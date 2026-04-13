package org.example.controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import org.example.entities.ForumComment;
import org.example.entities.Post;
import org.example.entities.User;
import org.example.services.ForumCommentService;
import org.example.services.ForumPostService;
import org.example.services.UserService;

import java.io.File;
import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;

/**
 * Back-office forum : statistiques, graphiques, publications et commentaires en cartes (style planning admin).
 * Aucun identifiant technique n'est affiché à l'écran.
 */
public class ForumBackofficeController implements Initializable {

    @FXML private Label totalPostsLabel;
    @FXML private Label totalLikesLabel;
    @FXML private Label categoriesCountLabel;
    @FXML private Label todayPostsLabel;
    @FXML private Label totalCommentsLabel;
    @FXML private Label todayCommentsLabel;
    @FXML private Label topCommentedPostLabel;
    @FXML private Label avgCommentsLabel;
    @FXML private PieChart categoryPieChart;
    @FXML private BarChart<String, Number> activityBarChart;
    @FXML private TextField searchField;
    @FXML private FlowPane postsCardsFlow;
    @FXML private FlowPane commentsCardsFlow;

    @FXML private VBox postEditPanel;
    @FXML private TextArea postEditContent;
    @FXML private Label postEditFileLabel;
    private File selectedEditFile;
    private int editingPostId = -1;

    @FXML private VBox commentEditPanel;
    @FXML private TextArea commentEditContent;
    private int editingCommentId = -1;

    private ForumPostService postService;
    private ForumCommentService commentService;
    private UserService userService;

    private final ObservableList<PostRow> postsData = FXCollections.observableArrayList();
    private final ObservableList<CommentRow> commentsData = FXCollections.observableArrayList();
    private final Map<Integer, String> authorDisplayCache = new HashMap<>();
    private List<Post> allPosts = new ArrayList<>();
    private List<ForumComment> allComments = new ArrayList<>();

    public static class PostRow {
        private final int id;
        private final String fullContent;
        private final String preview;
        private final String category;
        private final String author;
        private final int likes;
        private final String date;

        public PostRow(int id, String fullContent, String category, String author, int likes, String date) {
            this.id = id;
            this.fullContent = fullContent != null ? fullContent : "";
            this.preview = truncate(this.fullContent, 160);
            this.category = category != null ? category : "—";
            this.author = author != null ? author : "Membre";
            this.likes = likes;
            this.date = date != null ? date : "—";
        }

        public int getId() {
            return id;
        }

        public String getPreview() {
            return preview;
        }

        public String getFullContent() {
            return fullContent;
        }

        public String getCategory() {
            return category;
        }

        public String getAuthor() {
            return author;
        }

        public int getLikes() {
            return likes;
        }

        public String getDate() {
            return date;
        }
    }

    public static class CommentRow {
        private final int id;
        private final String fullContent;
        private final String preview;
        private final String linkedPostPreview;
        private final String author;
        private final int likes;
        private final String date;

        public CommentRow(int id, String fullContent, String linkedPostPreview, String author, int likes, String date) {
            this.id = id;
            this.fullContent = fullContent != null ? fullContent : "";
            this.preview = truncate(this.fullContent, 140);
            this.linkedPostPreview = linkedPostPreview != null ? linkedPostPreview : "—";
            this.author = author != null ? author : "Membre";
            this.likes = likes;
            this.date = date != null ? date : "—";
        }

        public int getId() {
            return id;
        }

        public String getPreview() {
            return preview;
        }

        public String getFullContent() {
            return fullContent;
        }

        public String getLinkedPostPreview() {
            return linkedPostPreview;
        }

        public String getAuthor() {
            return author;
        }

        public int getLikes() {
            return likes;
        }

        public String getDate() {
            return date;
        }
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        try {
            postService = new ForumPostService();
            commentService = new ForumCommentService();
            userService = new UserService();
            loadData();
            updateStats();
            updateCharts();
        } catch (SQLException e) {
            showError("Base de données", "Initialisation impossible : " + e.getMessage());
        }
    }

    private static String truncate(String s, int max) {
        if (s == null || s.isEmpty()) {
            return "—";
        }
        String t = s.replace('\n', ' ').trim();
        return t.length() <= max ? t : t.substring(0, max) + "…";
    }

    private void warmAuthorCache() {
        authorDisplayCache.clear();
        Set<Integer> ids = new HashSet<>();
        for (Post p : allPosts) {
            ids.add(p.getId_author_post());
        }
        for (ForumComment c : allComments) {
            ids.add(c.getId_author_comment());
        }
        for (int uid : ids) {
            try {
                User u = userService.getUserById(uid);
                if (u == null) {
                    authorDisplayCache.put(uid, "Membre");
                    continue;
                }
                String p = u.getPrenom() != null ? u.getPrenom().trim() : "";
                String n = u.getNom() != null ? u.getNom().trim() : "";
                String name = (p + " " + n).trim();
                if (name.isEmpty() && u.getEmail() != null && !u.getEmail().isBlank()) {
                    name = u.getEmail().trim();
                }
                authorDisplayCache.put(uid, name.isEmpty() ? "Membre" : name);
            } catch (SQLException e) {
                authorDisplayCache.put(uid, "Membre");
            }
        }
    }

    private String authorLabel(int userId) {
        return authorDisplayCache.getOrDefault(userId, "Membre");
    }

    private Map<Integer, String> buildPostSnippetById() {
        Map<Integer, String> m = new HashMap<>();
        for (Post p : allPosts) {
            m.put(p.getId_post(), truncate(p.getContent_post(), 72));
        }
        return m;
    }

    private void loadData() throws SQLException {
        allPosts = postService.afficher();
        allComments = commentService.afficher();
        warmAuthorCache();
        refreshPostsModel();
        refreshCommentsModel();
        renderPostCards();
        renderCommentCards();
    }

    private void refreshPostsModel() {
        postsData.clear();
        for (Post post : allPosts) {
            postsData.add(new PostRow(
                    post.getId_post(),
                    post.getContent_post(),
                    post.getCategory_post(),
                    authorLabel(post.getId_author_post()),
                    post.getLike_count_post(),
                    post.getCreated_at_post()
            ));
        }
    }

    private void refreshCommentsModel() {
        commentsData.clear();
        Map<Integer, String> postSnippets = buildPostSnippetById();
        List<ForumComment> sorted = new ArrayList<>(allComments);
        sorted.sort(Comparator.comparing(ForumComment::getCreated_at_comment,
                Comparator.nullsLast(Comparator.reverseOrder())));

        int limit = Math.min(50, sorted.size());
        for (int i = 0; i < limit; i++) {
            ForumComment c = sorted.get(i);
            String linked = postSnippets.getOrDefault(c.getPost_id(), "Publication associée");
            String dateStr = c.getCreated_at_comment() != null ? c.getCreated_at_comment().toString() : "";
            commentsData.add(new CommentRow(
                    c.getId_comment(),
                    c.getContent_comment(),
                    linked,
                    authorLabel(c.getId_author_comment()),
                    c.getLike_count(),
                    dateStr
            ));
        }
    }

    private void renderPostCards() {
        postsCardsFlow.getChildren().clear();
        for (PostRow row : postsData) {
            postsCardsFlow.getChildren().add(buildPostCard(row));
        }
    }

    private void renderCommentCards() {
        commentsCardsFlow.getChildren().clear();
        for (CommentRow row : commentsData) {
            commentsCardsFlow.getChildren().add(buildCommentCard(row));
        }
    }

    private VBox buildPostCard(PostRow row) {
        VBox card = new VBox(12);
        card.getStyleClass().add("apd-user-card");
        card.setPadding(new Insets(4, 4, 8, 4));
        card.setMinWidth(300);
        card.setMaxWidth(400);

        Label cat = new Label(row.getCategory());
        cat.getStyleClass().add("apd-user-card-badge");
        cat.setWrapText(true);

        Label excerpt = new Label(row.getPreview());
        excerpt.setWrapText(true);
        excerpt.getStyleClass().add("apd-user-card-name");

        Label meta = new Label("Par " + row.getAuthor() + " · " + row.getLikes() + " j'aime · " + row.getDate());
        meta.setWrapText(true);
        meta.getStyleClass().add("apd-user-card-stats");

        Button editBtn = new Button("Modifier");
        editBtn.getStyleClass().addAll("action-btn", "edit");
        editBtn.setOnAction(e -> handleEdit(row));

        Button deleteBtn = new Button("Supprimer");
        deleteBtn.getStyleClass().addAll("action-btn", "delete");
        deleteBtn.setOnAction(e -> handleDelete(row));

        HBox actions = new HBox(10, editBtn, deleteBtn);
        actions.setAlignment(Pos.CENTER_LEFT);

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        card.getChildren().addAll(cat, excerpt, meta, spacer, actions);
        return card;
    }

    private VBox buildCommentCard(CommentRow row) {
        VBox card = new VBox(12);
        card.getStyleClass().add("apd-user-card");
        card.setPadding(new Insets(4, 4, 8, 4));
        card.setMinWidth(300);
        card.setMaxWidth(400);

        Label pub = new Label("Publication : " + row.getLinkedPostPreview());
        pub.setWrapText(true);
        pub.getStyleClass().add("apd-user-card-badge");

        Label excerpt = new Label(row.getPreview());
        excerpt.setWrapText(true);
        excerpt.getStyleClass().add("apd-user-card-name");

        Label meta = new Label("Par " + row.getAuthor() + " · " + row.getLikes() + " j'aime · " + row.getDate());
        meta.setWrapText(true);
        meta.getStyleClass().add("apd-user-card-stats");

        Button editBtn = new Button("Modifier");
        editBtn.getStyleClass().addAll("action-btn", "edit");
        editBtn.setOnAction(e -> handleEditComment(row));

        Button deleteBtn = new Button("Supprimer");
        deleteBtn.getStyleClass().addAll("action-btn", "delete");
        deleteBtn.setOnAction(e -> handleDeleteComment(row));

        HBox actions = new HBox(10, editBtn, deleteBtn);
        actions.setAlignment(Pos.CENTER_LEFT);

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        card.getChildren().addAll(pub, excerpt, meta, spacer, actions);
        return card;
    }

    private void updateStats() {
        int totalPosts = allPosts.size();
        int totalLikes = allPosts.stream().mapToInt(Post::getLike_count_post).sum();
        long categoriesCount = allPosts.stream().map(Post::getCategory_post).filter(c -> c != null && !c.isBlank()).distinct().count();
        long todayPosts = allPosts.stream()
                .filter(p -> p.getCreated_at_post() != null
                        && p.getCreated_at_post().startsWith(LocalDate.now().toString()))
                .count();

        totalPostsLabel.setText(String.valueOf(totalPosts));
        totalLikesLabel.setText(String.valueOf(totalLikes));
        categoriesCountLabel.setText(String.valueOf(categoriesCount));
        todayPostsLabel.setText(String.valueOf(todayPosts));

        int totalComments = allComments.size();
        long todayComments = allComments.stream()
                .filter(c -> c.getCreated_at_comment() != null
                        && c.getCreated_at_comment().toString().startsWith(LocalDate.now().toString()))
                .count();

        Map<Integer, Long> commentsByPost = new HashMap<>();
        for (ForumComment c : allComments) {
            commentsByPost.merge(c.getPost_id(), 1L, Long::sum);
        }
        int topPostId = commentsByPost.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(0);

        String topLabel = "—";
        if (topPostId > 0) {
            topLabel = allPosts.stream()
                    .filter(p -> p.getId_post() == topPostId)
                    .findFirst()
                    .map(p -> truncate(p.getContent_post(), 56))
                    .orElse("Publication la plus discutée");
        }

        double avgComments = totalPosts > 0 ? (double) totalComments / totalPosts : 0;

        totalCommentsLabel.setText(String.valueOf(totalComments));
        todayCommentsLabel.setText(String.valueOf(todayComments));
        topCommentedPostLabel.setText(topLabel);
        avgCommentsLabel.setText(String.format(Locale.FRENCH, "%.1f", avgComments));
    }

    private void updateCharts() {
        Map<String, Long> categoryCount = new HashMap<>();
        for (Post post : allPosts) {
            String cat = post.getCategory_post() != null ? post.getCategory_post() : "Sans catégorie";
            categoryCount.merge(cat, 1L, Long::sum);
        }

        ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList();
        categoryCount.forEach((cat, count) -> pieData.add(new PieChart.Data(cat, count)));
        categoryPieChart.setData(pieData);

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Posts");

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("d MMM", Locale.FRENCH);
        for (int i = 6; i >= 0; i--) {
            LocalDate date = LocalDate.now().minusDays(i);
            String label = date.format(formatter);
            long count = allPosts.stream()
                    .filter(p -> p.getCreated_at_post() != null
                            && p.getCreated_at_post().startsWith(date.toString()))
                    .count();
            series.getData().add(new XYChart.Data<>(label, count));
        }

        activityBarChart.getData().clear();
        activityBarChart.getData().add(series);
    }

    @FXML
    private void handleRefresh() {
        try {
            loadData();
            updateStats();
            updateCharts();
        } catch (SQLException e) {
            showError("Actualisation", e.getMessage() != null ? e.getMessage() : e.toString());
        }
    }

    @FXML
    private void handleExport() {
        showInfo("Export", "Fonction d'export CSV / Excel à brancher ici.");
    }

    @FXML
    private void handleSearch() {
        String query = searchField.getText() == null ? "" : searchField.getText().toLowerCase(Locale.ROOT).trim();
        postsData.clear();
        if (query.isEmpty()) {
            for (Post post : allPosts) {
                postsData.add(new PostRow(
                        post.getId_post(),
                        post.getContent_post(),
                        post.getCategory_post(),
                        authorLabel(post.getId_author_post()),
                        post.getLike_count_post(),
                        post.getCreated_at_post()
                ));
            }
        } else {
            for (Post post : allPosts) {
                String content = post.getContent_post() != null ? post.getContent_post().toLowerCase(Locale.ROOT) : "";
                String cat = post.getCategory_post() != null ? post.getCategory_post().toLowerCase(Locale.ROOT) : "";
                if (content.contains(query) || cat.contains(query)) {
                    postsData.add(new PostRow(
                            post.getId_post(),
                            post.getContent_post(),
                            post.getCategory_post(),
                            authorLabel(post.getId_author_post()),
                            post.getLike_count_post(),
                            post.getCreated_at_post()
                    ));
                }
            }
        }
        renderPostCards();
    }

    private void handleEdit(PostRow row) {
        editingPostId = row.getId();
        postEditContent.setText(row.getFullContent());
        selectedEditFile = null;
        postEditFileLabel.setText("Aucun fichier (conserver l'existant ou en choisir un nouveau)");

        postEditPanel.setVisible(true);
        postEditPanel.setManaged(true);
        commentEditPanel.setVisible(false);
        commentEditPanel.setManaged(false);
        postEditPanel.requestFocus();
    }

    @FXML
    private void handlePostEditFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choisir un média");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif"),
                new FileChooser.ExtensionFilter("Vidéos", "*.mp4", "*.avi", "*.mov"),
                new FileChooser.ExtensionFilter("Tous les fichiers", "*.*")
        );
        File file = fileChooser.showOpenDialog(postEditPanel.getScene().getWindow());
        if (file != null) {
            selectedEditFile = file;
            postEditFileLabel.setText(file.getName());
        }
    }

    @FXML
    private void handlePostEditSave() {
        if (editingPostId == -1) {
            return;
        }

        String newContent = postEditContent.getText() != null ? postEditContent.getText().trim() : "";
        if (newContent.isEmpty()) {
            showError("Validation", "Le contenu ne peut pas être vide.");
            return;
        }

        try {
            Post post = postService.getPostById(editingPostId);
            if (post != null) {
                post.setContent_post(newContent);

                if (selectedEditFile != null) {
                    post.setMedia_url_post(selectedEditFile.getAbsolutePath());
                    String fileName = selectedEditFile.getName().toLowerCase(Locale.ROOT);
                    if (fileName.endsWith(".mp4") || fileName.endsWith(".avi") || fileName.endsWith(".mov")) {
                        post.setMedia_type_post("video");
                    } else {
                        post.setMedia_type_post("image");
                    }
                }

                postService.updatePost(editingPostId, post);
                handlePostEditCancel();
                handleRefresh();
                showInfo("Enregistré", "La publication a été mise à jour.");
            }
        } catch (SQLException e) {
            showError("Mise à jour", e.getMessage() != null ? e.getMessage() : e.toString());
        }
    }

    @FXML
    private void handlePostEditCancel() {
        editingPostId = -1;
        selectedEditFile = null;
        postEditContent.clear();
        postEditFileLabel.setText("Aucun fichier");
        postEditPanel.setVisible(false);
        postEditPanel.setManaged(false);
    }

    private void handleDelete(PostRow row) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Supprimer la publication");
        alert.setHeaderText(null);
        alert.setContentText("Confirmer la suppression définitive de cette publication et des données associées ?");

        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    postService.supprimer(row.getId());
                    handleRefresh();
                    showInfo("Supprimé", "La publication a été supprimée.");
                } catch (SQLException e) {
                    showError("Suppression", e.getMessage() != null ? e.getMessage() : e.toString());
                }
            }
        });
    }

    private void handleEditComment(CommentRow row) {
        editingCommentId = row.getId();
        commentEditContent.setText(row.getFullContent());

        commentEditPanel.setVisible(true);
        commentEditPanel.setManaged(true);
        postEditPanel.setVisible(false);
        postEditPanel.setManaged(false);
        commentEditPanel.requestFocus();
    }

    @FXML
    private void handleCommentEditSave() {
        if (editingCommentId == -1) {
            return;
        }

        String newContent = commentEditContent.getText() != null ? commentEditContent.getText().trim() : "";
        if (newContent.isEmpty()) {
            showError("Validation", "Le commentaire ne peut pas être vide.");
            return;
        }

        try {
            commentService.updateComment(editingCommentId, newContent);
            handleCommentEditCancel();
            handleRefresh();
            showInfo("Enregistré", "Le commentaire a été mis à jour.");
        } catch (SQLException e) {
            showError("Mise à jour", e.getMessage() != null ? e.getMessage() : e.toString());
        }
    }

    @FXML
    private void handleCommentEditCancel() {
        editingCommentId = -1;
        commentEditContent.clear();
        commentEditPanel.setVisible(false);
        commentEditPanel.setManaged(false);
    }

    private void handleDeleteComment(CommentRow row) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Supprimer le commentaire");
        alert.setHeaderText(null);
        alert.setContentText("Confirmer la suppression définitive de ce commentaire ?");

        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    commentService.supprimer(row.getId());
                    handleRefresh();
                    showInfo("Supprimé", "Le commentaire a été supprimé.");
                } catch (SQLException e) {
                    showError("Suppression", e.getMessage() != null ? e.getMessage() : e.toString());
                }
            }
        });
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
