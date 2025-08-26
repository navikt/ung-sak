package no.nav.ung.sak.formidling.klage.regler.strategy;

import no.nav.ung.kodeverk.dokument.DokumentMalType;
import no.nav.ung.sak.formidling.innhold.VedtaksbrevInnholdBygger;

public record VedtaksbrevStrategyResultat(
    DokumentMalType dokumentMalType,
    VedtaksbrevInnholdBygger bygger) {

    public static VedtaksbrevStrategyResultat medBrev(
        DokumentMalType dokumentMalType,
        VedtaksbrevInnholdBygger bygger) {
        return new VedtaksbrevStrategyResultat(
            dokumentMalType,
            bygger
        );
    }
}
