package com.destiny.club.dto.report;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
@AllArgsConstructor
public class TrialBalanceReport {
    private final LocalDate asOfDate;
    private final List<TrialBalanceLine> lines;
    private final BigDecimal totalDebit;
    private final BigDecimal totalCredit;

    public boolean isBalanced() {
        return totalDebit.compareTo(totalCredit) == 0;
    }
}
