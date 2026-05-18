FROM maven:3.9.9-eclipse-temurin-17 AS build

WORKDIR /workspace

COPY backend/pom.xml backend/pom.xml
RUN mvn -f backend/pom.xml -DskipTests dependency:go-offline

COPY backend backend
COPY frontend backend/src/main/resources/static

RUN mvn -f backend/pom.xml -DskipTests clean package

FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

RUN addgroup -S app && adduser -S app -G app

COPY --from=build /workspace/backend/target/concept-clarity-1.0.0.jar /app/app.jar

USER app

EXPOSE 8080

ENV JAVA_OPTS=""

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
