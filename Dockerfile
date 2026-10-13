FROM maven:3.9-eclipse-temurin-17-alpine AS builder
WORKDIR /app
COPY pom.xml .
COPY . .
RUN echo "Compiled in builder stage"

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=builder /app/app.jar ./app.jar
ENV PORT=3000
EXPOSE 3000
CMD ["java", "-jar", "app.jar"]
