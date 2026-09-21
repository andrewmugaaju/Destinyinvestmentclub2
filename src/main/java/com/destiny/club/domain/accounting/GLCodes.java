package com.destiny.club.domain.accounting;

/**
 * Codes of the system (control) accounts that the application posts to automatically.
 * These must match the codes seeded by the V3 Flyway migration.
 */
public final class GLCodes {

    private GLCodes() {
    }

    public static final String CASH_AND_BANK = "1000";
    public static final String LOANS_RECEIVABLE = "1100";
    public static final String MEMBER_SAVINGS = "2000";
    public static final String SHARE_CAPITAL = "3000";
    public static final String RETAINED_EARNINGS = "3900";
    public static final String LOAN_INTEREST_INCOME = "4000";
    public static final String FEES_AND_CHARGES_INCOME = "4100";
    public static final String INTEREST_EXPENSE_ON_SAVINGS = "5000";
    public static final String ADMINISTRATIVE_EXPENSES = "5100";
}
