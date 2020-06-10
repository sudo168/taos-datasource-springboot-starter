package net.ewant.taos.test;

import com.alibaba.druid.pool.DruidDataSourceFactory;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import net.ewant.taos.pool2.TaosDataSource;
import net.ewant.taos.support.TaosConfigProperties;

import javax.sql.DataSource;
import java.sql.*;
import java.util.Properties;

public class TestDataSource {

    static String databaseName = "test_database";
    static String tableName = "test_supper";
    static String tablePrefix = "t";
    static int tablesCount = 1;

    public static void main(String[] args) throws Exception {
        String url = "jdbc:TAOS://192.168.2.43:0/atom_index?user=root&password=taosdata";
        DataSource dataSource = getTaosDataSource(url);
        //DataSource dataSource = getHikariDataSource(url);
        //DataSource dataSource = getDruidDataSource(url);
        Connection conn = dataSource.getConnection();
        createDatabaseAndTable(conn);

        try (Statement stmt = conn.createStatement()){
            //stmt.executeUpdate("insert into t0 values('2019-09-18 15:55:00.001', 1.234, 6.789)");
            ResultSet resultSet = stmt.executeQuery("select count(*) from " + tableName);
            ResultSetMetaData metaData = resultSet.getMetaData();
            StringBuilder resRow;
            while (resultSet.next()) {
                resRow = new StringBuilder();
                for (int col = 1; col <= metaData.getColumnCount(); col++) {
                    resRow.append(metaData.getColumnName(col)).append("=").append(resultSet.getObject(col))
                            .append(" ");
                }
                System.out.println(resRow.toString());
            }
            System.out.println();
            String sql = "drop database if exists " + databaseName;
            stmt.executeUpdate(sql);
            System.out.printf("Successfully executed: %s\n", sql);
        }
        System.out.println("==========end=========");
    }

    private static DataSource getTaosDataSource(String url){
        TaosConfigProperties configProperties = new TaosConfigProperties();
        configProperties.setJdbcUrl(url);
        return new TaosDataSource(configProperties, null);
    }
    private static DataSource getDruidDataSource(String url) throws Exception {
        Properties properties = new Properties();
        properties.put(DruidDataSourceFactory.PROP_URL, url);
        return DruidDataSourceFactory.createDataSource(properties);
    }
    private static DataSource getHikariDataSource(String url) throws Exception {
        HikariConfig properties = new HikariConfig();
        properties.setJdbcUrl(url);
        return new HikariDataSource(properties);
    }

    private static void createDatabaseAndTable(Connection conn){
        System.out.println("Start creating databases and tables...");
        String sql = "";
        try (Statement stmt = conn.createStatement()){

            sql = "create database if not exists " + databaseName;
            stmt.executeUpdate(sql);
            System.out.printf("Successfully executed: %s\n", sql);

            sql = "use " + databaseName;
            stmt.executeUpdate(sql);
            System.out.printf("Successfully executed: %s\n", sql);

            sql = "create table if not exists " + tableName + " (ts timestamp, v1 double, v2 double) tags(symbol binary(10), exchange binary(20), location int)";
            stmt.executeUpdate(sql);
            System.out.printf("Successfully executed: %s\n", sql);

            for (int i = 0; i < tablesCount; i++) {
                sql = String.format("create table if not exists %s%d using %s tags(%s,%s,%d)", tablePrefix, i,
                        tableName, "'BTCUSDT'", "'AAX'", 1);
                stmt.executeUpdate(sql);
                System.out.printf("Successfully executed: %s\n", sql);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            System.out.printf("Failed to execute SQL: %s\n", sql);
            System.exit(4);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(4);
        }
        System.out.println("Successfully created databases and tables");
    }
}
