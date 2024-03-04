package no.nav.k9.sak.ytelse.pleiepengerbarn.revurdering;

import java.util.List;
import java.util.Objects;
import java.util.Set;

import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;
import no.nav.k9.kodeverk.arbeidsforhold.AktivitetStatus;
import no.nav.k9.kodeverk.arbeidsforhold.ArbeidType;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.registerendringer.Aktivitetsendringer;
import no.nav.k9.sak.registerendringer.Endringstype;

class UledRelevanteEndringerIAktivitetsperiode {


    /**
     * @param aktivitetsperiodeEndringer        Endringer i aktivitetsperiode pr arbeidsforhold/aktivitet
     * @param endringerPrMottaker               Endring i utbetaling/tilkjent ytelse pr mottaker
     * @param revurdertePerioderVilkårsperioder Utleder endringer i register som kan være årsak til endring i utbetaling
     * @return Liste med aktivitsendringer for endret periode med aktivitet og type endring
     */
    static List<Aktivitetsendringer> finnRelevanteEndringerIAktivitetsperiode(List<UtledAktivitetsperiodeEndring.AktivitetsperiodeEndring> aktivitetsperiodeEndringer,
                                                                              List<UtledTilkjentYtelseEndring.EndringerForMottaker> endringerPrMottaker,
                                                                              Set<DatoIntervallEntitet> revurdertePerioderVilkårsperioder) {
        return aktivitetsperiodeEndringer.stream()
            .map(aktivitetsendringer -> {
                var endringIRegisterOgUtbetaling = utledRelevantEndringstidslinje(aktivitetsendringer, endringerPrMottaker, revurdertePerioderVilkårsperioder);
                return new Aktivitetsendringer(aktivitetsendringer.identifikator().arbeidsgiver(), aktivitetsendringer.identifikator().ref(), endringIRegisterOgUtbetaling);
            })
            .toList();
    }

    private static LocalDateTimeline<Endringstype> utledRelevantEndringstidslinje(UtledAktivitetsperiodeEndring.AktivitetsperiodeEndring aktivitetsendringer,
                                                                                  List<UtledTilkjentYtelseEndring.EndringerForMottaker> endringerPrMottaker,
                                                                                  Set<DatoIntervallEntitet> revurdertePerioder) {
        var tidslinjeForEndringIUtbetaling = finnTidslinjeForEndringIUtbetaling(aktivitetsendringer, endringerPrMottaker);
        var utvidet = utvidMedDagenFørStp(tidslinjeForEndringIUtbetaling, revurdertePerioder);
        return aktivitetsendringer.endringstidslinje().intersection(utvidet);
    }

    private static LocalDateTimeline<Boolean> utvidMedDagenFørStp(LocalDateTimeline<Boolean> tidslinjeForEndringIUtbetaling, Set<DatoIntervallEntitet> vilkårsperioder) {
        return vilkårsperioder.stream().filter(p -> !tidslinjeForEndringIUtbetaling.intersection(p.toLocalDateInterval()).isEmpty())
            .map(p -> new LocalDateTimeline<>(p.getFomDato().minusDays(1), p.getFomDato().minusDays(1), true))
            .reduce(tidslinjeForEndringIUtbetaling, (t1, t2) -> t1.combine(t2, StandardCombinators::alwaysTrueForMatch, LocalDateTimeline.JoinStyle.CROSS_JOIN));
    }

    private static LocalDateTimeline<Boolean> finnTidslinjeForEndringIUtbetaling(UtledAktivitetsperiodeEndring.AktivitetsperiodeEndring aktivitetsendringer, List<UtledTilkjentYtelseEndring.EndringerForMottaker> endringerPrMottaker) {
        return endringerPrMottaker.stream()
            .filter(e -> Objects.equals(e.nøkkel().arbeidsgiver(), aktivitetsendringer.identifikator().arbeidsgiver()) &&
                e.nøkkel().arbeidsforholdRef().gjelderFor(aktivitetsendringer.identifikator().ref()) &&
                matcherStatusOgType(e.nøkkel().aktivitetStatus(), aktivitetsendringer.identifikator().arbeidType())
            )
            .map(UtledTilkjentYtelseEndring.EndringerForMottaker::tidslinjeMedEndringIYtelse)
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
