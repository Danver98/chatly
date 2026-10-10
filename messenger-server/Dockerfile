# Этап 1: Извлечение слоев из JAR-файла
FROM eclipse-temurin:21-jre-alpine AS builder
WORKDIR /application
ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} application.jar
RUN java -Djarmode=layertools -jar application.jar extract

# Этап 2: Сборка финального минимального образа
FROM eclipse-temurin:21-jre-alpine
WORKDIR /application

# Создаем безопасного non-root пользователя для запуска приложения
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Копируем разделенные слои из этапа builder
COPY --from=builder /application/dependencies/ ./
COPY --from=builder /application/spring-boot-loader/ ./
COPY --from=builder /application/snapshot-dependencies/ ./
COPY --from=builder /application/application/ ./

EXPOSE 8443

ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
