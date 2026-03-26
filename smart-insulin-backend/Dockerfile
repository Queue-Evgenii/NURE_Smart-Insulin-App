# Используем стабильный образ Temurin для сборки
FROM gradle:8-jdk17 AS build
COPY --chown=gradle:gradle . /home/gradle/src
WORKDIR /home/gradle/src

RUN gradle build -x test

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

COPY --from=build /home/gradle/src/build/libs/*-SNAPSHOT.jar app.jar

RUN mkdir -p /usr/local/lib/nativelibs
ENV LD_LIBRARY_PATH=/usr/local/lib/nativelibs

EXPOSE 8080

ENTRYPOINT ["java", "-Djna.library.path=/usr/local/lib/nativelibs", "-jar", "app.jar"]