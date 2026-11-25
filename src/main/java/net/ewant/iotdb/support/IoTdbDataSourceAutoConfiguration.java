package net.ewant.iotdb.support;

import net.ewant.iotdb.IoTdbConfigProperties;
import net.ewant.iotdb.IoTdbDataSource;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnClass(name = "org.apache.iotdb.jdbc.IoTDBDriver")
@ConditionalOnProperty(prefix = "spring.datasource.iotdb", name = "url")
@EnableConfigurationProperties(IoTdbConfigProperties.class)
public class IoTdbDataSourceAutoConfiguration {

    @Bean(name = "iotdbDataSource", destroyMethod = "close")
    @ConditionalOnMissingBean(name = "iotdbDataSource")
    public IoTdbDataSource iotdbDataSource(IoTdbConfigProperties properties) {
        return new IoTdbDataSource(properties);
    }
}
