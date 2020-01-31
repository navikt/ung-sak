package no.nav.foreldrepenger.dbstoette;

import java.io.FileNotFoundException;
import java.util.concurrent.atomic.AtomicBoolean;

import no.nav.vedtak.felles.lokal.dbstoette.DatabaseStøtte;

/**
 * Initielt skjemaoppsett + migrering av unittest-skjemaer
 */
public final class Databaseskjemainitialisering {
    private static final AtomicBoolean GUARD_UNIT_TEST_SKJEMAER = new AtomicBoolean();

    public static void main(String[] args) {
        migrerUnittestSkjemaer();
    }

    public static void migrerUnittestSkjemaer() {
        if (GUARD_UNIT_TEST_SKJEMAER.compareAndSet(false, true)) {
            try {
                DatabaseStøtte.kjørMigreringFor(DatasourceConfiguration.UNIT_TEST.get());
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static void settPlaceholdereOgJdniOppslag() {
        try {
            DatabaseStøtte.settOppJndiForDefaultDataSource(DatasourceConfiguration.UNIT_TEST.get());
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

}