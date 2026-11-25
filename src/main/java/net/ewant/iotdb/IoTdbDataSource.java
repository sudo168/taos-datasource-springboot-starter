package net.ewant.iotdb;

import javax.sql.DataSource;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * Lightweight IoTDB JDBC DataSource. Connections are owned by callers; this
 * class does not create a pool or keep connections open between requests.
 */
public class IoTdbDataSource implements DataSource, AutoCloseable {

    private final IoTdbConfigProperties config;

    public IoTdbDataSource(IoTdbConfigProperties config) {
        if (config == null) {
            throw new IllegalArgumentException("IoTDB configuration is required");
        }
        if (config.getUrl() == null || config.getUrl().trim().isEmpty()) {
            throw new IllegalArgumentException("spring.datasource.iotdb.url is required");
        }
        this.config = config;
    }

    @Override
    public Connection getConnection() throws SQLException {
        return connect(config.getUsername(), config.getPassword());
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        return connect(username, password);
    }

    private Connection connect(String username, String password) throws SQLException {
        loadDriver();
        Properties properties = new Properties();
        for (Map.Entry<String, String> entry : config.getProperties().entrySet()) {
            if (entry.getKey() != null && entry.getValue() != null) {
                properties.setProperty(entry.getKey(), entry.getValue());
            }
        }
        if (username != null) {
            properties.setProperty("user", username);
        }
        if (password != null) {
            properties.setProperty("password", password);
        }
        return DriverManager.getConnection(config.getUrl(), properties);
    }

    private void loadDriver() throws SQLException {
        String driverClassName = config.getDriverClassName();
        if (driverClassName == null || driverClassName.trim().isEmpty()) {
            return;
        }
        try {
            Class.forName(driverClassName);
        } catch (ClassNotFoundException exception) {
            throw new SQLException("IoTDB JDBC driver class not found: " + driverClassName, exception);
        }
    }

    @Override
    public PrintWriter getLogWriter() throws SQLException {
        return DriverManager.getLogWriter();
    }

    @Override
    public void setLogWriter(PrintWriter out) throws SQLException {
        DriverManager.setLogWriter(out);
    }

    @Override
    public void setLoginTimeout(int seconds) throws SQLException {
        if (seconds < 0) {
            throw new SQLException("login timeout must not be negative");
        }
        DriverManager.setLoginTimeout(seconds);
    }

    @Override
    public int getLoginTimeout() throws SQLException {
        return DriverManager.getLoginTimeout();
    }

    @Override
    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        return Logger.getLogger("org.apache.iotdb.jdbc");
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        if (iface != null && iface.isInstance(this)) {
            return iface.cast(this);
        }
        throw new SQLException("IoTDB DataSource does not wrap " + (iface == null ? "null" : iface.getName()));
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) {
        return iface != null && iface.isInstance(this);
    }

    @Override
    public void close() {
        // This DataSource does not retain connections or background resources.
    }
}
