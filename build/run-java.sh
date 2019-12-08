#!/usr/bin/env sh
set -eu

export JAVA_OPTS="${JAVA_OPTS:-} -Xmx2048m -Xms256m -Djava.security.egd=file:/dev/./urandom"
export STARTUP_CLASS=${STARTUP_CLASS:-"no.nav.foreldrepenger.web.server.jetty.JettyServer"}
export LOGBACK_CONFIG=${LOGBACK_CONFIG:-"./conf/logback.xml"}

exec java -cp "app.jar:lib/*"${EXTRA_CLASS_PATH:-""} ${DEFAULT_JAVA_OPTS:-} ${JAVA_OPTS} \
    -Dlogback.configurationFile=${LOGBACK_CONFIG?} \
    -Dconf=${CONF:-"./conf"} \
    -Dwebapp=${WEBAPP:-"./webapp"} \
    -Dapplication.name=${APP_NAME} ${STARTUP_CLASS?} $@
