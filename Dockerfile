# ---------- Сборка ----------
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build

# Сначала только pom — слой с зависимостями кэшируется и не пересобирается
# при каждой правке кода.
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src ./src
RUN mvn -B -q -DskipTests package

# ---------- Запуск ----------
FROM eclipse-temurin:17-jre
WORKDIR /app

# Не под root: базовое требование к образу.
RUN groupadd -r balloon && useradd -r -g balloon balloon

COPY --from=build /build/target/balloon-*.jar app.jar

# Конфигурация игры лежит снаружи образа и монтируется томом.
# Благодаря этому её можно править на работающем контейнере — это и есть
# обязательный сценарий 5: изменение параметров без пересборки.
VOLUME ["/app/config"]

RUN mkdir -p /app/data && chown -R balloon:balloon /app
USER balloon

EXPOSE 8080

HEALTHCHECK --interval=15s --timeout=3s --start-period=40s --retries=5 \
  CMD wget -qO- http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar", "--spring.profiles.active=docker"]
