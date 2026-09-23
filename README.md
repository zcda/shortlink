# ShortLink

基于 Spring Boot、MySQL、Redis、RabbitMQ、ShardingSphere 和 Vue 的短链接服务。

## 项目结构

```text
project/      短链接核心服务、跳转和统计落库
admin/        管理端服务
gateway/      网关服务
console-vue/  Vue 管理控制台
resources/    数据库表结构和初始化脚本
jmeter/       短链接跳转压测脚本及报告
```

## 依赖服务

使用 Docker Compose 启动 MySQL、Redis、RabbitMQ 和 Nacos：

```powershell
docker compose up -d
```

开发环境默认配置：

```text
MySQL:    <MYSQL_HOST>:<MYSQL_PORT>/<DATABASE>, <MYSQL_USER>/<MYSQL_PASSWORD>
Redis:    <REDIS_HOST>:<REDIS_PORT>, password <REDIS_PASSWORD>
RabbitMQ: <RABBITMQ_HOST>:<RABBITMQ_PORT>, <RABBITMQ_USER>/<RABBITMQ_PASSWORD>
Nacos:    <NACOS_HOST>:<NACOS_PORT>
```

## 启动核心服务

项目使用 Java 17 编译目标。当前机器使用 Java 24 时，根 `pom.xml` 已显式配置 Lombok 注解处理器。

```powershell
mvn "-DskipTests" spring-boot:run "-pl" project
```

## 统计写入模式

默认使用 RabbitMQ 异步写入：

```powershell
$env:SHORT_LINK_STATS_MODE="mq"
```

同步写库模式：

```powershell
$env:SHORT_LINK_STATS_MODE="sync"
```

两种模式共用 `ShortLinkStatsPersistenceService`，保证统计落库逻辑一致。

## RabbitMQ 消费调优

当前推荐配置为 8 个消费者、32 个数据库连接：

```powershell
$env:RABBITMQ_CONCURRENCY="8"
$env:RABBITMQ_MAX_CONCURRENCY="8"
$env:RABBITMQ_PREFETCH="100"
$env:DB_MAX_POOL_SIZE="32"
$env:DB_MIN_IDLE="8"
```

地区解析可在压测时关闭，避免高德 HTTP 请求影响数据库写入吞吐：

```powershell
$env:SHORT_LINK_STATS_LOCALE_ENABLED="false"
```

如果需要保留地区统计，可以启用 Redis IP 地区缓存：

```powershell
$env:SHORT_LINK_STATS_LOCALE_ENABLED="true"
$env:SHORT_LINK_STATS_LOCALE_CACHE_ENABLED="true"
```

## JMeter 压测

JMeter 脚本位于：

```text
jmeter/short-link-redirect-load-test.jmx
```

脚本默认压测 50 个线程、30 秒预热、300 秒持续时间。JMeter 不在 PATH 时，可先设置 `JMETER_HOME`：

```powershell
& "$env:JMETER_HOME\bin\jmeter.bat" -n `
  -t ".\jmeter\short-link-redirect-load-test.jmx" `
  -l ".\jmeter\result.jtl" `
  -e `
  -o ".\jmeter\report"
```

MQ 模式压测结束后，需要等待 `shortlink.stats.queue` 清空，再核对数据库统计数据。

## 统计写入链路

```text
短链接跳转
    ├── sync：ShortLinkStatsPersistenceService 直接写库
    └── mq：RabbitMQ → ShortLinkStatsSaveConsumerRabbitMq
                         ↓
                 ShortLinkStatsPersistenceService
```

统计数据写入的主要表包括访问日志、PV/UV/UIP、浏览器、设备、操作系统、网络、地区和今日统计表。

## 当前压测结论

在关闭高德地区查询的短时调优测试中，8 个消费者、32 个连接已经达到当前数据库写入瓶颈；增加到 16 个消费者或 64 个连接没有明显提升，推荐先使用 8/32 组合。
