FROM navikt/java:11-appdynamics
ENV APPD_ENABLED=true

LABEL org.opencontainers.image.source=https://github.com/navikt/k9-sak

RUN mkdir /app/lib
RUN mkdir /app/conf

# Config
COPY web/target/classes/logback.xml /app/conf/
COPY web/target/classes/jetty/jaspi-conf.xml /app/conf/

# Application Container (Jetty)
COPY web/target/app.jar /app/
COPY web/target/lib/*.jar /app/lib/

ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0 -Djava.security.egd=file:/dev/./urandom -Duser.timezone=Europe/Oslo "

# Application Start Konfigurasjon
COPY build/init-app.sh /init-scripts/init-app.sh

