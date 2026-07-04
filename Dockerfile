FROM maven:3.8-openjdk-17 AS builder
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -Dmaven.test.skip=true

# 替换为官方维护的轻量JRE17镜像
FROM eclipse-temurin:17-jre-alpine

# 设置时区为上海，解决容器时间、日志时间错乱
ENV TZ=Asia/Shanghai
RUN apk add --no-cache tzdata \
    && ln -sf /usr/share/zoneinfo/$TZ /etc/localtime \
    && echo $TZ > /etc/timezone

WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar
EXPOSE 8080

# JSON数组规范写法，无多余逗号
ENTRYPOINT [
    "java",
    "-Xms128m",
    "-Xmx200m",
    "-XX:MaxMetaspaceSize=64m",
    "-XX:+UseSerialGC",
    "-jar",
    "app.jar"
]