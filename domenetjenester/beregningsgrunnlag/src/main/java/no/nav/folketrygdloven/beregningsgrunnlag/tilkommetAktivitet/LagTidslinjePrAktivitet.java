package no.nav.folketrygdloven.beregningsgrunnlag.tilkommetAktivitet;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.UUID;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import no.nav.folketrygdloven.kalkulus.response.v1.tilkommetAktivitet.UtledetTilkommetAktivitet;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.kodeverk.uttak.UttakArbeidType;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.domene.typer.tid.TidslinjeUtil;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.Arbeidsgiver;

class LagTidslinjePrAktivitet {


    /**
     * Finner tidslinje pr ny aktivitet
     *
     * @param aktuellePerioder                 Perioder der vi er interessert i å finne nye aktiviteter (filtreringsperioder)
     * @param nyeAktiviteterPrEksternreferanse Nye aktiviteter gruppert på eksternreferanser i kalkulus
     * @param vilkårsperiodePrEksternreferanse Vilkårsperiode pr eksternreferanse i kalkulus
     * @return Map fra aktivitet til tidslinje
     */
    static Map<AktivitetstatusOgArbeidsgiver, LocalDateTimeline<Boolean>> lagTidslinjePrNyAktivitet(NavigableSet<DatoIntervallEntitet> aktuellePerioder,
                                                                                                    Map<UUID, List<UtledetTilkommetAktivitet>> nyeAktiviteterPrEksternreferanse,
                                                                                                    Map<UUID, DatoIntervallEntitet> vilkårsperiodePrEksternreferanse) {
        var tidslinjePrAktivitetMap = nyeAktiviteterPrEksternreferanse.entrySet().stream().map(e -> lagTidslinjeMap(
                e.getValue(),
                vilkårsperiodePrEksternreferanse.get(e.getKey()),
                aktuellePerioder))
            .reduce(LagTidslinjePrAktivitet::merge)
            .orElse(Map.of());
        return fjernTommeTidslinjer(tidslinjePrAktivitetMap);
    }

    private static HashMap<AktivitetstatusOgArbeidsgiver, LocalDateTimeline<Boolean>> fjernTommeTidslinjer(Map<AktivitetstatusOgArbeidsgiver, LocalDateTimeline<Boolean>> tidslinjePrAktivitetMap) {
        // Lager nytt map der vi ekskluderer tomme tidslinjer
        var resultat = new HashMap<AktivitetstatusOgArbeidsgiver, LocalDateTimeline<Boolean>>();
        tidslinjePrAktivitetMap.forEach((key, value) -> {
            if (!tidslinjePrAktivitetMap.get(key).isEmpty()) {
                resultat.put(key, value);
            }
        });
        return resultat;
    }


    private static Map<AktivitetstatusOgArbeidsgiver, LocalDateTimeline<Boolean>> merge(Map<AktivitetstatusOgArbeidsgiver, LocalDateTimeline<Boolean>> map1,
                                                                                        Map<AktivitetstatusOgArbeidsgiver, LocalDateTimeline<Boolean>> map2) {
        // Merger en entry fra map2 inn i map1. Ved konflikt brukes crossjoin for å slå sammen tidslinjer
        map2.forEach((key, value) -> {
            map1.merge(key, value, (t1, t2) -> {
                if (!t1.intersection(t2).isEmpty()) {
                    throw new IllegalStateException("Forventer ikke overlapp mellom map for ulike vilkårsperioder");
                }
                return t1.crossJoin(t2);
            });
        });
        return map1;
    }


    /**
     * Lager Map av fra aktivitet til tidslinje der denne regnes som ny/tilkommet
     *
     * @param nyeAktiviteter     Nye aktiviteter som tilkommer etter skjæringstidspunktet gitt fra vilkårsperioden
     * @param vilkårsperiode     Den aktuelle vilkårsperioden
     * @param filtreringPerioder Filtreringsperiode for å begrense resultatet
     * @return Map fra aktivitet til tidslinje
     */
    private static Map<AktivitetstatusOgArbeidsgiver, LocalDateTimeline<Boolean>> lagTidslinjeMap(List<UtledetTilkommetAktivitet> nyeAktiviteter,
                                                                                                  DatoIntervallEntitet vilkårsperiode,
                                                                                                  NavigableSet<DatoIntervallEntitet> filtreringPerioder) {
        return nyeAktiviteter.stream().collect(aktivitetTidslinjeMapCollector(vilkårsperiode, filtreringPerioder));
    }

    private static Collector<UtledetTilkommetAktivitet, ?, Map<AktivitetstatusOgArbeidsgiver, LocalDateTimeline<Boolean>>> aktivitetTidslinjeMapCollector(DatoIntervallEntitet aktuellVilkårsperiode, NavigableSet<DatoIntervallEntitet> filtreringPerioder) {
        return Collectors.toMap(LagTidslinjePrAktivitet::mapTilAktivitetstatusOgArbeidsgiver, aktivitet -> {
                // Finner tidslinje for ny aktivitet fra perioder og begrenser innenfor aktuell vilkårsperiode
                var tidslinjeForAktivitet = mapPerioderTilTidslinje(aktivitet);
                return tidslinjeForAktivitet.intersection(tilTidslinje(aktuellVilkårsperiode)).intersection(TidslinjeUtil.tilTidslinjeKomprimert(filtreringPerioder));
            }
        );
    }

    private static LocalDateTimeline<Boolean> mapPerioderTilTidslinje(UtledetTilkommetAktivitet aktivitet) {
        return aktivitet.getPerioder()
            .stream()
            .map(p -> DatoIntervallEntitet.fraOgMedTilOgMed(p.getFom(), p.getTom()))
            .map(LagTidslinjePrAktivitet::tilTidslinje)
            .reduce(LocalDateTimeline::crossJoin)
            .orElse(LocalDateTimeline.empty());
    }

    private static LocalDateTimeline<Boolean> tilTidslinje(DatoIntervallEntitet aktuellVilkårsperiode) {
        return new LocalDateTimeline<>(aktuellVilkårsperiode.getFomDato(), aktuellVilkårsperiode.getTomDato(), true);
    }

    private static AktivitetstatusOgArbeidsgiver mapTilAktivitetstatusOgArbeidsgiver(UtledetTilkommetAktivitet s) {
        final UttakArbeidType uttakArbeidType = UttakArbeidType.fraKode(s.getAktivitetStatus().getKode());
        final Arbeidsgiver arbeidsgiver;
        if (s.getArbeidsgiver() != null) {
            if (s.getArbeidsgiver().getArbeidsgiverAktørId() != null) {
                arbeidsgiver = Arbeidsgiver.person(new AktørId(s.getArbeidsgiver().getArbeidsgiverAktørId()));
            } else {
                arbeidsgiver = Arbeidsgiver.virksomhet(s.getArbeidsgiver().getArbeidsgiverOrgnr());
            }
        } else {
            arbeidsgiver = null;
        }
        return new AktivitetstatusOgArbeidsgiver(uttakArbeidType, arbeidsgiver);
    }

}
