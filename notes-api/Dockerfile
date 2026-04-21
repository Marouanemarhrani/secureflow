# syntax=docker/dockerfile:1.7

# ---------- Stage 1: Build ----------
FROM eclipse-temurin:21-jdk AS builder

WORKDIR /build

# Copy Maven wrapper and pom first — leverages Docker layer cache:
# if pom.xml hasn't changed, dependencies are pulled from cache
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B

# Now copy the source and build
COPY src/ src/
RUN ./mvnw clean package -DskipTests -B

# ---------- Stage 2: Runtime ----------
FROM eclipse-temurin:21-jre

# Create a non-root user to run the app (security best practice)
RUN groupadd --system app && useradd --system --gid app app

WORKDIR /app

# Copy ONLY the built jar from the builder stage
COPY --from=builder /build/target/notes-api-*.jar app.jar

# Drop privileges
USER app:app

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]