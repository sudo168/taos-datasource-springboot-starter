# taos-datasource-springboot-starter

###【功能】：

1. 内置驱动包（x64）, 客户端无需手动安装驱动包

2. 支持Http，为方便 Mac 端开发，实现了Http JDBC调用

3. 支持3种连接池：Druid、Hikari、Apache-common-pool2

4. 支持Mybatis

5. 基于多数据源设计

6. springboot应用中直接配置使用，方便快捷

7. 支持springboot JDBC actuator（DataSourceHealthIndicator）进行连接池的健康检查

###【配置使用】：

#### Maven 依赖：
```
<!--springboot-->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter</artifactId>
    <version>2.1.14.RELEASE</version>
</dependency>

<!--taos官方jdbcdriver-->
<dependency>
    <groupId>com.taosdata.jdbc</groupId>
    <artifactId>taos-jdbcdriver</artifactId>
    <version>2.0.3</version>
</dependency>

<!--taos-datasource依赖放到工程目录-->
<dependency>
    <groupId>net.ewent.taos</groupId>
    <artifactId>taos-datasource-springboot-starter</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    <scope>system</scope>
    <systemPath>${basedir}/src/main/lib/taos-datasource-springboot-starter-1.0.0-SNAPSHOT.jar</systemPath>
</dependency>

<!--mybatis-->
<dependency>
    <groupId>org.mybatis.spring.boot</groupId>
    <artifactId>mybatis-spring-boot-starter</artifactId>
    <version>2.1.2</version>
</dependency>

<!-- 以下3个连接池选一个使用即可 -->
<dependency>
    <groupId>org.apache.commons</groupId>
    <artifactId>commons-pool2</artifactId>
    <version>2.8.0</version>
</dependency>
<dependency>
    <groupId>com.alibaba</groupId>
    <artifactId>druid</artifactId>
    <version>1.1.22</version>
</dependency>
<dependency>
    <groupId>com.zaxxer</groupId>
    <artifactId>HikariCP</artifactId>
    <version>3.4.5</version>
</dependency>
```

#### application.yml配置：

> 配置前缀为：spring.datasource.taos

1、基础配置

```
spring.datasource.taos:
    jdbc-url: jdbc:TAOS://127.0.0.1:0/you_database?user=root&password=taosdata&enableMicrosecond=true
    use-http: false # 默认false, 不使用http JDBC
    mac-force-http: true # 默认true, 会自动检查系统环境，发现是Mac时直接使用http JDBC
    mapper-locations: # 与mybatis的mapperLocations配置作用一致，两者用其一即可
        - classpath*:sqlmap/*-mapper.xml
```

2、连接池配置

```
# Druid配置：
  spring.datasource.taos:
     druid:
        maxActive: 10 # maximum number of connection in the pool
        initialSize: 3 # initial number of connection
        maxWait: 10000 # maximum wait milliseconds for get connection from pool
        minIdle: 3 # minimum number of connection in the pool
        validationQuery: select server_status()
        # 更多配置请参看druid官方文档
        # https://github.com/alibaba/druid

# Hikari配置：
  spring.datasource.taos:
     hikari:
        minimumIdle: 3 # minimum number of idle connection
        maximumPoolSize: 10 # maximum number of connection in the pool
        connectionTimeout: 10000 # maximum wait milliseconds for get connection from pool
        connectionTestQuery: select server_status()
        # 更多配置请参看hikari官方文档
        # https://github.com/brettwooldridge/HikariCP

# Apache-common-pool2配置：
  spring.datasource.taos:
     pool2:
        maxTotal: 10
        maxIdle: 3
        testQuery: select server_status()
        # 更多配置请参看Apache-common-pool2官方文档
        # http://commons.apache.org/proper/commons-pool/index.html
```

3、Mybatis 集成
> 无需特别配置，常规使用就好

```
指定mapper扫描，如系统中没有其他SqlSessionFactory对象，sqlSessionFactoryRef配置可省略
@MapperScan(basePackages = "xxx.taos.mapper", sqlSessionFactoryRef = "taosSqlSessionFactoryBean")

@Mapper
public interface OrderMapper {
    ......
}

@Service
public class OrderServiceImpl implements OrderService {
    @Autowired
    OrderMapper orderMapper;
}
```

4、自定义驱动包

默认会优先加载系统lib中的驱动包，如果系统中没有安装驱动包，则加载jar包中的驱动使用

windows 下可以将 C:\TDengine\driver\taos.dll 拷贝到 C:\Windows\System32\ 目录下。
linux   下将建立如下软链 ln -s /usr/local/taos/driver/libtaos.so.x.x.x.x /usr/lib/libtaos.so 即可。

更多内容请查看taos官方文档：https://www.taosdata.com/cn/documentation20/connector/

### Apache IoTDB JDBC

此 starter 同时提供 Apache IoTDB 2.0.x JDBC 支持，默认使用 `iotdb-jdbc` 2.0.11。配置 `spring.datasource.iotdb.url` 后会自动创建名为 `iotdbDataSource` 的轻量 `DataSource`；未配置 IoTDB URL 时不会创建该 Bean。

```yaml
spring:
  datasource:
    iotdb:
      url: jdbc:iotdb://127.0.0.1:6667/
      username: ${IOTDB_USERNAME}
      password: ${IOTDB_PASSWORD}
      # 可选：IoTDB JDBC driver properties，按所用驱动版本填写
      properties: {}
```

使用标准 JDBC API 获取连接，并在调用方关闭连接、语句和结果集：

```java
@Autowired
@Qualifier("iotdbDataSource")
private DataSource iotdbDataSource;

public void query() throws SQLException {
    try (Connection connection = iotdbDataSource.getConnection();
         Statement statement = connection.createStatement();
         ResultSet results = statement.executeQuery("SELECT * FROM root.example.device LIMIT 100")) {
        while (results.next()) {
            // 按列或标签读取当前行
        }
    }
}
```

该 `DataSource` 不持有连接，也不内置连接池。生产应用可用 HikariCP、Druid 等连接池包装 `iotdbDataSource`，并由应用关闭连接池。`DataSource.setLoginTimeout` 遵循 JDBC `DriverManager` 的 JVM 级全局超时设置；如应用有多个 JDBC 驱动，应避免在运行期间从不同数据源反复修改该值。IoTDB 凭据建议通过环境变量或密钥管理服务注入，额外 `properties` 会原样传给 IoTDB JDBC 驱动。
