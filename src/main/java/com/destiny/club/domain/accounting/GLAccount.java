package com.destiny.club.domain.accounting;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "gl_accounts")
@Getter
@Setter
@NoArgsConstructor
public class GLAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String code;

    @Column(nullable = false, length = 150)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AccountType accountType;

    /** Control accounts are posted to automatically by the system (e.g. Cash, Member Savings, Loans Receivable). */
    @Column(nullable = false)
    private boolean systemAccount = false;

    @Column(nullable = false)
    private boolean active = true;

    @Column(length = 250)
    private String description;
}
