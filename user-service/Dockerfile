FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace

COPY pom.xml pom.xml
COPY jwt-common/pom.xml jwt-common/pom.xml
COPY user-service/pom.xml user-service/pom.xml
COPY auth-service/pom.xml auth-service/pom.xml
COPY order-service/pom.xml order-service/pom.xml
RUN mvn -B -pl user-service -am dependency:go-offline

COPY jwt-common/src jwt-common/src
COPY user-service/src user-service/src
RUN mvn -B -pl user-service -am package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app

COPY --from=build /workspace/user-service/target/user-service-*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
