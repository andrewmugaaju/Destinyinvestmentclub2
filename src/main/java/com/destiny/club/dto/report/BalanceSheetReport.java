package com.destiny.club.dto.report;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
@AllArgsConstructor
public class BalanceSheetReport {
    private final LocalDate asOfDate;
    private final List<LineItem> assets;
    private final List<LineItem> liabilities;
    private final List<LineItem> equity;
    private final BigDecimal netSurplus;
    private final BigDecimal totalAssets;
    private final BigDecimal totalLiabilities;
    private final BigDecimal totalEquity;

    public BigDecimal getTotalEquityIncludingSurplus() {
        return totalEquity.add(netSurplus);
    }

    public BigDecimal getTotalLiabilitiesAndEquity() {
        return totalLiabilities.add(getTotalEquityIncludingSurplus());
    }

    public boolean isBalanced() {
        return totalAssets.compareTo(getTotalLiabilitiesAndEquity()) == 0;
    }
}
