package no.nav.k9.sak.ytelse.beregning.regelmodell;

import java.util.List;
import java.util.stream.Collectors;

import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;

public class UttakResultat {
    private List<UttakResultatPeriode> uttakResultatPerioder;
    private FagsakYtelseType ytelseType;

    public UttakResultat(FagsakYtelseType ytelseType, List<UttakResultatPeriode> uttakResultatPerioder) {
        this.ytelseType = ytelseType;
        this.uttakResultatPerioder = uttakResultatPerioder;
    }

    public List<UttakResultatPeriode> getUttakResultatPerioder() {
        return uttakResultatPerioder;
    }

    public FagsakYtelseType getYtelseType() {
        return ytelseType;
    }

    public LocalDateTimeline<UttakResultatPeriode> getUttakPeriodeTimeline() {
        List<LocalDateSegment<UttakResultatPeriode>> uttaksPerioder = uttakResultatPerioder.stream()
            .map(periode -> new LocalDateSegment<>(periode.getFom(), periode.getTom(), periode))
            .collect(Collectors.toList());
        return new LocalDateTimeline<>(uttaksPerioder);
    }

    public LocalDateTimeline<List<UttakResultatPeriode>> getUttakPeriodeTimelineMedOverlapp() {
        List<LocalDateSegment<UttakResultatPeriode>> uttaksPerioder = uttakResultatPerioder.stream()
            .map(periode -> new LocalDateSegment<>(periode.getFom(), periode.getTom(), periode))
            .collect(Collectors.toList());
        return LocalDateTimeline.buildGroupOverlappingSegments(uttaksPerioder);
    }
}
