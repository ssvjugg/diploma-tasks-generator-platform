FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /workspace

COPY pom.xml ./

RUN mvn -N install

COPY edutask-common/pom.xml edutask-common/pom.xml
COPY edutask-llm-worker/pom.xml edutask-llm-worker/pom.xml

RUN mvn -f edutask-common/pom.xml dependency:go-offline -DskipTests

COPY edutask-common/src edutask-common/src

RUN mvn -f edutask-common/pom.xml install -DskipTests \
    && mvn -f edutask-llm-worker/pom.xml dependency:go-offline -DskipTests

COPY edutask-llm-worker/src edutask-llm-worker/src

RUN mvn -f edutask-llm-worker/pom.xml package -DskipTests

FROM eclipse-temurin:21-jre-alpine

WORKDIR /app
COPY --from=build /workspace/edutask-llm-worker/target/edutask-llm-worker-*.jar app.jar

EXPOSE 8082

ENTRYPOINT ["java", "-jar", "app.jar"]
