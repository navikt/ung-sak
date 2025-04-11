package no.nav.ung.sak.kontrakt.formidling.vedtaksbrev;


import jakarta.validation.Valid;

/**
 * @param harBrev                   true hvis det finnes brev for behandling
 * @param automatiskBrevOperasjoner satt hvis det skal brukes automatisk brev.
 * @param fritekstbrev              true hvis det skal skrives fritekstbrev. Både fritekstbrev og automatiskbrev kan ikke være true/satt samtidig
 * @param enableHindre              true hvis brevet kan undertrykkes
 * @param hindret                   true hvis brevet har blitt undertrykket
 *                                  <p>
 *                                  hvis en behandling først hadde automatisk brev som ble redigert av saksbehandler, som så får fritekstbrev etter at behandling endret seg så vil
 *                                  automatiskBrevOperasjoner bli null, fritekstbrev bli true, men redigertHtml vil fortsatt innholde den gamle teksten.
 *                                  </p>
 * @param forklaring                en forklaring av resultatet
 */
public record VedtaksbrevOperasjonerDto(
    boolean harBrev,
    @Valid
    AutomatiskBrevOperasjoner automatiskBrevOperasjoner,
    boolean fritekstbrev,
    boolean enableHindre,
    boolean hindret,
    //TODO legg ved original html for redigering
    // og html som er redigert
    // eller putt i eget endepunkt
    //TODO ta med dokumentMalType for å kunne skrive i frontend hva slags brev dette er? Kan være nyttig hvis man f.eks. går fra avslag til innvilgelse.
    String forklaring) {

    public static VedtaksbrevOperasjonerDto ingenBrev(String forklaring) {
        return new VedtaksbrevOperasjonerDto(false, null, false, false, false, forklaring);
    }

    public static VedtaksbrevOperasjonerDto automatiskBrev(String forklaring) {
        return new VedtaksbrevOperasjonerDto(true, new AutomatiskBrevOperasjoner(true, false), false, true, false, forklaring);
    }

    /**
     * @param enableRediger true hvis brevet kan redigeres
     * @param redigert true hvis brevet er redigert
     */
    public record AutomatiskBrevOperasjoner(boolean enableRediger, boolean redigert) {}
}




