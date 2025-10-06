package no.nav.ung.sak.formidling.vedtak.regler.strategy;

import no.nav.ung.kodeverk.dokument.DokumentMalType;
import no.nav.ung.sak.formidling.innhold.VedtaksbrevInnholdBygger;
import no.nav.ung.sak.formidling.vedtak.regler.IngenBrevÅrsakType;
import no.nav.ung.sak.formidling.vedtak.regler.VedtaksbrevEgenskaper;

public record VedtaksbrevStrategyResultat(
    DokumentMalType dokumentMalType,
    VedtaksbrevInnholdBygger bygger,
    VedtaksbrevEgenskaper vedtaksbrevEgenskaper,
    IngenBrevÅrsakType ingenBrevÅrsakType,
    String forklaring
) {

    public static VedtaksbrevStrategyResultat utenBrev(IngenBrevÅrsakType ingenBrevÅrsakType, String forklaring) {
        return new VedtaksbrevStrategyResultat(
            null,
            null,
            new VedtaksbrevEgenskaper(false,
                false,
                false,
                false),
            ingenBrevÅrsakType, forklaring
        );
    }

    public static VedtaksbrevStrategyResultat medUredigerbarBrev(
        DokumentMalType dokumentMalType,
        VedtaksbrevInnholdBygger bygger,
        String forklaring) {
        return new VedtaksbrevStrategyResultat(
            dokumentMalType,
            bygger,
            new VedtaksbrevEgenskaper(false,
                false,
                false,
                false),
            null,
            forklaring
        );
    }


    public static VedtaksbrevStrategyResultat medRedigerbarBrev(DokumentMalType dokumentMalType, VedtaksbrevInnholdBygger innholdBygger, String forklaring) {
        return new VedtaksbrevStrategyResultat(
            dokumentMalType,
            innholdBygger,
            new VedtaksbrevEgenskaper(true,
                true,
                true,
                true),
            null,
            forklaring
        );
    }
}
