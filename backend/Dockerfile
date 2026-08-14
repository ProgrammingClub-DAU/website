# Build stage
FROM maven:3.9.6-eclipse-temurin-17-focal AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn clean package -DskipTests

# Run stage
FROM eclipse-temurin:17-jre-focal
WORKDIR /app
RUN addgroup --system spring && adduser --system --ingroup spring spring
USER spring:spring
COPY --from=build /app/target/*.jar app.jar

# Baked in rather than left to the hosting dashboard. Without it the container
# silently falls back to the `default` profile: ddl-auto: update instead of
# validate, SQL logging on, and Swagger exposed. Because the datasource env vars
# bind regardless of profile, that misconfiguration starts cleanly and serves
# traffic — there is no error to notice. An operator can still override this.
ENV SPRING_PROFILES_ACTIVE=prod

# Documents the listen port for platforms that probe it.
EXPOSE 8080

# MaxRAMPercentage rather than a fixed -Xmx: the heap then tracks the container
# limit instead of silently wasting (or overrunning) memory if the instance is
# resized.
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-Xss512k", "-XX:MaxMetaspaceSize=128m", "-jar", "app.jar"]
