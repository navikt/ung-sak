package no.nav.k9.sak.ytelse.omsorgspenger.utvidetrett.aldersvilkår.regelmodell;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import no.nav.fpsak.nare.doc.RuleDocumentation;
import no.nav.fpsak.nare.evaluation.Evaluation;
import no.nav.fpsak.nare.specification.LeafSpecification;

@RuleDocumentation(HarMinstEttBarnRiktiAlder.ID)
public class HarMinstEttBarnRiktiAlder extends LeafSpecification<AldersvilkårBarnVilkårGrunnlag> {

    static final String ID = "OMP_VK_9.5.3";

    HarMinstEttBarnRiktiAlder() {
        super(ID);
    }

    @Override
    public Evaluation evaluate(AldersvilkårBarnVilkårGrunnlag grunnlag) {
        int maksAlder = switch (grunnlag.getFagsakYtelseType()) {
            case OMSORGSPENGER_AO -> 12;
            case OMSORGSPENGER_KS -> 18;
            case OMSORGSPENGER_MA -> 12;
            default -> throw new IllegalArgumentException("Ugyldig fagsaktype: " + grunnlag.getFagsakYtelseType());
        };
        LocalDate periodensStart = grunnlag.getVilkårsperiode().getFomDato();
        for (LocalDate fødselsdato : grunnlag.getFødselsdatoBarn()) {
            boolean barnetVarFødtPåFørsteSøknadsdato = !fødselsdato.isAfter(periodensStart);
            boolean barnetErUngtNok = ChronoUnit.YEARS.between(fødselsdato, periodensStart.withMonth(12).withDayOfMonth(31)) <= maksAlder;
            boolean barnetHarRiktigAlder = barnetVarFødtPåFørsteSøknadsdato && barnetErUngtNok;
            if (barnetHarRiktigAlder) {
                return ja();
            }
        }
        return kanIkkeVurdere(AldersvilkårBarnKanIkkeVurdereAutomatiskÅrsaker.KAN_IKKE_AUTOMATISK_INNVILGE_ALDERSVILKÅR_BARN.toRuleReason());
    }

}
