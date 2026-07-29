package no.nav.ung.sak.formidling.vedtak.resultat;

import no.nav.fpsak.tidsserie.LocalDateTimeline;

/**
 * Grunnlaget brev-strategiene utleder resultatet sitt fra. Inneholder <em>hele</em> vilkårsbildet for behandlingen,
 * ikke bare periodene som er til vurdering. Perioder utenfor vurdering har tomt sett med behandlingsårsaker.
 *
 * <p>Strategier og innholdbyggere skal normalt bruke {@link #tilVurdering()}. {@link #heleBildet()} er kun for
 * avslagsvurdering, som må kunne skille fullt avslag fra delvis avslag.</p>
 */
public record DetaljertResultatTidslinje(LocalDateTimeline<DetaljertResultat> heleBildet) {

    public static DetaljertResultatTidslinje av(LocalDateTimeline<DetaljertResultat> heleBildet) {
        return new DetaljertResultatTidslinje(heleBildet);
    }

    public static DetaljertResultatTidslinje tom() {
        return new DetaljertResultatTidslinje(LocalDateTimeline.empty());
    }

    public LocalDateTimeline<DetaljertResultat> tilVurdering() {
        return heleBildet.filterValue(DetaljertResultat::tilVurdering);
    }

    @Override
    public String toString() {
        return DetaljertResultat.timelineToString(heleBildet);
    }
}
