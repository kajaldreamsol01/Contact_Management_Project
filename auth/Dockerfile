FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace

COPY common ./common
COPY auth ./auth

WORKDIR /workspace/auth

RUN chmod +x gradlew
RUN ./gradlew clean bootJar --no-daemon

FROM eclipse-temurin:21-jre
WORKDIR /app

COPY --from=build /workspace/auth/build/libs/*.jar app.jar

EXPOSE 8081

ENTRYPOINT ["java","-jar","app.jar"]