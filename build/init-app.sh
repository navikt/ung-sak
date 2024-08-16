#!/usr/bin/env sh
set -eu

if test -f /var/run/secrets/nais.io/serviceuser/username
then
    export SYSTEMBRUKER_USERNAME="$(cat /var/run/secrets/nais.io/serviceuser/username)"
    export SYSTEMBRUKER_PASSWORD="$(cat /var/run/secrets/nais.io/serviceuser/password)"
fi

if test -f /var/run/secrets/nais.io/ldap/username
then
    export LDAP_USERNAME="$(cat /var/run/secrets/nais.io/ldap/username)"
    export LDAP_PASSWORD="$(cat /var/run/secrets/nais.io/ldap/password)"
fi

#start opentelemetyr-agenten dersom konfigurert
if [ ! -z "${OTEL_EXPORTER_OTLP_ENDPOINT}" ]; then
    echo "Configuring attatchment of opentelemetry-agent"
    JAVA_OPTS="${JAVA_OPTS} -javaagent:/app/opentelemetry-javaagent.jar"
fi
