package no.nav.ung.sak.formidling.vedtak.regler;

import no.nav.ung.kodeverk.dokument.DokumentMalType;
import no.nav.ung.sak.formidling.innhold.VedtaksbrevInnholdBygger;

public record VedtaksbrevStrategyResultat(
    DokumentMalType dokumentMalType,
    VedtaksbrevInnholdBygger bygger,
    String forklaring,
    IngenBrevÅrsakType ingenBrevÅrsakType) {
}
