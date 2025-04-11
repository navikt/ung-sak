package no.nav.ung.sak.formidling;

import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.sak.formidling.innhold.VedtaksbrevInnholdBygger;
import no.nav.ung.sak.formidling.vedtak.DetaljertResultat;
import no.nav.ung.sak.kontrakt.formidling.vedtaksbrev.VedtaksbrevOperasjonerDto;

public record VedtaksbrevRegelResulat(
    VedtaksbrevOperasjonerDto vedtaksbrevOperasjoner,
    VedtaksbrevInnholdBygger bygger,
    LocalDateTimeline<DetaljertResultat> detaljertResultatTimeline
) {
    public String safePrint() {
        return "VedtaksbrevRegelResulat{" +
            "vedtaksbrevOperasjoner=" + vedtaksbrevOperasjoner +
            " bygger=" + (bygger != null ? bygger.getClass().getSimpleName() : "null") +
            ", detaljertResultatTimeline=" + DetaljertResultat.timelineToString(detaljertResultatTimeline) +
            '}';
    }
}
