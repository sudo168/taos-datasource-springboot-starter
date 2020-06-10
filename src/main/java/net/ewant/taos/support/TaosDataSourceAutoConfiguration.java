package net.ewant.taos.support;

import com.alibaba.druid.pool.DruidDataSource;
import com.alibaba.druid.pool.DruidDataSourceFactory;
import com.taosdata.jdbc.TSDBDriver;
import com.zaxxer.hikari.HikariDataSource;
import net.ewant.taos.TaosJDBCLoader;
import net.ewant.taos.http.TaosHttpDriver;
import net.ewant.taos.pool2.TaosDataSource;
import org.apache.commons.pool2.ObjectPool;
import org.apache.ibatis.mapping.DatabaseIdProvider;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.scripting.LanguageDriver;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.type.TypeHandler;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.boot.autoconfigure.ConfigurationCustomizer;
import org.mybatis.spring.boot.autoconfigure.MybatisAutoConfiguration;
import org.mybatis.spring.boot.autoconfigure.MybatisProperties;
import org.mybatis.spring.boot.autoconfigure.SpringBootVFS;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;
import java.beans.PropertyDescriptor;
import java.io.IOException;
import java.sql.Driver;
import java.sql.DriverManager;
import java.util.Enumeration;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Configuration
@EnableConfigurationProperties(TaosConfigProperties.class)
public class TaosDataSourceAutoConfiguration implements InitializingBean {

    static final org.slf4j.Logger logger = LoggerFactory.getLogger(TaosDataSource.class);

    private boolean currentUseHttp;
    private TaosConfigProperties configProperties;

    static{
        try {
            TaosJDBCLoader.initialize();
            Class.forName("com.taosdata.jdbc.TSDBDriver");
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(9);
        }
    }

    static void useHttpDriver(){
        try {
            Enumeration<Driver> drivers = DriverManager.getDrivers();
            if(drivers != null){
                while (drivers.hasMoreElements()){
                    Driver driver = drivers.nextElement();
                    if(driver instanceof TSDBDriver){
                        DriverManager.deregisterDriver(driver);
                        break;
                    }
                }
            }
            DriverManager.registerDriver(new TaosHttpDriver());
        } catch (Exception e) {}
    }

    public TaosDataSourceAutoConfiguration(TaosConfigProperties configProperties){
        this.configProperties = configProperties;
        if(configProperties.isUseHttp()){
            useHttpDriver();
            currentUseHttp = true;
            logger.info("TAOS DataSource use HTTP Driver...");
        }else if("Mac".equals(TaosJDBCLoader.OSInfo.getOSName()) && configProperties.isMacForceHttp()){
            useHttpDriver();
            currentUseHttp = true;
            logger.info("TAOS DataSource use HTTP Driver for Mac...");
        }

    }

    @Override
    public void afterPropertiesSet() throws Exception {
    }

    @ConditionalOnClass(ObjectPool.class)
    @ConditionalOnProperty(value = "spring.datasource.taos.pool2")
    @EnableConfigurationProperties(TaosPool2ConfigProperties.class)
    class Pool2DataSourceAutoConfig{

        @Bean
        @ConditionalOnMissingBean(name = "taosDataSource")
        public TaosDataSource taosDataSource(TaosPool2ConfigProperties properties) throws Exception {
            return new TaosDataSource(configProperties, properties);
        }
    }

    @ConditionalOnClass(HikariDataSource.class)
    @ConditionalOnProperty(value = "spring.datasource.taos.hikari")
    @EnableConfigurationProperties(TaosHikariConfigProperties.class)
    class HikariDataSourceAutoConfig{
        @Bean
        @ConditionalOnMissingBean(name = "taosDataSource")
        public HikariDataSource taosDataSource(TaosHikariConfigProperties properties) throws Exception {
            properties.setDriverClassName(currentUseHttp ? TaosHttpDriver.class.getName() : TSDBDriver.class.getName());
            if(properties.getConnectionTestQuery() == null){
                properties.setConnectionTestQuery(TaosConfigProperties.DEFAULT_TEST_QUERY);
            }
            if(properties.getJdbcUrl() == null){
                properties.setJdbcUrl(configProperties.getJdbcUrl());
            }
            if(properties.getUsername() == null){
                properties.setUsername(configProperties.getUsername());
            }
            if(properties.getPassword() == null){
                properties.setPassword(configProperties.getPassword());
            }
            return new HikariDataSource(properties);
        }
    }

    @ConditionalOnClass(DruidDataSource.class)
    @ConditionalOnProperty(value = "spring.datasource.taos.druid")
    @EnableConfigurationProperties(TaosDruidConfigProperties.class)
    class DruidDataSourceAutoConfig{
        @Bean
        @ConditionalOnMissingBean(name = "taosDataSource")
        public DruidDataSource taosDataSource(TaosDruidConfigProperties properties) throws Exception {
            properties.put(DruidDataSourceFactory.PROP_DRIVERCLASSNAME, currentUseHttp ? TaosHttpDriver.class.getName() : TSDBDriver.class.getName());
            if(properties.getProperty(DruidDataSourceFactory.PROP_URL) == null){
                properties.put(DruidDataSourceFactory.PROP_URL, configProperties.getJdbcUrl());
            }
            if(properties.getProperty(DruidDataSourceFactory.PROP_USERNAME) == null){
                properties.put(DruidDataSourceFactory.PROP_USERNAME, configProperties.getUsername());
            }
            if(properties.getProperty(DruidDataSourceFactory.PROP_PASSWORD) == null){
                properties.put(DruidDataSourceFactory.PROP_PASSWORD, configProperties.getPassword());
            }
            if(properties.getProperty(TSDBDriver.PROPERTY_KEY_CONFIG_DIR) == null){
                properties.put(TSDBDriver.PROPERTY_KEY_CONFIG_DIR, configProperties.getConfigDir());
            }
            if(properties.getProperty(DruidDataSourceFactory.PROP_VALIDATIONQUERY) == null){
                properties.put(DruidDataSourceFactory.PROP_VALIDATIONQUERY, TaosConfigProperties.DEFAULT_TEST_QUERY);
            }
            return (DruidDataSource) DruidDataSourceFactory.createDataSource(properties);
        }
    }

    @ConditionalOnClass({SqlSessionFactoryBean.class, SqlSessionFactory.class})
    class MapperScannerBeanRegister{

        private final ResourcePatternResolver resourceResolver = new PathMatchingResourcePatternResolver();

        /**
         * @see MybatisAutoConfiguration
         */
        @Bean
        @ConditionalOnMissingBean(name = "taosSqlSessionFactoryBean")
        public SqlSessionFactory taosSqlSessionFactoryBean(DataSource taosDataSource, @Autowired(required = false) MybatisProperties properties, ResourceLoader resourceLoader, ObjectProvider<Interceptor[]> interceptorsProvider,
                                                           ObjectProvider<TypeHandler[]> typeHandlersProvider, ObjectProvider<List<ConfigurationCustomizer>> configurationCustomizersProvider,
                                                           ObjectProvider<LanguageDriver[]> languageDriversProvider, ObjectProvider<DatabaseIdProvider> databaseIdProvider) throws Exception {
            SqlSessionFactoryBean factory = new SqlSessionFactoryBean();
            factory.setDataSource(taosDataSource);
            factory.setVfs(SpringBootVFS.class);

            applyConfiguration(factory, properties, resourceLoader,
                    configurationCustomizersProvider.getIfAvailable(), languageDriversProvider.getIfAvailable());

            Interceptor[] interceptors = interceptorsProvider.getIfAvailable();
            TypeHandler[] typeHandlers = typeHandlersProvider.getIfAvailable();
            if(!ObjectUtils.isEmpty(interceptors)){
                factory.setPlugins(interceptors);
            }
            if(!ObjectUtils.isEmpty(typeHandlers)){
                factory.setTypeHandlers(typeHandlers);
            }
            if (databaseIdProvider.getIfAvailable() != null) {
                factory.setDatabaseIdProvider(databaseIdProvider.getIfAvailable());
            }

            return factory.getObject();
        }

        private void applyConfiguration(SqlSessionFactoryBean factory, MybatisProperties properties, ResourceLoader resourceLoader,
                                        List<ConfigurationCustomizer> configurationCustomizers, LanguageDriver[] languageDrivers) {
            if(properties == null){
                String[] mapperLocations = configProperties.getMapperLocations();
                if(mapperLocations != null && mapperLocations.length > 0){
                    Resource[] resources = Stream.of(Optional.ofNullable(mapperLocations).orElse(new String[0]))
                            .flatMap(location -> Stream.of(getResources(location))).toArray(Resource[]::new);
                    factory.setMapperLocations(resources);
                }
                return;
            }
            if (StringUtils.hasText(properties.getConfigLocation())) {
                factory.setConfigLocation(resourceLoader.getResource(properties.getConfigLocation()));
            }
            org.apache.ibatis.session.Configuration configuration = properties.getConfiguration();
            if (configuration == null && !StringUtils.hasText(properties.getConfigLocation())) {
                configuration = new org.apache.ibatis.session.Configuration();
            }
            if (configuration != null && !CollectionUtils.isEmpty(configurationCustomizers)) {
                for (ConfigurationCustomizer customizer : configurationCustomizers) {
                    customizer.customize(configuration);
                }
            }
            factory.setConfiguration(configuration);

            if (properties.getConfigurationProperties() != null) {
                factory.setConfigurationProperties(properties.getConfigurationProperties());
            }
            if (StringUtils.hasLength(properties.getTypeAliasesPackage())) {
                factory.setTypeAliasesPackage(properties.getTypeAliasesPackage());
            }
            if (properties.getTypeAliasesSuperType() != null) {
                factory.setTypeAliasesSuperType(properties.getTypeAliasesSuperType());
            }
            if (StringUtils.hasLength(properties.getTypeHandlersPackage())) {
                factory.setTypeHandlersPackage(properties.getTypeHandlersPackage());
            }
            String[] mapperLocations = configProperties.getMapperLocations();
            if(mapperLocations != null && mapperLocations.length > 0){
                Resource[] resources = Stream.of(Optional.ofNullable(mapperLocations).orElse(new String[0]))
                        .flatMap(location -> Stream.of(getResources(location))).toArray(Resource[]::new);
                factory.setMapperLocations(resources);
            }else if (!ObjectUtils.isEmpty(properties.resolveMapperLocations())) {
                factory.setMapperLocations(properties.resolveMapperLocations());
            }
            Set<String> factoryPropertyNames = Stream
                    .of(new BeanWrapperImpl(SqlSessionFactoryBean.class).getPropertyDescriptors()).map(PropertyDescriptor::getName)
                    .collect(Collectors.toSet());
            Class<? extends LanguageDriver> defaultLanguageDriver = properties.getDefaultScriptingLanguageDriver();
            if (factoryPropertyNames.contains("scriptingLanguageDrivers") && !ObjectUtils.isEmpty(languageDrivers)) {
                // Need to mybatis-spring 2.0.2+
                factory.setScriptingLanguageDrivers(languageDrivers);
                if (defaultLanguageDriver == null && languageDrivers.length == 1) {
                    defaultLanguageDriver = languageDrivers[0].getClass();
                }
            }
            if (factoryPropertyNames.contains("defaultScriptingLanguageDriver")) {
                // Need to mybatis-spring 2.0.2+
                factory.setDefaultScriptingLanguageDriver(defaultLanguageDriver);
            }
        }

        private Resource[] getResources(String location) {
            try {
                return resourceResolver.getResources(location);
            } catch (IOException e) {
                return new Resource[0];
            }
        }
    }
}
