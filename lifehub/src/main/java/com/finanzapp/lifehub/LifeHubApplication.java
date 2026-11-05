package com.finanzapp.lifehub;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Punto de entrada único del monolito modular de LifeHub.
 *
 * <p>FinanzApp es el primer módulo funcional de LifeHub. Sus controladores,
 * servicios y persistencia se ejecutan dentro de este único proceso.</p>
 */
@SpringBootApplication(
        scanBasePackages = {
                "com.finanzapp.lifehub",
                "com.finanzapp.user.controller",
                "com.finanzapp.user.service",
                "com.finanzapp.user.repository",
                "com.finanzapp.user.exception",
                "com.finanzapp.movements.controller",
                "com.finanzapp.movements.service",
                "com.finanzapp.movements.repository",
                "com.finanzapp.movements.exception",
                "com.finanzapp.tags.controller",
                "com.finanzapp.tags.service",
                "com.finanzapp.tags.repository",
                "com.finanzapp.tags.exception"
        },
        exclude = {
                DataSourceAutoConfiguration.class,
                DataSourceTransactionManagerAutoConfiguration.class,
                HibernateJpaAutoConfiguration.class,
                JpaRepositoriesAutoConfiguration.class,
                FlywayAutoConfiguration.class
        }
)
@EnableScheduling
public class LifeHubApplication {

    public static void main(String[] args) {
        SpringApplication.run(LifeHubApplication.class, args);
    }
}
