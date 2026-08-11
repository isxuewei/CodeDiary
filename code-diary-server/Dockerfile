# 基础镜像，使用 Eclipse Temurin 8（OpenJDK 8 的官方继任者，openjdk 已弃用）
FROM eclipse-temurin:8-jdk

# 设置时区为 Asia/Shanghai
RUN ln -sf /usr/share/zoneinfo/Asia/Shanghai /etc/localtime && echo "Asia/Shanghai" > /etc/timezone

# 定义工作目录
WORKDIR /app

# 传入构建参数，允许指定 jar 包名字
ARG JAR_FILE=./*.jar

# 把 jar 包复制到容器里
COPY ${JAR_FILE} app.jar

# 暴露端口（Spring Boot默认8080）
EXPOSE 8080

# 定义启动时的环境变量（比如JVM参数）
ENV JAVA_OPTS=""

# 容器启动命令
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
