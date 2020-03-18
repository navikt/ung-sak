#!/usr/bin/env sh
set -eu

export JAVA_OPTS="${JAVA_OPTS:-} -Djava.security.egd=file:/dev/./urandom -Duser.timezone='Europe/Oslo'"
export STARTUP_CLASS=${STARTUP_CLASS:-"no.nav.k9.sak.web.server.jetty.JettyServer"}


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

exec java -cp "app.jar:lib/*"${EXTRA_CLASS_PATH:-""} ${DEFAULT_JAVA_OPTS:-} ${JAVA_OPTS} \
    -Dwebapp=${WEBAPP:-"./webapp"} \
    -Dapplication.name=${APP_NAME} ${STARTUP_CLASS?} $@
