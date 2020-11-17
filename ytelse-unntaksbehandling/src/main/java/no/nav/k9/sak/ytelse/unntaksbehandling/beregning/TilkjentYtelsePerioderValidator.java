package no.nav.k9.sak.ytelse.unntaksbehandling.beregning;

import java.util.List;
import java.util.stream.Collectors;

import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.sak.kontrakt.beregningsresultat.TilkjentYtelsePeriodeDto;

class TilkjentYtelsePerioderValidator {
    public static void valider(List<TilkjentYtelsePeriodeDto> perioder) {


        validerOmOverlappendePerioder(perioder);
    }

    private static void validerOmOverlappendePerioder(List<TilkjentYtelsePeriodeDto> perioder) {
        // Sjekk at ingen overlapp mellom disse periodene
        List<LocalDateSegment<TilkjentYtelsePeriodeDto>> datoSegmenter = perioder.stream().map(p -> new LocalDateSegment<>(p.getFom(), p.getTom(), p)).collect(Collectors.toList());

        try {
            LocalDateTimeline<TilkjentYtelsePeriodeDto> localDateTimeline = new LocalDateTimeline<>(datoSegmenter);
        } catch (IllegalArgumentException e) {
            throw TilkjentYtelseOppdaterer.TilkjentYtelseOppdatererFeil.FACTORY.overlappendeTilkjentYtelsePerioder(e.getMessage()).toException();
        }
    }
}
