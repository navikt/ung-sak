package no.nav.ung.sak.formidling.vedtak.regler;

import no.nav.ung.sak.formidling.innhold.VedtaksbrevInnholdBygger;

public record VedtaksbrevRegelResultat(
    VedtaksbrevEgenskaper vedtaksbrevEgenskaper,
    VedtaksbrevInnholdBygger automatiskVedtaksbrevBygger,
    String forklaring) {

    public String safePrint() {
        return "VedtaksbrevRegelResulat{" +
            "vedtaksbrevEgenskaper=" + vedtaksbrevEgenskaper +
            ", bygger=" + (automatiskVedtaksbrevBygger != null ? automatiskVedtaksbrevBygger.getClass().getSimpleName() : "null") +
            ", forklaring='" + forklaring + '\'' +
            '}';
    }

    public static VedtaksbrevRegelResultat automatiskBrev(
            VedtaksbrevInnholdBygger bygger,
            String forklaring,
            boolean kanRedigere) {
        return new VedtaksbrevRegelResultat(
                new VedtaksbrevEgenskaper(
                        true,
                        null,
                        kanRedigere,
                        kanRedigere,
                        kanRedigere,
                        kanRedigere
                ),
                bygger,
                forklaring
        );
    }

    public static VedtaksbrevRegelResultat tomRedigerbarBrev(
        VedtaksbrevInnholdBygger bygger,
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
            forklaring
        );
    }

    public static VedtaksbrevRegelResultat ingenBrev(
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
            forklaring
        );
    }

}

