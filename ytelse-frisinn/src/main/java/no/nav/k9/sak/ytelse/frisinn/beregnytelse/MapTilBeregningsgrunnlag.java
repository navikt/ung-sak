package no.nav.k9.sak.ytelse.frisinn.beregnytelse;

import java.util.HashSet;
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

    static Optional<Beregningsgrunnlag> mapBeregningsgrunnlag(Beregningsgrunnlag beregningsgrunnlag,
                                                              Optional<Beregningsgrunnlag> beregningsgrunnlagOriginalBehandling,
                                                              DatoIntervallEntitet sisteSøknadsperiode,
                                                              Boolean skalBenytteTidligereResultat) {
        Set<BeregningsgrunnlagPeriode.Builder> perioder = new HashSet<>();
        if (skalBenytteTidligereResultat) {
            perioder.addAll(finnPerioderForNySøknad(beregningsgrunnlag, sisteSøknadsperiode));
            perioder.addAll(finnOriginalBehandlingPerioder(beregningsgrunnlagOriginalBehandling, sisteSøknadsperiode));
        } else {
            perioder.addAll(beregningsgrunnlag.getBeregningsgrunnlagPerioder().stream()
                .filter(p -> p.getDagsats() != null && p.getDagsats() > 0)
                .map(BeregningsgrunnlagPeriode::builder)
                .collect(Collectors.toSet()));
        }


        if (perioder.isEmpty()) {
            return Optional.empty();
        }

        Beregningsgrunnlag.Builder bgBuilder = Beregningsgrunnlag.builder(beregningsgrunnlag)
            .fjernAllePerioder();

        perioder.forEach(bgBuilder::leggTilBeregningsgrunnlagPeriode);

        return Optional.of(bgBuilder.build());
    }

    private static Set<BeregningsgrunnlagPeriode.Builder> finnOriginalBehandlingPerioder(Optional<Beregningsgrunnlag> beregningsgrunnlagOriginalBehandling, DatoIntervallEntitet sisteSøknadsperiode) {
        return beregningsgrunnlagOriginalBehandling.stream()
            .flatMap(orginal -> orginal.getBeregningsgrunnlagPerioder().stream()
                .filter(p -> p.getDagsats() != null && p.getDagsats() > 0)
                .filter(p -> !p.getPeriode().getTomDato().isAfter(sisteSøknadsperiode.getFomDato().withDayOfMonth(1).minusDays(1)))
                .map(BeregningsgrunnlagPeriode::builder))
            .collect(Collectors.toSet());
    }

    private static Set<BeregningsgrunnlagPeriode.Builder> finnPerioderForNySøknad(Beregningsgrunnlag beregningsgrunnlag, DatoIntervallEntitet sisteSøknadsperiode) {
        return beregningsgrunnlag.getBeregningsgrunnlagPerioder().stream()
            .filter(p -> p.getDagsats() != null && p.getDagsats() > 0)
            .filter(p -> !p.getPeriode().getFomDato().isBefore(sisteSøknadsperiode.getFomDato()))
            .map(BeregningsgrunnlagPeriode::builder)
            .collect(Collectors.toSet());
    }



}
