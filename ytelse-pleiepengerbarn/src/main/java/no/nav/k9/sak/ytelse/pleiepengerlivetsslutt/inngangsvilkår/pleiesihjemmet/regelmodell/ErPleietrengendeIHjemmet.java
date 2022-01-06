package no.nav.k9.sak.ytelse.pleiepengerlivetsslutt.inngangsvilkår.pleiesihjemmet.regelmodell;

import no.nav.fpsak.nare.doc.RuleDocumentation;
import no.nav.fpsak.nare.evaluation.Evaluation;
import no.nav.fpsak.nare.specification.LeafSpecification;

@RuleDocumentation(ErPleietrengendeIHjemmet.ID)
public class ErPleietrengendeIHjemmet extends LeafSpecification<PleiesHjemmeMellomregningData> {

    static final String ID = "PLS_VK_9.13.2";

    ErPleietrengendeIHjemmet() {
        super(ID);
    }

    @Override
    public Evaluation evaluate(PleiesHjemmeMellomregningData mellomregning) {
        // "Pleies hjemme" vurderes automatisk som oppfylt ved at bruker har svart "Ja" i søknadsdialog på at
        //    pleietrengende pleies hjemme - ellers får ikke bruker lov til å sende søknad i Søknadsdialog
        // Men dersom innleggelse registreres, vil dette trekkes fra periodene hvor pleietrengende pleies hjemme

        final var ikkePleiesHjemme = mellomregning.getBeregnedePerioderMedPleielokasjon()
            .stream()
            .allMatch(p -> Pleielokasjon.INNLAGT.equals(p.getPleielokasjon()));

        if (ikkePleiesHjemme) {
            return nei(PleiesHjemmeVilkårAvslagsårsaker.PLEIETRENGENDE_INNLAGT_I_STEDET_FOR_HJEMME.toRuleReason());
        }

        return ja();
    }
}
