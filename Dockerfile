# Build Stage
FROM maven:3.9.6-eclipse-temurin-17-alpine AS build
WORKDIR /app

# Copy pom files first to cache dependencies
COPY pom.xml .
COPY model-module/pom.xml model-module/
COPY dao-module/pom.xml dao-module/
COPY service-module/pom.xml service-module/
COPY api-module/pom.xml api-module/
COPY batch-module/pom.xml batch-module/

# Copy all source directories
COPY model-module/src model-module/src
COPY dao-module/src dao-module/src
COPY service-module/src service-module/src
COPY api-module/src api-module/src
COPY batch-module/src batch-module/src

# Package the application
RUN mvn clean package -DskipTests

# Run Stage
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Copy executable fat JAR from api-module build
COPY --from=build /app/api-module/target/api-module-0.0.1-SNAPSHOT.jar app.jar

# Ensure the temp uploads directory exists
RUN mkdir -p src/main/resources/temp-uploads

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
