package com.finanzapp.lifehub.modules.finance.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration(proxyBeanMethods = false)
@EnableJpaRepositories(
        basePackages = "com.finanzapp.movements.repository",
        entityManagerFactoryRef = "movementsEntityManagerFactory",
        transactionManagerRef = "movementsTransactionManager"
)
public class MovementsJpaConfiguration {
}
