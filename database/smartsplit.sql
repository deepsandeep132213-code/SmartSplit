-- SmartSplit Database Schema
-- MySQL 8. Run once to set up the database.
-- NOTE: 'groups' is a reserved word in MySQL 8 — always backtick-quote it in queries.

CREATE DATABASE IF NOT EXISTS smartsplit_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE smartsplit_db;

-- ─────────────────────────── users ────────────────────────────────────────
CREATE TABLE IF NOT EXISTS users (
    user_id       INT            NOT NULL AUTO_INCREMENT PRIMARY KEY,
    name          VARCHAR(100)   NOT NULL,
    email         VARCHAR(255)   NOT NULL,
    password_hash VARCHAR(255)   NOT NULL,
    created_at    DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uq_users_email (email)
) ENGINE=InnoDB;

-- ─────────────────────────── groups ───────────────────────────────────────
CREATE TABLE IF NOT EXISTS `groups` (
    group_id    INT          NOT NULL AUTO_INCREMENT PRIMARY KEY,
    group_name  VARCHAR(100) NOT NULL,
    created_by  INT          NOT NULL,
    is_archived TINYINT(1)   NOT NULL DEFAULT 0,
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_groups_created_by FOREIGN KEY (created_by) REFERENCES users (user_id)
) ENGINE=InnoDB;

-- ─────────────────────────── group_members ────────────────────────────────
CREATE TABLE IF NOT EXISTS group_members (
    group_id  INT      NOT NULL,
    user_id   INT      NOT NULL,
    joined_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (group_id, user_id),
    CONSTRAINT fk_gm_group FOREIGN KEY (group_id) REFERENCES `groups` (group_id) ON DELETE CASCADE,
    CONSTRAINT fk_gm_user  FOREIGN KEY (user_id)  REFERENCES users    (user_id)  ON DELETE CASCADE
) ENGINE=InnoDB;

-- ─────────────────────────── expenses ─────────────────────────────────────
CREATE TABLE IF NOT EXISTS expenses (
    expense_id   INT            NOT NULL AUTO_INCREMENT PRIMARY KEY,
    group_id     INT            NOT NULL,
    description  VARCHAR(255)   NOT NULL,
    amount       DECIMAL(12, 2) NOT NULL,
    paid_by      INT            NOT NULL,
    split_type   VARCHAR(20)    NOT NULL DEFAULT 'EQUAL',
    category     VARCHAR(30)    NOT NULL DEFAULT 'OTHER',
    expense_date DATE               NULL,
    notes        TEXT               NULL,
    created_at   DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_exp_group  FOREIGN KEY (group_id) REFERENCES `groups` (group_id) ON DELETE CASCADE,
    CONSTRAINT fk_exp_paidby FOREIGN KEY (paid_by)  REFERENCES users    (user_id),
    CONSTRAINT chk_exp_amount CHECK (amount > 0)
) ENGINE=InnoDB;

-- ─────────────────────────── expense_splits ───────────────────────────────
CREATE TABLE IF NOT EXISTS expense_splits (
    split_id   INT            NOT NULL AUTO_INCREMENT PRIMARY KEY,
    expense_id INT            NOT NULL,
    user_id    INT            NOT NULL,
    amount     DECIMAL(12, 2) NOT NULL,
    CONSTRAINT fk_split_expense FOREIGN KEY (expense_id) REFERENCES expenses (expense_id) ON DELETE CASCADE,
    CONSTRAINT fk_split_user    FOREIGN KEY (user_id)    REFERENCES users    (user_id),
    CONSTRAINT chk_split_amount CHECK (amount >= 0)
) ENGINE=InnoDB;
