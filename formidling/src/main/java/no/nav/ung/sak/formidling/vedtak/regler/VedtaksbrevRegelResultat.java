package no.nav.ung.sak.formidling.vedtak.regler;

import no.nav.ung.kodeverk.dokument.DokumentMalType;
import no.nav.ung.sak.formidling.innhold.VedtaksbrevInnholdBygger;

/**
 * Vedtaksbrev resultat for ett enkelt brev.
 */
public sealed interface VedtaksbrevRegelResultat permits IngenBrev, Vedtaksbrev {
    String forklaring();

    static Vedtaksbrev tomRedigerbarBrev(VedtaksbrevInnholdBygger bygger, String forklaring) {
        return new Vedtaksbrev(
            DokumentMalType.MANUELT_VEDTAK_DOK,
            bygger,
            new VedtaksbrevEgenskaper(
                true,
                true,
                true,
                false),
            forklaring);
    }

    static IngenBrev ingenBrev(IngenBrevÅrsakType ingenBrevÅrsakType, String forklaring) {
        return new IngenBrev(ingenBrevÅrsakType, forklaring);
    }

}

