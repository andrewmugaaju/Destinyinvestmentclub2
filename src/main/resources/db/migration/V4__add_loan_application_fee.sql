ALTER TABLE loan_products
    ADD COLUMN application_fee_amount DECIMAL(18,2) NOT NULL DEFAULT 0 AFTER default_term_months;

ALTER TABLE loan_accounts
    ADD COLUMN application_fee_amount DECIMAL(18,2) NOT NULL DEFAULT 0 AFTER term_months;
