package no.nav.k9.sak.ytelse.beregning.regler.feriepenger;

import no.nav.fpsak.nare.evaluation.Evaluation;
import no.nav.fpsak.nare.specification.LeafSpecification;
import no.nav.k9.sak.ytelse.beregning.regelmodell.feriepenger.BeregningsresultatFeriepengerRegelModell;

class SjekkOmYtelseErTilkjent extends LeafSpecification<BeregningsresultatFeriepengerRegelModell> {
    public static final String ID = "FP_BR 8.2";
    public static final String BESKRIVELSE = "Har det blitt tilkjent ytelse i den totale stønadsperioden?";


    SjekkOmYtelseErTilkjent() {
        super(ID, BESKRIVELSE);
    }

    @Override
    public Evaluation evaluate(BeregningsresultatFeriepengerRegelModell regelModell) {
        boolean utbetaltYtelse = regelModell.getBeregningsresultatPerioder().stream()
            .flatMap(p -> p.getBeregningsresultatAndelList().stream())
            .anyMatch(andel -> andel.getDagsats() > 0);

        return utbetaltYtelse ? ja() : nei();
    }
}
