package no.nav.k9.sak.ytelse.beregning.regler.feriepenger;

import java.util.LinkedHashMap;
import java.util.Map;

import no.nav.fpsak.nare.evaluation.Evaluation;
import no.nav.fpsak.nare.specification.LeafSpecification;
import no.nav.k9.sak.ytelse.beregning.regelmodell.feriepenger.BeregningsresultatFeriepengerRegelModell;

class BeregnFeriepenger extends LeafSpecification<BeregningsresultatFeriepengerRegelModell> {
    public static final String ID = "FP_BR 8.6";
    public static final String BESKRIVELSE = "Beregn feriepenger for periode som går over flere kalenderår.";

    BeregnFeriepenger() {
        super(ID, BESKRIVELSE);
    }

    @Override
    public Evaluation evaluate(BeregningsresultatFeriepengerRegelModell regelModell) {
        Map<String, Object> regelsporing = new LinkedHashMap<>();
        BeregnFeriepengerForPeriode.beregn(regelsporing, regelModell.getBeregningsresultatPerioder(), regelModell.getFeriepengerPeriodeBruker(), regelModell.getFeriepengerPeriodeRefusjon(), regelModell.harFeriepengeopptjeningFoHelg());
        return beregnet(regelsporing);
    }
}
