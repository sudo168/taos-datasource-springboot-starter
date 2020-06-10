package net.ewant.taos.support;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Properties;

@ConfigurationProperties("spring.datasource.taos.druid")
public class TaosDruidConfigProperties extends Properties {
}
