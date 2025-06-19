package com.taco.managementsystem.model.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "ACCOUNT")
public class Account {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @OneToOne
    @JoinColumn(name = "USER_ID", nullable = false)
    private User user;
    @Column(name = "BALANCE", nullable = false, precision = 19, scale = 2)
    @PositiveOrZero
    private BigDecimal balance;
    @Column(name = "INITIAL_DEPOSIT", nullable = false, precision = 19, scale = 2)
    @Positive
    private BigDecimal initialDeposit;
    @Version
    private Long version;
}
