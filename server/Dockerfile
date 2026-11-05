FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /src
COPY pom.xml .
COPY src ./src
RUN mvn -DskipTests package \
    && cp target/trval-server-*.jar /tmp/app.jar

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /tmp/app.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
