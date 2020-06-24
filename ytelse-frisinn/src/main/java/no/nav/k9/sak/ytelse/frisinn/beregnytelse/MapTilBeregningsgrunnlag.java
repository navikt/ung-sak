package no.nav.k9.sak.ytelse.frisinn.beregnytelse;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import no.nav.folketrygdloven.beregningsgrunnlag.modell.Beregningsgrunnlag;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPeriode;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;

class MapTilBeregningsgrunnlag {

    private MapTilBeregningsgrunnlag() {
        // Skjul
    }

    static List<Beregningsgrunnlag> mapBeregningsgrunnlag(List<Beregningsgrunnlag> beregningsgrunnlag) {
        return beregningsgrunnlag.stream()
            .flatMap(bg -> mapPerioderMedUtbetaling(bg).stream()).collect(Collectors.toList());
    }

    private static Optional<Beregningsgrunnlag> mapPerioderMedUtbetaling(Beregningsgrunnlag bg) {
        Set<BeregningsgrunnlagPeriode.Builder> perioder = bg.getBeregningsgrunnlagPerioder().stream()
            .filter(p -> p.getDagsats() != null && p.getDagsats() > 0)
            .map(BeregningsgrunnlagPeriode::builder).collect(Collectors.toSet());

        if (perioder.isEmpty()) {
            return Optional.empty();
        }

        // TODO Lag kopi-konstruktør for å ikkje endre på objektet som sendes inn
        Beregningsgrunnlag.Builder bgBuilder = Beregningsgrunnlag.builder(bg)
            .fjernAllePerioder();

        perioder.forEach(bgBuilder::leggTilBeregningsgrunnlagPeriode);
        return Optional.of(bgBuilder.build());
    }


}
