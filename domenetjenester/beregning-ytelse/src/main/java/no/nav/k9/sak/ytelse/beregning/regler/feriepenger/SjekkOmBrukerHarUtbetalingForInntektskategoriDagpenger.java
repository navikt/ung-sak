package no.nav.k9.sak.ytelse.beregning.regler.feriepenger;

import no.nav.fpsak.nare.evaluation.Evaluation;
import no.nav.fpsak.nare.specification.LeafSpecification;
import no.nav.k9.sak.ytelse.beregning.regelmodell.beregningsgrunnlag.Inntektskategori;
import no.nav.k9.sak.ytelse.beregning.regelmodell.feriepenger.BeregningsresultatFeriepengerRegelModell;

class SjekkOmBrukerHarUtbetalingForInntektskategoriDagpenger extends LeafSpecification<BeregningsresultatFeriepengerRegelModell> {
    public static final String ID = "FP_BR 8.10";
    public static final String BESKRIVELSE = "Mottar bruker ytelse for inntektskategori dagpenger?";


    SjekkOmBrukerHarUtbetalingForInntektskategoriDagpenger() {
        super(ID, BESKRIVELSE);
    }

    @Override
    public Evaluation evaluate(BeregningsresultatFeriepengerRegelModell regelModell) {
        boolean mottarYtelseForDagpenger = regelModell.getBeregningsresultatPerioder().stream()
            .flatMap(a -> a.getBeregningsresultatAndelList().stream())
            .filter(a -> Inntektskategori.DAGPENGER.equals(a.getInntektskategori()))
            .anyMatch(a -> a.getDagsats() > 0);
        return mottarYtelseForDagpenger ? ja() : nei();
    }
}
