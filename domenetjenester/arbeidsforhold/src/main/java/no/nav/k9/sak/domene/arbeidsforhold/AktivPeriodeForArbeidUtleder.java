package no.nav.k9.sak.domene.arbeidsforhold;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;
import no.nav.k9.kodeverk.opptjening.OpptjeningAktivitetType;
import no.nav.k9.sak.domene.iay.modell.AktivitetsAvtale;
import no.nav.k9.sak.domene.iay.modell.AktivitetsAvtaleInnhold;
import no.nav.k9.sak.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.k9.sak.domene.iay.modell.Yrkesaktivitet;
import no.nav.k9.sak.domene.iay.modell.YrkesaktivitetFilter;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;

public class AktivPeriodeForArbeidUtleder {


    public static LocalDateTimeline<AktivitetsAvtaleInnhold> utledAktivPeriode(Yrkesaktivitet registerAktivitet,
                                                                               InntektArbeidYtelseGrunnlag grunnlag,
                                                                               DatoIntervallEntitet vilkårsperiode,
                                                                               Map<OpptjeningAktivitetType, LocalDateTimeline<Boolean>> tidslinjePerYtelse) {
        var inaktivTidslinje = InaktivGrunnetPermisjonUtleder.utledTidslinjeForSammengengendePermisjonOver14Dager(registerAktivitet, tidslinjePerYtelse, vilkårsperiode);
        return gjeldendeAvtaler(grunnlag, vilkårsperiode.getFomDato(), registerAktivitet).stream()
            .sorted(AktivitetsAvtale.COMPARATOR).map(a ->
                utledPerioderEtterÅTattHensynTilPermisjoner(inaktivTidslinje, a)
            ).reduce(LocalDateTimeline.empty(), (t1, t2) -> t1.crossJoin(t2, StandardCombinators::coalesceRightHandSide));
    }

    private static Collection<AktivitetsAvtale> gjeldendeAvtaler(InntektArbeidYtelseGrunnlag grunnlag,
                                                                 LocalDate skjæringstidspunktForOpptjening,
                                                                 Yrkesaktivitet registerAktivitet) {
        if (registerAktivitet.erArbeidsforhold()) {
            var filter = new YrkesaktivitetFilter(grunnlag.getArbeidsforholdInformasjon(), registerAktivitet);

            return filter.getAnsettelsesPerioder(registerAktivitet);
        }
        return new YrkesaktivitetFilter(grunnlag.getArbeidsforholdInformasjon().orElse(null), registerAktivitet)
            .før(skjæringstidspunktForOpptjening)
            .getAktivitetsAvtalerForArbeid();
    }

    private static LocalDateTimeline<AktivitetsAvtaleInnhold> utledPerioderEtterÅTattHensynTilPermisjoner(LocalDateTimeline<Boolean> inaktivTidslinje, AktivitetsAvtale avtale) {
        var timeline = new LocalDateTimeline<>(List.of(new LocalDateSegment<>(avtale.getPeriode().toLocalDateInterval(), avtale.getAktivitetsAvtaleInnhold())));
        timeline = timeline.disjoint(inaktivTidslinje);
        return timeline;
    }


}
