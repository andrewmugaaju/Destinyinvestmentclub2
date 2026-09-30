-- Loan products are gone: every loan account already carries its own interest rate, term and
-- application fee (copied from a product at application time before this migration), so there
-- is nothing left that actually depended on the product catalog itself. One generic loan type,
-- no products.
ALTER TABLE loan_accounts DROP FOREIGN KEY fk_loan_accounts_product;
ALTER TABLE loan_accounts DROP COLUMN loan_product_id;
DROP TABLE loan_products;
