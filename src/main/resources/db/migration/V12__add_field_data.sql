-- Field collections: a field officer (working in a rotating pair, unrestricted by which
-- permanent group the member belongs to) collects savings cash out in the field. It's booked
-- against a dedicated "Field Cash Control" holding account rather than straight into a till,
-- since the cash is physically with the field team until it's later banked/handed over.
INSERT INTO gl_accounts (code, name, account_type, system_account, cash_account, active, description) VALUES
    ('3380', 'Field Cash Control', 'ASSET', TRUE, FALSE, TRUE,
     'Cash collected by field officers, held pending banking - cleared once handed over to a till or bank account');

CREATE TABLE field_data (
    id                      BIGINT AUTO_INCREMENT PRIMARY KEY,
    client_id               BIGINT       NOT NULL,
    savings_transaction_id  BIGINT       NOT NULL,
    collector_one_name      VARCHAR(120) NOT NULL,
    collector_two_name      VARCHAR(120) NULL,
    created_by              VARCHAR(60),
    created_at              DATETIME     NOT NULL,
    CONSTRAINT uk_field_data_txn UNIQUE (savings_transaction_id),
    CONSTRAINT fk_field_data_client FOREIGN KEY (client_id) REFERENCES clients (id),
    CONSTRAINT fk_field_data_txn FOREIGN KEY (savings_transaction_id) REFERENCES savings_transactions (id)
) ENGINE=InnoDB;
