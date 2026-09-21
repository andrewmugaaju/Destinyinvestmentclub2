package com.destiny.club.dto.report;

import com.destiny.club.domain.client.Client;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
@AllArgsConstructor
public class MemberStatementReport {
    private final Client client;
    private final LocalDate fromDate;
    private final LocalDate toDate;
    private final BigDecimal openingSavingsBalance;
    private final BigDecimal openingLoanBalance;
    private final List<MemberStatementRow> rows;
    private final BigDecimal closingSavingsBalance;
    private final BigDecimal closingLoanBalance;
}
