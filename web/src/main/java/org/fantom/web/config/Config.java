package org.fantom.web.config;

import org.fantom.repositories.widget.IdGenerator;
import org.fantom.repositories.widget.WidgetRepository;
import org.fantom.repository.InMemoryWidgetRepository;
import org.fantom.services.widget.WidgetService;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import java.util.Arrays;
import java.util.Objects;
import java.util.UUID;

enum WidgetIdType {
    integer, string
}

enum RepositoryType {
    memory, db
}

@Validated
@Configuration
@EnableConfigurationProperties
@ConfigurationProperties("config")
public class Config {

    private WidgetIdType widgetIdType;
    private RepositoryType repositoryType;

    public void setWidgetIdType(String widgetIdType) {
        Objects.requireNonNull(widgetIdType);
        this.widgetIdType = WidgetIdType.valueOf(widgetIdType);
    }

    public void setRepositoryType(String repositoryType) {
        Objects.requireNonNull(repositoryType);
        this.repositoryType = RepositoryType.valueOf(repositoryType);;
    }

    //    @Bean
    public IdGenerator<Integer> intIdGenerator() {
        return new IdGenerator<>() {
            private int nextValue = 0;

            @Override
            public Integer generate() {
                return nextValue++;
            }
        };
    }

    //    @Bean
    public IdGenerator<String> stringIdGenerator() {
        return () -> UUID.randomUUID().toString();
    }

    @Bean
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
    public <ID> WidgetRepository<ID> inMemoryWidgetRepository(IdGenerator<ID> idGenerator) {
        return new InMemoryWidgetRepository<>(idGenerator);
    }

    @Bean
    public WidgetRepository<?> widgetRepository() {
        switch (repositoryType) {
            case memory:
                return new InMemoryWidgetRepository<>(idGenerator());
            case db:
                throw new RuntimeException("Db repository is not implemented");
            default:
                throw new RuntimeException("Invalid widget repository type, expected one of "+ Arrays.toString(RepositoryType.values()) + ", got " + repositoryType);
        }
    }

    @Bean
    public <ID> WidgetService<ID> widgetService(WidgetRepository<ID> repository) {
        return new WidgetService<>(repository);
    }
}
