package com.destiny.club.domain.accounting;

/**
 * The five fundamental account classifications used across the chart of accounts,
 * trial balance, balance sheet and income &amp; expenditure statement.
 */
public enum AccountType {
    ASSET,
    LIABILITY,
    EQUITY,
    INCOME,
    EXPENSE;

    public boolean isBalanceSheetAccount() {
        return this == ASSET || this == LIABILITY || this == EQUITY;
    }

    public boolean isIncomeStatementAccount() {
        return this == INCOME || this == EXPENSE;
    }

    /** True for account types whose balance normally increases on the debit side. */
    public boolean normalBalanceIsDebit() {
        return this == ASSET || this == EXPENSE;
    }
}
