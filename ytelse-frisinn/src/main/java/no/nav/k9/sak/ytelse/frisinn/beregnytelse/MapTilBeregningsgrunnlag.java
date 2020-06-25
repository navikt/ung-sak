package no.nav.k9.sak.ytelse.frisinn.beregnytelse;

import java.util.HashSet;
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


    static Optional<Beregningsgrunnlag> mapBeregningsgrunnlag(List<Beregningsgrunnlag> beregningsgrunnlag) {
        Set<BeregningsgrunnlagPeriode.Builder> perioder = new HashSet<>(finnPerioderFraListeMedDagsats(beregningsgrunnlag));
        if (perioder.isEmpty()) {
            return Optional.empty();
        }
        Beregningsgrunnlag.Builder bgBuilder = Beregningsgrunnlag.builder(beregningsgrunnlag.get(0))
            .fjernAllePerioder();
        perioder.forEach(bgBuilder::leggTilBeregningsgrunnlagPeriode);
        return Optional.of(bgBuilder.build());
    }

    static Optional<Beregningsgrunnlag> mapBeregningsgrunnlagForNyeSøknadsperioder(Beregningsgrunnlag beregningsgrunnlag,
                                                                                   Optional<Beregningsgrunnlag> beregningsgrunnlagOriginalBehandling,
                                                                                   DatoIntervallEntitet sisteSøknadsperiode) {
        Set<BeregningsgrunnlagPeriode.Builder> perioder = new HashSet<>();
        perioder.addAll(finnPerioderForNySøknad(beregningsgrunnlag, sisteSøknadsperiode));
        perioder.addAll(finnOriginalBehandlingPerioder(beregningsgrunnlagOriginalBehandling, sisteSøknadsperiode));
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

    private static Set<BeregningsgrunnlagPeriode.Builder> finnPerioderFraListeMedDagsats(List<Beregningsgrunnlag> beregningsgrunnlag) {
        return beregningsgrunnlag.stream().flatMap(bg -> bg.getBeregningsgrunnlagPerioder().stream())
            .filter(p -> p.getDagsats() != null && p.getDagsats() > 0)
            .map(BeregningsgrunnlagPeriode::builder)
            .collect(Collectors.toSet());
    }


}
