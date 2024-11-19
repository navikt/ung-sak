package no.nav.ung.sak.domene.arbeidsforhold;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;
import no.nav.ung.kodeverk.opptjening.OpptjeningAktivitetType;
import no.nav.ung.sak.domene.iay.modell.AktivitetsAvtale;
import no.nav.ung.sak.domene.iay.modell.AktivitetsAvtaleInnhold;
import no.nav.ung.sak.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.ung.sak.domene.iay.modell.Yrkesaktivitet;
import no.nav.ung.sak.domene.iay.modell.YrkesaktivitetFilter;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;

public class AktivPeriodeForArbeidUtleder {


    /**
     * Finner tidslinje der arbeidsforholdet er ansett som aktivt ut i fra ansettelsesperioder og permisjoner
     *
     * @param registerAktivitet  En yrkesaktivitet/arbeidsforhold
     * @param grunnlag           Inntekt-arbeid-ytelse grunnlaget
     * @param vilkårsperiode     Aktuell vilkårsperiode
     * @param tidslinjePerYtelse Tidslinje pr mottatt ytelse for bruker
     * @return Aktiv tidslinje for arbeidsforholdet
     */
    public static LocalDateTimeline<AktivitetsAvtaleInnhold> utledAktivTidslinje(Yrkesaktivitet registerAktivitet,
                                                                                 InntektArbeidYtelseGrunnlag grunnlag,
                                                                                 Collection<DatoIntervallEntitet> vilkårsperiode,
                                                                                 Map<OpptjeningAktivitetType, LocalDateTimeline<Boolean>> tidslinjePerYtelse) {
        var inaktivTidslinje = InaktivGrunnetPermisjonUtleder.utledTidslinjeForSammengengendePermisjonOver14Dager(registerAktivitet, tidslinjePerYtelse, vilkårsperiode);
        return gjeldendeAvtaler(grunnlag, registerAktivitet).stream()
            .sorted(AktivitetsAvtale.COMPARATOR).map(a ->
                utledPerioderEtterÅTattHensynTilPermisjoner(inaktivTidslinje, a)
            ).reduce(LocalDateTimeline.empty(), (t1, t2) -> t1.crossJoin(t2, StandardCombinators::coalesceRightHandSide));
    }

    private static Collection<AktivitetsAvtale> gjeldendeAvtaler(InntektArbeidYtelseGrunnlag grunnlag,
                                                                 Yrkesaktivitet registerAktivitet) {
        if (registerAktivitet.erArbeidsforhold()) {
            var filter = new YrkesaktivitetFilter(grunnlag.getArbeidsforholdInformasjon(), registerAktivitet);

            return filter.getAnsettelsesPerioder(registerAktivitet);
        }
        return new YrkesaktivitetFilter(grunnlag.getArbeidsforholdInformasjon().orElse(null), registerAktivitet)
            .getAktivitetsAvtalerForArbeid();
    }

    private static LocalDateTimeline<AktivitetsAvtaleInnhold> utledPerioderEtterÅTattHensynTilPermisjoner(LocalDateTimeline<Boolean> inaktivTidslinje, AktivitetsAvtale avtale) {
        var timeline = new LocalDateTimeline<>(List.of(new LocalDateSegment<>(avtale.getPeriode().toLocalDateInterval(), avtale.getAktivitetsAvtaleInnhold())));
        timeline = timeline.disjoint(inaktivTidslinje);
        return timeline;
    }


}
