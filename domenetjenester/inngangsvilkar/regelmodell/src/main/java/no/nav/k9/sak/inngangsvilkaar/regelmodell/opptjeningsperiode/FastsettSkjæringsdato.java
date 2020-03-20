package no.nav.k9.sak.inngangsvilkaar.regelmodell.opptjeningsperiode;

import java.util.HashMap;
import java.util.Map;

import no.nav.fpsak.nare.doc.RuleDocumentation;
import no.nav.fpsak.nare.evaluation.Evaluation;
import no.nav.fpsak.nare.specification.LeafSpecification;
import no.nav.k9.sak.inngangsvilkaar.regelmodell.opptjening.OpptjeningsperiodeGrunnlag;

@RuleDocumentation(FastsettSkjæringsdato.ID)
public class FastsettSkjæringsdato extends LeafSpecification<OpptjeningsperiodeGrunnlag> {

    static final String ID = "FP_VK 21.5";
    static final String BESKRIVELSE = "opptjeningsvilkar for beregning settes til første dag etter siste aktivitetsdag";

    FastsettSkjæringsdato() {
        super(ID, BESKRIVELSE);
    }

    @Override
    public Evaluation evaluate(OpptjeningsperiodeGrunnlag regelmodell) {
        regelmodell.setSkjæringsdatoOpptjening(regelmodell.getFørsteUttaksDato());

        Map<String, Object> resultater = new HashMap<>();
        resultater.put("skjæringstidspunktOpptjening", String.valueOf(regelmodell.getSkjæringsdatoOpptjening()));
        return beregnet(resultater);
    }
}
