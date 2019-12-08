package no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.perioder;

import java.time.LocalDate;
import java.util.Optional;

import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.regelmodell.AndelGradering;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.regelmodell.Periode;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.regelmodell.PeriodeModell;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.regelmodell.PeriodisertBruttoBeregningsgrunnlag;

class IdentifiserPeriodeDerBruttoBgPåHøyerePrioriterteAndelerErMinst6G {
    private IdentifiserPeriodeDerBruttoBgPåHøyerePrioriterteAndelerErMinst6G() {
        // skjul default
    }

    static Optional<LocalDate> vurder(
        PeriodeModell input,
        AndelGradering andelGradering,
        Periode gradering) {
        Optional<LocalDate> høyerePrioritertAndeler6GFomOpt = input.getPeriodisertBruttoBeregningsgrunnlagList().stream()
            .filter(periodisertBg -> periodisertBg.getPeriode().overlapper(gradering))
            .filter(periodisertBg -> ErHøyerePrioriterteAndelerBruttoMinst6G.vurder(input.getGrunnbeløp(), periodisertBg, andelGradering))
            .map(PeriodisertBruttoBeregningsgrunnlag::getPeriode)
            .map(Periode::getFom)
            .sorted()
            .findFirst();
        return høyerePrioritertAndeler6GFomOpt.map(høyerePrioritertFom ->
        {
            LocalDate graderingFom = gradering.getFom();
            return høyerePrioritertFom.isBefore(graderingFom) ? graderingFom : høyerePrioritertFom;
        });
    }
}
