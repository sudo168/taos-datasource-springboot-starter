package net.ewant.taos.support;

import com.zaxxer.hikari.HikariConfig;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("spring.datasource.taos.hikari")
public class TaosHikariConfigProperties extends HikariConfig {
}
