package com.destiny.club.dto.report;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
@AllArgsConstructor
public class IncomeStatementReport {
    private final LocalDate fromDate;
    private final LocalDate toDate;
    private final List<LineItem> income;
    private final List<LineItem> expenses;
    private final BigDecimal totalIncome;
    private final BigDecimal totalExpenses;

    public BigDecimal getNetSurplus() {
        return totalIncome.subtract(totalExpenses);
    }
}
