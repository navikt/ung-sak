package no.nav.k9.sak.db.util;

import org.slf4j.LoggerFactory;

import no.nav.k9.felles.konfigurasjon.env.Environment;
import no.nav.k9.felles.testutilities.db.EntityManagerAwareExtension;

public class JpaExtension extends EntityManagerAwareExtension {

    private static final boolean isNotRunningUnderMaven = Environment.current()
            .getProperty("maven.cmd.line.args") == null;

    static {
        if (isNotRunningUnderMaven) {
            LoggerFactory.getLogger(JpaExtension.class).info("Kjører IKKE under maven");
            // prøver alltid migrering hvis endring, ellers funker det dårlig i IDE.
            Databaseskjemainitialisering.migrerUnittestSkjemaer();
        }
        Databaseskjemainitialisering.settJdniOppslag();
    }

}
