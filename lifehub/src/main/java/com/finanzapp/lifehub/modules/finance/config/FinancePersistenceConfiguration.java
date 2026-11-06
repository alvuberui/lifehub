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
    @ConfigurationProperties("lifehub.datasource.identity")
    DataSourceProperties identityDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    @Primary
    HikariDataSource identityDataSource(
            @Qualifier("identityDataSourceProperties") DataSourceProperties properties) {
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

    @Bean(name = "identityEntityManagerFactory")
    LocalContainerEntityManagerFactoryBean identityEntityManagerFactory(
            @Qualifier("identityDataSource") DataSource dataSource) {
        return entityManagerFactory(dataSource, "identity", "com.finanzapp.lifehub.modules.identity.user.repository.entities");
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

    @Bean(name = "identityTransactionManager")
    @Primary
    PlatformTransactionManager identityTransactionManager(
            @Qualifier("identityEntityManagerFactory") EntityManagerFactory entityManagerFactory) {
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
    Flyway identityFlyway(@Qualifier("identityDataSource") DataSource dataSource) {
        return flyway(dataSource, "classpath:db/migration/user", "identity", "identity_flyway_schema_history");
    }

    @Bean(initMethod = "migrate")
    @ConditionalOnProperty(name = "lifehub.flyway.enabled", havingValue = "true", matchIfMissing = true)
    Flyway movementsFlyway(@Qualifier("movementsDataSource") DataSource dataSource) {
        return flyway(dataSource, "classpath:db/migration/movements", "movements", "movements_flyway_schema_history");
    }

    @Bean(initMethod = "migrate")
    @ConditionalOnProperty(name = "lifehub.flyway.enabled", havingValue = "true", matchIfMissing = true)
    Flyway tagsFlyway(@Qualifier("tagsDataSource") DataSource dataSource) {
        return flyway(dataSource, "classpath:db/migration/tags", "tags", "tags_flyway_schema_history");
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

    private Flyway flyway(DataSource dataSource, String location, String schema, String historyTable) {
        return Flyway.configure()
                .dataSource(dataSource)
                .locations(location)
                .schemas(schema)
                .defaultSchema(schema)
                .table(historyTable)
                .baselineOnMigrate(false)
                .cleanDisabled(true)
                .validateOnMigrate(true)
                .load();
    }
}
