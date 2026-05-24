FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /workspace

COPY pom.xml ./

RUN mvn -N install

COPY edutask-common/pom.xml edutask-common/pom.xml
COPY edutask-judge-worker/pom.xml edutask-judge-worker/pom.xml

RUN mvn -f edutask-common/pom.xml dependency:go-offline -DskipTests

COPY edutask-common/src edutask-common/src

RUN mvn -f edutask-common/pom.xml install -DskipTests \
    && mvn -f edutask-judge-worker/pom.xml dependency:go-offline -DskipTests

COPY edutask-judge-worker/src edutask-judge-worker/src

RUN mvn -f edutask-judge-worker/pom.xml package -DskipTests

FROM eclipse-temurin:21-jre-alpine

WORKDIR /app
COPY --from=build /workspace/edutask-judge-worker/target/edutask-judge-worker-*.jar app.jar

EXPOSE 8083

ENTRYPOINT ["java", "-jar", "app.jar"]
