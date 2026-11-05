package com.finanzapp.lifehub.modules.finance.config;

import com.zaxxer.hikari.HikariDataSource;
import jakarta.persistence.EntityManagerFactory;
import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;
import java.util.Map;

@Configuration(proxyBeanMethods = false)
@EnableTransactionManagement
public class FinancePersistenceConfiguration {

    @Value("${lifehub.jpa.hibernate.ddl-auto:validate}")
    private String hibernateDdlAuto;

    @Bean
    @Primary
    @ConfigurationProperties("lifehub.datasource.user")
    DataSourceProperties userDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    @Primary
    HikariDataSource userDataSource(
            @Qualifier("userDataSourceProperties") DataSourceProperties properties) {
        return properties.initializeDataSourceBuilder()
                .type(HikariDataSource.class)
                .build();
    }

    @Bean
    @ConfigurationProperties("lifehub.datasource.movements")
    DataSourceProperties movementsDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    HikariDataSource movementsDataSource(
            @Qualifier("movementsDataSourceProperties") DataSourceProperties properties) {
        return properties.initializeDataSourceBuilder()
                .type(HikariDataSource.class)
                .build();
    }

    @Bean
    @ConfigurationProperties("lifehub.datasource.tags")
    DataSourceProperties tagsDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    HikariDataSource tagsDataSource(
            @Qualifier("tagsDataSourceProperties") DataSourceProperties properties) {
        return properties.initializeDataSourceBuilder()
                .type(HikariDataSource.class)
                .build();
    }

    @Bean(name = "userEntityManagerFactory")
    LocalContainerEntityManagerFactoryBean userEntityManagerFactory(
            @Qualifier("userDataSource") DataSource dataSource) {
        return entityManagerFactory(dataSource, "user", "com.finanzapp.user.repository.entities");
    }

    @Bean(name = "movementsEntityManagerFactory")
    LocalContainerEntityManagerFactoryBean movementsEntityManagerFactory(
            @Qualifier("movementsDataSource") DataSource dataSource) {
        return entityManagerFactory(dataSource, "movements", "com.finanzapp.movements.repository.entities");
    }

    @Bean(name = "tagsEntityManagerFactory")
    LocalContainerEntityManagerFactoryBean tagsEntityManagerFactory(
            @Qualifier("tagsDataSource") DataSource dataSource) {
        return entityManagerFactory(dataSource, "tags", "com.finanzapp.tags.repository.entities");
    }

    @Bean(name = "userTransactionManager")
    @Primary
    PlatformTransactionManager userTransactionManager(
            @Qualifier("userEntityManagerFactory") EntityManagerFactory entityManagerFactory) {
        return new JpaTransactionManager(entityManagerFactory);
    }

    @Bean(name = "movementsTransactionManager")
    PlatformTransactionManager movementsTransactionManager(
            @Qualifier("movementsEntityManagerFactory") EntityManagerFactory entityManagerFactory) {
        return new JpaTransactionManager(entityManagerFactory);
    }

    @Bean(name = "tagsTransactionManager")
    PlatformTransactionManager tagsTransactionManager(
            @Qualifier("tagsEntityManagerFactory") EntityManagerFactory entityManagerFactory) {
        return new JpaTransactionManager(entityManagerFactory);
    }

    @Bean(initMethod = "migrate")
    @ConditionalOnProperty(name = "lifehub.flyway.enabled", havingValue = "true", matchIfMissing = true)
    Flyway userFlyway(@Qualifier("userDataSource") DataSource dataSource) {
        return flyway(dataSource, "classpath:db/migration/user");
    }

    @Bean(initMethod = "migrate")
    @ConditionalOnProperty(name = "lifehub.flyway.enabled", havingValue = "true", matchIfMissing = true)
    Flyway movementsFlyway(@Qualifier("movementsDataSource") DataSource dataSource) {
        return flyway(dataSource, "classpath:db/migration/movements");
    }

    @Bean(initMethod = "migrate")
    @ConditionalOnProperty(name = "lifehub.flyway.enabled", havingValue = "true", matchIfMissing = true)
    Flyway tagsFlyway(@Qualifier("tagsDataSource") DataSource dataSource) {
        return flyway(dataSource, "classpath:db/migration/tags");
    }

    private LocalContainerEntityManagerFactoryBean entityManagerFactory(
            DataSource dataSource,
            String persistenceUnit,
            String entityPackage) {
        var factory = new LocalContainerEntityManagerFactoryBean();
        factory.setDataSource(dataSource);
        factory.setPackagesToScan(entityPackage);
        factory.setPersistenceUnitName(persistenceUnit);

        var vendorAdapter = new HibernateJpaVendorAdapter();
        factory.setJpaVendorAdapter(vendorAdapter);
        factory.setJpaPropertyMap(Map.of(
                "hibernate.hbm2ddl.auto", hibernateDdlAuto,
                "hibernate.show_sql", "false",
                "hibernate.format_sql", "false"
        ));
        return factory;
    }

    private Flyway flyway(DataSource dataSource, String location) {
        return Flyway.configure()
                .dataSource(dataSource)
                .locations(location)
                .baselineOnMigrate(true)
                .baselineVersion("1")
                .validateOnMigrate(true)
                .load();
    }
}
