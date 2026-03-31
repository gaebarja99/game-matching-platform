package com.gamematcher.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdminMileageSchemaUpdater implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        try {
            String databaseName = jdbcTemplate.queryForObject("SELECT DATABASE()", String.class);
            if (databaseName == null || databaseName.isBlank()) {
                return;
            }

            normalizeTypeColumn(databaseName, "notifications");
            normalizeTypeColumn(databaseName, "mileage_purchases");
        } catch (Exception e) {
            log.debug("Skipped admin mileage schema update: {}", e.getMessage());
        }
    }

    private void normalizeTypeColumn(String databaseName, String tableName) {
        String dataType = jdbcTemplate.queryForObject(
                """
                SELECT DATA_TYPE
                FROM information_schema.COLUMNS
                WHERE TABLE_SCHEMA = ?
                  AND TABLE_NAME = ?
                  AND COLUMN_NAME = 'type'
                """,
                String.class,
                databaseName,
                tableName
        );

        if (dataType == null || "varchar".equalsIgnoreCase(dataType)) {
            return;
        }

        jdbcTemplate.execute("ALTER TABLE " + tableName + " MODIFY COLUMN type VARCHAR(50) NOT NULL");
        log.info("Updated {}.type column to VARCHAR(50)", tableName);
    }
}
