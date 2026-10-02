package no.nav.ung.sak.web.server.abac;

import no.nav.k9.felles.sikkerhet.abac.BeskyttetRessurs;

/**
 * Eksterne systemer som kan gis tilgang til endepunkter med
 * {@code @BeskyttetRessurs(resource = EKSTERN_SYSTEM, eksterneSystemer = {...})}. Format: {@code <cluster>:<namespace>:<app>}.
 * <p>
 * Systemet må i tillegg være pre-autorisert i Nais ({@code accessPolicy.inbound} i deploy/*.yml).
 *
 * @see BeskyttetRessurs#eksterneSystemer()
 */
public final class EksterneSystemer {

    public static final String TILLEGGSSTØNADER_DEV = "dev-gcp:tilleggsstonader:tilleggsstonader-integrasjoner";
    public static final String TILLEGGSSTØNADER_PROD = "prod-gcp:tilleggsstonader:tilleggsstonader-integrasjoner";

    private EksterneSystemer() {
    }
}
