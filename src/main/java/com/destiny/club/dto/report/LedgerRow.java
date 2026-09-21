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
    private final String description;
    private final BigDecimal debit;
    private final BigDecimal credit;
    private final BigDecimal runningBalance;
}
