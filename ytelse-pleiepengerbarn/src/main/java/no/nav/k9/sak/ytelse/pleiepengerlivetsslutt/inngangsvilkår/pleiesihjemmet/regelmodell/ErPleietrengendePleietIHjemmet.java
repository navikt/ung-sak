package no.nav.k9.sak.ytelse.pleiepengerlivetsslutt.inngangsvilkår.pleiesihjemmet.regelmodell;

import no.nav.fpsak.nare.doc.RuleDocumentation;
import no.nav.fpsak.nare.evaluation.Evaluation;
import no.nav.fpsak.nare.specification.LeafSpecification;

@RuleDocumentation(ErPleietrengendePleietIHjemmet.ID)
public class ErPleietrengendePleietIHjemmet extends LeafSpecification<PleiesIHjemmetVilkårGrunnlag> {

    public static final String ID = "PLS_VK_9.13";

    public ErPleietrengendePleietIHjemmet() {
        super(ID);
    }

    @Override
    public Evaluation evaluate(PleiesIHjemmetVilkårGrunnlag grunnlag) {
        // "Pleies i hjemmet" vurderes automatisk som oppfylt ved at bruker har svart Ja på at pleietrengende pleies i hjemmet
        //
        // Dersom bruker i ettertid oppgir at pleietrengende likevel ikke ble pleiet i hjemmet, så korrigeres det gjennom
        // trekk av perioder fra Punsj - men ettersom trekk av perioder kan skyldes flere årsaker, så endres ikke vilkårsutfallet
        return ja();
    }
}
