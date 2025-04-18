package no.nav.ung.sak.domene.behandling.steg.uttak;

import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateSegmentCombinator;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.kodeverk.ungdomsytelse.uttak.UngdomsytelseUttakAvslagsårsak;
import no.nav.ung.sak.behandlingslager.ytelse.uttak.UngdomsytelseUttakPeriode;

import java.util.List;

public class UttaksperiodeMapper {

    static List<UngdomsytelseUttakPeriode> mapTilUttaksperioder(List<LocalDateTimeline<no.nav.ung.sak.domene.behandling.steg.uttak.regler.UttakResultat>> uttakstidslinjer) {
        final var resulatatTidslinje = uttakstidslinjer.stream()
            .reduce(UttaksperiodeMapper::kombinerTidslinjer)
            .orElse(LocalDateTimeline.empty());
        return resulatatTidslinje
            .compress()
            .stream()
            .map(s -> new UngdomsytelseUttakPeriode(s.getFom(), s.getTom(), s.getValue().avslagsårsak()))
            .toList();
    }

    private static LocalDateTimeline<no.nav.ung.sak.domene.behandling.steg.uttak.regler.UttakResultat> kombinerTidslinjer(LocalDateTimeline<no.nav.ung.sak.domene.behandling.steg.uttak.regler.UttakResultat> t1, LocalDateTimeline<no.nav.ung.sak.domene.behandling.steg.uttak.regler.UttakResultat> t2) {
        return t1.combine(t2, kombinerUttaksresultater(), LocalDateTimeline.JoinStyle.CROSS_JOIN);
    }

    private static LocalDateSegmentCombinator<no.nav.ung.sak.domene.behandling.steg.uttak.regler.UttakResultat, no.nav.ung.sak.domene.behandling.steg.uttak.regler.UttakResultat, no.nav.ung.sak.domene.behandling.steg.uttak.regler.UttakResultat> kombinerUttaksresultater() {
        return (di, lhs, rhs) -> {
            // Hvis en av segmentene er null, bruk det andre
            if (lhs == null) {
                return new LocalDateSegment<>(di, rhs.getValue());
            } else if (rhs == null) {
                return new LocalDateSegment<>(di, lhs.getValue());
            }

            // Hvis begge segmentene er avslått, velg den høyest prioriterte avslagsårsaken
            if (lhs.getValue().avslagsårsak() != null && rhs.getValue().avslagsårsak() != null) {
                return velgPrioritertAvslagsårsak(di, lhs, rhs);
            }

            // Hvis ett av segmentene er avslått, bruk dette
            if (lhs.getValue().avslagsårsak() != null) {
                return new LocalDateSegment<>(di, lhs.getValue());
            } else if (rhs.getValue().avslagsårsak() != null) {
                return new LocalDateSegment<>(di, rhs.getValue());
            }

            // Forventer ikke å ha to regler som begge gir innvilget i samme periode
            throw new IllegalStateException("Fant to overlappende segmenter for innvilgelse");
        };
    }

    private static LocalDateSegment<no.nav.ung.sak.domene.behandling.steg.uttak.regler.UttakResultat> velgPrioritertAvslagsårsak(LocalDateInterval di, LocalDateSegment<no.nav.ung.sak.domene.behandling.steg.uttak.regler.UttakResultat> lhs, LocalDateSegment<no.nav.ung.sak.domene.behandling.steg.uttak.regler.UttakResultat> rhs) {
        if (lhs.getValue().avslagsårsak().equals(UngdomsytelseUttakAvslagsårsak.SØKERS_DØDSFALL) || rhs.getValue().avslagsårsak().equals(UngdomsytelseUttakAvslagsårsak.SØKERS_DØDSFALL)) {
            return new LocalDateSegment<>(di, no.nav.ung.sak.domene.behandling.steg.uttak.regler.UttakResultat.forAvslag(UngdomsytelseUttakAvslagsårsak.SØKERS_DØDSFALL));
        } else {
            return new LocalDateSegment<>(di, no.nav.ung.sak.domene.behandling.steg.uttak.regler.UttakResultat.forAvslag(lhs.getValue().avslagsårsak()));
        }
    }

}
