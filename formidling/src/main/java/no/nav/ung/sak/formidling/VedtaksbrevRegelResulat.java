package no.nav.ung.sak.formidling;

import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.sak.formidling.innhold.VedtaksbrevInnholdBygger;
import no.nav.ung.sak.formidling.vedtak.DetaljertResultat;

public record VedtaksbrevRegelResulat(
    VedtaksbrevInnholdBygger bygger,
    LocalDateTimeline<DetaljertResultat> detaljertResultatTimeline
) {
}
