package com.destiny.club.dto.report;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@AllArgsConstructor
public class LedgerRow {
    private final LocalDate date;
    private final String reference;
    /** The member/group this transaction was for, e.g. "Jane Mukasa" - null when there isn't one (a manual entry). */
    private final String personName;
    private final String description;
    /** Plain-English label for what this entry actually is - "Savings Deposit", "Loan Repayment", etc. */
    private final String typeLabel;
    /** Where "View" should take you: the specific transaction's own detail page (with Edit/Void), or the journal entry as a fallback. */
    private final String viewUrl;
    private final BigDecimal debit;
    private final BigDecimal credit;
    private final BigDecimal runningBalance;
}
