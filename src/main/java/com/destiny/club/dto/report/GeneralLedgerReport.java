package com.destiny.club.dto.report;

import com.destiny.club.domain.accounting.GLAccount;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
@AllArgsConstructor
public class GeneralLedgerReport {
    private final GLAccount account;
    private final LocalDate fromDate;
    private final LocalDate toDate;
    private final BigDecimal openingBalance;
    private final List<LedgerRow> rows;
    private final BigDecimal closingBalance;
}
