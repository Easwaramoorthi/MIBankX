package com.mi.bankx.model;

import lombok.*;
import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "accounts")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING) // Ensures "SAVINGS" or "CURRENT" stored as a string
    @Column(nullable = false)
    private AccountType type;

    @Column(nullable = false)
    private BigDecimal balance;

    @ManyToOne // Many accounts belong to one customer
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;
}