package no.nav.k9.sak.ytelse.opplaeringspenger.inngangsvilkår.nødvendighet.regelmodell;

import java.util.Objects;

import no.nav.fpsak.nare.doc.RuleDocumentation;
import no.nav.fpsak.nare.evaluation.Evaluation;
import no.nav.fpsak.nare.specification.LeafSpecification;

@RuleDocumentation(ErGodkjentInstitusjon.ID)
public class ErGodkjentInstitusjon extends LeafSpecification<NødvendighetMellomregningData> {

    public static final String ID = "OLP_VK_9.14.1";

    public ErGodkjentInstitusjon() {
        super(ID);
    }

    @Override
    public Evaluation evaluate(NødvendighetMellomregningData mellomregningData) {
        var godkjentTidslinje = mellomregningData.getInstitusjonVurderingTidslinje()
            .filterValue(vurdering -> Objects.equals(vurdering, InstitusjonVurdering.GODKJENT))
            .intersection(mellomregningData.getTidslinjeTilVurdering());
        var ikkeGodkjentTidslinje = mellomregningData.getTidslinjeTilVurdering()
            .disjoint(godkjentTidslinje);

        if (godkjentTidslinje.isEmpty() || ikkeGodkjentTidslinje.isEmpty()) {
            return ikkeGodkjentTidslinje.isEmpty() ? ja() : nei();
        } else {
            throw new IllegalArgumentException("Utviklerfeil: periodene skal være splittet før de sendes hit.");
        }
    }
}
