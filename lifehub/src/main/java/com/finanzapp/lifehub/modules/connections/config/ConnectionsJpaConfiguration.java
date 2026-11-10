package com.finanzapp.lifehub.modules.connections.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration(proxyBeanMethods = false)
@EnableJpaRepositories(
        basePackages = "com.finanzapp.lifehub.modules.connections.repository",
        entityManagerFactoryRef = "connectionsEntityManagerFactory",
        transactionManagerRef = "connectionsTransactionManager"
)
public class ConnectionsJpaConfiguration {
}
