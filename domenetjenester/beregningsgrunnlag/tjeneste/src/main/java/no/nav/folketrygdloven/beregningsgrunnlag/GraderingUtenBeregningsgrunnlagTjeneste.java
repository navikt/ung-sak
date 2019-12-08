package no.nav.folketrygdloven.beregningsgrunnlag;

import no.nav.folketrygdloven.beregningsgrunnlag.gradering.AktivitetGradering;
import no.nav.folketrygdloven.beregningsgrunnlag.gradering.AndelGradering;
import no.nav.folketrygdloven.beregningsgrunnlag.gradering.AndelGradering.Gradering;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPeriode;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPrStatusOgAndel;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class GraderingUtenBeregningsgrunnlagTjeneste {

    private GraderingUtenBeregningsgrunnlagTjeneste() {
        // Skjuler default konstruktør
    }

    static boolean harAndelerMedGraderingUtenGrunnlag(BeregningsgrunnlagEntitet beregningsgrunnlag, AktivitetGradering aktivitetGradering) {
        return !finnAndelerMedGraderingUtenBG(beregningsgrunnlag, aktivitetGradering).isEmpty();
    }

    public static List<BeregningsgrunnlagPrStatusOgAndel> finnAndelerMedGraderingUtenBG(BeregningsgrunnlagEntitet beregningsgrunnlag, AktivitetGradering aktivitetGradering) {
        List<BeregningsgrunnlagPrStatusOgAndel> graderingsandelerUtenBG = new ArrayList<>();
        aktivitetGradering.getAndelGradering().forEach(andelGradering -> {
            List<BeregningsgrunnlagPrStatusOgAndel> andeler = finnTilsvarendeAndelITilsvarendePeriode(andelGradering, beregningsgrunnlag);
            graderingsandelerUtenBG.addAll(andeler);
        });
        return graderingsandelerUtenBG;
    }

    private static List<BeregningsgrunnlagPrStatusOgAndel> finnTilsvarendeAndelITilsvarendePeriode(AndelGradering andelGradering, BeregningsgrunnlagEntitet beregningsgrunnlag) {
        List<BeregningsgrunnlagPrStatusOgAndel> andeler = new ArrayList<>();
        andelGradering.getGraderinger().forEach(gradering ->{
            Optional<BeregningsgrunnlagPeriode> korrektBGPeriode = finnTilsvarendeBGPeriode(gradering, beregningsgrunnlag.getBeregningsgrunnlagPerioder());
            Optional<BeregningsgrunnlagPrStatusOgAndel> korrektBGAndel = korrektBGPeriode.flatMap(p -> finnTilsvarendeAndelIPeriode(andelGradering, p));
            if (korrektBGAndel.isPresent() && harIkkeTilkjentBGEtterRedusering(korrektBGAndel.get())) {
                andeler.add(korrektBGAndel.get());
            }
        });
        return andeler;
    }

    private static boolean harIkkeTilkjentBGEtterRedusering(BeregningsgrunnlagPrStatusOgAndel andel) {
        return andel.getRedusertPrÅr() != null && andel.getRedusertPrÅr().compareTo(BigDecimal.ZERO) <= 0;
    }

    private static Optional<BeregningsgrunnlagPrStatusOgAndel> finnTilsvarendeAndelIPeriode(AndelGradering andelGradering, BeregningsgrunnlagPeriode periode) {
        return periode.getBeregningsgrunnlagPrStatusOgAndelList().stream().filter(andel -> bgAndelMatcherGraderingAndel(andel, andelGradering)).findFirst();
    }

    private static boolean bgAndelMatcherGraderingAndel(BeregningsgrunnlagPrStatusOgAndel andel, AndelGradering andelGradering) {
        if (!andel.getAktivitetStatus().equals(andelGradering.getAktivitetStatus())) {
            return false;
        }
        if (!Objects.equals(andelGradering.getArbeidsgiver(), andel.getArbeidsgiver().orElse(null))) {
            return false;
        }
        return andelGradering.getArbeidsforholdRef().gjelderFor(andel.getArbeidsforholdRef().orElse(InternArbeidsforholdRef.nullRef()));
    }

    private static Optional<BeregningsgrunnlagPeriode> finnTilsvarendeBGPeriode(Gradering gradering, List<BeregningsgrunnlagPeriode> beregningsgrunnlagPerioder) {
        return beregningsgrunnlagPerioder.stream().filter(p -> gradering.getPeriode().overlapper(p.getPeriode())).findFirst();
    }
}
