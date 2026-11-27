FROM maven:3.10.2-eclipse-temurin-21-noble AS build
WORKDIR /build
COPY .mvn/wrapper/maven-wrapper.properties .mvn/wrapper/maven-wrapper.properties
COPY mvnw pom.xml ./
COPY src ./src
# Use the project's pinned Maven wrapper. Tests run separately with clean verify.
RUN sed -i 's/\r$//' mvnw && chmod +x mvnw \
    && ./mvnw --batch-mode --no-transfer-progress -DskipTests package

FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S tts && adduser -S tts -G tts
WORKDIR /app
COPY --from=build --chown=tts:tts /build/target/text-to-speech-manager-*.jar app.jar
USER tts
EXPOSE 9093
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=70.0", "-jar", "/app/app.jar"]
