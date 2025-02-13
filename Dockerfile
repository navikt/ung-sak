FROM europe-north1-docker.pkg.dev/nais-management-233d/k9saksbehandling/navikt/sif-baseimages/java-21:2025.02.13.0951Z

LABEL org.opencontainers.image.source=https://github.com/navikt/ung-sak

ENV JAVA_OPTS="-XX:+UseParallelGC -XX:MaxRAMPercentage=75.0 -XX:ActiveProcessorCount=4 -Djdk.virtualThreadScheduler.parallelism=8 -Djava.security.egd=file:/dev/./urandom -Duser.timezone=Europe/Oslo "

# Config
COPY web/target/classes/logback.xml /app/conf/

# Avhengigheter
COPY web/target/lib/*.jar /app/lib/
COPY formidling/target/pdfgen /app/pdfgen

# Application Container (Jetty)
COPY web/target/app.jar /app/
