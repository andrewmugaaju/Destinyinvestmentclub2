-- "Cash and Bank" lumped till cash and bank balances into one account. Split it into two
-- separate payment channels: the existing account (all its history stays intact) becomes
-- "Cash at Bank", and a new "Cash at Hand" account is added for physical/till cash.
UPDATE gl_accounts
SET name = 'Cash at Bank',
    description = 'Bank balances used for teller receipts and payments'
WHERE code = '1000';

INSERT INTO gl_accounts (code, name, account_type, system_account, cash_account, active, description) VALUES
    ('1005', 'Cash at Hand', 'ASSET', FALSE, TRUE, TRUE, 'Physical cash held at the till/office, separate from bank balances');
