FROM gradle:8.4.0-jdk17-alpine as builder
WORKDIR /builder

COPY build.gradle settings.gradle ./
RUN gradle dependencies

COPY src/main src/main
RUN gradle bootJar

FROM openjdk:17-alpine

COPY --from=builder "/builder/build/libs/*.jar" application.jar

RUN mkdir -p ~/.postgresql && \
    wget "https://storage.yandexcloud.net/cloud-certs/CA.pem" \
         --output-document ~/.postgresql/root.crt && \
    chmod 0600 ~/.postgresql/root.crt

ENTRYPOINT ["java", "-jar", "application.jar"]










