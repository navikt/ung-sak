package no.nav.ung.sak.formidling.vedtak.regler;

import no.nav.ung.kodeverk.dokument.DokumentMalType;
import no.nav.ung.sak.formidling.innhold.VedtaksbrevInnholdBygger;

/**
 * Vedtaksbrev resultat for ett enkelt brev.
 */
public record VedtaksbrevResultat(
    VedtaksbrevEgenskaper vedtaksbrevEgenskaper,
    DokumentMalType dokumentMalType,
    VedtaksbrevInnholdBygger vedtaksbrevBygger,
    String forklaring,
    IngenBrevÅrsakType ingenBrevÅrsakType) {

    public String safePrint() {
        return "VedtaksbrevResultat{" +
            "vedtaksbrevEgenskaper=" + vedtaksbrevEgenskaper +
            ", dokumentMalType=" + dokumentMalType +
            ", bygger=" + (vedtaksbrevBygger != null ? vedtaksbrevBygger.getClass().getSimpleName() : "null") +
            ", forklaring='" + forklaring + '\'' +
            ", ingenBrevÅrsakType='" + ingenBrevÅrsakType + '\'' +
            '}';
    }

    public static VedtaksbrevResultat automatiskBrev(
        DokumentMalType dokumentMalType,
        VedtaksbrevInnholdBygger bygger,
        String forklaring,
        boolean kanRedigere) {
        return new VedtaksbrevResultat(
            new VedtaksbrevEgenskaper(
                kanRedigere,
                kanRedigere,
                kanRedigere,
                kanRedigere
            ),
            dokumentMalType,
            bygger,
            forklaring,
            null);
    }

    public static VedtaksbrevResultat tomRedigerbarBrev(
        VedtaksbrevInnholdBygger bygger,
        String forklaring
    ) {
        return new VedtaksbrevResultat(
            new VedtaksbrevEgenskaper(
                true,
                true,
                true,
                false),
            DokumentMalType.MANUELT_VEDTAK_DOK,
            bygger,
            forklaring,
            null);
    }

    public static VedtaksbrevResultat ingenBrev(
        IngenBrevÅrsakType ingenBrevÅrsakType,
        String forklaring
    ) {
        return new VedtaksbrevResultat(
            null,
            null,
            null,
            forklaring,
            ingenBrevÅrsakType);
    }

}

