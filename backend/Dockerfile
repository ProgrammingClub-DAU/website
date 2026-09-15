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
# Heap  (-Xmx160m / -Xms64m)          ≤ 160 MB   — Spring Boot + Hibernate data
# Metaspace (-XX:MaxMetaspaceSize)       160 MB   — class metadata
# Code Cache (-XX:ReservedCodeCacheSize)  48 MB   — JIT-compiled code
# Thread stacks (Xss256k × ~50 threads)  ~12 MB   — request + scheduler threads
# JVM internals / GC / off-heap          ~50 MB   — NIO, Hikari, misc
# OS / kernel                            ~50 MB   — always present
#                                       ──────────
# Worst-case total                      ~480 MB   — leaves ~32 MB headroom
#
# Metaspace went 96 MB -> 160 MB, paid for out of heap and code cache.
#
# 96 MB was sized for the Phase 1 application. Phase 2 added the event, gallery,
# snapshot and leetcode packages, and Apache POI for the attendance export, which
# loads several hundred classes the first time a spreadsheet is written. Class
# metadata is effectively never reclaimed here, so the cap was reached in normal
# use, and from that moment every request that had to load a class failed with
# OutOfMemoryError: Metaspace — sign-in included, that being the request members
# hit first. Heap is the safer place to take it from: this service holds no large
# objects, it serves paginated lists and single rows.
#
# Why not MaxRAMPercentage=75? 75% × 512 MB = 384 MB heap alone.
# Add Metaspace + CodeCache + OS = well over 512 MB → OOM at startup.
#
# ExitOnOutOfMemoryError: a JVM out of Metaspace cannot load another class, so it
# cannot serve one more request. Staying alive only keeps Render routing traffic
# to a process that answers everything with a 500. Exiting hands it back to the
# platform, which restarts it.
ENTRYPOINT ["java", \
            "-Xms64m", "-Xmx160m", \
            "-Xss256k", \
            "-XX:MaxMetaspaceSize=160m", \
            "-XX:ReservedCodeCacheSize=48m", \
            "-XX:+UseSerialGC", \
            "-XX:+TieredCompilation", "-XX:TieredStopAtLevel=1", \
            "-XX:+ExitOnOutOfMemoryError", \
            "-jar", "app.jar"]
