package org.fantom.web.config;

import org.fantom.repositories.widget.IdGenerator;
import org.fantom.repositories.widget.WidgetRepository;
import org.fantom.repository.InMemoryWidgetRepository;
import org.fantom.services.widget.WidgetService;
import org.fantom.web.repositories.widget.SqlWidgetRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.validation.annotation.Validated;

import java.util.Arrays;
import java.util.Objects;
import java.util.UUID;

@Configuration
@ConditionalOnProperty(value = "config.repositoryType", havingValue = "db")
@EnableJpaRepositories(value = "org.fantom.web.repositories")//, bootstrapMode = BootstrapMode.LAZY)
@Import({
        DataSourceAutoConfiguration.class,
        DataSourceTransactionManagerAutoConfiguration.class,
        HibernateJpaAutoConfiguration.class
})
class HibernateConfig {}

@Validated
@Configuration
@EnableConfigurationProperties
@ConfigurationProperties("config")
@Import(HibernateConfig.class)
public class Config {

    private enum RepositoryType {
        memory, db
    }

    private WidgetIdType widgetIdType;
    private RepositoryType repositoryType;

    @Lazy
    @Autowired
    private SqlWidgetRepository sqlWidgetRepository;

    public void setWidgetIdType(String widgetIdType) {
        try {
            this.widgetIdType = WidgetIdType.valueOf(widgetIdType);
        } catch (NullPointerException ignored) {
            this.widgetIdType = null;
        }
    }

    public void setRepositoryType(String repositoryType) {
        Objects.requireNonNull(repositoryType);
        this.repositoryType = RepositoryType.valueOf(repositoryType);;
    }

    @Bean
    public WidgetIdType idType() {
        if (this.widgetIdType == null && this.repositoryType != RepositoryType.db) {
            throw new IllegalArgumentException("You have to specify widget id type if non-db repo impl is used");
        }
        // use integer as default id type
        return widgetIdType == null ? WidgetIdType.integer : widgetIdType;
    }

    public IdGenerator<Integer> intIdGenerator() {
        return new IdGenerator<>() {
            private int nextValue = 0;

            @Override
            public Integer generate() {
                return nextValue++;
            }
        };
    }

    public IdGenerator<String> stringIdGenerator() {
        return () -> UUID.randomUUID().toString();
    }

    public IdGenerator<?> idGenerator() {
        switch (widgetIdType) {
            case integer:
                return intIdGenerator();
            case string:
                return stringIdGenerator();
            default:
                throw new RuntimeException("Invalid widget id type, expected one of" + Arrays.toString(WidgetIdType.values()) +", got " + widgetIdType);
        }
    }

    @Bean
    public WidgetRepository<?> widgetRepository() {
        System.out.println("creating widget repository by type " + repositoryType.name());
        switch (repositoryType) {
            case memory:
                return new InMemoryWidgetRepository<>(idGenerator());
            case db:
                return sqlWidgetRepository;
            default:
                throw new RuntimeException("Invalid widget repository type, expected one of "+ Arrays.toString(RepositoryType.values()) + ", got " + repositoryType);
        }
    }

    @Bean
    public <ID> WidgetService<ID> widgetService(@Qualifier("widgetRepository") WidgetRepository<ID> widgetRepository) {
        return new WidgetService<>(widgetRepository);
    }
}
