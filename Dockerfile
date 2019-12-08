FROM navikt/java:11-appdynamics
ENV APPD_ENABLED=true
RUN mkdir /app/lib
RUN mkdir /app/conf

# Config
COPY web/target/classes/logback.xml /app/conf/
COPY web/target/classes/jetty/jaspi-conf.xml /app/conf/

# Application Container (Jetty)
COPY web/target/app.jar /app/
COPY web/target/lib/*.jar /app/lib/

# Application Start Command
COPY build/run-java.sh /
RUN chmod +x /run-java.sh

# TODO alt under bør bygges i egen docker container / layer?

# Prep for running in VTP environment, correct log format
RUN mkdir /app/vtp-lib
COPY web/target/test-classes/logback-dev.xml /app/vtp-lib/logback-test.xml

