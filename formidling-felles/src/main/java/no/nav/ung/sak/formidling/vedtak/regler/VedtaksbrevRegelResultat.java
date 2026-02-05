package no.nav.ung.sak.formidling.vedtak.regler;

/**
 * Vedtaksbrev resultat for ett enkelt brev.
 */
public sealed interface VedtaksbrevRegelResultat permits IngenBrev, Vedtaksbrev {
    String forklaring();

    static IngenBrev ingenBrev(IngenBrevÅrsakType ingenBrevÅrsakType, String forklaring) {
        return new IngenBrev(ingenBrevÅrsakType, forklaring);
    }

}

