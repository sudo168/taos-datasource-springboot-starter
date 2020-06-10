package net.ewant.taos.support;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("spring.datasource.taos.pool2")
public class TaosPool2ConfigProperties extends GenericObjectPoolConfig {

    private String testQuery;

    public TaosPool2ConfigProperties(){
        setTestWhileIdle(true);
        setMinEvictableIdleTimeMillis(60000);
        setTimeBetweenEvictionRunsMillis(30000);
        setNumTestsPerEvictionRun(-1);
    }

    public String getTestQuery() {
        return testQuery;
    }

    public void setTestQuery(String testQuery) {
        this.testQuery = testQuery;
    }
}
