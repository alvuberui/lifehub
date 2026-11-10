package com.finanzapp.lifehub.modules.home.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration(proxyBeanMethods = false)
@EnableJpaRepositories(
        basePackages = "com.finanzapp.lifehub.modules.home",
        entityManagerFactoryRef = "homeEntityManagerFactory",
        transactionManagerRef = "homeTransactionManager"
)
public class HomeJpaConfiguration {
}
