package no.nav.ung.sak.kontrakt.formidling;


/**
 *
 * @param harBrev  true hvis det finnes brev for behandling
 * @param enableRediger true hvis saksbehandler kan velge mellom manuell og automatisk brev
 * @param redigert true hvis brev er redigert eller må redigeres.
 * @param enableHindre true hvis brevet kan undertrykkes
 * @param hindret true hvis brevet har blitt undertrykket
 */
public record VedtaksbrevOperasjonerDto(
    boolean harBrev,
    boolean enableRediger,
    boolean redigert,
    boolean enableHindre,
    boolean hindret
    //TODO legg ved original html for redigering
    // og html som er redigert
    // eller putt i eget endepunkt
    //TODO ta med dokumentMalType for å kunne skrive i frontend hva slags brev dette er?
) {
}
