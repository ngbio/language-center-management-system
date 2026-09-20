/*
  Migration for an existing LanguageCenterDB.
  Run once when the main database was created before password reset support.
*/
USE LanguageCenterDB;

ALTER TABLE `user`
    ADD COLUMN password_changed_at DATETIME NULL AFTER updated_at;

CREATE TABLE password_reset_token (
    id          INT NOT NULL AUTO_INCREMENT,
    user_id     INT NOT NULL,
    token_hash  CHAR(64) NOT NULL,
    expires_at  DATETIME NOT NULL,
    used_at     DATETIME NULL,
    created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT uq_password_reset_token_hash UNIQUE (token_hash),
    CONSTRAINT fk_password_reset_token_user
        FOREIGN KEY (user_id) REFERENCES `user`(id) ON DELETE CASCADE,
    INDEX idx_password_reset_user_created (user_id, created_at),
    INDEX idx_password_reset_expiry (expires_at)
) ENGINE=InnoDB;
