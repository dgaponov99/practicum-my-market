package com.github.dgaponov99.practicum.mymarket.integration.service;

import com.github.dgaponov99.practicum.mymarket.integration.PostgreSQLTestcontainer;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.context.ImportTestcontainers;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Testcontainers;

@DataJpaTest
@ComponentScan(basePackages = "com.github.dgaponov99.practicum.mymarket.service")
@Testcontainers
@ImportTestcontainers(PostgreSQLTestcontainer.class)
@Transactional
public abstract class ServiceIT {

}
