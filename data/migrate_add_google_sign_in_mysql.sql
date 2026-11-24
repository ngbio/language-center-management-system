-- Add Google sign-in to an existing database. Safe to run again.
USE LanguageCenterDB;

CREATE TABLE IF NOT EXISTS google_account (
    id INT NOT NULL AUTO_INCREMENT,
    user_id INT NOT NULL,
    firebase_uid VARCHAR(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT uq_google_account_user UNIQUE (user_id),
    CONSTRAINT uq_google_account_firebase_uid UNIQUE (firebase_uid),
    CONSTRAINT fk_google_account_user FOREIGN KEY (user_id) REFERENCES `user`(id) ON DELETE CASCADE
) ENGINE=InnoDB;
