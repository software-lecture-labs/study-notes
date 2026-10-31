package com.lectures.business.design.sales.persistence;

import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.postgresql.ds.PGSimpleDataSource;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.postgresql.PostgreSQLContainer;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class PostgresOrderRepositoryTest extends OrderRepositoryContract {

    private static PostgreSQLContainer postgres;

    @BeforeAll
    static void startPostgres() {
        assumeTrue(DockerClientFactory.instance().isDockerAvailable(), "Docker is not available");
        postgres = new PostgreSQLContainer("postgres:17-alpine");
        postgres.start();
    }

    @AfterAll
    static void stopPostgres() {
        if (postgres != null) {
            postgres.stop();
        }
    }

    @Override
    protected OrderRepository createRepository() {
        PGSimpleDataSource dataSource = new PGSimpleDataSource();
        dataSource.setUrl(postgres.getJdbcUrl());
        dataSource.setUser(postgres.getUsername());
        dataSource.setPassword(postgres.getPassword());

        cleanAndMigrate(dataSource);
        JdbcOrderRepositoryTest.seedCustomers(dataSource);
        return new JdbcOrderRepository(dataSource);
    }

    private static void cleanAndMigrate(DataSource dataSource) {
        Flyway flyway = Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration/sales")
                .cleanDisabled(false)
                .load();
        flyway.clean();
        flyway.migrate();
    }
}
