package org.example.entities;

public class Post {
    private int id_post;
    private String content_post;
    private String media_url_post;
    private String media_type_post;
    private String visibility_post;
    private String created_at_post;
    private String updated_at_post;
    private int like_count_post;
    private String category_post;
    private int id_author_post;

    public Post(){}
    public Post(String content_post, String media_url_post, String media_type_post,String visibility_post, String created_at_post, String updated_at_post, int like_count_post, String category_post, int id_author_post) {
        this.content_post = content_post;
        this.media_url_post = media_url_post;
        this.media_type_post = media_type_post;
        this.visibility_post = visibility_post;
        this.created_at_post = created_at_post;
        this.updated_at_post = updated_at_post;
        this.like_count_post = like_count_post;
        this.category_post = category_post;
        this.id_author_post = id_author_post;
    }

    public int getId_post() {
        return id_post;
    }

    public void setId_post(int id_post) {
        this.id_post = id_post;
    }

    public String getMedia_url_post() {
        return media_url_post;
    }

    public void setMedia_url_post(String media_url_post) {
        this.media_url_post = media_url_post;
    }

    public String getMedia_type_post() {
        return media_type_post;
    }

    public void setMedia_type_post(String media_type_post) {
        this.media_type_post = media_type_post;
    }

    public String getContent_post() {
        return content_post;
    }

    public void setContent_post(String content_post) {
        this.content_post = content_post;
    }

    public String getCreated_at_post() {
        return created_at_post;
    }

    public void setCreated_at_post(String created_at_post) {
        this.created_at_post = created_at_post;
    }

    public String getVisibility_post() {
        return visibility_post;
    }

    public void setVisibility_post(String visibility_post) {
        this.visibility_post = visibility_post;
    }

    public String getUpdated_at_post() {
        return updated_at_post;
    }

    public void setUpdated_at_post(String updated_at_post) {
        this.updated_at_post = updated_at_post;
    }

    public int getLike_count_post() {
        return like_count_post;
    }

    public void setLike_count_post(int like_count_post) {
        this.like_count_post = like_count_post;
    }

    public String getCategory_post() {
        return category_post;
    }

    public void setCategory_post(String category_post) {
        this.category_post = category_post;
    }

    public int getId_author_post() {
        return id_author_post;
    }

    public void setId_author_post(int id_author_post) {
        this.id_author_post = id_author_post;
    }

    @Override
    public String toString() {
        return "Post{" +
                "id_post=" + id_post +
                ", content_post='" + content_post + '\'' +
                ", media_url_post='" + media_url_post + '\'' +
                ", media_type_post='" + media_type_post + '\'' +
                ", visibility_post='" + visibility_post + '\'' +
                ", created_at_post='" + created_at_post + '\'' +
                ", updated_at_post='" + updated_at_post + '\'' +
                ", like_count_post=" + like_count_post +
                ", category_post='" + category_post + '\'' +
                ", id_author_post=" + id_author_post +
                '}';
    }

}
