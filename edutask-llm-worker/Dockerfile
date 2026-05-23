FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /workspace

COPY pom.xml ./
COPY edutask-common/pom.xml edutask-common/pom.xml
COPY edutask-core/pom.xml edutask-core/pom.xml
COPY edutask-gateway/pom.xml edutask-gateway/pom.xml
COPY edutask-judge-worker/pom.xml edutask-judge-worker/pom.xml
COPY edutask-llm-worker/pom.xml edutask-llm-worker/pom.xml

RUN mvn -pl edutask-llm-worker -am dependency:go-offline -DskipTests

COPY edutask-common/src edutask-common/src
COPY edutask-llm-worker/src edutask-llm-worker/src

RUN mvn -pl edutask-llm-worker -am package -DskipTests

FROM eclipse-temurin:21-jre-alpine

WORKDIR /app
COPY --from=build /workspace/edutask-llm-worker/target/edutask-llm-worker-*.jar app.jar

EXPOSE 8082

ENTRYPOINT ["java", "-jar", "app.jar"]
