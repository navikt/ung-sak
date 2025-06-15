# syntax=docker/dockerfile:1.7.0-labs
FROM ghcr.io/navikt/sif-baseimages/java-21:2025.06.04.0911Z

LABEL org.opencontainers.image.source=https://github.com/navikt/ung-sak

ENV JAVA_OPTS="-XX:+UseParallelGC -XX:MaxRAMPercentage=75.0 -XX:ActiveProcessorCount=4 -Djdk.virtualThreadScheduler.parallelism=8 -Djava.security.egd=file:/dev/./urandom -Duser.timezone=Europe/Oslo "

# Config
COPY web/target/classes/logback.xml /app/conf/

##eksterne avhengigheter (har de i eget lag for bedre bruk av docker build cache)
COPY --link --exclude=no.nav.ung.sak* web/target/lib/ /app/lib/

#fonter, templates
COPY formidling/target/pdfgen /app/pdfgen

# Application Container (Jetty)
COPY web/target/lib/no.nav.ung.sak*.jar /app/lib/
COPY web/target/app.jar /app/

EXPOSE 8901
