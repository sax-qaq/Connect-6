# Stage 1: Build the application
FROM maven:3.8.1-jdk-11 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package

# Stage 2: Run the application
FROM openjdk:11-jre-slim
COPY --from=build /app/target/connect6demo-1.0.0.jar /app.jar
ENTRYPOINT ["java", "-jar", "/app.jar"]
