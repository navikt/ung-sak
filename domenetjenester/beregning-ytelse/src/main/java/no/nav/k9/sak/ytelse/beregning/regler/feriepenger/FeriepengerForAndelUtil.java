package no.nav.k9.sak.ytelse.beregning.regler.feriepenger;

import java.util.List;

import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.sak.ytelse.beregning.regelmodell.BeregningsresultatAndel;
import no.nav.k9.sak.ytelse.beregning.regelmodell.BeregningsresultatPeriode;

public class FeriepengerForAndelUtil {

    private FeriepengerForAndelUtil() {
    }

    public static LocalDateTimeline<Boolean> utledTidslinjerHarAndelSomKanGiFeriepenger(List<BeregningsresultatPeriode> beregningsresultatPerioder) {
        return new LocalDateTimeline<>(beregningsresultatPerioder.stream()
            .filter(periode -> periode.getBeregningsresultatAndelList().stream().anyMatch(BeregningsresultatAndel::girRettTilFeriepenger))
            .map(periode -> new LocalDateSegment<>(periode.getFom(), periode.getTom(), true))
            .toList()
        );
    }

}
