package no.nav.k9.sak.ytelse.frisinn.beregnytelse;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import no.nav.folketrygdloven.beregningsgrunnlag.modell.Beregningsgrunnlag;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPeriode;

class MapTilBeregningsgrunnlag {

    private MapTilBeregningsgrunnlag() {
        // Skjul
    }

    static List<Beregningsgrunnlag> mapBeregningsgrunnlag(Beregningsgrunnlag beregningsgrunnlag) {
        Set<BeregningsgrunnlagPeriode.Builder> perioder = beregningsgrunnlag.getBeregningsgrunnlagPerioder().stream()
            .filter(p -> p.getDagsats() != null && p.getDagsats() > 0)
            .map(BeregningsgrunnlagPeriode::builder)
            .collect(Collectors.toSet());

        if (perioder.isEmpty()) {
            return Collections.emptyList();
        }

        Beregningsgrunnlag.Builder bgBuilder = Beregningsgrunnlag.builder(beregningsgrunnlag)
            .fjernAllePerioder();

        perioder.forEach(bgBuilder::leggTilBeregningsgrunnlagPeriode);

        return List.of(bgBuilder.build());
    }

}
