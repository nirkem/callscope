# Two stages: build the jar with the full JDK, then run it on the smaller JRE image.

FROM eclipse-temurin:21-jdk AS build
WORKDIR /build

# Dependencies first: this layer is reused until pom.xml changes, not rebuilt on every code edit.
COPY mvnw pom.xml ./
COPY .mvn .mvn
RUN sh ./mvnw -q -B dependency:go-offline

# Tests run in CI and locally; they read ../contracts, which isn't in this build context.
COPY src src
RUN sh ./mvnw -q -B package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /build/target/conversation-service-*.jar app.jar

# Run as a non-root user.
RUN useradd --create-home appuser
USER appuser

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
