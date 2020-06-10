package net.ewant.taos.http;

import com.taosdata.jdbc.TSDBConstants;

import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.*;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TaosHttpPreparedStatement extends TaosHttpStatement implements PreparedStatement {

    String rawSql;
    String sql;
    ArrayList<Object> parameters = new ArrayList();

    public TaosHttpPreparedStatement(TaosHttpConnection connector, String rawSql) {
        super(connector);
        this.rawSql = rawSql;
        this.preprocessSql();
    }

    private void preprocessSql() {
        this.rawSql = this.rawSql.replaceAll("or (.*) is null", "");
        this.rawSql = this.rawSql.replaceAll(" where ", " WHERE ");
        this.rawSql = this.rawSql.replaceAll(" or ", " OR ");
        this.rawSql = this.rawSql.replaceAll(" and ", " AND ");
        this.rawSql = this.rawSql.replaceAll(" is null", " IS NULL");
        this.rawSql = this.rawSql.replaceAll(" is not null", " IS NOT NULL");
        this.rawSql = this.rawSql.replaceAll("WHERE 1=0", "WHERE _c0=1");
        this.rawSql = this.rawSql.replaceAll("WHERE 1=2", "WHERE _c0=1");
        this.rawSql = this.rawSql.replaceAll("\"", "");
        this.rawSql = this.rawSql.replaceAll("SELECT 1 FROM", "SELECT * FROM");
        this.rawSql = this.rawSql.replaceAll("OR (.*) IS NULL", "");
        this.rawSql = this.rawSql.replaceAll("(.*) IS NULL OR", "");
        this.rawSql = this.rawSql.replaceAll("\\(\\((.*) IS NOT NULL\\) AND", "(");
        this.rawSql = this.rawSql.replaceAll("\\((.*) IS NOT NULL\\) AND", "");
        this.rawSql = this.rawSql.replaceAll("\\(\\((.*) IS NOT NULL\\)\\) AND", "");
        Pattern pattern = Pattern.compile("FROM\\s+((\\(.+\\))\\s+SUB_QRY)", 2);
        Matcher matcher = pattern.matcher(this.rawSql);
        String tableFullName = "";
        if (matcher.find() && matcher.groupCount() == 2) {
            String subQry = matcher.group(2);
            Pattern pattern1 = Pattern.compile("FROM\\s+(\\w+\\.\\w+)", 2);
            Matcher matcher1 = pattern1.matcher(subQry);
            if (matcher1.find() && matcher1.groupCount() == 1) {
                tableFullName = matcher1.group(1);
            }

            this.rawSql = this.rawSql.replace(matcher.group(1), tableFullName);
        }
    }

    private String getNativeSql() {
        this.sql = this.rawSql;

        for(int i = 0; i < this.parameters.size(); ++i) {
            Object para = this.parameters.get(i);
            if (para == null) {
                this.sql = this.sql.replaceFirst("[?]", "NULL");
            } else {
                String paraStr = para.toString();
                if (para instanceof Timestamp || para instanceof String) {
                    paraStr = "'" + paraStr + "'";
                }

                this.sql = this.sql.replaceFirst("[?]", paraStr);
            }
        }

        this.parameters.clear();
        return this.sql;
    }

    @Override
    public ResultSet executeQuery() throws SQLException {
        return super.executeQuery(this.getNativeSql());
    }

    @Override
    public int executeUpdate() throws SQLException {
        return super.executeUpdate(this.getNativeSql());
    }

    @Override
    public void setNull(int parameterIndex, int sqlType) throws SQLException {
        setObject(parameterIndex, new String("NULL"));
    }

    @Override
    public void setBoolean(int parameterIndex, boolean x) throws SQLException {
        setObject(parameterIndex, x);
    }

    @Override
    public void setByte(int parameterIndex, byte x) throws SQLException {
        setObject(parameterIndex, x);
    }

    @Override
    public void setShort(int parameterIndex, short x) throws SQLException {
        setObject(parameterIndex, x);
    }

    @Override
    public void setInt(int parameterIndex, int x) throws SQLException {
        setObject(parameterIndex, x);
    }

    @Override
    public void setLong(int parameterIndex, long x) throws SQLException {
        setObject(parameterIndex, x);
    }

    @Override
    public void setFloat(int parameterIndex, float x) throws SQLException {
        setObject(parameterIndex, x);
    }

    @Override
    public void setDouble(int parameterIndex, double x) throws SQLException {
        setObject(parameterIndex, x);
    }

    @Override
    public void setBigDecimal(int parameterIndex, BigDecimal x) throws SQLException {
        setObject(parameterIndex, x);
    }

    @Override
    public void setString(int parameterIndex, String x) throws SQLException {
        setObject(parameterIndex, x);
    }

    @Override
    public void setBytes(int parameterIndex, byte[] x) throws SQLException {
        setObject(parameterIndex, x);
    }

    @Override
    public void setDate(int parameterIndex, Date x) throws SQLException {
        setObject(parameterIndex, x);
    }

    @Override
    public void setTime(int parameterIndex, Time x) throws SQLException {
        setObject(parameterIndex, x);
    }

    @Override
    public void setTimestamp(int parameterIndex, Timestamp x) throws SQLException {
        setObject(parameterIndex, x);
    }

    @Override
    public void setAsciiStream(int parameterIndex, InputStream x, int length) throws SQLException {
        throw new SQLException(TSDBConstants.UNSUPPORT_METHOD_EXCEPTIONZ_MSG);
    }

    @Override
    public void setUnicodeStream(int parameterIndex, InputStream x, int length) throws SQLException {
        throw new SQLException(TSDBConstants.UNSUPPORT_METHOD_EXCEPTIONZ_MSG);
    }

    @Override
    public void setBinaryStream(int parameterIndex, InputStream x, int length) throws SQLException {
        throw new SQLException(TSDBConstants.UNSUPPORT_METHOD_EXCEPTIONZ_MSG);
    }

    @Override
    public void clearParameters() throws SQLException {
        parameters.clear();
    }

    @Override
    public void setObject(int parameterIndex, Object x, int targetSqlType) throws SQLException {
        throw new SQLException(TSDBConstants.UNSUPPORT_METHOD_EXCEPTIONZ_MSG);
    }

    @Override
    public void setObject(int parameterIndex, Object x) throws SQLException {
        parameters.add(x);
    }

    @Override
    public boolean execute() throws SQLException {
        return super.execute(this.getNativeSql());
    }

    @Override
    public void addBatch() throws SQLException {
        super.addBatch(this.getNativeSql());
    }

    @Override
    public void setCharacterStream(int parameterIndex, Reader reader, int length) throws SQLException {
        throw new SQLException(TSDBConstants.UNSUPPORT_METHOD_EXCEPTIONZ_MSG);
    }

    @Override
    public void setRef(int parameterIndex, Ref x) throws SQLException {
        throw new SQLException(TSDBConstants.UNSUPPORT_METHOD_EXCEPTIONZ_MSG);
    }

    @Override
    public void setBlob(int parameterIndex, Blob x) throws SQLException {
        throw new SQLException(TSDBConstants.UNSUPPORT_METHOD_EXCEPTIONZ_MSG);
    }

    @Override
    public void setClob(int parameterIndex, Clob x) throws SQLException {
        throw new SQLException(TSDBConstants.UNSUPPORT_METHOD_EXCEPTIONZ_MSG);
    }

    @Override
    public void setArray(int parameterIndex, Array x) throws SQLException {
        throw new SQLException(TSDBConstants.UNSUPPORT_METHOD_EXCEPTIONZ_MSG);
    }

    @Override
    public ResultSetMetaData getMetaData() throws SQLException {
        throw new SQLException(TSDBConstants.UNSUPPORT_METHOD_EXCEPTIONZ_MSG);
    }

    @Override
    public void setDate(int parameterIndex, Date x, Calendar cal) throws SQLException {
        throw new SQLException(TSDBConstants.UNSUPPORT_METHOD_EXCEPTIONZ_MSG);
    }

    @Override
    public void setTime(int parameterIndex, Time x, Calendar cal) throws SQLException {
        throw new SQLException(TSDBConstants.UNSUPPORT_METHOD_EXCEPTIONZ_MSG);
    }

    @Override
    public void setTimestamp(int parameterIndex, Timestamp x, Calendar cal) throws SQLException {
        throw new SQLException(TSDBConstants.UNSUPPORT_METHOD_EXCEPTIONZ_MSG);
    }

    @Override
    public void setNull(int parameterIndex, int sqlType, String typeName) throws SQLException {
        throw new SQLException(TSDBConstants.UNSUPPORT_METHOD_EXCEPTIONZ_MSG);
    }

    @Override
    public void setURL(int parameterIndex, URL x) throws SQLException {
        throw new SQLException(TSDBConstants.UNSUPPORT_METHOD_EXCEPTIONZ_MSG);
    }

    @Override
    public ParameterMetaData getParameterMetaData() throws SQLException {
        throw new SQLException(TSDBConstants.UNSUPPORT_METHOD_EXCEPTIONZ_MSG);
    }

    @Override
    public void setRowId(int parameterIndex, RowId x) throws SQLException {
        throw new SQLException(TSDBConstants.UNSUPPORT_METHOD_EXCEPTIONZ_MSG);
    }

    @Override
    public void setNString(int parameterIndex, String value) throws SQLException {
        throw new SQLException(TSDBConstants.UNSUPPORT_METHOD_EXCEPTIONZ_MSG);
    }

    @Override
    public void setNCharacterStream(int parameterIndex, Reader value, long length) throws SQLException {
        throw new SQLException(TSDBConstants.UNSUPPORT_METHOD_EXCEPTIONZ_MSG);
    }

    @Override
    public void setNClob(int parameterIndex, NClob value) throws SQLException {
        throw new SQLException(TSDBConstants.UNSUPPORT_METHOD_EXCEPTIONZ_MSG);
    }

    @Override
    public void setClob(int parameterIndex, Reader reader, long length) throws SQLException {
        throw new SQLException(TSDBConstants.UNSUPPORT_METHOD_EXCEPTIONZ_MSG);
    }

    @Override
    public void setBlob(int parameterIndex, InputStream inputStream, long length) throws SQLException {
        throw new SQLException(TSDBConstants.UNSUPPORT_METHOD_EXCEPTIONZ_MSG);
    }

    @Override
    public void setNClob(int parameterIndex, Reader reader, long length) throws SQLException {
        throw new SQLException(TSDBConstants.UNSUPPORT_METHOD_EXCEPTIONZ_MSG);
    }

    @Override
    public void setSQLXML(int parameterIndex, SQLXML xmlObject) throws SQLException {
        throw new SQLException(TSDBConstants.UNSUPPORT_METHOD_EXCEPTIONZ_MSG);
    }

    @Override
    public void setObject(int parameterIndex, Object x, int targetSqlType, int scaleOrLength) throws SQLException {
        throw new SQLException(TSDBConstants.UNSUPPORT_METHOD_EXCEPTIONZ_MSG);
    }

    @Override
    public void setAsciiStream(int parameterIndex, InputStream x, long length) throws SQLException {
        throw new SQLException(TSDBConstants.UNSUPPORT_METHOD_EXCEPTIONZ_MSG);
    }

    @Override
    public void setBinaryStream(int parameterIndex, InputStream x, long length) throws SQLException {
        throw new SQLException(TSDBConstants.UNSUPPORT_METHOD_EXCEPTIONZ_MSG);
    }

    @Override
    public void setCharacterStream(int parameterIndex, Reader reader, long length) throws SQLException {
        throw new SQLException(TSDBConstants.UNSUPPORT_METHOD_EXCEPTIONZ_MSG);
    }

    @Override
    public void setAsciiStream(int parameterIndex, InputStream x) throws SQLException {
        throw new SQLException(TSDBConstants.UNSUPPORT_METHOD_EXCEPTIONZ_MSG);
    }

    @Override
    public void setBinaryStream(int parameterIndex, InputStream x) throws SQLException {
        throw new SQLException(TSDBConstants.UNSUPPORT_METHOD_EXCEPTIONZ_MSG);
    }

    @Override
    public void setCharacterStream(int parameterIndex, Reader reader) throws SQLException {
        throw new SQLException(TSDBConstants.UNSUPPORT_METHOD_EXCEPTIONZ_MSG);
    }

    @Override
    public void setNCharacterStream(int parameterIndex, Reader value) throws SQLException {
        throw new SQLException(TSDBConstants.UNSUPPORT_METHOD_EXCEPTIONZ_MSG);
    }

    @Override
    public void setClob(int parameterIndex, Reader reader) throws SQLException {
        throw new SQLException(TSDBConstants.UNSUPPORT_METHOD_EXCEPTIONZ_MSG);
    }

    @Override
    public void setBlob(int parameterIndex, InputStream inputStream) throws SQLException {
        throw new SQLException(TSDBConstants.UNSUPPORT_METHOD_EXCEPTIONZ_MSG);
    }

    @Override
    public void setNClob(int parameterIndex, Reader reader) throws SQLException {
        throw new SQLException(TSDBConstants.UNSUPPORT_METHOD_EXCEPTIONZ_MSG);
    }
}
