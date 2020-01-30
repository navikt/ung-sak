#!/usr/bin/env sh
set -eu

export JAVA_OPTS="${JAVA_OPTS:-} -Xmx2048m -Xms256m -Djava.security.egd=file:/dev/./urandom -Duser.timezone='Europe/Oslo'"
export STARTUP_CLASS=${STARTUP_CLASS:-"no.nav.foreldrepenger.web.server.jetty.JettyServer"}

export SYSTEMBRUKER_USERNAME="$(cat /var/run/secrets/nais.io/serviceuser/username)"
export SYSTEMBRUKER_PASSWORD="$(cat /var/run/secrets/nais.io/serviceuser/password)"

exec java -cp "app.jar:lib/*"${EXTRA_CLASS_PATH:-""} ${DEFAULT_JAVA_OPTS:-} ${JAVA_OPTS} \
    -Dwebapp=${WEBAPP:-"./webapp"} \
    -Dapplication.name=${APP_NAME} ${STARTUP_CLASS?} $@
