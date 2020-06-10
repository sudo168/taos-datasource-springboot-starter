package net.ewant.taos.pool2;

import net.ewant.taos.support.TaosConfigProperties;
import net.ewant.taos.support.TaosPool2ConfigProperties;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.logging.Logger;

public class TaosDataSource implements DataSource {

    static final org.slf4j.Logger logger = LoggerFactory.getLogger(TaosDataSource.class);

    private TaosConnectionPool connectionPool;

    public TaosDataSource(TaosConfigProperties configProperties, TaosPool2ConfigProperties pool2ConfigProperties){
        if(pool2ConfigProperties == null){
            pool2ConfigProperties = new TaosPool2ConfigProperties();
        }
        logger.info("Create connection pool on: {}", configProperties.getJdbcUrl());
        this.connectionPool = new TaosConnectionPool(configProperties, pool2ConfigProperties);
    }

    @Override
    public Connection getConnection() throws SQLException {
        return this.connectionPool.getResource();
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        return this.connectionPool.getResource();
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        return null;
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return false;
    }

    @Override
    public PrintWriter getLogWriter() throws SQLException {
        return null;
    }

    @Override
    public void setLogWriter(PrintWriter out) throws SQLException {

    }

    @Override
    public void setLoginTimeout(int seconds) throws SQLException {
    }

    @Override
    public int getLoginTimeout() throws SQLException {
        return 0;
    }

    @Override
    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        return null;
    }
}
