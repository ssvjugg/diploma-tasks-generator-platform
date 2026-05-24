FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /workspace

COPY pom.xml ./

RUN mvn -N install

COPY edutask-common/pom.xml edutask-common/pom.xml
COPY edutask-core/pom.xml edutask-core/pom.xml

RUN mvn -f edutask-common/pom.xml dependency:go-offline -DskipTests

COPY edutask-common/src edutask-common/src

RUN mvn -f edutask-common/pom.xml install -DskipTests \
    && mvn -f edutask-core/pom.xml dependency:go-offline -DskipTests

COPY edutask-core/src edutask-core/src

RUN mvn -f edutask-core/pom.xml package -DskipTests

FROM eclipse-temurin:21-jre-alpine

WORKDIR /app
COPY --from=build /workspace/edutask-core/target/edutask-core-*.jar app.jar

EXPOSE 8081

ENTRYPOINT ["java", "-jar", "app.jar"]
