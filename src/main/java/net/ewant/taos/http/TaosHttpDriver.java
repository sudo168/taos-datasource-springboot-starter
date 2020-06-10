package net.ewant.taos.http;

import com.taosdata.jdbc.TSDBConstants;

import java.sql.*;
import java.util.Properties;
import java.util.logging.Logger;

public class TaosHttpDriver implements Driver {

    private static final String URL_PREFIX = "jdbc:TAOS://";

    @Override
    public Connection connect(String url, Properties info) throws SQLException {
        if (url == null) {
            throw new SQLException(TSDBConstants.WrapErrMsg("url is not set!"));
        }
        if(!url.toLowerCase().startsWith(URL_PREFIX.toLowerCase())){
            return null;
        }
        return new TaosHttpConnection(url, info);
    }

    @Override
    public boolean acceptsURL(String url) throws SQLException {
        return true;
    }

    @Override
    public DriverPropertyInfo[] getPropertyInfo(String url, Properties info) throws SQLException {
        return new DriverPropertyInfo[0];
    }

    @Override
    public int getMajorVersion() {
        return 1;
    }

    @Override
    public int getMinorVersion() {
        return 1;
    }

    @Override
    public boolean jdbcCompliant() {
        return false;
    }

    @Override
    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        return null;
    }
}
