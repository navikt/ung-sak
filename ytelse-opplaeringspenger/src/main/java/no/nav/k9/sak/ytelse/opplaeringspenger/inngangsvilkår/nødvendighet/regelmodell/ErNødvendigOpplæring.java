package no.nav.k9.sak.ytelse.opplaeringspenger.inngangsvilkår.nødvendighet.regelmodell;

import java.util.Objects;

import no.nav.fpsak.nare.doc.RuleDocumentation;
import no.nav.fpsak.nare.evaluation.Evaluation;
import no.nav.fpsak.nare.specification.LeafSpecification;

@RuleDocumentation(ErNødvendigOpplæring.ID)
public class ErNødvendigOpplæring extends LeafSpecification<NødvendighetMellomregningData> {

    public static final String ID = "OLP_VK_9.14.1";

    public ErNødvendigOpplæring() {
        super(ID);
    }

    @Override
    public Evaluation evaluate(NødvendighetMellomregningData mellomregningData) {
        var ikkeGodkjentTidslinje = mellomregningData.getTidslinjeTilVurdering()
            .disjoint(mellomregningData.getOpplæringVurderingTidslinje()
                .filterValue(vurdering -> Objects.equals(vurdering, OpplæringVurdering.NØDVENDIG)));

        return ikkeGodkjentTidslinje.isEmpty() ? ja() : nei();
    }
}
