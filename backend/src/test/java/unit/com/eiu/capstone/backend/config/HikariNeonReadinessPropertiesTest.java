package unit.com.eiu.capstone.backend.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.util.Properties;

import org.junit.jupiter.api.Test;

class HikariNeonReadinessPropertiesTest {

    @Test
    void applicationPropertiesDeclareSleepFriendlyPoolAndNeonSocketTimeout() throws Exception {
        Properties properties = new Properties();
        try (InputStream in = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream("application.properties")) {
            assertTrue(in != null, "application.properties must be on the test classpath");
            properties.load(in);
        }

        // Pool drops to zero when idle so Railway Serverless can sleep (no Neon keepalive chatter).
        assertEquals("${HIKARI_MINIMUM_IDLE:0}", properties.getProperty("spring.datasource.hikari.minimum-idle"));
        assertEquals("${HIKARI_IDLE_TIMEOUT:10000}", properties.getProperty("spring.datasource.hikari.idle-timeout"));
        assertEquals("${HIKARI_KEEPALIVE_TIME:0}", properties.getProperty("spring.datasource.hikari.keepalive-time"));
        assertEquals("${HIKARI_TCP_KEEPALIVE:false}",
                properties.getProperty("spring.datasource.hikari.data-source-properties.tcpKeepAlive"));
        assertEquals("30", properties.getProperty("spring.datasource.hikari.data-source-properties.socketTimeout"));
        // VARCHAR-mapped Java enums + reWriteBatchedInserts need unspecified string params
        // so Postgres can cast into native enums (testcase_type, assertion_kind, …).
        assertEquals("unspecified",
                properties.getProperty("spring.datasource.hikari.data-source-properties.stringtype"));
        assertEquals("true",
                properties.getProperty("spring.datasource.hikari.data-source-properties.reWriteBatchedInserts"));
        assertEquals("${HIKARI_MAXIMUM_POOL_SIZE:5}",
                properties.getProperty("spring.datasource.hikari.maximum-pool-size"));
        assertEquals("30000", properties.getProperty("spring.datasource.hikari.connection-timeout"));
        assertEquals("1800000", properties.getProperty("spring.datasource.hikari.max-lifetime"));
        assertEquals("${APP_DEADLINE_REMINDER_ENABLED:true}",
                properties.getProperty("app.deadline-reminder.enabled"));
        assertEquals("${APP_DEADLINE_REMINDER_FIXED_RATE_MS:3600000}",
                properties.getProperty("app.deadline-reminder.fixed-rate-ms"));
        assertFalse(properties.containsKey("spring.datasource.hikari.connection-test-query"));
        assertFalse(properties.containsKey("spring.datasource.hikari.validation-timeout"));
    }
}
