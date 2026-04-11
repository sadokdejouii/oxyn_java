-- Forum Database Schema
-- Run this SQL to create the forum tables in your MySQL database

-- Forum Posts Table
CREATE TABLE IF NOT EXISTS forum_posts (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    username VARCHAR(255) NOT NULL,
    user_avatar VARCHAR(255),
    content LONGTEXT NOT NULL,
    attachment_path VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    view_count INT DEFAULT 0,
    INDEX idx_created_at (created_at DESC),
    INDEX idx_user_id (user_id)
);

-- Forum Comments Table
CREATE TABLE IF NOT EXISTS forum_comments (
    id INT AUTO_INCREMENT PRIMARY KEY,
    post_id INT NOT NULL,
    user_id INT NOT NULL,
    username VARCHAR(255) NOT NULL,
    user_avatar VARCHAR(255),
    content LONGTEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (post_id) REFERENCES forum_posts(id) ON DELETE CASCADE,
    INDEX idx_post_id (post_id),
    INDEX idx_created_at (created_at DESC)
);

-- Forum Reactions Table
CREATE TABLE IF NOT EXISTS forum_reactions (
    id INT AUTO_INCREMENT PRIMARY KEY,
    post_id INT NOT NULL,
    user_id INT NOT NULL,
    emoji VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY unique_reaction (post_id, user_id, emoji),
    FOREIGN KEY (post_id) REFERENCES forum_posts(id) ON DELETE CASCADE,
    INDEX idx_post_id (post_id),
    INDEX idx_emoji (emoji)
);

-- Optional: Add indexes for performance
CREATE INDEX idx_posts_latest ON forum_posts(created_at DESC, id DESC);
CREATE INDEX idx_comments_by_post ON forum_comments(post_id, created_at DESC);
