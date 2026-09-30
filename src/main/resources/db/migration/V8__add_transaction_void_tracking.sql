-- Lets a teller/manager correct a mistaken transaction by voiding it (a reversing journal
-- entry is posted and the account/loan is recomputed from its remaining non-voided history)
-- rather than ever hard-deleting a posted financial record.
ALTER TABLE savings_transactions ADD COLUMN voided BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE savings_transactions ADD COLUMN voided_by VARCHAR(60) NULL;
ALTER TABLE savings_transactions ADD COLUMN voided_at DATETIME NULL;
ALTER TABLE savings_transactions ADD COLUMN void_reason VARCHAR(250) NULL;
ALTER TABLE savings_transactions ADD COLUMN reversal_journal_entry_id BIGINT NULL;

ALTER TABLE loan_transactions ADD COLUMN voided BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE loan_transactions ADD COLUMN voided_by VARCHAR(60) NULL;
ALTER TABLE loan_transactions ADD COLUMN voided_at DATETIME NULL;
ALTER TABLE loan_transactions ADD COLUMN void_reason VARCHAR(250) NULL;
ALTER TABLE loan_transactions ADD COLUMN reversal_journal_entry_id BIGINT NULL;

ALTER TABLE deposit_transactions ADD COLUMN voided BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE deposit_transactions ADD COLUMN voided_by VARCHAR(60) NULL;
ALTER TABLE deposit_transactions ADD COLUMN voided_at DATETIME NULL;
ALTER TABLE deposit_transactions ADD COLUMN void_reason VARCHAR(250) NULL;
ALTER TABLE deposit_transactions ADD COLUMN reversal_journal_entry_id BIGINT NULL;
