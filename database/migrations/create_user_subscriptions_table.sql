-- Table pour suivre les abonnements actifs des utilisateurs
CREATE TABLE IF NOT EXISTS user_subscriptions (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    offer_id INT NOT NULL,
    order_id INT NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    status ENUM('active', 'expired', 'cancelled') DEFAULT 'active',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (offer_id) REFERENCES gym_subscription_offers(id) ON DELETE CASCADE,
    FOREIGN KEY (order_id) REFERENCES gym_subscription_orders(id) ON DELETE CASCADE,
    
    INDEX idx_user_status (user_id, status),
    INDEX idx_end_date (end_date),
    INDEX idx_order_id (order_id)
);
