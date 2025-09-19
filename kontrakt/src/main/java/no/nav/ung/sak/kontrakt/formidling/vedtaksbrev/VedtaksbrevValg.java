package no.nav.ung.sak.kontrakt.formidling.vedtaksbrev;


import no.nav.ung.kodeverk.dokument.DokumentMalType;

/**
 * Response objekt - skal ikke sendes inn
 *
 * @param dokumentMalType           maltype for brevet
 * @param enableHindre              true hvis valg for hindring av brev er relevant og skal vises
 * @param hindret                   true hvis brevet har blitt undertrykket - blir nullstilt ved tilbakehopp
 *                                  <p>
 *                                  hvis en behandling først hadde automatisk brev som ble redigert av saksbehandler, som så får fritekstbrev etter at behandling endret seg så vil
 *                                  redigertHtml fortsatt innholde den gamle teksten.
 *                                  </p>
 * @param kanOverstyreHindre        true hvis det er mulig å overstyre hindring brevet
 * @param enableRediger             true hvis valget for redigering er relevant og skal vises
 * @param redigert                  true hvis det det brevet har blitt redigert - blir nullstilt ved tilbakehopp
 * @param kanOverstyreRediger       true hvis det er mulig å redigere et automatisk brev.
 * @param forklaring                en forklaring av resultatet
 * @param redigertBrevHtml
 * @param tidligereRedigertBrevHtml redigert tekst før tilbakehopp
 */
public record VedtaksbrevValg(
    DokumentMalType dokumentMalType,
    boolean enableHindre,
    boolean hindret,
    boolean kanOverstyreHindre,
    boolean enableRediger,
    boolean redigert,
    boolean kanOverstyreRediger,
    String forklaring,
    String redigertBrevHtml,
    String tidligereRedigertBrevHtml) {

}
