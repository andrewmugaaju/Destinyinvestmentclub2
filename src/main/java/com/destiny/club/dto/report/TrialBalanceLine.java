package com.destiny.club.dto.report;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@AllArgsConstructor
public class TrialBalanceLine {
    private final String code;
    private final String name;
    private final String accountType;
    private final BigDecimal debit;
    private final BigDecimal credit;
}
