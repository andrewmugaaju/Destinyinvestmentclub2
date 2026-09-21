package com.destiny.club.domain.loan;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "loan_products")
@Getter
@Setter
@NoArgsConstructor
public class LoanProduct {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String code;

    @Column(nullable = false, length = 120)
    private String name;

    /** Annual nominal interest rate as a percentage, e.g. 12.00 for 12% per annum. */
    @Column(nullable = false, precision = 6, scale = 3)
    private BigDecimal annualInterestRate;

    @Column(nullable = false)
    private Integer defaultTermMonths;

    /** Standard non-refundable fee charged when a loan under this product is disbursed. */
    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal applicationFeeAmount = BigDecimal.ZERO;

    @Column(nullable = false)
    private boolean active = true;

    @Column(length = 250)
    private String description;
}
