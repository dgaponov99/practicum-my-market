package com.github.dgaponov99.practicum.mymarket.payment.integration.service;

import com.github.dgaponov99.practicum.mymarket.payment.integration.PostgreSQLTestcontainer;
import liquibase.Liquibase;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.integration.spring.SpringLiquibase;
import liquibase.integration.spring.SpringResourceAccessor;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.context.ImportTestcontainers;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Testcontainers
@ImportTestcontainers(PostgreSQLTestcontainer.class)
public abstract class ServiceIT {

    @Autowired
    SpringLiquibase springLiquibase;

    @BeforeEach
    void setup() throws Exception {
        try (var liquibase = createLiquibase()) {
            liquibase.dropAll();
            liquibase.update();
        }
    }

    protected Liquibase createLiquibase() throws Exception {
        var connection = springLiquibase.getDataSource().getConnection();
        var database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(new JdbcConnection(connection));

        return new Liquibase(springLiquibase.getChangeLog(),
                new SpringResourceAccessor(springLiquibase.getResourceLoader()),
                database);
    }

}
