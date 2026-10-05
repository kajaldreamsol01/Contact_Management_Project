FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace

COPY common ./common
COPY contact-management ./contact-management

WORKDIR /workspace/contact-management
RUN chmod +x gradlew
RUN ./gradlew clean bootJar --no-daemon

FROM eclipse-temurin:21-jre
WORKDIR /app

COPY --from=build /workspace/contact-management/build/libs/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java","-jar","app.jar"]
