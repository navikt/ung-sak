package no.nav.ung.sak.formidling.vedtak.regler;

import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.sak.formidling.innhold.VedtaksbrevInnholdBygger;
import no.nav.ung.sak.formidling.vedtak.DetaljertResultat;

public record VedtaksbrevRegelResultat(
    VedtaksbrevEgenskaper vedtaksbrevEgenskaper,
    VedtaksbrevInnholdBygger automatiskVedtaksbrevBygger,
    LocalDateTimeline<DetaljertResultat> detaljertResultatTimeline,
    String forklaring) {
    public String safePrint() {
        return "VedtaksbrevRegelResultat{" +
            "vedtaksbrevEgenskaper=" + vedtaksbrevEgenskaper +
            ", bygger=" + (automatiskVedtaksbrevBygger != null ? automatiskVedtaksbrevBygger.getClass().getSimpleName() : "null") +
            ", detaljertResultatTimeline=" + DetaljertResultat.timelineToString(detaljertResultatTimeline) +
            ", forklaring='" + forklaring + '\'' +
            '}';
    }

    public static VedtaksbrevRegelResultat automatiskBrev(
        VedtaksbrevInnholdBygger bygger,
        LocalDateTimeline<DetaljertResultat> detaljertResultatTimeline,
        String forklaring,
        boolean kanRedigere) {
        return new VedtaksbrevRegelResultat(
            new VedtaksbrevEgenskaper(
                true,
                null,
                kanRedigere,
                kanRedigere,
                kanRedigere,
                kanRedigere), bygger,
            detaljertResultatTimeline,
            forklaring
        );
    }

    public static VedtaksbrevRegelResultat tomRedigerbarBrev(
        VedtaksbrevInnholdBygger bygger,
        LocalDateTimeline<DetaljertResultat> detaljertResultatTimeline,
        String forklaring
    ) {
        return new VedtaksbrevRegelResultat(
            new VedtaksbrevEgenskaper(
                true,
                null,
                true,
                true,
                true,
                false),
            bygger,
            detaljertResultatTimeline,
            forklaring
        );
    }

    public static VedtaksbrevRegelResultat ingenBrev(
        LocalDateTimeline<DetaljertResultat> detaljertResultatTimeline,
        IngenBrevÅrsakType ingenBrevÅrsakType,
        String forklaring
    ) {
        return new VedtaksbrevRegelResultat(
            new VedtaksbrevEgenskaper(
                false,
                ingenBrevÅrsakType,
                false,
                false,
                false,
                false), null,
            detaljertResultatTimeline,
            forklaring
        );
    }

}

