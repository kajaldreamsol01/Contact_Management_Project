FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace

COPY common ./common
COPY master ./master

WORKDIR /workspace/master
RUN chmod +x gradlew
RUN ./gradlew clean bootJar --no-daemon

FROM eclipse-temurin:21-jre
WORKDIR /app

COPY --from=build /workspace/master/build/libs/*.jar app.jar

EXPOSE 8083
ENTRYPOINT ["java","-jar","app.jar"]
