package no.nav.ung.sak.formidling.vedtak.regler;

import no.nav.ung.kodeverk.dokument.DokumentMalType;
import no.nav.ung.sak.formidling.innhold.VedtaksbrevInnholdBygger;

/**
 * Vedtaksbrev resultat for ett enkelt brev.
 *
 */
public record VedtaksbrevResultat(
        VedtaksbrevEgenskaper vedtaksbrevEgenskaper,
        DokumentMalType dokumentMalType,
        VedtaksbrevInnholdBygger vedtaksbrevBygger,
        String forklaring) {

    public String safePrint() {
        return "VedtaksbrevResultat{" +
            "vedtaksbrevEgenskaper=" + vedtaksbrevEgenskaper +
            ", dokumentMalType=" + dokumentMalType +
            ", bygger=" + (vedtaksbrevBygger != null ? vedtaksbrevBygger.getClass().getSimpleName() : "null") +
            ", forklaring='" + forklaring + '\'' +
            '}';
    }

    public static VedtaksbrevResultat automatiskBrev(
            DokumentMalType dokumentMalType,
            VedtaksbrevInnholdBygger bygger,
            String forklaring,
            boolean kanRedigere) {
        return new VedtaksbrevResultat(
                new VedtaksbrevEgenskaper(
                        true,
                        null,
                        kanRedigere,
                        kanRedigere,
                        kanRedigere,
                        kanRedigere
                ),
                dokumentMalType,
                bygger,
                forklaring
        );
    }

    public static VedtaksbrevResultat tomRedigerbarBrev(
        VedtaksbrevInnholdBygger bygger,
        String forklaring
    ) {
        return new VedtaksbrevResultat(
            new VedtaksbrevEgenskaper(
                true,
                null,
                true,
                true,
                true,
                false),
                DokumentMalType.MANUELT_VEDTAK_DOK,
                bygger,
            forklaring
        );
    }

    public static VedtaksbrevResultat ingenBrev(
        IngenBrevÅrsakType ingenBrevÅrsakType,
        String forklaring
    ) {
        return new VedtaksbrevResultat(
            new VedtaksbrevEgenskaper(
                false,
                ingenBrevÅrsakType,
                false,
                false,
                false,
                false),
                null,
                null,
            forklaring
        );
    }

}

