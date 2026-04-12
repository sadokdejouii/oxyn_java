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
import javafx.util.Callback;
import org.example.entities.Post;
import org.example.services.ForumPostService;

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

    // Service
    private ForumPostService postService;

    // Data
    private final ObservableList<PostRow> postsData = FXCollections.observableArrayList();
    private List<Post> allPosts = new ArrayList<>();

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

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        try {
            postService = new ForumPostService();
            setupTableColumns();
            setupActionButtons();
            loadData();
            updateStats();
            updateCharts();
        } catch (SQLException e) {
            showError("Database Error", "Failed to initialize: " + e.getMessage());
        }
    }

    private void setupTableColumns() {
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        contentColumn.setCellValueFactory(new PropertyValueFactory<>("content"));
        categoryColumn.setCellValueFactory(new PropertyValueFactory<>("category"));
        authorColumn.setCellValueFactory(new PropertyValueFactory<>("author"));
        likesColumn.setCellValueFactory(new PropertyValueFactory<>("likes"));
        dateColumn.setCellValueFactory(new PropertyValueFactory<>("date"));
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

    private void loadData() throws SQLException {
        allPosts = postService.afficher();
        refreshTable();
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

    private void updateStats() {
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
        TextInputDialog dialog = new TextInputDialog(row.getContent());
        dialog.setTitle("Edit Post");
        dialog.setHeaderText("Edit post content (ID: " + row.getId() + ")");
        dialog.setContentText("Content:");

        dialog.showAndWait().ifPresent(newContent -> {
            try {
                Post post = postService.getPostById(row.getId());
                if (post != null) {
                    post.setContent_post(newContent);
                    postService.updatePost(row.getId(), post);
                    handleRefresh();
                    showInfo("Success", "Post updated successfully");
                }
            } catch (SQLException e) {
                showError("Update Error", "Failed to update post: " + e.getMessage());
            }
        });
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
