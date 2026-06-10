# syntax=docker/dockerfile:1
# One multi-stage build for all five Spring services. The build stage is shared
# (and cached) across them; only the runtime COPY differs per SERVICE arg.
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /src
COPY pom.xml .
COPY proto/ proto/
COPY gateway/ gateway/
COPY auth-service/ auth-service/
COPY product-service/ product-service/
COPY cart-service/ cart-service/
COPY session-service/ session-service/
RUN --mount=type=cache,target=/root/.m2 mvn -B -q -DskipTests clean package

FROM eclipse-temurin:21-jre AS runtime
RUN apt-get update && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*
ARG SERVICE
WORKDIR /app
COPY --from=build /src/${SERVICE}/target/*.jar /app/app.jar
ENV JAVA_OPTS=""
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
