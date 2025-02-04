package no.nav.ung.sak.domene.behandling.steg.uttak;

import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateSegmentCombinator;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.kodeverk.ungdomsytelse.uttak.UngdomsytelseUttakAvslagsårsak;
import no.nav.ung.sak.behandlingslager.ytelse.uttak.UngdomsytelseUttakPeriode;
import no.nav.ung.sak.domene.behandling.steg.uttak.regler.UttakResultat;

import java.util.List;

public class UttaksperiodeMapper {

    static List<UngdomsytelseUttakPeriode> mapTilUttaksperioder(List<LocalDateTimeline<UttakResultat>> uttakstidslinjer) {
        return uttakstidslinjer.stream()
            .reduce(UttaksperiodeMapper::kombinerTidslinjer)
            .orElse(LocalDateTimeline.empty())
            .compress()
            .stream()
            .map(s -> new UngdomsytelseUttakPeriode(s.getFom(), s.getTom(), s.getValue().utbetalingsgrad(), s.getValue().avslagsårsak()))
            .toList();
    }

    private static LocalDateTimeline<UttakResultat> kombinerTidslinjer(LocalDateTimeline<UttakResultat> t1, LocalDateTimeline<UttakResultat> t2) {
        return t1.combine(t2, kombinerUttaksresultater(), LocalDateTimeline.JoinStyle.CROSS_JOIN);
    }

    private static LocalDateSegmentCombinator<UttakResultat, UttakResultat, UttakResultat> kombinerUttaksresultater() {
        return (di, lhs, rhs) -> {
            if (lhs == null) {
                return new LocalDateSegment<>(di, rhs.getValue());
            } else if (rhs == null) {
                return new LocalDateSegment<>(di, lhs.getValue());
            } else if (lhs.getValue().avslagsårsak() != null && rhs.getValue().avslagsårsak() != null) {
                return velgPrioritertAvslagsårsak(di, lhs, rhs);
            } else if (lhs.getValue().avslagsårsak() != null) {
                return new LocalDateSegment<>(di, lhs.getValue());
            } else if (rhs.getValue().avslagsårsak() != null) {
                return new LocalDateSegment<>(di, rhs.getValue());
            }
            throw new IllegalStateException("Fant to overlappende segmenter for innvilgelse");
        };
    }

    private static LocalDateSegment<UttakResultat> velgPrioritertAvslagsårsak(LocalDateInterval di, LocalDateSegment<UttakResultat> lhs, LocalDateSegment<UttakResultat> rhs) {
        if (lhs.getValue().avslagsårsak().equals(UngdomsytelseUttakAvslagsårsak.SØKERS_DØDSFALL) || rhs.getValue().avslagsårsak().equals(UngdomsytelseUttakAvslagsårsak.SØKERS_DØDSFALL)) {
            return new LocalDateSegment<>(di, UttakResultat.forAvslag(UngdomsytelseUttakAvslagsårsak.SØKERS_DØDSFALL));
        } else {
            return new LocalDateSegment<>(di, UttakResultat.forAvslag(lhs.getValue().avslagsårsak()));
        }
    }
}
