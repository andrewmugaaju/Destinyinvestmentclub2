-- Savings accounts are individual-client-only now; groups no longer have their own savings.
-- (No group savings accounts exist in any environment as of this migration, so this is a
-- clean drop, not a data migration.)
ALTER TABLE savings_accounts DROP FOREIGN KEY fk_savings_accounts_group;
ALTER TABLE savings_accounts DROP COLUMN group_id;
