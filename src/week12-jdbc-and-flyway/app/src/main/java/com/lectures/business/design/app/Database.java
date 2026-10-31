package com.lectures.business.design.app;

import java.util.Map;
import java.util.UUID;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.h2.jdbcx.JdbcDataSource;
import org.postgresql.ds.PGSimpleDataSource;

final class Database {

    private Database() {
    }

    static DataSource fromEnvironment(Map<String, String> env) {
        String url = env.get("SALES_DB_URL");
        if (url == null || url.isBlank()) {
            return h2InMemory();
        }
        PGSimpleDataSource postgres = new PGSimpleDataSource();
        postgres.setUrl(url);
        postgres.setUser(env.getOrDefault("SALES_DB_USER", "johndoe"));
        postgres.setPassword(env.getOrDefault("SALES_DB_PASSWORD", ""));
        return postgres;
    }

    static DataSource h2InMemory() {
        JdbcDataSource h2 = new JdbcDataSource();
        h2.setURL("jdbc:h2:mem:sales-" + UUID.randomUUID()
                + ";MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH;DB_CLOSE_DELAY=-1");
        h2.setUser("sa");
        return h2;
    }

    static DataSource migrated(DataSource dataSource) {
        Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration/sales")
                .load()
                .migrate();
        return dataSource;
    }
}
