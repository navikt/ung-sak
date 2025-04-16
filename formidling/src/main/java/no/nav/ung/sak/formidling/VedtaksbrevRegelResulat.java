package no.nav.ung.sak.formidling;

import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.sak.formidling.innhold.VedtaksbrevInnholdBygger;
import no.nav.ung.sak.formidling.vedtak.DetaljertResultat;

public record VedtaksbrevRegelResulat(
    VedtaksbrevEgenskaper vedtaksbrevEgenskaper,
    VedtaksbrevInnholdBygger automatiskVedtaksbrevBygger,
    LocalDateTimeline<DetaljertResultat> detaljertResultatTimeline,
    String forklaring) {
    public String safePrint() {
        return "VedtaksbrevRegelResulat{" +
            "vedtaksbrevEgenskaper=" + vedtaksbrevEgenskaper +
            ", bygger=" + (automatiskVedtaksbrevBygger != null ? automatiskVedtaksbrevBygger.getClass().getSimpleName() : "null") +
            ", detaljertResultatTimeline=" + DetaljertResultat.timelineToString(detaljertResultatTimeline) +
            ", forklaring='" + forklaring + '\'' +
            '}';
    }

    public static VedtaksbrevRegelResulat automatiskBrev(
        VedtaksbrevInnholdBygger bygger,
        LocalDateTimeline<DetaljertResultat> detaljertResultatTimeline,
        String forklaring,
        boolean kanRedigere) {
        return new VedtaksbrevRegelResulat(
            new VedtaksbrevEgenskaper(
                true,
                kanRedigere,
                kanRedigere,
                kanRedigere,
                kanRedigere), bygger,
            detaljertResultatTimeline,
            forklaring
        );
    }

    public static VedtaksbrevRegelResulat tomRedigerbarBrev(
        VedtaksbrevInnholdBygger bygger,
        LocalDateTimeline<DetaljertResultat> detaljertResultatTimeline,
        String forklaring
    ) {
        return new VedtaksbrevRegelResulat(
            new VedtaksbrevEgenskaper(
                true,
                true,
                true,
                true,
                false),
            bygger,
            detaljertResultatTimeline,
            forklaring
        );
    }

    public static VedtaksbrevRegelResulat ingenBrev(
        LocalDateTimeline<DetaljertResultat> detaljertResultatTimeline,
        String forklaring
    ) {
        return new VedtaksbrevRegelResulat(
            new VedtaksbrevEgenskaper(
                false,
                false,
                false,
                false,
                false), null,
            detaljertResultatTimeline,
            forklaring
        );
    }

    public record VedtaksbrevEgenskaper(
        boolean harBrev,
        boolean kanHindre,
        boolean kanOverstyreHindre,
        boolean kanRedigere,
        boolean kanOverstyreRediger
    ) {
    }
}

