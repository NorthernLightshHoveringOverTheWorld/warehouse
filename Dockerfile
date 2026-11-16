FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /workspace
COPY pom.xml .
RUN mvn -B dependency:go-offline
COPY src ./src
RUN mvn -B package

FROM eclipse-temurin:17-jre
WORKDIR /app
RUN groupadd --system warehouse && useradd --system --gid warehouse warehouse
COPY --from=build /workspace/target/warehouse-1.0-SNAPSHOT.jar app.jar
USER warehouse
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
