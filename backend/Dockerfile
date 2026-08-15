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

# ── Memory budget for Render's 512 MB free tier ──────────────────────────────
#
# Total container limit:                       512 MB
#
# Heap  (-Xmx192m / -Xms64m)          ≤ 192 MB   — Spring Boot + Hibernate data
# Code Cache (-XX:ReservedCodeCacheSize)  64 MB   — JIT-compiled code
# Metaspace (-XX:MaxMetaspaceSize)        96 MB   — class metadata
# Thread stacks (Xss256k × ~50 threads)  ~12 MB  — request + scheduler threads
# JVM internals / GC / off-heap          ~50 MB  — NIO, Hikari, misc
# OS / kernel                            ~50 MB  — always present
#                                       ──────────
# Worst-case total                      ~464 MB  — leaves ~48 MB headroom
#
# Why not MaxRAMPercentage=75? 75% × 512 MB = 384 MB heap alone.
# Add Metaspace (128 MB) + CodeCache + OS = well over 512 MB → OOM at startup.
ENTRYPOINT ["java", \
            "-Xms64m", "-Xmx192m", \
            "-Xss256k", \
            "-XX:MaxMetaspaceSize=96m", \
            "-XX:ReservedCodeCacheSize=64m", \
            "-XX:+UseSerialGC", \
            "-XX:+TieredCompilation", "-XX:TieredStopAtLevel=1", \
            "-jar", "app.jar"]
