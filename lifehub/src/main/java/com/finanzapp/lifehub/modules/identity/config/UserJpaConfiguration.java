package com.finanzapp.lifehub.modules.identity.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration(proxyBeanMethods = false)
@EnableJpaRepositories(
        basePackages = "com.finanzapp.lifehub.modules.identity.user.repository",
        entityManagerFactoryRef = "identityEntityManagerFactory",
        transactionManagerRef = "identityTransactionManager"
)
public class UserJpaConfiguration {
}
