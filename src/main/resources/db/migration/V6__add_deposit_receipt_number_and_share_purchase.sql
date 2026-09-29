-- The regular deposit screen now captures a separate teller-entered receipt/reference
-- number (distinct from the system-generated `reference`), and supports a third split
-- line, "Shares", which has no savings/loan account behind it.
ALTER TABLE deposit_transactions ADD COLUMN receipt_number VARCHAR(60) NULL AFTER reference;
ALTER TABLE deposit_allocations MODIFY COLUMN target_account_id BIGINT NULL;
