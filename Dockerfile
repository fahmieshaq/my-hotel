# ============================================================
# STAGE 1: Build the Spring Boot application
# The build stage includes the JDK, Maven Wrapper, and source code.
# ============================================================
FROM eclipse-temurin:21-jdk AS build

WORKDIR /workspace

# Copy Maven Wrapper and project configuration.
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./

# Make the wrapper executable inside Linux.
RUN chmod +x mvnw

# Download dependencies before copying application source.
# Docker can reuse this cached layer when source code changes.
RUN --mount=type=cache,target=/root/.m2 \
    ./mvnw -B dependency:go-offline

# Copy application source code.
COPY src/ src/

# Build the executable JAR.
# Tests are run separately in CI; compilation still happens here.
RUN --mount=type=cache,target=/root/.m2 \
    ./mvnw -B package -DskipTests


# ============================================================
# STAGE 2: Run the application
# The runtime stage contains only the JRE and application JAR, resulting in a smaller, cleaner image.
# ============================================================
FROM eclipse-temurin:21-jre

WORKDIR /app

# Create a dedicated non-root user.
RUN groupadd --system spring && \
    useradd --system --gid spring spring

# Copy only the executable application JAR.
COPY --from=build --chown=spring:spring \
    /workspace/target/*.jar /app/app.jar

# Run the application without root privileges.
USER spring

# Document the application's listening port.
EXPOSE 8080

# Start Spring Boot.
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
