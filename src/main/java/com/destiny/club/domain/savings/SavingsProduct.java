package com.destiny.club.domain.savings;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "savings_products")
@Getter
@Setter
@NoArgsConstructor
public class SavingsProduct {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String code;

    @Column(nullable = false, length = 120)
    private String name;

    /** Annual interest rate paid to the member, as a percentage, e.g. 5.00 for 5%. */
    @Column(nullable = false, precision = 6, scale = 3)
    private BigDecimal annualInterestRate = BigDecimal.ZERO;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal minOpeningBalance = BigDecimal.ZERO;

    @Column(nullable = false)
    private boolean active = true;

    @Column(length = 250)
    private String description;
}
