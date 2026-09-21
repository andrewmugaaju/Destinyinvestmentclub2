INSERT INTO gl_accounts (code, name, account_type, system_account, active, description) VALUES
    ('1000', 'Cash and Bank',              'ASSET',     TRUE,  TRUE, 'Cash held and bank balances used for all teller receipts and payments'),
    ('1100', 'Loans Receivable',           'ASSET',     TRUE,  TRUE, 'Outstanding principal owed by members on active loans'),
    ('1900', 'Other Assets',               'ASSET',     FALSE, TRUE, 'Other assets not otherwise classified'),

    ('2000', 'Member Savings',             'LIABILITY', TRUE,  TRUE, 'Total savings deposits held on behalf of members and groups'),
    ('2100', 'Other Liabilities',          'LIABILITY', FALSE, TRUE, 'Other liabilities not otherwise classified'),

    ('3000', 'Member Share Capital',       'EQUITY',    FALSE, TRUE, 'Share capital contributed by members'),
    ('3900', 'Retained Earnings',          'EQUITY',    FALSE, TRUE, 'Accumulated surplus brought forward from prior periods'),

    ('4000', 'Loan Interest Income',       'INCOME',    TRUE,  TRUE, 'Interest earned on member loans'),
    ('4100', 'Fees and Charges Income',    'INCOME',    FALSE, TRUE, 'Application fees, penalties and other charges income'),

    ('5000', 'Interest Expense on Savings','EXPENSE',   FALSE, TRUE, 'Interest paid or credited to members on their savings'),
    ('5100', 'Administrative Expenses',    'EXPENSE',   FALSE, TRUE, 'Rent, salaries, utilities and other running costs'),
    ('5200', 'Loan Loss Provision Expense','EXPENSE',   FALSE, TRUE, 'Provision for loans considered doubtful or written off');
