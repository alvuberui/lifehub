package com.finanzapp.user.repository.entities;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "users")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "username", nullable = false, unique = true)
    private String email;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "lastname", nullable = false)
    private String lastname;

    @Column(name = "birthdate", nullable = false)
    private String birthdate;

    @Column(name = "initial_wealth", nullable = false)
    private BigDecimal initialWealth;

    @Column(name = "monthly_saving_percentage", nullable = false)
    private BigDecimal monthlySavingPercentage;

    @Column(name = "monthly_necessary_expenses_percentage", nullable = false)
    private BigDecimal monthlyNecessaryExpensesPercentage;

    @Column(name = "monthly_discretionary_expenses_percentage", nullable = false)
    private BigDecimal monthlyDiscretionaryExpensesPercentage;

    @Column(name = "monthly_investment_percentage", nullable = false)
    private BigDecimal monthlyInvestmentPercentage;
}
