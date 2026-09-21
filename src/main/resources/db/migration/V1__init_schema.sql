-- Core schema for the Destiny Investment Club savings, loans and accounting system.

CREATE TABLE roles (
    id   BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(30) NOT NULL,
    CONSTRAINT uk_roles_name UNIQUE (name)
) ENGINE=InnoDB;

CREATE TABLE users (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    username   VARCHAR(60)  NOT NULL,
    password   VARCHAR(255) NOT NULL,
    full_name  VARCHAR(120) NOT NULL,
    email      VARCHAR(120),
    enabled    BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at DATETIME     NOT NULL,
    CONSTRAINT uk_users_username UNIQUE (username)
) ENGINE=InnoDB;

CREATE TABLE user_roles (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_user_roles_role FOREIGN KEY (role_id) REFERENCES roles (id)
) ENGINE=InnoDB;

CREATE TABLE member_groups (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    group_number VARCHAR(30)  NOT NULL,
    group_name   VARCHAR(150) NOT NULL,
    meeting_day  VARCHAR(30),
    formed_date  DATE,
    status       VARCHAR(20)  NOT NULL,
    created_at   DATETIME     NOT NULL,
    CONSTRAINT uk_groups_number UNIQUE (group_number)
) ENGINE=InnoDB;

CREATE TABLE clients (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    client_number VARCHAR(30)  NOT NULL,
    first_name    VARCHAR(80)  NOT NULL,
    last_name     VARCHAR(80)  NOT NULL,
    phone         VARCHAR(20),
    email         VARCHAR(120),
    address       VARCHAR(250),
    id_number     VARCHAR(40),
    date_joined   DATE,
    status        VARCHAR(20)  NOT NULL,
    group_id      BIGINT,
    created_at    DATETIME     NOT NULL,
    CONSTRAINT uk_clients_number UNIQUE (client_number),
    CONSTRAINT fk_clients_group FOREIGN KEY (group_id) REFERENCES member_groups (id)
) ENGINE=InnoDB;

CREATE TABLE gl_accounts (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    code           VARCHAR(20)  NOT NULL,
    name           VARCHAR(150) NOT NULL,
    account_type   VARCHAR(20)  NOT NULL,
    system_account BOOLEAN      NOT NULL DEFAULT FALSE,
    active         BOOLEAN      NOT NULL DEFAULT TRUE,
    description    VARCHAR(250),
    CONSTRAINT uk_gl_accounts_code UNIQUE (code)
) ENGINE=InnoDB;

CREATE TABLE journal_entries (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    reference        VARCHAR(60)  NOT NULL,
    transaction_date DATE         NOT NULL,
    description      VARCHAR(250) NOT NULL,
    source_type      VARCHAR(40)  NOT NULL,
    source_id        BIGINT,
    created_by       VARCHAR(60),
    created_at       DATETIME     NOT NULL,
    CONSTRAINT uk_journal_entries_reference UNIQUE (reference)
) ENGINE=InnoDB;

CREATE TABLE journal_entry_lines (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    journal_entry_id BIGINT         NOT NULL,
    gl_account_id    BIGINT         NOT NULL,
    entry_type       VARCHAR(10)    NOT NULL,
    amount           DECIMAL(18,2)  NOT NULL,
    client_id        BIGINT,
    narration        VARCHAR(250),
    CONSTRAINT fk_jel_journal_entry FOREIGN KEY (journal_entry_id) REFERENCES journal_entries (id),
    CONSTRAINT fk_jel_gl_account FOREIGN KEY (gl_account_id) REFERENCES gl_accounts (id)
) ENGINE=InnoDB;

CREATE INDEX idx_jel_gl_account_date ON journal_entry_lines (gl_account_id);

CREATE TABLE savings_products (
    id                    BIGINT AUTO_INCREMENT PRIMARY KEY,
    code                  VARCHAR(20)   NOT NULL,
    name                  VARCHAR(120)  NOT NULL,
    annual_interest_rate  DECIMAL(6,3)  NOT NULL,
    min_opening_balance   DECIMAL(18,2) NOT NULL,
    active                BOOLEAN       NOT NULL DEFAULT TRUE,
    description           VARCHAR(250),
    CONSTRAINT uk_savings_products_code UNIQUE (code)
) ENGINE=InnoDB;

CREATE TABLE savings_accounts (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    account_number      VARCHAR(30)   NOT NULL,
    client_id           BIGINT,
    group_id            BIGINT,
    savings_product_id  BIGINT        NOT NULL,
    opened_date         DATE          NOT NULL,
    status              VARCHAR(20)   NOT NULL,
    balance             DECIMAL(18,2) NOT NULL DEFAULT 0,
    created_at          DATETIME      NOT NULL,
    CONSTRAINT uk_savings_accounts_number UNIQUE (account_number),
    CONSTRAINT fk_savings_accounts_client FOREIGN KEY (client_id) REFERENCES clients (id),
    CONSTRAINT fk_savings_accounts_group FOREIGN KEY (group_id) REFERENCES member_groups (id),
    CONSTRAINT fk_savings_accounts_product FOREIGN KEY (savings_product_id) REFERENCES savings_products (id)
) ENGINE=InnoDB;

CREATE TABLE savings_transactions (
    id                       BIGINT AUTO_INCREMENT PRIMARY KEY,
    savings_account_id       BIGINT        NOT NULL,
    transaction_type         VARCHAR(20)   NOT NULL,
    amount                   DECIMAL(18,2) NOT NULL,
    running_balance          DECIMAL(18,2) NOT NULL,
    transaction_date         DATE          NOT NULL,
    journal_entry_id         BIGINT,
    deposit_transaction_id   BIGINT,
    narration                VARCHAR(250),
    created_by               VARCHAR(60),
    created_at               DATETIME      NOT NULL,
    CONSTRAINT fk_savings_txn_account FOREIGN KEY (savings_account_id) REFERENCES savings_accounts (id)
) ENGINE=InnoDB;

CREATE TABLE loan_products (
    id                   BIGINT AUTO_INCREMENT PRIMARY KEY,
    code                 VARCHAR(20)  NOT NULL,
    name                 VARCHAR(120) NOT NULL,
    annual_interest_rate DECIMAL(6,3) NOT NULL,
    default_term_months  INT          NOT NULL,
    active               BOOLEAN      NOT NULL DEFAULT TRUE,
    description          VARCHAR(250),
    CONSTRAINT uk_loan_products_code UNIQUE (code)
) ENGINE=InnoDB;

CREATE TABLE loan_accounts (
    id                     BIGINT AUTO_INCREMENT PRIMARY KEY,
    loan_account_number    VARCHAR(30)   NOT NULL,
    client_id              BIGINT,
    group_id               BIGINT,
    loan_product_id        BIGINT        NOT NULL,
    principal_amount       DECIMAL(18,2) NOT NULL,
    annual_interest_rate   DECIMAL(6,3)  NOT NULL,
    term_months            INT           NOT NULL,
    application_date       DATE,
    disbursement_date      DATE,
    status                 VARCHAR(20)   NOT NULL,
    outstanding_principal  DECIMAL(18,2) NOT NULL DEFAULT 0,
    outstanding_interest   DECIMAL(18,2) NOT NULL DEFAULT 0,
    approved_by            VARCHAR(60),
    created_at             DATETIME      NOT NULL,
    CONSTRAINT uk_loan_accounts_number UNIQUE (loan_account_number),
    CONSTRAINT fk_loan_accounts_client FOREIGN KEY (client_id) REFERENCES clients (id),
    CONSTRAINT fk_loan_accounts_group FOREIGN KEY (group_id) REFERENCES member_groups (id),
    CONSTRAINT fk_loan_accounts_product FOREIGN KEY (loan_product_id) REFERENCES loan_products (id)
) ENGINE=InnoDB;

CREATE TABLE loan_repayment_installments (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    loan_account_id     BIGINT        NOT NULL,
    installment_number  INT           NOT NULL,
    due_date            DATE          NOT NULL,
    principal_due       DECIMAL(18,2) NOT NULL,
    interest_due        DECIMAL(18,2) NOT NULL,
    principal_paid      DECIMAL(18,2) NOT NULL DEFAULT 0,
    interest_paid       DECIMAL(18,2) NOT NULL DEFAULT 0,
    fully_paid          BOOLEAN       NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_installment_loan FOREIGN KEY (loan_account_id) REFERENCES loan_accounts (id)
) ENGINE=InnoDB;

CREATE TABLE loan_transactions (
    id                      BIGINT AUTO_INCREMENT PRIMARY KEY,
    loan_account_id         BIGINT        NOT NULL,
    transaction_type        VARCHAR(20)   NOT NULL,
    amount                  DECIMAL(18,2) NOT NULL,
    principal_portion       DECIMAL(18,2) NOT NULL DEFAULT 0,
    interest_portion        DECIMAL(18,2) NOT NULL DEFAULT 0,
    transaction_date        DATE          NOT NULL,
    journal_entry_id        BIGINT,
    deposit_transaction_id  BIGINT,
    narration               VARCHAR(250),
    created_by              VARCHAR(60),
    created_at              DATETIME      NOT NULL,
    CONSTRAINT fk_loan_txn_account FOREIGN KEY (loan_account_id) REFERENCES loan_accounts (id)
) ENGINE=InnoDB;

CREATE TABLE deposit_transactions (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    reference         VARCHAR(40)   NOT NULL,
    client_id         BIGINT,
    group_id          BIGINT,
    transaction_date  DATE          NOT NULL,
    total_amount      DECIMAL(18,2) NOT NULL,
    journal_entry_id  BIGINT,
    narration         VARCHAR(250),
    created_by        VARCHAR(60),
    created_at        DATETIME      NOT NULL,
    CONSTRAINT uk_deposit_txn_reference UNIQUE (reference),
    CONSTRAINT fk_deposit_txn_client FOREIGN KEY (client_id) REFERENCES clients (id),
    CONSTRAINT fk_deposit_txn_group FOREIGN KEY (group_id) REFERENCES member_groups (id)
) ENGINE=InnoDB;

CREATE TABLE deposit_allocations (
    id                       BIGINT AUTO_INCREMENT PRIMARY KEY,
    deposit_transaction_id   BIGINT        NOT NULL,
    allocation_type          VARCHAR(20)   NOT NULL,
    target_account_id        BIGINT        NOT NULL,
    amount                   DECIMAL(18,2) NOT NULL,
    CONSTRAINT fk_deposit_alloc_txn FOREIGN KEY (deposit_transaction_id) REFERENCES deposit_transactions (id)
) ENGINE=InnoDB;
