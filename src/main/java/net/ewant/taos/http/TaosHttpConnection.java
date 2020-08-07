package net.ewant.taos.http;

import com.taosdata.jdbc.TSDBConstants;
import com.taosdata.jdbc.TSDBDatabaseMetaData;
import com.taosdata.jdbc.TSDBDriver;
import net.ewant.taos.pool2.TaosDataSource;
import net.ewant.taos.support.TaosConfigProperties;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.sql.*;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executor;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class TaosHttpConnection implements Connection {

    static final org.slf4j.Logger logger = LoggerFactory.getLogger(TaosHttpConnection.class);

    String database;
    String username = "root";
    String password = "taosdata";
    HttpClient httpClient;
    TSDBDatabaseMetaData dbMetaData = null;
    static final Pattern SHOW_PATTERN = Pattern.compile("tables", Pattern.CASE_INSENSITIVE);
    static final Pattern SELECT_PATTERN = Pattern.compile("(from[ ]+[']?)([^ ]+)[']?", Pattern.CASE_INSENSITIVE);
    static final Pattern SELECT_TABLE_PATTERN = Pattern.compile("(from[ ]+)([']?[^ ]+[']?)", Pattern.CASE_INSENSITIVE);
    static final Pattern INSERT_PATTERN = Pattern.compile("(into[ ]+[']?)([^ (]+)[']?", Pattern.CASE_INSENSITIVE);
    static final Pattern INSERT_TABLE_PATTERN = Pattern.compile("(into[ ]+)([']?[^ (]+[']?)", Pattern.CASE_INSENSITIVE);
    static final Pattern META_PATTERN = Pattern.compile("(describe[ ]+[']?)([^ ]+)[']?", Pattern.CASE_INSENSITIVE);
    static final Pattern DROP_PATTERN = Pattern.compile("(drop[ ]+table[ ]+[']?)(if[ ]+exists[ ]+[']?)?([^ (]+)[']?", Pattern.CASE_INSENSITIVE);
    static final Pattern CREATE_PATTERN = Pattern.compile("(create[ ]+table[ ]+[']?)(if[ ]+not[ ]+exists[ ]+[']?)?([^ (]+)[']?([ ]+using[ ]+[']?)?([^ (]*)", Pattern.CASE_INSENSITIVE);
    static final Pattern COLUMN_PATTERN = Pattern.compile("select(.+)from", Pattern.CASE_INSENSITIVE);

    public TaosHttpConnection(String url, Properties info) {
        initProperties(url, info);
        if(database == null){
            throw new IllegalArgumentException("No database defined in " + url);
        }
        this.dbMetaData = new TSDBDatabaseMetaData("TAOS", url, username);
        this.dbMetaData.setConnection(this);
    }

    private void initProperties(String url, Properties info) {
        String uriString = url;
        if(uriString.toLowerCase().startsWith("jdbc:")){
            uriString = uriString.substring(5);
        }
        URI uri = URI.create(uriString);
        this.database = uri.getPath().substring(1);
        Map<String, String> query = parseQuery(uri.getQuery());
        if(info != null){
            info.stringPropertyNames().forEach(k->{
                query.put(k, info.getProperty(k));
            });
        }
        String username = query.get(TSDBDriver.PROPERTY_KEY_USER);
        String password = query.get(TSDBDriver.PROPERTY_KEY_PASSWORD);
        if(username != null){
            this.username = username;
        }
        if(password != null){
            this.password = password;
        }

        String host = uri.getHost();
        int port = uri.getPort();
        if(port < 1){
            port = 6041;
        }
        this.httpClient = new HttpClient("http://" + host + ":" + port + "/rest/sql");
    }

    private Map<String, String> parseQuery(String query){
        Map<String, String> result = new HashMap<>();
        if(query != null){
            Stream.of(query.split("&")).forEach(item->{
                String[] split = item.split("=");
                result.put(split[0],split.length>1?split[1]:null);
            });
        }
        return result;
    }

    public static void main(String[] args) throws Exception {
        String uriString = "jdbc:TAOS://192.168.2.43:0/atom_iceberg?user=root&password=taosdata&enableMicrosecond=true";
        TaosConfigProperties configProperties = new TaosConfigProperties();
        configProperties.setJdbcUrl(uriString);
        TaosDataSource dataSource = new TaosDataSource(configProperties, null);
        Connection connection = dataSource.getConnection();
        Statement statement = connection.createStatement();
        ResultSet resultSet = statement.executeQuery("show tables");
        //resultSet = statement.executeQuery("select * from TRADER_QUOTA limit 2");
        resultSet = statement.executeQuery("select count(ts), sum(value), sum(value) as sum_val, sum(rank) sum_rank from TRADER_QUOTA limit 2");
        resultSet = statement.executeQuery("describe TRADER_QUOTA");
        resultSet = statement.executeQuery("create table if not exists t1(ts timestamp, price double) tags(symbol binary(10))");
        resultSet = statement.executeQuery("drop table if exists t1");
        System.out.println("ok...");
    }

    private String appendDatabase(String sql, StringBuffer tableBuffer, StringBuffer columnBuffer){
        if(TaosConfigProperties.DEFAULT_TEST_QUERY.equalsIgnoreCase(sql)){
            return sql;
        }
        Matcher matcher = SELECT_PATTERN.matcher(sql);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()){
            matcher.appendReplacement(buffer, "$1"+database+".$2");
            matcher.appendTail(buffer);
        }
        if(buffer.length() > 0){
            String newSql = buffer.toString();
            Matcher tableMatcher = SELECT_TABLE_PATTERN.matcher(newSql);
            if(tableMatcher.find()){
                tableBuffer.append(tableMatcher.group(2));
            }
            Matcher columnMatcher = COLUMN_PATTERN.matcher(newSql);
            if(columnMatcher.find()){
                columnBuffer.append(columnMatcher.group(1));
            }
            return newSql;
        }
        matcher = INSERT_PATTERN.matcher(sql);
        while (matcher.find()){
            matcher.appendReplacement(buffer, "$1"+database+".$2");
            matcher.appendTail(buffer);
        }
        if(buffer.length() > 0){
            Matcher tableMatcher = INSERT_TABLE_PATTERN.matcher(buffer.toString());
            if(tableMatcher.find()){
                tableBuffer.append(tableMatcher.group(2));
            }
            return buffer.toString();
        }
        matcher = META_PATTERN.matcher(sql);
        while (matcher.find()){
            matcher.appendReplacement(buffer, "$1"+database+".$2");
            matcher.appendTail(buffer);
        }
        if(buffer.length() > 0){
            return buffer.toString();
        }
        matcher = CREATE_PATTERN.matcher(sql);
        while (matcher.find()){
            String groupEnd = matcher.group(matcher.groupCount());
            if(groupEnd == null || groupEnd.length() == 0){
                matcher.appendReplacement(buffer, "$1$2"+database+".$3");
            }else{
                matcher.appendReplacement(buffer, "$1$2"+database+".$3$4"+database+".$5");
            }
            matcher.appendTail(buffer);
        }
        if(buffer.length() > 0){
            return buffer.toString();
        }
        matcher = DROP_PATTERN.matcher(sql);
        while (matcher.find()){
            matcher.appendReplacement(buffer, "$1$2"+database+".$3");
            matcher.appendTail(buffer);
        }
        if(buffer.length() > 0){
            return buffer.toString();
        }
        matcher = SHOW_PATTERN.matcher(sql);
        while (matcher.find()){
            matcher.appendReplacement(buffer, database+".$0");
            matcher.appendTail(buffer);
        }
        if(buffer.length() > 0){
            return buffer.toString();
        }
        return sql;
    }

    public String executeQuery(String sql, StringBuffer tableBuffer, StringBuffer columnBuffer){
        String token = new String(Base64.getEncoder().encode((username + ":" + password).getBytes()));
        sql = appendDatabase(sql, tableBuffer, columnBuffer);
        logger.info(sql);
        return httpClient.addHeader("Authorization", "Basic " + token).doPost(sql);
    }

    @Override
    public Statement createStatement() throws SQLException {
        return new TaosHttpStatement(this);
    }

    @Override
    public PreparedStatement prepareStatement(String sql) throws SQLException {
        return new TaosHttpPreparedStatement(this, sql);
    }

    @Override
    public CallableStatement prepareCall(String sql) throws SQLException {
        throw new SQLException(TSDBConstants.UNSUPPORT_METHOD_EXCEPTIONZ_MSG);
    }

    @Override
    public String nativeSQL(String sql) throws SQLException {
        throw new SQLException(TSDBConstants.UNSUPPORT_METHOD_EXCEPTIONZ_MSG);
    }

    @Override
    public void setAutoCommit(boolean autoCommit) throws SQLException {
    }

    @Override
    public boolean getAutoCommit() throws SQLException {
        return true;
    }

    @Override
    public void commit() throws SQLException {
    }

    @Override
    public void rollback() throws SQLException {
        throw new SQLException(TSDBConstants.UNSUPPORT_METHOD_EXCEPTIONZ_MSG);
    }

    @Override
    public void close() throws SQLException {
        httpClient.close();
    }

    @Override
    public boolean isClosed() throws SQLException {
        return !httpClient.isAvailable();
    }

    @Override
    public DatabaseMetaData getMetaData() throws SQLException {
        return this.dbMetaData;
    }

    @Override
    public void setReadOnly(boolean readOnly) throws SQLException {
    }

    @Override
    public boolean isReadOnly() throws SQLException {
        return false;
    }

    @Override
    public void setCatalog(String catalog) throws SQLException {
    }

    @Override
    public String getCatalog() throws SQLException {
        return null;
    }

    @Override
    public void setTransactionIsolation(int level) throws SQLException {
    }

    @Override
    public int getTransactionIsolation() throws SQLException {
        throw new SQLException(TSDBConstants.UNSUPPORT_METHOD_EXCEPTIONZ_MSG);
    }

    @Override
    public SQLWarning getWarnings() throws SQLException {
        throw new SQLException(TSDBConstants.UNSUPPORT_METHOD_EXCEPTIONZ_MSG);
    }

    @Override
    public void clearWarnings() throws SQLException {

    }

    @Override
    public Statement createStatement(int resultSetType, int resultSetConcurrency) throws SQLException {
        throw new SQLException(TSDBConstants.UNSUPPORT_METHOD_EXCEPTIONZ_MSG);
    }

    @Override
    public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency) throws SQLException {
        // This method is implemented in the current way to support Spark
        if (resultSetType != ResultSet.TYPE_FORWARD_ONLY) {
            throw new SQLException(TSDBConstants.INVALID_VARIABLES);
        }

        if (resultSetConcurrency != ResultSet.CONCUR_READ_ONLY) {
            throw new SQLException(TSDBConstants.INVALID_VARIABLES);
        }

        return this.prepareStatement(sql);
    }

    @Override
    public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency) throws SQLException {
        throw new SQLException(TSDBConstants.UNSUPPORT_METHOD_EXCEPTIONZ_MSG);
    }

    @Override
    public Map<String, Class<?>> getTypeMap() throws SQLException {
        throw new SQLException(TSDBConstants.UNSUPPORT_METHOD_EXCEPTIONZ_MSG);
    }

    @Override
    public void setTypeMap(Map<String, Class<?>> map) throws SQLException {
        throw new SQLException(TSDBConstants.UNSUPPORT_METHOD_EXCEPTIONZ_MSG);
    }

    @Override
    public void setHoldability(int holdability) throws SQLException {
        throw new SQLException(TSDBConstants.UNSUPPORT_METHOD_EXCEPTIONZ_MSG);
    }

    @Override
    public int getHoldability() throws SQLException {
        throw new SQLException(TSDBConstants.UNSUPPORT_METHOD_EXCEPTIONZ_MSG);
    }

    @Override
    public Savepoint setSavepoint() throws SQLException {
        throw new SQLException(TSDBConstants.UNSUPPORT_METHOD_EXCEPTIONZ_MSG);
    }

    @Override
    public Savepoint setSavepoint(String name) throws SQLException {
        throw new SQLException(TSDBConstants.UNSUPPORT_METHOD_EXCEPTIONZ_MSG);
    }

    @Override
    public void rollback(Savepoint savepoint) throws SQLException {
        throw new SQLException(TSDBConstants.UNSUPPORT_METHOD_EXCEPTIONZ_MSG);
    }

    @Override
    public void releaseSavepoint(Savepoint savepoint) throws SQLException {
        throw new SQLException(TSDBConstants.UNSUPPORT_METHOD_EXCEPTIONZ_MSG);
    }

    @Override
    public Statement createStatement(int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
        throw new SQLException(TSDBConstants.UNSUPPORT_METHOD_EXCEPTIONZ_MSG);
    }

    @Override
    public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
        return this.prepareStatement(sql, resultSetType, resultSetConcurrency);
    }

    @Override
    public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
        throw new SQLException(TSDBConstants.UNSUPPORT_METHOD_EXCEPTIONZ_MSG);
    }

    @Override
    public PreparedStatement prepareStatement(String sql, int autoGeneratedKeys) throws SQLException {
        throw new SQLException(TSDBConstants.UNSUPPORT_METHOD_EXCEPTIONZ_MSG);
    }

    @Override
    public PreparedStatement prepareStatement(String sql, int[] columnIndexes) throws SQLException {
        throw new SQLException(TSDBConstants.UNSUPPORT_METHOD_EXCEPTIONZ_MSG);
    }

    @Override
    public PreparedStatement prepareStatement(String sql, String[] columnNames) throws SQLException {
        throw new SQLException(TSDBConstants.UNSUPPORT_METHOD_EXCEPTIONZ_MSG);
    }

    @Override
    public Clob createClob() throws SQLException {
        throw new SQLException(TSDBConstants.UNSUPPORT_METHOD_EXCEPTIONZ_MSG);
    }

    @Override
    public Blob createBlob() throws SQLException {
        throw new SQLException(TSDBConstants.UNSUPPORT_METHOD_EXCEPTIONZ_MSG);
    }

    @Override
    public NClob createNClob() throws SQLException {
        throw new SQLException(TSDBConstants.UNSUPPORT_METHOD_EXCEPTIONZ_MSG);
    }

    @Override
    public SQLXML createSQLXML() throws SQLException {
        throw new SQLException(TSDBConstants.UNSUPPORT_METHOD_EXCEPTIONZ_MSG);
    }

    @Override
    public boolean isValid(int timeout) throws SQLException {
        return !this.isClosed();
    }

    @Override
    public void setClientInfo(String name, String value) throws SQLClientInfoException {
    }

    @Override
    public void setClientInfo(Properties properties) throws SQLClientInfoException {
    }

    @Override
    public String getClientInfo(String name) throws SQLException {
        return null;
    }

    @Override
    public Properties getClientInfo() throws SQLException {
        return null;
    }

    @Override
    public Array createArrayOf(String typeName, Object[] elements) throws SQLException {
        throw new SQLException(TSDBConstants.UNSUPPORT_METHOD_EXCEPTIONZ_MSG);
    }

    @Override
    public Struct createStruct(String typeName, Object[] attributes) throws SQLException {
        throw new SQLException(TSDBConstants.UNSUPPORT_METHOD_EXCEPTIONZ_MSG);
    }

    @Override
    public void setSchema(String schema) throws SQLException {
        throw new SQLException(TSDBConstants.UNSUPPORT_METHOD_EXCEPTIONZ_MSG);
    }

    @Override
    public String getSchema() throws SQLException {
        throw new SQLException(TSDBConstants.UNSUPPORT_METHOD_EXCEPTIONZ_MSG);
    }

    @Override
    public void abort(Executor executor) throws SQLException {
        throw new SQLException(TSDBConstants.UNSUPPORT_METHOD_EXCEPTIONZ_MSG);
    }

    @Override
    public void setNetworkTimeout(Executor executor, int milliseconds) throws SQLException {
    }

    @Override
    public int getNetworkTimeout() throws SQLException {
        return 0;
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
