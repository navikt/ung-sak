package no.nav.k9.sak.ytelse.omsorgspenger.inngangsvilkår.omsorgenfor.regelmodell;

import jakarta.enterprise.context.ApplicationScoped;
import no.nav.fpsak.nare.Ruleset;
import no.nav.fpsak.nare.doc.RuleDocumentation;
import no.nav.fpsak.nare.evaluation.Evaluation;
import no.nav.fpsak.nare.specification.Specification;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.inngangsvilkår.IkkeOppfylt;
import no.nav.k9.sak.inngangsvilkår.Oppfylt;
import no.nav.k9.sak.inngangsvilkår.omsorg.regelmodell.OmsorgenForAvslagsårsaker;
import no.nav.k9.sak.inngangsvilkår.omsorg.regelmodell.OmsorgenForKnekkpunkter;
import no.nav.k9.sak.inngangsvilkår.omsorg.regelmodell.OmsorgenForVilkår;
import no.nav.k9.sak.inngangsvilkår.omsorg.regelmodell.OmsorgenForVilkårGrunnlag;

@FagsakYtelseTypeRef(FagsakYtelseType.OMSORGSPENGER)
@ApplicationScoped
@RuleDocumentation(value = OMPOmsorgenForVilkår.ID, specificationReference = "")
public class OMPOmsorgenForVilkår implements OmsorgenForVilkår {

    public static final String ID = "OMP_VK 9.5";

    @Override
    public Evaluation evaluer(OmsorgenForVilkårGrunnlag grunnlag) {
        throw new IllegalStateException();
    }

    @Override
    public Evaluation evaluer(OmsorgenForVilkårGrunnlag input, Object outputContainer) {
        var omsorgenForKnekkpunkter = (OmsorgenForKnekkpunkter) outputContainer;
        //TODO periodiser regelen
        var evaluate = getSpecification().evaluate(input);
        input.oppdaterKnekkpunkter(omsorgenForKnekkpunkter);

        return evaluate;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Specification<OmsorgenForVilkårGrunnlag> getSpecification() {
        Ruleset<OmsorgenForVilkårGrunnlag> rs = new Ruleset<>();
        return rs.hvisRegel(HarSøkerOmsorgenForBarn.ID, "Har søker omsorgen for et barn.")
            .hvis(new HarSøkerOmsorgenForBarn(), new Oppfylt())
            .ellers(new IkkeOppfylt(OmsorgenForAvslagsårsaker.IKKE_DOKUMENTERT_OMSORGEN_FOR.toRuleReason()));
    }

    @Override
    public boolean skalHaAksjonspunkt(LocalDateTimeline<OmsorgenForVilkårGrunnlag> samletOmsorgenForTidslinje, boolean medAlleGamleVurderingerPåNytt) {
        return false;
    }
}
