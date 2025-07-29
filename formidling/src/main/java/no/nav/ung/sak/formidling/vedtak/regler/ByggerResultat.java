package no.nav.ung.sak.formidling.vedtak.regler;

import no.nav.ung.sak.formidling.innhold.VedtaksbrevInnholdBygger;

public record ByggerResultat(
    VedtaksbrevInnholdBygger bygger,
    String forklaring,
    IngenBrevÅrsakType ingenBrevÅrsakType) {
}
