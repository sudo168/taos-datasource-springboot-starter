package net.ewant.iotdb;

import net.ewant.iotdb.support.IoTdbDataSourceAutoConfiguration;
import net.ewant.taos.support.TaosConfigProperties;
import net.ewant.taos.support.TaosDataSourceConfiguredCondition;
import net.ewant.taos.support.TaosDataSourceAutoConfiguration;
import org.junit.Before;
import org.junit.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.SystemEnvironmentPropertySource;

import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.util.Collections;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class IoTdbDataSourceTest {

    private static final String URL = "jdbc:mockiotdb://localhost:6667/";

    @Before
    public void resetDriverState() throws Exception {
        Class.forName(MockIoTdbDriver.class.getName());
        MockIoTdbDriver.lastUrl.set(null);
        MockIoTdbDriver.lastProperties.set(null);
    }

    @Test
    public void connectsWithConfiguredCredentialsAndProperties() throws Exception {
        IoTdbConfigProperties config = config();
        config.setUsername("config-user");
        config.setPassword("config-password");
        config.getProperties().put("fetchSize", "512");

        IoTdbDataSource dataSource = new IoTdbDataSource(config);
        try (Connection connection = dataSource.getConnection()) {
            assertNotNull(connection);
        }

        assertEquals(URL, MockIoTdbDriver.lastUrl.get());
        Properties actual = MockIoTdbDriver.lastProperties.get();
        assertNotNull(actual);
        assertEquals("config-user", actual.getProperty("user"));
        assertEquals("config-password", actual.getProperty("password"));
        assertEquals("512", actual.getProperty("fetchSize"));
    }

    @Test
    public void connectionCredentialsOverrideConfiguredCredentials() throws Exception {
        IoTdbConfigProperties config = config();
        config.setUsername("config-user");
        config.setPassword("config-password");
        IoTdbDataSource dataSource = new IoTdbDataSource(config);

        try (Connection connection = dataSource.getConnection("request-user", "request-password")) {
            assertNotNull(connection);
        }

        assertEquals("request-user", MockIoTdbDriver.lastProperties.get().getProperty("user"));
        assertEquals("request-password", MockIoTdbDriver.lastProperties.get().getProperty("password"));
    }

    @Test
    public void requiresJdbcUrl() {
        try {
            new IoTdbDataSource(new IoTdbConfigProperties());
        } catch (IllegalArgumentException exception) {
            assertTrue(exception.getMessage().contains("spring.datasource.iotdb.url"));
            return;
        }
        throw new AssertionError("Expected a missing URL to be rejected");
    }

    @Test
    public void defaultJdbcDriverIsAvailable() throws Exception {
        Class<?> driver = Class.forName(new IoTdbConfigProperties().getDriverClassName());
        assertTrue(Driver.class.isAssignableFrom(driver));
    }

    @Test
    public void loginTimeoutUsesDriverManagerSetting() throws Exception {
        int originalTimeout = DriverManager.getLoginTimeout();
        try {
            IoTdbDataSource dataSource = new IoTdbDataSource(config());
            dataSource.setLoginTimeout(7);
            assertEquals(7, dataSource.getLoginTimeout());
        } finally {
            DriverManager.setLoginTimeout(originalTimeout);
        }
    }

    @Test
    public void autoConfigurationIsOptInAndDoesNotCreateTaosDataSource() {
        try (AnnotationConfigApplicationContext context = context(null)) {
            assertFalse(context.containsBean("iotdbDataSource"));
            assertFalse(context.containsBean("taosDataSource"));
        }

        try (AnnotationConfigApplicationContext context = context(URL)) {
            assertTrue(context.containsBean("iotdbDataSource"));
            assertNotNull(context.getBean("iotdbDataSource", IoTdbDataSource.class));
            assertFalse(context.containsBean("taosDataSource"));
            assertEquals(0, context.getBeanFactory().getBeanNamesForType(
                    net.ewant.taos.support.TaosConfigProperties.class, false, false).length);
        }
    }

    @Test
    public void taosAutoConfigurationRecognizesEnvironmentVariableProperties() {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        context.getEnvironment().getPropertySources().addFirst(new SystemEnvironmentPropertySource(
                "taos-test-env",
                Collections.<String, Object>singletonMap(
                        "SPRING_DATASOURCE_TAOS_JDBCURL", "jdbc:TAOS://localhost:6030/example")));
        context.register(TaosConditionTestConfiguration.class);
        try (AnnotationConfigApplicationContext managedContext = context) {
            managedContext.refresh();
            assertNotNull(managedContext.getBean(TaosConfigProperties.class));
        }
    }

    private static AnnotationConfigApplicationContext context(String url) {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        if (url != null) {
            context.getEnvironment().getPropertySources().addFirst(new MapPropertySource(
                    "iotdb-test", Collections.<String, Object>singletonMap("spring.datasource.iotdb.url", url)));
        }
        context.register(IoTdbDataSourceAutoConfiguration.class, TaosDataSourceAutoConfiguration.class);
        context.refresh();
        return context;
    }

    private static IoTdbConfigProperties config() {
        IoTdbConfigProperties config = new IoTdbConfigProperties();
        config.setUrl(URL);
        config.setDriverClassName(MockIoTdbDriver.class.getName());
        return config;
    }

    public static class MockIoTdbDriver implements Driver {

        private static final AtomicReference<String> lastUrl = new AtomicReference<>();
        private static final AtomicReference<Properties> lastProperties = new AtomicReference<>();

        static {
            try {
                DriverManager.registerDriver(new MockIoTdbDriver());
            } catch (SQLException exception) {
                throw new ExceptionInInitializerError(exception);
            }
        }

        @Override
        public Connection connect(String url, Properties info) {
            if (!acceptsURL(url)) {
                return null;
            }
            lastUrl.set(url);
            Properties copy = new Properties();
            copy.putAll(info);
            lastProperties.set(copy);
            return (Connection) Proxy.newProxyInstance(
                    Connection.class.getClassLoader(),
                    new Class<?>[]{Connection.class},
                    (proxy, method, args) -> {
                        if ("isClosed".equals(method.getName())) {
                            return false;
                        }
                        if ("toString".equals(method.getName())) {
                            return "MockIoTdbConnection";
                        }
                        return null;
                    });
        }

        @Override
        public boolean acceptsURL(String url) {
            return url != null && url.startsWith("jdbc:mockiotdb:");
        }

        @Override
        public DriverPropertyInfo[] getPropertyInfo(String url, Properties info) {
            return new DriverPropertyInfo[0];
        }

        @Override
        public int getMajorVersion() {
            return 1;
        }

        @Override
        public int getMinorVersion() {
            return 0;
        }

        @Override
        public boolean jdbcCompliant() {
            return false;
        }

        @Override
        public Logger getParentLogger() {
            return Logger.getLogger("net.ewant.iotdb.test");
        }
    }

    @Configuration
    @Conditional(TaosDataSourceConfiguredCondition.class)
    @EnableConfigurationProperties(TaosConfigProperties.class)
    public static class TaosConditionTestConfiguration {
    }
}
