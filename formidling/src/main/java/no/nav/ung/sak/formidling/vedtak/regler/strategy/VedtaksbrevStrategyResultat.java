package no.nav.ung.sak.formidling.vedtak.regler.strategy;

import no.nav.ung.kodeverk.dokument.DokumentMalType;
import no.nav.ung.sak.formidling.innhold.VedtaksbrevInnholdBygger;
import no.nav.ung.sak.formidling.vedtak.regler.IngenBrevÅrsakType;

public record VedtaksbrevStrategyResultat(
    DokumentMalType dokumentMalType,
    VedtaksbrevInnholdBygger bygger,
    boolean kanRedigere,
    boolean kanHindre,
    IngenBrevÅrsakType ingenBrevÅrsakType,
    String forklaring
) {

    public static VedtaksbrevStrategyResultat utenBrev(IngenBrevÅrsakType ingenBrevÅrsakType, String forklaring) {
        return new VedtaksbrevStrategyResultat(
            null,
            null,
            false, false, ingenBrevÅrsakType, forklaring
        );
    }

    public static VedtaksbrevStrategyResultat medBrev(
        DokumentMalType dokumentMalType,
        boolean kanRedigere, VedtaksbrevInnholdBygger bygger,
        String forklaring) {
        return new VedtaksbrevStrategyResultat(
            dokumentMalType,
            bygger,
            kanRedigere,
            false,
            null,
            forklaring
        );
    }


}
