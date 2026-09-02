FROM maven:3.9.11-eclipse-temurin-17 AS builder

WORKDIR /workspace
COPY pom.xml ./
COPY src ./src
RUN mvn -B -Dmaven.test.skip=true package

FROM eclipse-temurin:17-jre-jammy

RUN apt-get update \
    && apt-get install -y --no-install-recommends curl fonts-noto-cjk \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app
COPY --from=builder /workspace/target/researchflow-*.jar /app/researchflow.jar
RUN mkdir -p /data/documents /app/logs

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/researchflow.jar"]
