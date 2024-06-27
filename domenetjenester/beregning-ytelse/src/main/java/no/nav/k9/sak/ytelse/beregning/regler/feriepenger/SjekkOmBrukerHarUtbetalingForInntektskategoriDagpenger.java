package no.nav.k9.sak.ytelse.beregning.regler.feriepenger;

import no.nav.fpsak.nare.evaluation.Evaluation;
import no.nav.fpsak.nare.specification.LeafSpecification;
import no.nav.k9.sak.ytelse.beregning.regelmodell.beregningsgrunnlag.Inntektskategori;
import no.nav.k9.sak.ytelse.beregning.regelmodell.feriepenger.BeregningsresultatFeriepengerRegelModell;

import java.time.LocalDate;

class SjekkOmBrukerHarUtbetalingForInntektskategoriDagpenger extends LeafSpecification<BeregningsresultatFeriepengerRegelModell> {
    public static final String ID = "FP_BR 8.10";
    public static final String BESKRIVELSE = "Mottar bruker ytelse for inntektskategori dagpenger?";
    private static final LocalDate FØRSTE_DAG_MED_OPPTJENING_AV_FERIETILLEGG = LocalDate.of(2022,1,1);


    SjekkOmBrukerHarUtbetalingForInntektskategoriDagpenger() {
        super(ID, BESKRIVELSE);
    }

    @Override
    public Evaluation evaluate(BeregningsresultatFeriepengerRegelModell regelModell) {
        boolean mottarYtelseForDagpenger = regelModell.getBeregningsresultatPerioder().stream()
            .filter(periode -> !periode.getFom().isBefore(FØRSTE_DAG_MED_OPPTJENING_AV_FERIETILLEGG))
            .flatMap(periode -> periode.getBeregningsresultatAndelList().stream())
            .filter(andel -> Inntektskategori.DAGPENGER.equals(andel.getInntektskategori()))
            .anyMatch(a -> a.getDagsats() > 0);
        return mottarYtelseForDagpenger ? ja() : nei();
    }
}
