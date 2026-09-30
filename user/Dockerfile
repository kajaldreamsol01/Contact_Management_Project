FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace

COPY common ./common
COPY user ./user

WORKDIR /workspace/user

RUN chmod +x gradlew
RUN ./gradlew clean bootJar --no-daemon

FROM eclipse-temurin:21-jre
WORKDIR /app

COPY --from=build /workspace/user/build/libs/*.jar app.jar

EXPOSE 8084

ENTRYPOINT ["java","-jar","app.jar"]