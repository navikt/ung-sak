FROM ghcr.io/navikt/baseimages/temurin:21

LABEL org.opencontainers.image.source=https://github.com/navikt/ung-sak

RUN mkdir /app/lib
RUN mkdir /app/conf

ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0 -XX:ActiveProcessorCount=4 -Djdk.virtualThreadScheduler.parallelism=8 -Djava.security.egd=file:/dev/./urandom -Duser.timezone=Europe/Oslo "

# Config
COPY web/target/classes/logback.xml /app/conf/

# Avhengigheter
COPY web/target/lib/*.jar /app/lib/
COPY formidling/target/pdfgen /app/pdfgen

# Application Container (Jetty)
COPY web/target/app.jar /app/
