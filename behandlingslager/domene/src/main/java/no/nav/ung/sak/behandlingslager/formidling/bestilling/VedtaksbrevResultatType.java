package no.nav.ung.sak.behandlingslager.formidling.bestilling;

/**
 * Enum for resultat av vedtaksbrev vurdering
 */
public enum VedtaksbrevResultatType {

    /**
     * Vedtaksbrev er ikke relevant for denne behandlingen
     */
    IKKE_RELEVANT,

    /**
     * Vedtaksbrev er bestilt
     */
    BESTILT,

    /**
     * Vedtaksbrev er hindret av saksbehandler
     */
    HINDRET_SAKSBEHANDLER;
}
