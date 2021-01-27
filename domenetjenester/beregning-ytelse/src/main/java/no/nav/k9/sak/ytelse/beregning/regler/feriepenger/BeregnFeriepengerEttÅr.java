package no.nav.k9.sak.ytelse.beregning.regler.feriepenger;

import java.util.LinkedHashMap;
import java.util.Map;

import no.nav.fpsak.nare.evaluation.Evaluation;
import no.nav.fpsak.nare.specification.LeafSpecification;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.k9.sak.ytelse.beregning.regelmodell.feriepenger.BeregningsresultatFeriepengerRegelModell;

class BeregnFeriepengerEttÅr extends LeafSpecification<BeregningsresultatFeriepengerRegelModell> {
    public static final String ID = "FP_BR 8.7";
    public static final String BESKRIVELSE = "Beregn feriepenger for periode som går over ett kalenderår.";

    BeregnFeriepengerEttÅr() {
        super(ID, BESKRIVELSE);
    }

    @Override
    public Evaluation evaluate(BeregningsresultatFeriepengerRegelModell regelModell) {
        Map<String, Object> resultater = new LinkedHashMap<>();

        LocalDateInterval feriepengerPeriode = regelModell.getFeriepengerPeriode();
        BeregnFeriepengerForPeriode.beregn(resultater, regelModell.getBeregningsresultatPerioder(), feriepengerPeriode, regelModell.harFeriepengeopptjeningFoHelg());

        return beregnet(resultater);
    }
}
