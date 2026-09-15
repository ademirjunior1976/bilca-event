FROM maven:3.9.11-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml ./
RUN mvn -B -DskipTests dependency:go-offline
COPY src ./src
RUN mvn -B -DskipTests package

FROM eclipse-temurin:21-jre
RUN addgroup --system --gid 10001 bilca && adduser --system --uid 10001 --ingroup bilca bilca
WORKDIR /app
COPY --from=build /workspace/target/bilca-event-0.0.1-SNAPSHOT.jar app.jar
USER bilca
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
