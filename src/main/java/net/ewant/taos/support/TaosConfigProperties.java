package net.ewant.taos.support;

import com.taosdata.jdbc.TSDBDriver;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Properties;

@ConfigurationProperties(prefix = "spring.datasource.taos")
public class TaosConfigProperties {

    //public static final String DEFAULT_TEST_QUERY_OLD = "select count(*) from log.log";
    // For TDengine above v1.6.4.1 use
    public static final String DEFAULT_TEST_QUERY = "select server_status()";

    private String jdbcUrl;
    private String username;
    private String password;
    private String configDir;
    private boolean useHttp;
    private boolean macForceHttp = true;
    private String[] mapperLocations;

    public String getJdbcUrl() {
        return jdbcUrl;
    }

    public void setJdbcUrl(String jdbcUrl) {
        this.jdbcUrl = jdbcUrl;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getConfigDir() {
        return configDir;
    }

    public void setConfigDir(String configDir) {
        this.configDir = configDir;
    }

    public boolean isUseHttp() {
        return useHttp;
    }

    public void setUseHttp(boolean useHttp) {
        this.useHttp = useHttp;
    }

    public boolean isMacForceHttp() {
        return macForceHttp;
    }

    public void setMacForceHttp(boolean macForceHttp) {
        this.macForceHttp = macForceHttp;
    }

    public String[] getMapperLocations() {
        return mapperLocations;
    }

    public void setMapperLocations(String[] mapperLocations) {
        this.mapperLocations = mapperLocations;
    }

    /**
     * properties配置参数优先级最高，JDBC URL的优先级次之，configDir配置文件的优先级最低
     * @return
     */
    public Properties getProperties(){
        Properties connProps = new Properties();
        if(username != null){
            connProps.setProperty(TSDBDriver.PROPERTY_KEY_USER, username);
        }
        if(password != null){
            connProps.setProperty(TSDBDriver.PROPERTY_KEY_PASSWORD, password);
        }
        if(configDir != null){
            connProps.setProperty(TSDBDriver.PROPERTY_KEY_CONFIG_DIR, configDir);
        }
        //connProps.setProperty(TSDBDriver.PROPERTY_KEY_CHARSET, "UTF-8");
        //connProps.setProperty(TSDBDriver.PROPERTY_KEY_LOCALE, "en_US.UTF-8");
        //connProps.setProperty(TSDBDriver.PROPERTY_KEY_TIME_ZONE, "UTC-8");
        return connProps;
    }
}
