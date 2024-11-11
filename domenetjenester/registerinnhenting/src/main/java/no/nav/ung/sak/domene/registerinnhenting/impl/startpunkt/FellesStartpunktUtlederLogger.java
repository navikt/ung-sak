package no.nav.ung.sak.domene.registerinnhenting.impl.startpunkt;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.ung.sak.behandlingslager.hendelser.StartpunktType;

public class FellesStartpunktUtlederLogger {

    private static final Logger LOGGER = LoggerFactory.getLogger(FellesStartpunktUtlederLogger.class);

    FellesStartpunktUtlederLogger() {
        // For CDI
    }

    public static void loggEndringSomFørteTilStartpunkt(String klasseNavn, StartpunktType startpunkt, String endring, Object id1, Object id2) {
        skrivLoggMedStartpunkt(klasseNavn, startpunkt, endring, håndtereNull(id1), håndtereNull(id2));
    }

    private static String håndtereNull(Object id) {
        return id != null ? id.toString() : null;
    }

    static void loggEndringSomFørteTilStartpunkt(String klasseNavn, StartpunktType startpunkt, String endring, UUID id1, UUID id2) {
        LOGGER.info("{}: Setter startpunkt til {}. Og har endring i {}. GrunnlagId1: {}, grunnlagId2: {}", klasseNavn, startpunkt.getKode(), endring, id1, id2);// NOSONAR //$NON-NLS-1$
    }

    static void skrivLoggMedStartpunkt(String klasseNavn, StartpunktType startpunkt, String endring, String id1, String id2) {
        LOGGER.info("{}: Setter startpunkt til {}. Og har endring i {}. GrunnlagId1: {}, grunnlagId2: {}", klasseNavn, startpunkt.getKode(), endring, id1, id2);// NOSONAR //$NON-NLS-1$
    }
}
