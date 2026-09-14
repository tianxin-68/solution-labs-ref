# bi_queryer 改造为 Spring Boot 方案

## 1. 背景与目标

当前 `bi_queryer` 是传统 SSM Web 工程：

- 构建方式：Maven，`packaging=war`，产物名 `bi_queryer`。
- 启动方式：`web.xml` 注册 `ContextLoaderListener`、`DispatcherServlet`、`CharacterEncodingFilter`、`RequestContextListener`、自定义 `com.bi.queryer.sys.startup.StartUp`。
- Spring 配置：根上下文加载 `classpath*:spring-*.xml`，MVC 上下文加载 `classpath:web-mvc.xml`。
- MVC：`web-mvc.xml` 开启 `mvc:annotation-driven`、扫描 `com.bi.queryer`、注册 `AuthorityInterceptor`、`CommonsMultipartResolver`、JSP 视图解析器。
- 数据访问：`spring-db-pool.xml` 中配置多数据源、自定义 `XBasicDataSource`、`DynamicDataSource`、`DataSourceTransactionManager`、MyBatis `SqlSessionFactoryBean`、`MapperScannerConfigurer`。
- Redis：`spring-redis.xml` 中使用 Jedis + Spring Data Redis 1.5 的 XML 配置。
- 配置文件：`jdbc.properties`、`jdbc.dev.properties`、`jdbc.test.properties`、`jdbc.ut.properties`、`jdbc.debug.properties`、`app.properties`、`log4j.properties` 等。

改造目标：

- 将应用升级为 Spring Boot 启动模型，减少 `web.xml` 和 XML 容器启动依赖。
- 保持现有 Controller、Service、Mapper、SQL、动态数据源、Redis、拦截器、JSP 页面行为兼容。
- 支持本地 `java -jar` 启动和服务端标准化部署。
- 改造过程可灰度、可回滚，避免一次性大范围重写业务代码。

## 2. 总体路线

建议采用“兼容迁移优先，逐步原生化”的两阶段路线。

### 阶段一：Spring Boot 兼容启动

目标是让现有系统先在 Spring Boot 容器内跑起来，不立即大规模删除 XML。

关键动作：

- 引入 Spring Boot 2.3.x 或 2.7.x。
- 保持 Java 8 的前提下，优先选择 Spring Boot 2.3.x/2.7.x；不建议直接上 Spring Boot 3.x，因为 Boot 3 需要 Java 17 且从 `javax.*` 迁移到 `jakarta.*`，改造面过大。
- 新增 `SsmServerApplication` 启动类。
- 使用 `@ImportResource` 继续加载现有 `spring-applicationContext.xml`、`spring-db-pool.xml`、`spring-redis.xml`，确保 Bean 命名和注入关系不变。
- 用 `WebMvcConfigurer` 接管 `web-mvc.xml` 中的 MVC 配置，或者阶段一继续 `@ImportResource("classpath:web-mvc.xml")`。
- 用 `ServletListenerRegistrationBean` 或 `ApplicationRunner` 替代 `web.xml` 中的 `StartUp` Listener。
- 暂时保留 JSP 目录结构和视图解析规则。

### 阶段二：配置原生化

目标是减少 XML、统一配置模型和依赖版本。

关键动作：

- 将多数据源、事务、MyBatis、Redis、MVC 拦截器从 XML 迁移为 Java Config。
- 将 `jdbc*.properties`、`app.properties` 合并或映射到 `application.yml`，按 profile 区分环境。
- 将 Log4j 1.x 迁移为 Logback 或 Log4j2。
- 清理重复、冲突、过旧依赖。
- 将 WAR 部署逐步切换为可执行 JAR 部署；如必须兼容外部 Tomcat，可保留 WAR 模式。

## 3. 版本与依赖策略

### 3.1 推荐版本

保守推荐：

- Java：继续 Java 8。
- Spring Boot：`2.3.12.RELEASE` 或 `2.7.18`。
- Spring Framework：由 Spring Boot BOM 统一管理，不再手工声明 `spring-*` 版本。
- Servlet：由 `spring-boot-starter-web` 管理。
- MyBatis：优先使用 `mybatis-spring-boot-starter`；阶段一可保留现有 `mybatis` + `mybatis-spring`。

选型说明：

- 当前 POM 已大量使用 Spring 5.2.5，Spring Boot 2.3.x 与 Spring 5.2.x 匹配度较高，迁移成本最低。
- 如果团队希望使用更长尾的 Spring Boot 2 版本，可选 2.7.18，但需要更仔细验证旧 Jedis、Spring Data Redis、MyBatis 版本兼容性。
- 不建议直接升级到 Spring Boot 3.x，除非同步规划 Java 17、Tomcat 10、`javax.servlet` 到 `jakarta.servlet` 的全量改造。

### 3.2 POM 改造方向

阶段一建议修改为：

```xml
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>2.3.12.RELEASE</version>
    <relativePath/>
</parent>

<packaging>jar</packaging>

<dependencies>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-jdbc</artifactId>
    </dependency>
    <dependency>
        <groupId>org.mybatis.spring.boot</groupId>
        <artifactId>mybatis-spring-boot-starter</artifactId>
        <version>2.1.4</version>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>

<build>
    <finalName>bi_queryer</finalName>
    <plugins>
        <plugin>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-maven-plugin</artifactId>
        </plugin>
    </plugins>
</build>
```

清理建议：

- 删除手工声明的 `spring-aop`、`spring-beans`、`spring-context`、`spring-core`、`spring-expression`、`spring-jdbc`、`spring-test`、`spring-tx`、`spring-web`、`spring-webmvc` 等，交给 Boot BOM。
- 移除 `spring-orm 3.2.0.RELEASE`，当前与 Spring 5.2.5 不一致。
- 删除 `servlet-api 2.5`，由内嵌 Tomcat 提供。
- 处理重复 Jedis 依赖：当前同时存在 `jedis 2.8.0` 和 `jedis 2.4.2`，应保留一个版本。
- 评估并升级高风险老依赖：`log4j 1.2.16`、`fastjson 1.2.70`、`commons-fileupload 1.3.2`、`commons-httpclient 3.1`、`mysql-connector-java 5.1.48`。

## 4. 工程结构改造

建议目标结构：

```text
bi_queryer
├── pom.xml
├── src/main/java
│   └── cn/bi_queryer/bi
│       ├── SsmServerApplication.java
│       ├── config
│       │   ├── WebMvcConfig.java
│       │   ├── DataSourceConfig.java
│       │   ├── MybatisConfig.java
│       │   ├── RedisConfig.java
│       │   └── StartupConfig.java
│       ├── sys
│       └── ssm
├── src/main/resources
│   ├── application.yml
│   ├── application-dev.yml
│   ├── application-test.yml
│   ├── mybatis-config.xml
│   └── mybatis/mapper
└── src/main/webapp
    └── WEB-INF/page
```

阶段一可保留：

- `src/main/resources/spring-applicationContext.xml`
- `src/main/resources/spring-db-pool.xml`
- `src/main/resources/spring-redis.xml`
- `src/main/resources/web-mvc.xml`
- `src/main/webapp/WEB-INF/page`

阶段二再逐步删除 XML。

## 5. 启动入口改造

新增启动类：

```java
package com.bi.queryer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ImportResource;

@SpringBootApplication(scanBasePackages = "com.bi.queryer")
@ImportResource({
        "classpath:spring-applicationContext.xml",
        "classpath:spring-db-pool.xml",
        "classpath:spring-redis.xml"
})
public class SsmServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(SsmServerApplication.class, args);
    }
}
```

如果阶段一继续复用 MVC XML，可临时增加：

```java
@ImportResource({
        "classpath:spring-applicationContext.xml",
        "classpath:spring-db-pool.xml",
        "classpath:spring-redis.xml",
        "classpath:web-mvc.xml"
})
```

注意事项：

- `spring-applicationContext.xml` 和 `web-mvc.xml` 都扫描 `com.bi.queryer`，Boot 启动类也会扫描。阶段一要避免重复注册 Bean。
- 更推荐阶段一只保留根配置 XML，MVC 配置改为 Java Config，减少 Controller 被重复扫描的风险。

## 6. web.xml 替换方案

原 `web.xml` 功能映射如下：

| web.xml 配置 | Spring Boot 替代方案 |
| --- | --- |
| `ContextLoaderListener` | Boot 自动创建根容器 |
| `DispatcherServlet` | `spring-boot-starter-web` 自动注册 |
| `CharacterEncodingFilter` | `server.servlet.encoding.*` 或 `FilterRegistrationBean` |
| `StartUp` Listener | `ApplicationRunner` / `ServletListenerRegistrationBean` |
| `RequestContextListener` | `RequestContextListener` Bean |
| `IntrospectorCleanupListener` | 一般可删除；如观察到内存泄漏告警再注册 |
| `session-timeout=120` | `server.servlet.session.timeout=120m` |
| `welcome-file=index.jsp` | 由 MVC 视图或静态首页处理 |

推荐配置：

```yaml
server:
  servlet:
    context-path: /
    session:
      timeout: 120m
    encoding:
      charset: UTF-8
      enabled: true
      force: true
```

`StartUp` 替代方案：

```java
@Configuration
public class StartupConfig {

    @Bean
    public ApplicationRunner envInitializer() {
        return args -> EnvVariableManager.initialize(null);
    }
}
```

如果 `EnvVariableManager.initialize` 强依赖 `ServletContextEvent`，则用 `ServletContextInitializer`：

```java
@Bean
public ServletContextInitializer envServletInitializer() {
    return servletContext -> EnvVariableManager.initialize(servletContext);
}
```

需要同步调整 `EnvVariableManager`，将初始化入口从 `ServletContextEvent` 解耦为 `ServletContext` 或普通配置对象。

## 7. MVC 改造方案

`web-mvc.xml` 中的能力需要迁移为：

- `@EnableWebMvc`：不建议直接加。Boot 已提供 MVC 自动配置，直接实现 `WebMvcConfigurer`。
- 拦截器：注册 `AuthorityInterceptor`。
- 静态资源：若未来有静态目录，配置 `addResourceHandlers`。
- 消息转换器：保留 `text/html;charset=UTF-8` JSON 兼容逻辑。
- 文件上传：用 `spring.servlet.multipart.*` 或继续声明 `CommonsMultipartResolver`。
- JSP：配置 prefix/suffix。

示例：

```java
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Bean
    public AuthorityInterceptor authorityInterceptor() {
        return new AuthorityInterceptor();
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authorityInterceptor()).addPathPatterns("/**");
    }

    @Override
    public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
        MappingJackson2HttpMessageConverter jackson = new MappingJackson2HttpMessageConverter();
        jackson.setSupportedMediaTypes(Collections.singletonList(
                MediaType.valueOf("text/html;charset=UTF-8")
        ));
        converters.add(jackson);

        StringHttpMessageConverter stringConverter = new StringHttpMessageConverter(StandardCharsets.UTF_8);
        stringConverter.setSupportedMediaTypes(Collections.singletonList(MediaType.TEXT_PLAIN));
        converters.add(stringConverter);
    }
}
```

JSP 配置：

```yaml
spring:
  mvc:
    view:
      prefix: /WEB-INF/page/
      suffix: .jsp
```

文件上传：

```yaml
spring:
  servlet:
    multipart:
      max-file-size: 50MB
      max-request-size: 50MB
      enabled: true
```

如果必须继续使用 `CommonsMultipartResolver`，需要保留 `commons-fileupload`，并显式声明 `multipartResolver` Bean。

## 8. 数据源与事务改造方案

当前系统有以下数据源：

- `default`
- `etl`
- `doris_master`
- `doris_slave01`
- `hw_doris_master`
- `hw_doris_slave01`
- `data_studio`
- `trino_master`
- `trino_slave01`
- `trino_slave02`
- `olap`
- `olap_ut`
- `agent`

同时使用：

- `com.bi.queryer.sys.db.XBasicDataSource`
- `com.bi.queryer.sys.db.DynamicDataSource`
- `com.bi.queryer.sys.db.DataSourceContextHolder`
- `DataSourceTransactionManager`
- AOP 事务 advice 按方法名前缀匹配 `save*`、`update*`、`delete*`、`add*`、`insert*`

当前动态切换链路必须保留：

```text
BaseDao 接收 DataSourceType
-> DataSourceContextHolder.setType(dsType.getId()) 写入 ThreadLocal
-> SqlSessionFactory.openSession() 获取连接
-> DynamicDataSource.determineCurrentLookupKey() 读取 DataSourceContextHolder.getType()
-> AbstractRoutingDataSource 路由到 targetDataSources 中的 default / doris_master / trino_master 等数据源
```

因此，Spring Boot 改造后必须继续让 MyBatis 的 `SqlSessionFactory` 绑定到 `dynamicDataSource`，不能让 Boot 自动生成的单一 `spring.datasource` 接管 MyBatis。否则 `BaseDao` 中按 `DataSourceType` 切换 MySQL、Doris、Trino 的逻辑会失效。

### 8.1 阶段一

保留 `spring-db-pool.xml`。

原因：

- 多数据源数量多。
- 存在自定义 `DynamicDataSource` 和 `XBasicDataSource`。
- 业务代码中 `BaseDao` 多处直接设置 `DataSourceContextHolder`，需要保持运行时行为稳定。

需要注意：

- Boot 默认会尝试自动配置单数据源。若保留 XML 里的 `dynamicDataSource`，需避免 Boot 自动数据源配置误判。
- `sqlSessionFactory` 必须继续引用 `dynamicDataSource`，不能引用某个具体物理数据源，例如 `defaultDataSource`。
- `DataSourceContextHolder` 当前基于 `ThreadLocal`，`BaseDao` 多数方法只 `setType`，没有在 `finally` 中统一 `clearType()`。在 Spring Boot 内嵌容器、线程池复用、异步请求或后续引入 `@Async` 时，存在同一线程上一次 Doris/Trino 路由污染后续默认 MySQL 查询的风险。阶段一至少要保持“每次 DAO 调用前显式设置数据源”；阶段二建议在 `BaseDao` 或统一 AOP 中保证 `finally` 清理。
- 可通过显式 Bean 名称、`@Primary` 或排除自动配置处理：

```java
@SpringBootApplication(exclude = {
        DataSourceAutoConfiguration.class
})
```

如果排除 `DataSourceAutoConfiguration`，MyBatis 自动配置也可能受影响，因此阶段一 MyBatis 也建议继续用 XML。

### 8.2 阶段二

将数据源迁移到 Java Config：

```yaml
ssm:
  datasource:
    default:
      driver-class-name: com.mysql.jdbc.Driver
      url: jdbc:mysql://...
      username: xxx
      password: xxx
      validation-query: select 1
    olap:
      driver-class-name: com.mysql.jdbc.Driver
      url: jdbc:mysql://...
      username: xxx
      password: xxx
```

Java Config 方向：

```java
@Configuration
@EnableTransactionManagement
public class DataSourceConfig {

    @Bean
    public DataSource defaultDataSource(SsmDataSourceProperties properties) {
        return buildBasicDataSource(properties.getDefault());
    }

    @Bean
    @Primary
    public DataSource dynamicDataSource(Map<String, DataSource> dataSources) {
        DynamicDataSource dynamic = new DynamicDataSource();
        Map<Object, Object> targets = new HashMap<>();
        targets.put("default", dataSources.get("defaultDataSource"));
        targets.put("olap", dataSources.get("olapDataSource"));
        dynamic.setTargetDataSources(targets);
        dynamic.setDefaultTargetDataSource(dataSources.get("defaultDataSource"));
        return dynamic;
    }

    @Bean
    public PlatformTransactionManager transactionManager(DataSource dynamicDataSource) {
        return new DataSourceTransactionManager(dynamicDataSource);
    }
}
```

事务 XML advice 的迁移方式：

- 优先改造为 `@Transactional`，在 Service 层标注事务边界。
- 短期可保留 XML 中的 `tx:advice`。
- 如迁移为 Java AOP，需要明确 pointcut 范围，避免误拦截 Controller 或 DAO。

建议最终目标：

- Service 写方法显式使用 `@Transactional(rollbackFor = Throwable.class)`。
- 查询方法不加事务或使用 `readOnly = true`。
- 统一由 `dynamicDataSource` 参与事务。

## 9. MyBatis 改造方案

当前 MyBatis 配置：

- `mybatis-config.xml`
- Mapper XML：`classpath*:mybatis/**/*.xml`
- Mapper 接口包：`com.bi.queryer.mapper`
- PageHelper 插件在 `mybatis-config.xml` 中配置。

阶段一保留 XML：

```xml
<bean id="sqlSessionFactory" class="org.mybatis.spring.SqlSessionFactoryBean">
    <property name="configLocation" value="classpath:mybatis-config.xml"/>
    <property name="dataSource" ref="dynamicDataSource"/>
    <property name="mapperLocations" value="classpath*:mybatis/**/*.xml"/>
</bean>
```

阶段二迁移为 Boot 配置：

```yaml
mybatis:
  config-location: classpath:mybatis-config.xml
  mapper-locations: classpath*:mybatis/**/*.xml
```

启动类或配置类：

```java
@MapperScan("com.bi.queryer.mapper")
```

PageHelper 可继续在 `mybatis-config.xml` 中保留，也可迁移为 Bean。建议先不动，减少分页 SQL 行为差异。

## 10. Redis 改造方案

当前 Redis 配置特点：

- 使用 `JedisPoolConfig`
- 使用 `JedisConnectionFactory`
- `RedisTemplate` value/hashValue 使用 `JdkSerializationRedisSerializer`
- `StringRedisTemplate` 使用字符串序列化
- `redisTemplate.enableTransactionSupport=true`

阶段一保留 `spring-redis.xml`，确保缓存序列化和事务行为不变。

阶段二迁移为：

```yaml
spring:
  redis:
    host: ${redis.hostName}
    port: ${redis.port}
    database: ${redis.database}
    timeout: ${redis.timeout}ms
    jedis:
      pool:
        max-active: ${redis.maxTotal}
        max-idle: ${redis.maxIdle}
        max-wait: ${redis.maxWaitMillis}ms
```

Java Config 中必须保留序列化策略：

```java
@Bean
public RedisTemplate<Object, Object> redisTemplate(RedisConnectionFactory factory) {
    RedisTemplate<Object, Object> template = new RedisTemplate<>();
    template.setConnectionFactory(factory);
    template.setKeySerializer(new StringRedisSerializer());
    template.setValueSerializer(new JdkSerializationRedisSerializer());
    template.setHashKeySerializer(new StringRedisSerializer());
    template.setHashValueSerializer(new JdkSerializationRedisSerializer());
    template.setEnableTransactionSupport(true);
    return template;
}
```

不要在第一轮直接切换为 JSON 序列化，否则历史缓存兼容性会受影响。

## 11. 配置文件改造方案

当前配置分散在：

- `jdbc.properties`
- `jdbc.dev.properties`
- `jdbc.test.properties`
- `jdbc.ut.properties`
- `jdbc.debug.properties`
- `app.properties`
- `META-INF/app.properties`
- `logging.properties`
- `log4j.properties`

建议迁移为：

```text
application.yml
application-dev.yml
application-test.yml
application-ut.yml
application-debug.yml
```

阶段一可以继续使用 `JDBCPropertyPlaceHolder` 读取 `jdbc.properties`，只新增 Boot 必需配置：

```yaml
spring:
  profiles:
    active: dev
server:
  port: 8080
```

阶段二再将旧 key 映射到新配置结构。为了降低风险，可以保留旧 key 名称：

```yaml
jdbc:
  driver: com.mysql.jdbc.Driver
  url: jdbc:mysql://...
  username: xxx
  password: xxx
  validation: select 1
```

这样 `${jdbc.driver}` 等占位符在 XML 兼容阶段仍可继续使用。

## 12. 日志改造方案

当前使用：

- `log4j 1.2.16`
- `slf4j-log4j12 1.7.5`
- `log4j.properties`
- MyBatis `logImpl=LOG4J`

建议分两步：

阶段一：

- 若短期只求启动兼容，可暂保留 Log4j 1.x，但需要排除 Boot 默认 logging 冲突。
- 更推荐直接迁移到 Logback，减少 Log4j 1.x 安全和兼容风险。

阶段二：

- 删除 `log4j` 和 `slf4j-log4j12`。
- 使用 `spring-boot-starter-logging` 默认 Logback。
- 新增 `logback-spring.xml`。
- 将 MyBatis 日志改为 `SLF4J`：

```xml
<setting name="logImpl" value="SLF4J"/>
```

## 13. JSP 与部署形态

当前 Web 目录只有 `download/1.txt` 和 `WEB-INF/web.xml`，但 MVC 配置指向 `/WEB-INF/page/*.jsp`。需要确认实际 JSP 页面是否在完整工程或部署包中存在。

如果必须保留 JSP：

- 可继续使用 WAR。
- 或使用可执行 WAR/JAR，但 JSP 在内嵌 Tomcat 下需要额外依赖和打包验证。
- 增加依赖：

```xml
<dependency>
    <groupId>org.apache.tomcat.embed</groupId>
    <artifactId>tomcat-embed-jasper</artifactId>
</dependency>
<dependency>
    <groupId>javax.servlet</groupId>
    <artifactId>jstl</artifactId>
</dependency>
```

推荐：

- 若系统主要提供 API，逐步移除 JSP 视图依赖。
- 若仍有页面渲染，短期保留 WAR 或可执行 WAR，避免 JAR 内 JSP 资源路径问题。

部署建议：

- 第一阶段：可执行 WAR 或普通 WAR，兼容现有部署方式。
- 第二阶段：确认无 JSP 或 JSP 可正常内嵌运行后，切换可执行 JAR。

## 14. 代码改造清单

### 14.1 必做

- 新增 `com.bi.queryer.SsmServerApplication`。
- 调整 POM，引入 Spring Boot parent 和 starter。
- 移除 `web.xml` 启动依赖，或将 `web.xml` 仅作为 WAR 兼容文件保留。
- 将 `StartUp` 初始化逻辑改为 Boot 生命周期。
- 注册 `AuthorityInterceptor`。
- 保持 `dynamicDataSource`、`sqlSessionFactory`、`redisTemplate` Bean 名称不变。
- 增加 `application.yml`，配置端口、编码、session、JSP prefix/suffix、上传大小。
- 验证所有 Mapper XML 可被扫描。

### 14.2 建议做

- 清理重复 Spring 依赖，由 Boot BOM 管理。
- 清理重复 Jedis 依赖。
- 将 `spring-orm 3.2.0.RELEASE` 升级或删除。
- 将 `log4j 1.x` 切换为 Logback/Log4j2。
- 统一 `jdbc.*` 配置到 profile。
- 将事务从 XML advice 迁移到 `@Transactional`。

### 14.3 暂不建议第一阶段做

- 不要直接升级 Spring Boot 3.x。
- 不要同时切换 Java 17。
- 不要同时重写 MyBatis Mapper。
- 不要直接改变 Redis 序列化方式。
- 不要直接替换动态数据源实现。
- 不要一次性将所有 XML 删除。

## 15. 分阶段实施计划

### P0：改造前盘点

输出物：

- Controller/API 清单。
- Mapper XML 与 Mapper 接口绑定清单。
- 数据源使用清单，包括 `DataSourceContextHolder.setType(...)` 调用点。
- Redis key 与序列化使用清单。
- JSP 页面实际使用清单。
- 运行时依赖冲突报告：`mvn dependency:tree`。

验收标准：

- 明确哪些接口是核心回归范围。
- 明确部署形态：可执行 JAR、可执行 WAR 或外部 Tomcat WAR。

### P1：Boot 兼容启动

任务：

- 修改 POM 为 Spring Boot。
- 新增启动类。
- 引入 `@ImportResource` 加载现有 XML。
- 新增 `application.yml`。
- 处理日志和 Servlet 依赖冲突。
- 本地启动通过。

验收标准：

- `mvn clean package` 通过。
- `java -jar target/bi_queryer.jar` 或 `java -jar target/bi_queryer.war` 可启动。
- Spring 容器无重复 Bean、循环依赖、XML 解析异常。
- 健康检查接口或已有基础接口可访问。

### P2：MVC 与启动生命周期迁移

任务：

- 将 `web-mvc.xml` 迁移为 `WebMvcConfig`。
- 替换 `StartUp` Listener。
- 替换字符编码、session、上传配置。
- 删除 `web.xml` 对 Boot 启动的强依赖。

验收标准：

- 所有 Controller 路由保持不变。
- 权限拦截器行为保持不变。
- 文件上传接口通过。
- JSP 页面或相关跳转通过。

### P3：数据源、MyBatis、Redis Java Config 化

任务：

- 将 `spring-db-pool.xml` 迁移为 `DataSourceConfig` 和 `MybatisConfig`。
- 将数据源配置迁移到 `application-*.yml`。
- 将 `spring-redis.xml` 迁移为 `RedisConfig`。
- 保持 `dynamicDataSource`、`sqlSessionFactory`、`redisTemplate` 等 Bean 名称。

验收标准：

- 13 个数据源均能按 key 路由。
- 事务回滚逻辑与原系统一致。
- MyBatis 分页行为一致。
- Redis 读写兼容历史缓存。

### P4：依赖治理与日志治理

任务：

- 删除旧 Spring 显式依赖。
- 删除 Servlet 2.5。
- 处理重复 Jedis。
- 替换 Log4j 1.x。
- 升级高风险三方库。

验收标准：

- `mvn dependency:tree` 无明显版本冲突。
- 日志格式、路径、级别符合线上规范。
- 核心接口压测无明显性能回退。

## 16. 风险与应对

| 风险 | 影响 | 应对 |
| --- | --- | --- |
| XML 与 Boot 自动扫描重复注册 Bean | 启动失败或 Bean 覆盖 | 阶段一控制扫描范围，必要时设置 `spring.main.allow-bean-definition-overriding=true` 仅作临时措施 |
| 多数据源自动配置冲突 | MyBatis 使用错误数据源 | 阶段一保留 XML，必要时排除 `DataSourceAutoConfiguration` |
| Redis 序列化变化 | 历史缓存无法读取 | 第一阶段保留 JDK 序列化 |
| JSP 在可执行 JAR 中不可用 | 页面 404 或视图解析失败 | 优先使用可执行 WAR，或确认 JSP 打包方案 |
| Log4j 迁移导致日志丢失 | 排障困难 | 先双环境验证日志路径、级别、滚动策略 |
| Spring Boot BOM 改变三方依赖版本 | 运行期兼容问题 | 使用 `dependencyManagement` 锁定关键老依赖，逐个升级 |
| 事务 advice 迁移遗漏 | 数据一致性风险 | 第一阶段保留 XML 事务，后续逐个 Service 显式加 `@Transactional` |
| `EnvVariableManager` 依赖 ServletContextEvent | 启动初始化失败 | 抽象初始化参数，改为 `ServletContext` 或配置属性 |
| `DataSourceContextHolder` 的 `ThreadLocal` 未清理 | 线程复用时可能把上一次 Doris/Trino/MySQL 路由带到后续请求，导致查错库或写错库 | 在 `BaseDao` 每次 DB 操作的 `finally` 中调用 `clearType()`，或通过 DAO/AOP 统一清理；事务场景要在事务完成后再清理 |

## 17. 回归验证范围

基础验证：

- 应用启动。
- 编码过滤器生效。
- session timeout 配置生效。
- 权限拦截器生效。
- 全局异常处理生效。
- 上传接口生效。

接口验证：

- `HealthController` 相关接口。
- 登录态/用户态接口。
- 权限接口。
- 查询接口：`SSMApiController`、`OlapApiController`、`SSDQueryController`。
- 模板接口：`TemplateController`、`AnalysisTemplateController`。
- 导出接口：`SSDExporterController`。
- 缓存刷新接口：`CacheController`。
- LLM/Agent 相关接口。

数据验证：

- 默认 MySQL 查询。
- OLAP 查询。
- Doris 查询。
- Trino 查询。
- ETL 数据源查询。
- Agent 数据源查询。
- 事务提交和回滚。
- PageHelper 分页。

缓存验证：

- `RedisTemplate` 写入和读取。
- `StringRedisTemplate` 写入和读取。
- 历史 JDK 序列化 value 读取。
- 缓存刷新功能。

部署验证：

- 本地启动。
- 测试环境启动。
- 停机钩子。
- 日志输出。
- JVM 参数。
- 端口和 context-path。

## 18. 推荐落地顺序

推荐按以下顺序实施：

1. 建分支，冻结一版可回归基线。
2. 执行 `mvn dependency:tree`，整理冲突依赖。
3. 引入 Spring Boot 2.3.12，保留 XML 兼容启动。
4. 解决启动期 Bean 冲突、日志冲突、Servlet 依赖冲突。
5. 完成基础接口回归。
6. 将 `web-mvc.xml` 迁移到 Java Config。
7. 将 `StartUp` 从 Listener 迁移到 Boot 生命周期。
8. 将 Redis 配置迁移到 Java Config。
9. 将 MyBatis 和多数据源迁移到 Java Config。
10. 清理 XML 和旧依赖。
11. 执行完整回归、压测、上线灰度。

## 19. 建议的最小可交付版本

最小可交付版本只做以下改动：

- Spring Boot 2.3.12 parent。
- `spring-boot-starter-web`、`spring-boot-starter-jdbc`、`spring-boot-maven-plugin`。
- 新增 `SsmServerApplication`。
- `@ImportResource` 加载现有 Spring XML。
- 新增 `application.yml`。
- 保留 `spring-db-pool.xml`、`spring-redis.xml`、`mybatis-config.xml`、Mapper XML。
- 保留现有业务代码。

这个版本的价值是先验证“容器启动模型切换”是否可行；若失败，回滚成本最低。

## 20. 结论

该项目可以改造成 Spring Boot，但不适合一次性重写式升级。最佳方案是：

- 第一阶段以 Spring Boot 承载现有 SSM XML 配置，实现兼容启动。
- 第二阶段逐步迁移 MVC、启动生命周期、Redis、MyBatis、多数据源、事务配置。
- 第三阶段治理依赖、日志和部署形态。

改造重点不在 Controller 或业务 Service，而在启动模型、依赖版本、多数据源、MyBatis、Redis 序列化、JSP/部署形态和日志体系。只要第一阶段严格保持 Bean 名称、Mapper 扫描路径、动态数据源路由 key、Redis 序列化方式不变，整体风险可控。
