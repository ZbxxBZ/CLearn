-- CLearn database bootstrap script.
-- Run this on the MySQL/MariaDB server as root or another privileged user.
--
-- Example:
--   mysql -u root -p < docs/sql/clearn_mysql_init.sql
--
-- This script only creates the database and application account.
-- Table creation is handled by Flyway when clearn-api starts.

CREATE DATABASE IF NOT EXISTS clearn
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

CREATE USER IF NOT EXISTS 'clearn'@'%' IDENTIFIED BY 'clearn';
ALTER USER 'clearn'@'%' IDENTIFIED BY 'clearn';

GRANT ALL PRIVILEGES ON clearn.* TO 'clearn'@'%';
FLUSH PRIVILEGES;

SELECT
    SCHEMA_NAME AS database_name,
    DEFAULT_CHARACTER_SET_NAME AS charset_name,
    DEFAULT_COLLATION_NAME AS collation_name
FROM information_schema.SCHEMATA
WHERE SCHEMA_NAME = 'clearn';

SELECT
    USER AS user_name,
    HOST AS host_pattern
FROM mysql.user
WHERE USER = 'clearn';
