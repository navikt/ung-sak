# syntax=docker/dockerfile:1.7.0-labs
FROM ghcr.io/navikt/k9-felles/felles-java-25:7.1.3 AS duplikatfjerner

COPY --link --exclude=no.nav.ung.sak* web/target/lib/ /build/lib/
USER root
RUN ["java", "scripts/RyddBiblioteker", "DUPLIKAT", "/app/lib", "/build/lib"]



FROM ghcr.io/navikt/k9-felles/felles-java-25:7.1.3
LABEL org.opencontainers.image.source=https://github.com/navikt/ung-sak

ENV JAVA_OPTS="-XX:+UseParallelGC -XX:MaxRAMPercentage=75.0 -XX:ActiveProcessorCount=4 -Djdk.virtualThreadScheduler.parallelism=8 -Djava.security.egd=file:/dev/./urandom -Duser.timezone=Europe/Oslo "

COPY --link --from=duplikatfjerner /build/lib/ /app/lib/
USER root
RUN ["java", "scripts/RyddBiblioteker", "UBRUKT", "/app/lib"]
USER apprunner

COPY --link web/target/classes/logback.xml /app/conf/

#fonter, templates
COPY formidling/target/pdfgen /app/pdfgen

##kopier prosjektets moduler
COPY --link web/target/lib/no.nav.ung.sak* /app/lib/
COPY --link web/target/app.jar /app/

EXPOSE 8901
