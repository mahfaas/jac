FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace

COPY pom.xml pom.xml
COPY user-service/pom.xml user-service/pom.xml
RUN mvn -f user-service/pom.xml -B dependency:go-offline

COPY user-service/src user-service/src
RUN mvn -f user-service/pom.xml -B package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app

COPY --from=build /workspace/user-service/target/user-service-*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
