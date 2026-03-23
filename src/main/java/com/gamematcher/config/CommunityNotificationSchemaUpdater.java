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
public class CommunityNotificationSchemaUpdater implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        try {
            String databaseName = jdbcTemplate.queryForObject("SELECT DATABASE()", String.class);
            if (databaseName == null || databaseName.isBlank()) {
                return;
            }

            String dataType = jdbcTemplate.queryForObject(
                    """
                    SELECT DATA_TYPE
                    FROM information_schema.COLUMNS
                    WHERE TABLE_SCHEMA = ?
                      AND TABLE_NAME = 'community_notifications'
                      AND COLUMN_NAME = 'type'
                    """,
                    String.class,
                    databaseName
            );

            if (dataType != null && !"varchar".equalsIgnoreCase(dataType)) {
                jdbcTemplate.execute(
                        "ALTER TABLE community_notifications MODIFY COLUMN type VARCHAR(50) NOT NULL"
                );
                log.info("Updated community_notifications.type column to VARCHAR(50)");
            }
        } catch (Exception e) {
            log.debug("Skipped community_notifications.type schema update: {}", e.getMessage());
        }
    }
}
