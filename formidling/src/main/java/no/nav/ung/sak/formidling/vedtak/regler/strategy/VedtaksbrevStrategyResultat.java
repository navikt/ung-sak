package no.nav.ung.sak.formidling.vedtak.regler.strategy;

import no.nav.ung.kodeverk.dokument.DokumentMalType;
import no.nav.ung.sak.formidling.innhold.VedtaksbrevInnholdBygger;
import no.nav.ung.sak.formidling.vedtak.regler.IngenBrevÅrsakType;

public record VedtaksbrevStrategyResultat(
    DokumentMalType dokumentMalType,
    VedtaksbrevInnholdBygger bygger,
    String forklaring,
    IngenBrevÅrsakType ingenBrevÅrsakType) {
}
