# Dockerfile for Google Cloud Run / Container deployment
FROM eclipse-temurin:21-jdk-alpine

WORKDIR /app

# Copy source and web assets
COPY src ./src
COPY web ./web

# Compile Java
RUN mkdir bin && javac -encoding UTF-8 -d bin $(find src -name "*.java")

# Expose default port
EXPOSE 8088

# Run
CMD ["java", "-cp", "bin", "com.android.Main"]
