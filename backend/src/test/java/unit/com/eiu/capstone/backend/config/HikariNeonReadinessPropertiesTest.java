package unit.com.eiu.capstone.backend.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.util.Properties;

import org.junit.jupiter.api.Test;

class HikariNeonReadinessPropertiesTest {

    @Test
    void applicationPropertiesDeclareNeonReadinessWithoutRetuningPool() throws Exception {
        Properties properties = new Properties();
        try (InputStream in = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream("application.properties")) {
            assertTrue(in != null, "application.properties must be on the test classpath");
            properties.load(in);
        }

        assertEquals("120000", properties.getProperty("spring.datasource.hikari.keepalive-time"));
        assertEquals("30", properties.getProperty("spring.datasource.hikari.data-source-properties.socketTimeout"));
        assertEquals("true", properties.getProperty("spring.datasource.hikari.data-source-properties.tcpKeepAlive"));
        assertEquals("10", properties.getProperty("spring.datasource.hikari.maximum-pool-size"));
        assertEquals("2", properties.getProperty("spring.datasource.hikari.minimum-idle"));
        assertEquals("30000", properties.getProperty("spring.datasource.hikari.connection-timeout"));
        assertEquals("1800000", properties.getProperty("spring.datasource.hikari.max-lifetime"));
        assertFalse(properties.containsKey("spring.datasource.hikari.connection-test-query"));
        assertFalse(properties.containsKey("spring.datasource.hikari.validation-timeout"));
    }
}
