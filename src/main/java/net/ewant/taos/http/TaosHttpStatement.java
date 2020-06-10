package net.ewant.taos.http;

import com.taosdata.jdbc.TSDBConstants;

import java.sql.*;

public class TaosHttpStatement implements Statement {

    final TaosHttpConnection connector;

    TaosHttpResultSet resultSet;

    private boolean isClosed;

    public TaosHttpStatement(TaosHttpConnection connector) {
        this.connector = connector;
    }

    private void doQuery(String sql) throws SQLException {
        if (isClosed) {
            throw new SQLException("Invalid method call on a closed statement.");
        }
        try {
            StringBuffer tableBuffer = new StringBuffer();
            StringBuffer columnBuffer = new StringBuffer();
            String result = this.connector.executeQuery(sql, tableBuffer, columnBuffer);
            String[] columns = columnBuffer.toString().trim().split(",");
            resultSet = new TaosHttpResultSet(connector, tableBuffer.toString(), columns, result);
        } catch (SQLException e) {
            throw e;
        }
    }

    @Override
    public ResultSet executeQuery(String sql) throws SQLException {
        doQuery(sql);
        return resultSet;
    }

    @Override
    public int executeUpdate(String sql) throws SQLException {
        doQuery(sql);
        return resultSet.httpResult.getRows();
    }

    @Override
    public boolean execute(String sql) throws SQLException {
        doQuery(sql);
        return resultSet.httpResult.ok();
    }

    @Override
    public ResultSet getResultSet() throws SQLException {
        if (isClosed) {
            throw new SQLException("Invalid method call on a closed statement.");
        }
        return resultSet;
    }

    @Override
    public void close() throws SQLException {
        isClosed = true;
        resultSet.close();
    }

    @Override
    public int getMaxFieldSize() throws SQLException {
        throw new SQLException(TSDBConstants.UNSUPPORT_METHOD_EXCEPTIONZ_MSG);
    }

    @Override
    public void setMaxFieldSize(int max) throws SQLException {
        throw new SQLException(TSDBConstants.UNSUPPORT_METHOD_EXCEPTIONZ_MSG);
    }

    @Override
    public int getMaxRows() throws SQLException {
        // always set maxRows to zero, meaning unlimitted rows in a resultSet
        return 0;
    }

    @Override
    public void setMaxRows(int max) throws SQLException {
    }

    @Override
    public void setEscapeProcessing(boolean enable) throws SQLException {
        throw new SQLException(TSDBConstants.UNSUPPORT_METHOD_EXCEPTIONZ_MSG);
    }

    @Override
    public int getQueryTimeout() throws SQLException {
        return 0;
    }

    @Override
    public void setQueryTimeout(int seconds) throws SQLException {
    }

    @Override
    public void cancel() throws SQLException {
        throw new SQLException(TSDBConstants.UNSUPPORT_METHOD_EXCEPTIONZ_MSG);
    }

    @Override
    public SQLWarning getWarnings() throws SQLException {
        throw new SQLException(TSDBConstants.UNSUPPORT_METHOD_EXCEPTIONZ_MSG);
    }

    @Override
    public void clearWarnings() throws SQLException {
        throw new SQLException(TSDBConstants.UNSUPPORT_METHOD_EXCEPTIONZ_MSG);
    }

    @Override
    public void setCursorName(String name) throws SQLException {
        throw new SQLException(TSDBConstants.UNSUPPORT_METHOD_EXCEPTIONZ_MSG);
    }

    @Override
    public int getUpdateCount() throws SQLException {
        return resultSet.httpResult.getRows();
    }

    @Override
    public boolean getMoreResults() throws SQLException {
        throw new SQLException(TSDBConstants.UNSUPPORT_METHOD_EXCEPTIONZ_MSG);
    }

    @Override
    public void setFetchDirection(int direction) throws SQLException {
        throw new SQLException(TSDBConstants.UNSUPPORT_METHOD_EXCEPTIONZ_MSG);
    }

    @Override
    public int getFetchDirection() throws SQLException {
        throw new SQLException(TSDBConstants.UNSUPPORT_METHOD_EXCEPTIONZ_MSG);
    }

    @Override
    public void setFetchSize(int rows) throws SQLException {
    }

    @Override
    public int getFetchSize() throws SQLException {
        return 1024;
    }

    @Override
    public int getResultSetConcurrency() throws SQLException {
        throw new SQLException(TSDBConstants.UNSUPPORT_METHOD_EXCEPTIONZ_MSG);
    }

    @Override
    public int getResultSetType() throws SQLException {
        throw new SQLException(TSDBConstants.UNSUPPORT_METHOD_EXCEPTIONZ_MSG);
    }

    @Override
    public void addBatch(String sql) throws SQLException {
        throw new SQLException(TSDBConstants.UNSUPPORT_METHOD_EXCEPTIONZ_MSG);
    }

    @Override
    public void clearBatch() throws SQLException {
        throw new SQLException(TSDBConstants.UNSUPPORT_METHOD_EXCEPTIONZ_MSG);
    }

    @Override
    public int[] executeBatch() throws SQLException {
        throw new SQLException(TSDBConstants.UNSUPPORT_METHOD_EXCEPTIONZ_MSG);
    }

    @Override
    public Connection getConnection() throws SQLException {
        throw new SQLException(TSDBConstants.UNSUPPORT_METHOD_EXCEPTIONZ_MSG);
    }

    @Override
    public boolean getMoreResults(int current) throws SQLException {
        throw new SQLException(TSDBConstants.UNSUPPORT_METHOD_EXCEPTIONZ_MSG);
    }

    @Override
    public ResultSet getGeneratedKeys() throws SQLException {
        throw new SQLException(TSDBConstants.UNSUPPORT_METHOD_EXCEPTIONZ_MSG);
    }

    @Override
    public int executeUpdate(String sql, int autoGeneratedKeys) throws SQLException {
        throw new SQLException(TSDBConstants.UNSUPPORT_METHOD_EXCEPTIONZ_MSG);
    }

    @Override
    public int executeUpdate(String sql, int[] columnIndexes) throws SQLException {
        throw new SQLException(TSDBConstants.UNSUPPORT_METHOD_EXCEPTIONZ_MSG);
    }

    @Override
    public int executeUpdate(String sql, String[] columnNames) throws SQLException {
        throw new SQLException(TSDBConstants.UNSUPPORT_METHOD_EXCEPTIONZ_MSG);
    }

    @Override
    public boolean execute(String sql, int autoGeneratedKeys) throws SQLException {
        throw new SQLException(TSDBConstants.UNSUPPORT_METHOD_EXCEPTIONZ_MSG);
    }

    @Override
    public boolean execute(String sql, int[] columnIndexes) throws SQLException {
        throw new SQLException(TSDBConstants.UNSUPPORT_METHOD_EXCEPTIONZ_MSG);
    }

    @Override
    public boolean execute(String sql, String[] columnNames) throws SQLException {
        throw new SQLException(TSDBConstants.UNSUPPORT_METHOD_EXCEPTIONZ_MSG);
    }

    @Override
    public int getResultSetHoldability() throws SQLException {
        throw new SQLException(TSDBConstants.UNSUPPORT_METHOD_EXCEPTIONZ_MSG);
    }

    @Override
    public boolean isClosed() throws SQLException {
        return isClosed;
    }

    @Override
    public void setPoolable(boolean poolable) throws SQLException {
        throw new SQLException(TSDBConstants.UNSUPPORT_METHOD_EXCEPTIONZ_MSG);
    }

    @Override
    public boolean isPoolable() throws SQLException {
        throw new SQLException(TSDBConstants.UNSUPPORT_METHOD_EXCEPTIONZ_MSG);
    }

    @Override
    public void closeOnCompletion() throws SQLException {
        throw new SQLException(TSDBConstants.UNSUPPORT_METHOD_EXCEPTIONZ_MSG);
    }

    @Override
    public boolean isCloseOnCompletion() throws SQLException {
        throw new SQLException(TSDBConstants.UNSUPPORT_METHOD_EXCEPTIONZ_MSG);
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        throw new SQLException(TSDBConstants.UNSUPPORT_METHOD_EXCEPTIONZ_MSG);
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        throw new SQLException(TSDBConstants.UNSUPPORT_METHOD_EXCEPTIONZ_MSG);
    }
}
