package org.example.controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Callback;
import java.io.File;
import javafx.stage.FileChooser;
import org.example.entities.Post;
import org.example.entities.ForumComment;
import org.example.services.ForumPostService;
import org.example.services.ForumCommentService;

import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class ForumBackofficeController implements Initializable {

    // FXML Elements
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
    @FXML private TableView<PostRow> postsTable;
    @FXML private TableColumn<PostRow, Integer> idColumn;
    @FXML private TableColumn<PostRow, String> contentColumn;
    @FXML private TableColumn<PostRow, String> categoryColumn;
    @FXML private TableColumn<PostRow, String> authorColumn;
    @FXML private TableColumn<PostRow, Integer> likesColumn;
    @FXML private TableColumn<PostRow, String> dateColumn;
    @FXML private TableColumn<PostRow, Void> actionsColumn;

    // Comments Table
    @FXML private TableView<CommentRow> commentsTable;
    @FXML private TableColumn<CommentRow, Integer> commentIdColumn;
    @FXML private TableColumn<CommentRow, String> commentContentColumn;
    @FXML private TableColumn<CommentRow, Integer> commentPostColumn;
    @FXML private TableColumn<CommentRow, String> commentAuthorColumn;
    @FXML private TableColumn<CommentRow, Integer> commentLikesColumn;
    @FXML private TableColumn<CommentRow, String> commentDateColumn;
    @FXML private TableColumn<CommentRow, Void> commentActionsColumn;

    // Inline Edit Panels - Posts
    @FXML private VBox postEditPanel;
    @FXML private TextArea postEditContent;
    @FXML private Label postEditFileLabel;
    private File selectedEditFile;
    private int editingPostId = -1;

    // Inline Edit Panels - Comments
    @FXML private VBox commentEditPanel;
    @FXML private TextArea commentEditContent;
    private int editingCommentId = -1;

    // Services
    private ForumPostService postService;
    private ForumCommentService commentService;

    // Data
    private final ObservableList<PostRow> postsData = FXCollections.observableArrayList();
    private final ObservableList<CommentRow> commentsData = FXCollections.observableArrayList();
    private List<Post> allPosts = new ArrayList<>();
    private List<ForumComment> allComments = new ArrayList<>();

    // Row wrapper for table
    public static class PostRow {
        private final int id;
        private final String content;
        private final String category;
        private final String author;
        private final int likes;
        private final String date;

        public PostRow(int id, String content, String category, String author, int likes, String date) {
            this.id = id;
            this.content = content.length() > 50 ? content.substring(0, 50) + "..." : content;
            this.category = category;
            this.author = author;
            this.likes = likes;
            this.date = date;
        }

        public int getId() { return id; }
        public String getContent() { return content; }
        public String getCategory() { return category; }
        public String getAuthor() { return author; }
        public int getLikes() { return likes; }
        public String getDate() { return date; }
    }

    // Comment row wrapper for table
    public static class CommentRow {
        private final int id;
        private final String content;
        private final String fullContent;
        private final int postId;
        private final String author;
        private final int likes;
        private final String date;

        public CommentRow(int id, String content, int postId, String author, int likes, String date) {
            this.id = id;
            this.fullContent = content;
            this.content = content.length() > 60 ? content.substring(0, 60) + "..." : content;
            this.postId = postId;
            this.author = author;
            this.likes = likes;
            this.date = date;
        }

        public int getId() { return id; }
        public String getContent() { return content; }
        public String getFullContent() { return fullContent; }
        public int getPostId() { return postId; }
        public String getAuthor() { return author; }
        public int getLikes() { return likes; }
        public String getDate() { return date; }
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        try {
            postService = new ForumPostService();
            commentService = new ForumCommentService();
            setupTableColumns();
            setupActionButtons();
            setupCommentActionButtons();
            loadData();
            updateStats();
            updateCharts();
        } catch (SQLException e) {
            showError("Database Error", "Failed to initialize: " + e.getMessage());
        }
    }

    private void setupTableColumns() {
        // Posts table
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        contentColumn.setCellValueFactory(new PropertyValueFactory<>("content"));
        categoryColumn.setCellValueFactory(new PropertyValueFactory<>("category"));
        authorColumn.setCellValueFactory(new PropertyValueFactory<>("author"));
        likesColumn.setCellValueFactory(new PropertyValueFactory<>("likes"));
        dateColumn.setCellValueFactory(new PropertyValueFactory<>("date"));

        // Comments table
        commentIdColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        commentContentColumn.setCellValueFactory(new PropertyValueFactory<>("content"));
        commentPostColumn.setCellValueFactory(new PropertyValueFactory<>("postId"));
        commentAuthorColumn.setCellValueFactory(new PropertyValueFactory<>("author"));
        commentLikesColumn.setCellValueFactory(new PropertyValueFactory<>("likes"));
        commentDateColumn.setCellValueFactory(new PropertyValueFactory<>("date"));
    }

    private void setupActionButtons() {
        Callback<TableColumn<PostRow, Void>, TableCell<PostRow, Void>> cellFactory = new Callback<>() {
            @Override
            public TableCell<PostRow, Void> call(final TableColumn<PostRow, Void> param) {
                return new TableCell<>() {
                    private final Button editBtn = new Button("✏️ Edit");
                    private final Button deleteBtn = new Button("🗑️ Delete");
                    private final HBox pane = new HBox(8, editBtn, deleteBtn);

                    {
                        editBtn.getStyleClass().addAll("action-btn", "edit");
                        deleteBtn.getStyleClass().addAll("action-btn", "delete");

                        editBtn.setOnAction(event -> {
                            PostRow row = getTableView().getItems().get(getIndex());
                            handleEdit(row);
                        });

                        deleteBtn.setOnAction(event -> {
                            PostRow row = getTableView().getItems().get(getIndex());
                            handleDelete(row);
                        });
                    }

                    @Override
                    protected void updateItem(Void item, boolean empty) {
                        super.updateItem(item, empty);
                        setGraphic(empty ? null : pane);
                    }
                };
            }
        };

        actionsColumn.setCellFactory(cellFactory);
    }

    private void setupCommentActionButtons() {
        Callback<TableColumn<CommentRow, Void>, TableCell<CommentRow, Void>> cellFactory = new Callback<>() {
            @Override
            public TableCell<CommentRow, Void> call(final TableColumn<CommentRow, Void> param) {
                return new TableCell<>() {
                    private final Button editBtn = new Button("✏️ Edit");
                    private final Button deleteBtn = new Button("🗑️ Delete");
                    private final HBox pane = new HBox(8, editBtn, deleteBtn);

                    {
                        editBtn.getStyleClass().addAll("action-btn", "edit");
                        deleteBtn.getStyleClass().addAll("action-btn", "delete");

                        editBtn.setOnAction(event -> {
                            CommentRow row = getTableView().getItems().get(getIndex());
                            handleEditComment(row);
                        });

                        deleteBtn.setOnAction(event -> {
                            CommentRow row = getTableView().getItems().get(getIndex());
                            handleDeleteComment(row);
                        });
                    }

                    @Override
                    protected void updateItem(Void item, boolean empty) {
                        super.updateItem(item, empty);
                        setGraphic(empty ? null : pane);
                    }
                };
            }
        };
        commentActionsColumn.setCellFactory(cellFactory);
    }

    private void loadData() throws SQLException {
        allPosts = postService.afficher();
        allComments = commentService.afficher();
        refreshTable();
        refreshCommentsTable();
    }

    private void refreshTable() {
        postsData.clear();
        for (Post post : allPosts) {
            postsData.add(new PostRow(
                post.getId_post(),
                post.getContent_post(),
                post.getCategory_post(),
                "User " + post.getId_author_post(),
                post.getLike_count_post(),
                post.getCreated_at_post()
            ));
        }
        postsTable.setItems(postsData);
    }

    private void refreshCommentsTable() {
        commentsData.clear();
        // Sort by date descending (newest first)
        List<ForumComment> sortedComments = new ArrayList<>(allComments);
        sortedComments.sort((a, b) -> {
            if (a.getCreated_at_comment() == null) return 1;
            if (b.getCreated_at_comment() == null) return -1;
            return b.getCreated_at_comment().compareTo(a.getCreated_at_comment());
        });

        // Show only recent 50 comments
        int limit = Math.min(50, sortedComments.size());
        for (int i = 0; i < limit; i++) {
            ForumComment c = sortedComments.get(i);
            commentsData.add(new CommentRow(
                c.getId_comment(),
                c.getContent_comment(),
                c.getPost_id(),
                "User " + c.getId_author_comment(),
                c.getLike_count(),
                c.getCreated_at_comment() != null ? c.getCreated_at_comment().toString() : ""
            ));
        }
        commentsTable.setItems(commentsData);
    }

    private void updateStats() {
        // Post stats
        int totalPosts = allPosts.size();
        int totalLikes = allPosts.stream().mapToInt(Post::getLike_count_post).sum();
        long categoriesCount = allPosts.stream().map(Post::getCategory_post).distinct().count();
        long todayPosts = allPosts.stream()
            .filter(p -> p.getCreated_at_post() != null &&
                   p.getCreated_at_post().startsWith(LocalDate.now().toString()))
            .count();

        totalPostsLabel.setText(String.valueOf(totalPosts));
        totalLikesLabel.setText(String.valueOf(totalLikes));
        categoriesCountLabel.setText(String.valueOf(categoriesCount));
        todayPostsLabel.setText(String.valueOf(todayPosts));

        // Comment stats
        int totalComments = allComments.size();
        long todayComments = allComments.stream()
            .filter(c -> c.getCreated_at_comment() != null &&
                   c.getCreated_at_comment().toString().startsWith(LocalDate.now().toString()))
            .count();

        // Top commented post
        Map<Integer, Long> commentsByPost = new HashMap<>();
        for (ForumComment c : allComments) {
            commentsByPost.merge(c.getPost_id(), 1L, Long::sum);
        }
        int topPostId = commentsByPost.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey).orElse(0);

        // Average comments per post
        double avgComments = totalPosts > 0 ? (double) totalComments / totalPosts : 0;

        totalCommentsLabel.setText(String.valueOf(totalComments));
        todayCommentsLabel.setText(String.valueOf(todayComments));
        topCommentedPostLabel.setText(topPostId > 0 ? "Post #" + topPostId : "-");
        avgCommentsLabel.setText(String.format("%.1f", avgComments));
    }

    private void updateCharts() {
        // Pie Chart - Posts by Category
        Map<String, Long> categoryCount = new HashMap<>();
        for (Post post : allPosts) {
            String cat = post.getCategory_post() != null ? post.getCategory_post() : "Uncategorized";
            categoryCount.merge(cat, 1L, Long::sum);
        }

        ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList();
        categoryCount.forEach((cat, count) -> pieData.add(new PieChart.Data(cat, count)));
        categoryPieChart.setData(pieData);

        // Bar Chart - Activity over last 7 days
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Posts");

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd");
        for (int i = 6; i >= 0; i--) {
            LocalDate date = LocalDate.now().minusDays(i);
            String label = date.format(formatter);
            long count = allPosts.stream()
                .filter(p -> p.getCreated_at_post() != null && 
                       p.getCreated_at_post().startsWith(date.toString()))
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
            showError("Refresh Error", "Failed to refresh data: " + e.getMessage());
        }
    }

    @FXML
    private void handleExport() {
        showInfo("Export", "Export functionality would save data to CSV/Excel");
    }

    @FXML
    private void handleSearch() {
        String query = searchField.getText().toLowerCase().trim();
        if (query.isEmpty()) {
            refreshTable();
            return;
        }

        postsData.clear();
        for (Post post : allPosts) {
            if (post.getContent_post().toLowerCase().contains(query) ||
                (post.getCategory_post() != null && post.getCategory_post().toLowerCase().contains(query))) {
                postsData.add(new PostRow(
                    post.getId_post(),
                    post.getContent_post(),
                    post.getCategory_post(),
                    "User " + post.getId_author_post(),
                    post.getLike_count_post(),
                    post.getCreated_at_post()
                ));
            }
        }
        postsTable.setItems(postsData);
    }

    private void handleEdit(PostRow row) {
        // Show inline edit panel instead of dialog
        editingPostId = row.getId();
        postEditContent.setText(row.getContent());
        selectedEditFile = null;
        postEditFileLabel.setText("No file selected (keep existing or choose new)");

        postEditPanel.setVisible(true);
        postEditPanel.setManaged(true);
        commentEditPanel.setVisible(false);
        commentEditPanel.setManaged(false);

        // Scroll to edit panel
        postEditPanel.requestFocus();
    }

    @FXML
    private void handlePostEditFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Media File");
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif"),
            new FileChooser.ExtensionFilter("Videos", "*.mp4", "*.avi", "*.mov"),
            new FileChooser.ExtensionFilter("All Files", "*.*")
        );
        File file = fileChooser.showOpenDialog(postEditPanel.getScene().getWindow());
        if (file != null) {
            selectedEditFile = file;
            postEditFileLabel.setText(file.getName());
        }
    }

    @FXML
    private void handlePostEditSave() {
        if (editingPostId == -1) return;

        String newContent = postEditContent.getText().trim();
        if (newContent.isEmpty()) {
            showError("Validation Error", "Post content cannot be empty");
            return;
        }

        try {
            Post post = postService.getPostById(editingPostId);
            if (post != null) {
                post.setContent_post(newContent);

                // Handle file attachment
                if (selectedEditFile != null) {
                    post.setMedia_url_post(selectedEditFile.getAbsolutePath());
                    // Determine media type
                    String fileName = selectedEditFile.getName().toLowerCase();
                    if (fileName.endsWith(".mp4") || fileName.endsWith(".avi") || fileName.endsWith(".mov")) {
                        post.setMedia_type_post("video");
                    } else {
                        post.setMedia_type_post("image");
                    }
                }

                postService.updatePost(editingPostId, post);
                handlePostEditCancel();
                handleRefresh();
                showInfo("Success", "Post updated successfully");
            }
        } catch (SQLException e) {
            showError("Update Error", "Failed to update post: " + e.getMessage());
        }
    }

    @FXML
    private void handlePostEditCancel() {
        editingPostId = -1;
        selectedEditFile = null;
        postEditContent.clear();
        postEditFileLabel.setText("No file selected");
        postEditPanel.setVisible(false);
        postEditPanel.setManaged(false);
    }

    private void handleDelete(PostRow row) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirm Delete");
        alert.setHeaderText("Delete Post");
        alert.setContentText("Are you sure you want to delete post ID " + row.getId() + "?");

        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    postService.supprimer(row.getId());
                    handleRefresh();
                    showInfo("Success", "Post deleted successfully");
                } catch (SQLException e) {
                    showError("Delete Error", "Failed to delete post: " + e.getMessage());
                }
            }
        });
    }

    private void handleEditComment(CommentRow row) {
        // Show inline edit panel instead of dialog
        editingCommentId = row.getId();
        commentEditContent.setText(row.getFullContent());

        commentEditPanel.setVisible(true);
        commentEditPanel.setManaged(true);
        postEditPanel.setVisible(false);
        postEditPanel.setManaged(false);

        // Scroll to edit panel
        commentEditPanel.requestFocus();
    }

    @FXML
    private void handleCommentEditSave() {
        if (editingCommentId == -1) return;

        String newContent = commentEditContent.getText().trim();
        if (newContent.isEmpty()) {
            showError("Validation Error", "Comment content cannot be empty");
            return;
        }

        try {
            commentService.updateComment(editingCommentId, newContent);
            handleCommentEditCancel();
            handleRefresh();
            showInfo("Success", "Comment updated successfully");
        } catch (SQLException e) {
            showError("Update Error", "Failed to update comment: " + e.getMessage());
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
        alert.setTitle("Confirm Delete");
        alert.setHeaderText("Delete Comment");
        alert.setContentText("Are you sure you want to delete comment ID " + row.getId() + "?");

        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    commentService.supprimer(row.getId());
                    handleRefresh();
                    showInfo("Success", "Comment deleted successfully");
                } catch (SQLException e) {
                    showError("Delete Error", "Failed to delete comment: " + e.getMessage());
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
