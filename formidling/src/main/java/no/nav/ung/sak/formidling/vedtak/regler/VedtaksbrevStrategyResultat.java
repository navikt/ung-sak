package no.nav.ung.sak.formidling.vedtak.regler;

import no.nav.ung.sak.formidling.innhold.VedtaksbrevInnholdBygger;

record VedtaksbrevStrategyResultat(
    VedtaksbrevInnholdBygger bygger,
    String forklaring,
    IngenBrevÅrsakType ingenBrevÅrsakType) {
}
