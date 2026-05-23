FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /workspace

COPY pom.xml ./
COPY edutask-common/pom.xml edutask-common/pom.xml
COPY edutask-core/pom.xml edutask-core/pom.xml
COPY edutask-gateway/pom.xml edutask-gateway/pom.xml
COPY edutask-judge-worker/pom.xml edutask-judge-worker/pom.xml
COPY edutask-llm-worker/pom.xml edutask-llm-worker/pom.xml

RUN mvn -pl edutask-gateway -am dependency:go-offline -DskipTests

COPY edutask-gateway/src edutask-gateway/src

RUN mvn -pl edutask-gateway -am package -DskipTests

FROM eclipse-temurin:21-jre-alpine

WORKDIR /app
COPY --from=build /workspace/edutask-gateway/target/edutask-gateway-*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
