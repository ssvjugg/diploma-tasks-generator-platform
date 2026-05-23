FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /workspace

COPY pom.xml ./

RUN mvn -N install

COPY edutask-gateway/pom.xml edutask-gateway/pom.xml

RUN mvn -f edutask-gateway/pom.xml dependency:go-offline -DskipTests

COPY edutask-gateway/src edutask-gateway/src

RUN mvn -f edutask-gateway/pom.xml package -DskipTests

FROM eclipse-temurin:21-jre-alpine

WORKDIR /app
COPY --from=build /workspace/edutask-gateway/target/edutask-gateway-*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
