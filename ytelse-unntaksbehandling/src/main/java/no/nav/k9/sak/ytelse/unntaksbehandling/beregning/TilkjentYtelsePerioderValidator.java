package no.nav.k9.sak.ytelse.unntaksbehandling.beregning;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

import java.time.LocalDate;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.Vilkår;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriode;
import no.nav.k9.sak.kontrakt.beregningsresultat.TilkjentYtelsePeriodeDto;

class TilkjentYtelsePerioderValidator {
    public static void valider(List<TilkjentYtelsePeriodeDto> perioder, Vilkår vilkår) {
        requireNonNull(vilkår, "vilkår er påkrevd, men var null");
        validerOmOverlappendePerioder(perioder);
        validerVilkårsperiode(perioder, vilkår);
    }

    static void validerVilkårsperiode(List<TilkjentYtelsePeriodeDto> perioder, Vilkår vilkår) {
        var vilkårFom = vilkår.getPerioder().stream()
            .map(VilkårPeriode::getFom)
            .min(LocalDate::compareTo)
            .orElseThrow(feil("Finner ikke vilkårets fom"));

        var vilkårTom = vilkår.getPerioder().stream()
            .map(VilkårPeriode::getTom)
            .max(LocalDate::compareTo)
            .orElseThrow(feil("Finner ikke vilkårets tom"));

        var tilkjentYtelseFom = perioder.stream()
            .map(TilkjentYtelsePeriodeDto::getFom)
            .min(LocalDate::compareTo)
            .orElseThrow(feil("Finner ikke når tilkjent ytelse fom"));

        var tilkjentYtelseTom = perioder.stream()
            .map(TilkjentYtelsePeriodeDto::getTom)
            .max(LocalDate::compareTo)
            .orElseThrow(feil("Finner ikke når tilkjent ytelse fom"));

        if (tilkjentYtelseFom.isBefore(vilkårFom) || tilkjentYtelseTom.isAfter(vilkårTom)) {
            throw TilkjentYtelseOppdaterer.TilkjentYtelseOppdatererFeil.FACTORY.tilkjentYtelseIkkeInnenforVilkår().toException();
        }
    }

    private static Supplier<IllegalArgumentException> feil(String feilmelding) {
        return () -> new IllegalArgumentException(format("Kan ikke validere.  %s", feilmelding));
    }

    static void validerOmOverlappendePerioder(List<TilkjentYtelsePeriodeDto> perioder) {
        // Sjekk at ingen overlapp mellom disse periodene
        List<LocalDateSegment<TilkjentYtelsePeriodeDto>> datoSegmenter = perioder.stream().map(p -> new LocalDateSegment<>(p.getFom(), p.getTom(), p)).collect(Collectors.toList());

        try {
            @SuppressWarnings("unused") LocalDateTimeline<TilkjentYtelsePeriodeDto> localDateTimeline = new LocalDateTimeline<>(datoSegmenter);
        } catch (IllegalArgumentException e) {
            throw TilkjentYtelseOppdaterer.TilkjentYtelseOppdatererFeil.FACTORY.overlappendeTilkjentYtelsePerioder(e.getMessage()).toException();
        }
    }
}
