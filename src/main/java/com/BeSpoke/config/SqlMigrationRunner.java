package com.BeSpoke.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.Comparator;

/**
 * Applies every script in classpath:db/postgres/ in filename order on each boot,
 * after the seeders — no manual psql step. The scripts are idempotent by
 * construction (every statement keys off the row it changes), so re-running is
 * a no-op: the same philosophy as SeedRunner's per-record top-ups.
 *
 * PostgreSQL only: H2 (tests) is skipped. Hibernate ddl-auto still owns plain
 * column/table additions; these files carry what it cannot express — data
 * reshaping, catalogue seeds, backfills and CHECK-constraint rebuilds.
 *
 * Each file runs as one statement batch on a non-autocommit connection, so a
 * failure rolls the whole file back — and aborts startup, because serving
 * traffic on a half-migrated schema is worse than not starting.
 */
// ponytail: filename-ordered idempotent scripts, not versioned history; move to
// Flyway if scripts ever stop being safe to re-run.
@Component
@Order(10) // after SeedRunner(1) and MaterialLibrarySeeder(2): the catalogue
           // scripts attach to the seeded 'bespoke-living' vendor.
public class SqlMigrationRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SqlMigrationRunner.class);

    private final DataSource dataSource;

    public SqlMigrationRunner(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        try (Connection probe = dataSource.getConnection()) {
            String product = probe.getMetaData().getDatabaseProductName();
            if (!"PostgreSQL".equals(product)) {
                log.info("[SQL-MIGRATIONS] skipped — database is {}, scripts are PostgreSQL-only", product);
                return;
            }
        }
        Resource[] scripts = new PathMatchingResourcePatternResolver()
                .getResources("classpath:db/postgres/*.sql");
        Arrays.sort(scripts, Comparator.comparing(Resource::getFilename,
                Comparator.nullsLast(Comparator.naturalOrder())));
        for (Resource script : scripts) {
            String sql;
            try (var in = script.getInputStream()) {
                sql = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            }
            // One multi-statement execute: string literals may contain semicolons,
            // so the file is never split here — the driver parses it.
            try (Connection connection = dataSource.getConnection();
                 Statement statement = connection.createStatement()) {
                connection.setAutoCommit(false);
                try {
                    statement.execute(sql);
                    connection.commit();
                } catch (SQLException e) {
                    connection.rollback();
                    throw new IllegalStateException(
                            "Migration " + script.getFilename() + " failed — startup aborted", e);
                }
            }
            log.info("[SQL-MIGRATIONS] applied {}", script.getFilename());
        }
    }
}
