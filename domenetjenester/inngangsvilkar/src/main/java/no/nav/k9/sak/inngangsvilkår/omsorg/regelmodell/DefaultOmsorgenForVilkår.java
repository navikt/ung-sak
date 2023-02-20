package no.nav.k9.sak.inngangsvilkår.omsorg.regelmodell;

import jakarta.enterprise.context.ApplicationScoped;
import no.nav.fpsak.nare.Ruleset;
import no.nav.fpsak.nare.doc.RuleDocumentation;
import no.nav.fpsak.nare.evaluation.Evaluation;
import no.nav.fpsak.nare.specification.Specification;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.inngangsvilkår.IkkeOppfylt;
import no.nav.k9.sak.inngangsvilkår.Oppfylt;

@FagsakYtelseTypeRef
@ApplicationScoped
@RuleDocumentation(value = DefaultOmsorgenForVilkår.ID, specificationReference = "")
public class DefaultOmsorgenForVilkår implements OmsorgenForVilkår {

    public static final String ID = "PSB_VK 9.10";

    @Override
    public Evaluation evaluer(OmsorgenForVilkårGrunnlag grunnlag) {
        return getSpecification().evaluate(grunnlag);
    }

    @Override
    public Evaluation evaluer(OmsorgenForVilkårGrunnlag input, Object outputContainer) {
        // Ignorerer output container
        return evaluer(input);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Specification<OmsorgenForVilkårGrunnlag> getSpecification() {
        Ruleset<OmsorgenForVilkårGrunnlag> rs = new Ruleset<>();
        return rs.hvisRegel(HarSøkerOmsorgenForPleietrengende.ID, "Har søker omsorgen for brukeren.")
            .hvis(new HarSøkerOmsorgenForPleietrengende(), new Oppfylt())
            .ellers(new IkkeOppfylt(OmsorgenForAvslagsårsaker.IKKE_DOKUMENTERT_OMSORGEN_FOR.toRuleReason()));
    }

    @Override
    public boolean skalHaAksjonspunkt(LocalDateTimeline<OmsorgenForVilkårGrunnlag> samletOmsorgenForTidslinje, boolean medAlleGamleVurderingerPåNytt) {
        for (LocalDateSegment<OmsorgenForVilkårGrunnlag> s : samletOmsorgenForTidslinje.toSegments()) {
            final OmsorgenForVilkårGrunnlag grunnlag = s.getValue();
            if ((grunnlag.getHarBlittVurdertSomOmsorgsPerson() == null || medAlleGamleVurderingerPåNytt) && (
                grunnlag.getRelasjonMellomSøkerOgPleietrengende() == null
                    || grunnlag.getRelasjonMellomSøkerOgPleietrengende().getRelasjonsRolle() == null
                    || grunnlag.getRelasjonMellomSøkerOgPleietrengende().getRelasjonsRolle() != RelasjonsRolle.BARN)) {
                return true;
            }
        }
        return false;
    }
}
