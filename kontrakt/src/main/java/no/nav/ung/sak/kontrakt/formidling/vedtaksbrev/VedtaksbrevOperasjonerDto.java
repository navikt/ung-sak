package no.nav.ung.sak.kontrakt.formidling.vedtaksbrev;


import jakarta.validation.Valid;

import java.util.UUID;

/**
 * @param harBrev                   true hvis det finnes brev for behandling
 * @param automatiskBrevOperasjoner satt hvis det skal brukes automatisk brev.
 * @param fritekstbrev              true hvis det skal skrives fritekstbrev. Både fritekstbrev og automatiskbrev kan ikke være true/satt samtidig
 * @param enableHindre              true hvis valg for hindring av brev er relevant og skal vises
 * @param hindret                   true hvis brevet har blitt undertrykket - blir nullstilt ved tilbakehopp
 *                                  <p>
 *                                  hvis en behandling først hadde automatisk brev som ble redigert av saksbehandler, som så får fritekstbrev etter at behandling endret seg så vil
 *                                  automatiskBrevOperasjoner bli null, fritekstbrev bli true, men redigertHtml vil fortsatt innholde den gamle teksten.
 *                                  </p>
 * @param kanHindre                 true hvis det er mulig å hindre brevet
 * @param enableRediger             true hvis valget for redigering er relevant og skal vises
 * @param redigert                  true hvis det det brevet har blitt redigert - blir nullstilt ved tilbakehopp
 * @param kanRedigere               true hvis det er mulig å redigere brevet
 * @param forklaring                en forklaring av resultatet
 * @param tidligereKladdId          Id på tidligere versjon av redigert brev ved tilbakehopp
 */
public record VedtaksbrevOperasjonerDto(
    boolean harBrev,
    @Valid @Deprecated
    AutomatiskBrevOperasjoner automatiskBrevOperasjoner,
    @Deprecated
    boolean fritekstbrev,
    boolean enableHindre,
    boolean hindret,
    boolean kanHindre,
    boolean enableRediger,
    boolean redigert,
    boolean kanRedigere,
    String forklaring,
    UUID tidligereKladdId) {

    public static VedtaksbrevOperasjonerDto ingenBrev(String forklaring) {
        return new VedtaksbrevOperasjonerDto(false,
            null,
            false,
            false,
            false,
            false,
            false,
            false,
            false,
            forklaring,
            null);
    }

    public static VedtaksbrevOperasjonerDto automatiskBrev(String forklaring, boolean enableRedigerOgHindring) {
        return new VedtaksbrevOperasjonerDto(true,
            new AutomatiskBrevOperasjoner(enableRedigerOgHindring, false),
            false,
            enableRedigerOgHindring,
            false,
            enableRedigerOgHindring,
            enableRedigerOgHindring,
            false,
            enableRedigerOgHindring,
            forklaring,
            null);
    }

    /**
     * @param enableRediger true hvis brevet kan redigeres
     * @param redigert      true hvis brevet er redigert
     */
    @Deprecated
    public record AutomatiskBrevOperasjoner(boolean enableRediger, boolean redigert) {
    }
}




