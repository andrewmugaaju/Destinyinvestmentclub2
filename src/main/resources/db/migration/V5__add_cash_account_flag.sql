ALTER TABLE gl_accounts
    ADD COLUMN cash_account BOOLEAN NOT NULL DEFAULT FALSE AFTER system_account;

UPDATE gl_accounts SET cash_account = TRUE WHERE code = '1000';
