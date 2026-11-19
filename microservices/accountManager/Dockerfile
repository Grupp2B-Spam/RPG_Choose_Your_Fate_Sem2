FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY target/account-manager.jar app.jar
ENV ACCOUNT_MANAGER_BIND_ADDRESS=0.0.0.0 \
    JWT_PUBLIC_KEY_FILE=/app/config/account-jwt-public.pem
EXPOSE 8092
USER 10001:10001
ENTRYPOINT ["java", "-jar", "app.jar"]
