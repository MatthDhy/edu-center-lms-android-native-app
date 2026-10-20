# Multi-stage build để tạo file JAR nhẹ và an toàn
FROM maven:3.9-eclipse-temurin-17-alpine AS build
WORKDIR /app

# Cache dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Biên dịch mã nguồn
COPY src ./src
RUN mvn clean package -DskipTests

# Runtime container
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Tạo non-root user cho bảo mật
RUN addgroup -S celehub && adduser -S celehub -G celehub
USER celehub

COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
