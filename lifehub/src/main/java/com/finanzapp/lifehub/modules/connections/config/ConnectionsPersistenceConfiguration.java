package com.finanzapp.lifehub.modules.connections.config;

import com.zaxxer.hikari.HikariDataSource;
import jakarta.persistence.EntityManagerFactory;
import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;
import java.util.Map;

@Configuration(proxyBeanMethods = false)
@EnableTransactionManagement
public class ConnectionsPersistenceConfiguration {

    @Bean
    @ConfigurationProperties("lifehub.datasource.connections")
    DataSourceProperties connectionsDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    HikariDataSource connectionsDataSource(
            @Qualifier("connectionsDataSourceProperties") DataSourceProperties properties) {
        return properties.initializeDataSourceBuilder()
                .type(HikariDataSource.class)
                .build();
    }

    @Bean(name = "connectionsEntityManagerFactory")
    LocalContainerEntityManagerFactoryBean connectionsEntityManagerFactory(
            @Qualifier("connectionsDataSource") DataSource dataSource,
            @org.springframework.beans.factory.annotation.Value("${lifehub.jpa.hibernate.ddl-auto:validate}") String ddlAuto) {
        var factory = new LocalContainerEntityManagerFactoryBean();
        factory.setDataSource(dataSource);
        factory.setPackagesToScan("com.finanzapp.lifehub.modules.connections.repository.entities");
        factory.setPersistenceUnitName("connections");
        var vendorAdapter = new HibernateJpaVendorAdapter();
        factory.setJpaVendorAdapter(vendorAdapter);
        factory.setJpaPropertyMap(Map.of(
                "hibernate.hbm2ddl.auto", ddlAuto,
                "hibernate.show_sql", "false",
                "hibernate.format_sql", "false"
        ));
        return factory;
    }

    @Bean(name = "connectionsTransactionManager")
    PlatformTransactionManager connectionsTransactionManager(
            @Qualifier("connectionsEntityManagerFactory") EntityManagerFactory entityManagerFactory) {
        return new JpaTransactionManager(entityManagerFactory);
    }

    @Bean(initMethod = "migrate")
    @ConditionalOnProperty(name = "lifehub.flyway.enabled", havingValue = "true", matchIfMissing = true)
    Flyway connectionsFlyway(@Qualifier("connectionsDataSource") DataSource dataSource) {
        return Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration/connections")
                .schemas("connections")
                .defaultSchema("connections")
                .table("connections_flyway_schema_history")
                .baselineOnMigrate(false)
                .cleanDisabled(true)
                .validateOnMigrate(true)
                .load();
    }
}
