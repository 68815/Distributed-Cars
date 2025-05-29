# 🚗 分布式多车导航器组件（Distributed Multi-Car Navigator）

这个仓库是一个**分布式多车协同导航系统**中的核心组件 —— **导航器模块**。它负责接收路径规划任务、执行路径搜索算法，并将结果通过 Redis 进行共享，供其他服务或车辆读取使用。

---

## 📌 项目概述

本项目为一个**微服务架构下的导航器组件**，适用于需要在多个车辆之间进行协调路径规划的场景。该组件：

- 从消息中间件监听“开始路径规划”事件；
- 使用 A* 等启发式算法进行路径搜索；
- 将规划好的路径写入 Redis，供其他系统调用；
- 支持横向扩展，可部署多个实例以应对大规模车辆调度需求。

---

## 🧩 功能特性

- ✅ 支持多种路径规划策略（如 A* 算法）；
- ✅ 通过消息队列（ActiveMQ）监听路径请求；
- ✅ 路径结果存储至 Redis，便于多系统共享；
- ✅ 可集成到 Spring Boot 微服务架构中；
- ✅ 支持热重启（DevTools）、健康检查等现代开发功能。

---

## 🏗️ 技术栈

| 技术          | 描述 |
|-------------|------|
| Java 21     | 主语言 |
| Spring Boot | 快速构建微服务 |
| Redis       | 存储路径信息 |
| ActiveMQ    | 消息通信 |
| Lettuce     | Redis 客户端 |
| gradle      | 项目构建工具 |

---

## 📦 模块说明

- `PathPlanningStrategy`：路径规划策略接口，支持插拔不同算法。
- `RedisInteraction`：与 Redis 交互的工具类，用于保存路径。
- `ActiveMQListener`：监听消息服务器，触发路径规划流程。

---

## 🔧 配置说明

你需要在 `application.properties` 中配置以下内容：

```yaml
# Redis 配置
spring.redis.host=your_redis_host
spring.redis.port=6379
spring.redis.password=your_redis_password_if_set

  # RabbitMQ 配置
spring.rabbitmq.host=your_rabbitmq_host
spring.rabbitmq.port=5672
spring.rabbitmq.username=your_rabbitmq_username
spring.rabbitmq.password=your_rabbitmq_password

  # 管理端点健康检查配置（可选）
management.health.jms.enabled=false  # 关闭 JMS 健康检查
#我们不提供redis服务器或activemq服务器，需要自行搭建。
```

---

## 🚀 启动方式

### 使用 Maven 启动：

```bash
mvn spring-boot:run
```

### 或者打包后运行：

```bash
mvn clean package
java -jar target/distributed-cars-navigator.jar
```

---

## 📝 示例行为流程

1. 其他服务发送一条路径规划指令到消息中间件（消息格式："Car001"）；
2. 导航器组件监听到消息后，获取起点和终点；
3. 使用 A*/JPS/PSO 等算法进行路径搜索；
4. 搜索完成后，将路径写入 Redis；
5. 其他服务可以从 Redis 获取路径并执行。

---

## 🤝 开发建议

- 如果你希望添加新的路径规划算法，只需实现 `PathPlanningStrategy` 接口即可；
- 可通过 Spring Profiles 切换本地/测试/生产环境；
- Redis 和 MQ 的地址应根据部署环境动态配置（推荐使用配置中心）；
- 若需提高性能，可以考虑引入缓存机制或并发路径规划线程池。

---

## 📚 参考文档

- [Spring Boot](https://docs.spring.io/spring-boot/docs/current/reference/html/)
- [Redis](https://redis.io/documentation)
- [ActiveMQ](https://activemq.apache.org/)

---

## 📧 联系我们

如有问题，请提交 Issue 或联系作者邮箱：[Axty0903@outlook.com]

---

> 🚨 注意：本项目为实验性项目，若用于生产环境请自行评估稳定性及安全性。欢迎贡献代码！
