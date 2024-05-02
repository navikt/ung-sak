package no.nav.k9.sak.ytelse.beregning.regler.feriepenger;

import no.nav.fpsak.nare.evaluation.Evaluation;
import no.nav.fpsak.nare.specification.LeafSpecification;
import no.nav.k9.sak.ytelse.beregning.regelmodell.beregningsgrunnlag.Inntektskategori;
import no.nav.k9.sak.ytelse.beregning.regelmodell.feriepenger.BeregningsresultatFeriepengerRegelModell;

class SjekkOmBrukerHarInntektkategoriDagpenger extends LeafSpecification<BeregningsresultatFeriepengerRegelModell> {
    public static final String ID = "FP_BR 8.1";
    public static final String BESKRIVELSE = "Er brukers inntektskategori arbeidstaker eller sjømann?";


    SjekkOmBrukerHarInntektkategoriDagpenger() {
        super(ID, BESKRIVELSE);
    }

    @Override
    public Evaluation evaluate(BeregningsresultatFeriepengerRegelModell regelModell) {
        boolean erArbeidstakerEllerSjømann = regelModell.getInntektskategorier().stream()
            .anyMatch(Inntektskategori::erArbeidstakerEllerSjømann);
        return erArbeidstakerEllerSjømann ? ja() : nei();
    }
}
