package com.destiny.club.dto.report;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@AllArgsConstructor
public class LineItem {
    private final String code;
    private final String name;
    private final BigDecimal amount;
}
