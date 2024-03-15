package no.nav.k9.sak.ytelse.pleiepengerbarn.revurdering;

import java.util.List;
import java.util.Objects;
import java.util.Set;

import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;
import no.nav.k9.kodeverk.arbeidsforhold.AktivitetStatus;
import no.nav.k9.kodeverk.arbeidsforhold.ArbeidType;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.registerendringer.Aktivitetsendringer;
import no.nav.k9.sak.registerendringer.Endringstype;

class UtledRelevanteEndringerIAktivitetsperiode {


    /**
     * @param aktivitetsperiodeEndringer        Endringer i aktivitetsperiode pr arbeidsforhold/aktivitet
     * @param endringerPrMottaker               Endring i utbetaling/tilkjent ytelse pr mottaker
     * @param revurdertePerioderVilkårsperioder Utleder endringer i register som kan være årsak til endring i utbetaling
     * @return Liste med aktivitsendringer for endret periode med aktivitet og type endring
     */
    static List<Aktivitetsendringer> finnRelevanteEndringerIAktivitetsperiode(List<AktivitetsperiodeEndring> aktivitetsperiodeEndringer,
                                                                              List<UtbetalingsendringerForMottaker> endringerPrMottaker,
                                                                              Set<DatoIntervallEntitet> revurdertePerioderVilkårsperioder) {
        return aktivitetsperiodeEndringer.stream()
            .map(aktivitetsendringer -> {
                var endringIRegisterOgUtbetaling = utledRelevantEndringstidslinje(aktivitetsendringer, endringerPrMottaker, revurdertePerioderVilkårsperioder);
                return new Aktivitetsendringer(aktivitetsendringer.identifikator().arbeidsgiver(), aktivitetsendringer.identifikator().ref(), endringIRegisterOgUtbetaling);
            })
            .toList();
    }

    private static LocalDateTimeline<Endringstype> utledRelevantEndringstidslinje(AktivitetsperiodeEndring aktivitetsendringer,
                                                                                  List<UtbetalingsendringerForMottaker> endringerPrMottaker,
                                                                                  Set<DatoIntervallEntitet> revurdertePerioder) {
        var tidslinjeForEndringIUtbetaling = finnTidslinjeForEndringIUtbetaling(aktivitetsendringer, endringerPrMottaker);
        var relevanteRegisterEndringerFraFørStp = finnEndringerDagenFørSkjæringstidspunktet(tidslinjeForEndringIUtbetaling, aktivitetsendringer.endringstidslinje(), revurdertePerioder);
        var relevanteEndringerIPeriode = aktivitetsendringer.endringstidslinje().intersection(tidslinjeForEndringIUtbetaling);
        return relevanteEndringerIPeriode.crossJoin(relevanteRegisterEndringerFraFørStp, StandardCombinators::coalesceRightHandSide); // velger endring før stp dersom endring i begge perioder
    }

    private static LocalDateTimeline<Endringstype> finnEndringerDagenFørSkjæringstidspunktet(LocalDateTimeline<Boolean> tidslinjeForEndringIUtbetaling,
                                                                                             LocalDateTimeline<Endringstype> tidslinjeForEndringIRegister,
                                                                                             Set<DatoIntervallEntitet> vilkårsperioder) {
        return vilkårsperioder.stream()
            .map(p -> finnTidslinjeSomPåvirkesAvEndringerFørStp(tidslinjeForEndringIUtbetaling, tidslinjeForEndringIRegister, p))
            .reduce(LocalDateTimeline.empty(), LocalDateTimeline::crossJoin);
    }

    private static LocalDateTimeline<Endringstype> finnTidslinjeSomPåvirkesAvEndringerFørStp(LocalDateTimeline<Boolean> tidslinjeForEndringIUtbetaling, LocalDateTimeline<Endringstype> tidslinjeForEndringIRegister, DatoIntervallEntitet p) {
        var dagenFørStp = p.getFomDato().minusDays(1);
        var endringerDagenFørStp = tidslinjeForEndringIRegister.intersection(new LocalDateInterval(dagenFørStp, dagenFørStp));
        var segmenter = endringerDagenFørStp.toSegments();
        if (segmenter.size() > 1) {
            throw new IllegalStateException("Kan ikke ha flere enn ett segment for overlapp på en dag");
        }
        if (!segmenter.isEmpty()) {
            // Mapper endring som skjer før skjæringstidspunktet til tidsrommet for endret utbetaling
            return new LocalDateTimeline<>(p.toLocalDateInterval(), segmenter.iterator().next().getValue())
                .intersection(tidslinjeForEndringIUtbetaling);
        }
        return new LocalDateTimeline<>(List.of());
    }

    private static LocalDateTimeline<Boolean> finnTidslinjeForEndringIUtbetaling(AktivitetsperiodeEndring aktivitetsendringer, List<UtbetalingsendringerForMottaker> endringerPrMottaker) {
        return endringerPrMottaker.stream()
            .filter(e -> Objects.equals(e.nøkkel().arbeidsgiver(), aktivitetsendringer.identifikator().arbeidsgiver()) &&
                e.nøkkel().arbeidsforholdRef().gjelderFor(aktivitetsendringer.identifikator().ref()) &&
                matcherStatusOgType(e.nøkkel().aktivitetStatus(), aktivitetsendringer.identifikator().arbeidType())
            )
            .map(UtbetalingsendringerForMottaker::tidslinjeMedEndringIYtelse)
            .map(t -> t.mapValue(it -> true))
            .reduce(LocalDateTimeline.empty(), (t1, t2) -> t1.combine(t2, StandardCombinators::alwaysTrueForMatch, LocalDateTimeline.JoinStyle.CROSS_JOIN));
    }

    private static boolean matcherStatusOgType(AktivitetStatus aktivitetStatus, ArbeidType arbeidType) {
        if (aktivitetStatus.erFrilanser()) {
            return arbeidType.equals(ArbeidType.FRILANSER_OPPDRAGSTAKER_MED_MER);
        } else if (aktivitetStatus.erArbeidstaker()) {
            return arbeidType.equals(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD);
        }
        return false;
    }
}
