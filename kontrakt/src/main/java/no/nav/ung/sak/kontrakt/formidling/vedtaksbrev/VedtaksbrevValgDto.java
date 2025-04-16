package no.nav.ung.sak.kontrakt.formidling.vedtaksbrev;


import jakarta.validation.Valid;

/**
 * Response objekt - skal ikke sendes inn
 *
 * @param harBrev                   true hvis det finnes brev for behandling
 * @param automatiskBrevOperasjoner satt hvis det skal brukes automatisk brev.
 * @param fritekstbrev              true hvis det skal skrives fritekstbrev. Både fritekstbrev og automatiskbrev kan ikke være true/satt samtidig
 * @param enableHindre              true hvis valg for hindring av brev er relevant og skal vises
 * @param hindret                   true hvis brevet har blitt undertrykket - blir nullstilt ved tilbakehopp
 *                                  <p>
 *                                  hvis en behandling først hadde automatisk brev som ble redigert av saksbehandler, som så får fritekstbrev etter at behandling endret seg så vil
 *                                  automatiskBrevOperasjoner bli null, fritekstbrev bli true, men redigertHtml vil fortsatt innholde den gamle teksten.
 *                                  </p>
 * @param kanOverstyreHindre        true hvis det er mulig å hindre brevet
 * @param enableRediger             true hvis valget for redigering er relevant og skal vises
 * @param redigert                  true hvis det det brevet har blitt redigert - blir nullstilt ved tilbakehopp
 * @param kanOverstyreRediger       true hvis det er mulig å redigere et automatisk brev.
 * @param forklaring                en forklaring av resultatet
 * @param redigertBrevHtml
 */
public record VedtaksbrevValgDto(
    boolean harBrev,
    @Valid @Deprecated
    AutomatiskBrevOperasjoner automatiskBrevOperasjoner,
    @Deprecated
    boolean fritekstbrev,
    boolean enableHindre,
    boolean hindret,
    boolean kanOverstyreHindre,
    boolean enableRediger,
    boolean redigert,
    boolean kanOverstyreRediger,
    String forklaring,
    String redigertBrevHtml) {

    /**
     * @param enableRediger true hvis brevet kan redigeres
     * @param redigert      true hvis brevet er redigert
     */
    @Deprecated
    public record AutomatiskBrevOperasjoner(boolean enableRediger, boolean redigert) {
    }
}




