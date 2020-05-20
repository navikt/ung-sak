package no.nav.k9.sak.domene.behandling.steg.foreslåvedtak;

import no.nav.folketrygdloven.beregningsgrunnlag.modell.Beregningsgrunnlag;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPeriode;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

// TODO Se om ikke denne kan flyttes til kalkulus
public final class VurderUgunst {

    private VurderUgunst() {
        // Skjuler default konst
    }

    public static boolean erUgunst(Beregningsgrunnlag originalGrunnlag, Beregningsgrunnlag revurderingGrunnlag) {
        if (revurderingGrunnlag == null) {
            return originalGrunnlag != null;
        } else if (originalGrunnlag == null) {
            // Aldri ugunst hvis originalt grunnlag ikke fantes
            return false;
        }
        List<BeregningsgrunnlagPeriode> originalePerioder = originalGrunnlag.getBeregningsgrunnlagPerioder();
        List<BeregningsgrunnlagPeriode> revurderingsPerioder = revurderingGrunnlag.getBeregningsgrunnlagPerioder();

        Set<LocalDate> allePeriodeDatoer = finnAllePeriodersStartdatoer(revurderingsPerioder, originalePerioder);

        for (LocalDate dato : allePeriodeDatoer) {
            Long dagsatsRevurderingsgrunnlag = finnGjeldendeDagsatsForDenneDatoen(dato, revurderingsPerioder);
            Long dagsatsOriginaltGrunnlag = finnGjeldendeDagsatsForDenneDatoen(dato, originalePerioder);
            if (dagsatsOriginaltGrunnlag != null && (dagsatsRevurderingsgrunnlag == null || dagsatsRevurderingsgrunnlag < dagsatsOriginaltGrunnlag)) {
                return true;
            }
        }
        return false;
    }

    private static Set<LocalDate> finnAllePeriodersStartdatoer(List<BeregningsgrunnlagPeriode> revurderingsPerioder, List<BeregningsgrunnlagPeriode> originalePerioder) {
        Set<LocalDate> startDatoer = new HashSet<>();
        revurderingsPerioder.stream().map(BeregningsgrunnlagPeriode::getBeregningsgrunnlagPeriodeFom).forEach(startDatoer::add);
        originalePerioder.stream().map(BeregningsgrunnlagPeriode::getBeregningsgrunnlagPeriodeFom).forEach(startDatoer::add);
        return startDatoer;
    }

    private static Long finnGjeldendeDagsatsForDenneDatoen(LocalDate dato, List<BeregningsgrunnlagPeriode> perioder) {
        // Hvis dato er før starten på den første perioden bruker vi første periodes dagsats
        Optional<BeregningsgrunnlagPeriode> førsteKronologiskePeriode = perioder.stream()
            .min(Comparator.comparing(BeregningsgrunnlagPeriode::getBeregningsgrunnlagPeriodeFom));
        if (førsteKronologiskePeriode.filter(periode -> dato.isBefore(periode.getBeregningsgrunnlagPeriodeFom())).isPresent()) {
            return førsteKronologiskePeriode.get().getDagsats();
        }
        for (BeregningsgrunnlagPeriode periode : perioder) {
            if (periode.getPeriode().inkluderer(dato)) {
                return periode.getDagsats();
            }
        }
        throw new IllegalStateException("Finner ikke dagsats for denne perioden");
    }

}
