-- H2 (MODE=MySQL) 测试库表结构，与 sql/schema/init_core_tables.sql 核心表对齐

DROP TABLE IF EXISTS wrong_question;
DROP TABLE IF EXISTS question;
DROP TABLE IF EXISTS question_bank;
DROP TABLE IF EXISTS sys_user;

CREATE TABLE sys_user (
    id            BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    username      VARCHAR(64)  NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    nickname      VARCHAR(64),
    role          VARCHAR(32)  NOT NULL DEFAULT 'USER',
    create_time   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted    TINYINT      NOT NULL DEFAULT 0
);
CREATE UNIQUE INDEX uk_username ON sys_user (username, is_deleted);
CREATE INDEX idx_create_time ON sys_user (create_time);

CREATE TABLE question_bank (
    id          BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id     BIGINT       NOT NULL,
    title       VARCHAR(200) NOT NULL,
    description VARCHAR(1000),
    is_public   TINYINT      NOT NULL DEFAULT 1,
    create_time TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted  TINYINT      NOT NULL DEFAULT 0
);
CREATE INDEX idx_user_id ON question_bank (user_id);
CREATE INDEX idx_user_public ON question_bank (user_id, is_public, is_deleted);

CREATE TABLE question (
    id               BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    question_bank_id BIGINT       NOT NULL,
    question_type    VARCHAR(32)  NOT NULL DEFAULT 'SINGLE',
    stem             CLOB         NOT NULL,
    options_json     VARCHAR(4000),
    answer_json      VARCHAR(1000),
    analysis         CLOB,
    raw_llm_json     CLOB,
    sort_no          INT          NOT NULL DEFAULT 0,
    create_time      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted       TINYINT      NOT NULL DEFAULT 0
);
CREATE INDEX idx_bank_sort ON question (question_bank_id, sort_no, is_deleted);
CREATE INDEX idx_bank_id ON question (question_bank_id);

CREATE TABLE wrong_question (
    id              BIGINT    NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT    NOT NULL,
    question_id     BIGINT    NOT NULL,
    wrong_count     INT       NOT NULL DEFAULT 1,
    last_wrong_time TIMESTAMP,
    create_time     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted      TINYINT   NOT NULL DEFAULT 0
);
CREATE UNIQUE INDEX uk_user_question ON wrong_question (user_id, question_id);
CREATE INDEX idx_user_create ON wrong_question (user_id, create_time);
