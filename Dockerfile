# docker build -t blk-hacking-ind-mallikarjun-halagali .
# OS Selection: Alpine Linux chosen for minimal footprint (~5MB base), reduced attack surface,
# and faster container startup - ideal for production-grade microservices.
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn clean package -DskipTests -B

# Runtime stage: Alpine-based JRE for minimal image size (~200MB vs ~400MB with full JDK)
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 5477
ENTRYPOINT ["java", "-jar", "app.jar"]
