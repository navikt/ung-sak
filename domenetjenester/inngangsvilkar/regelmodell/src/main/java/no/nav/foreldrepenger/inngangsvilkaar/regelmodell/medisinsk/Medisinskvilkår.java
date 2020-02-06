package no.nav.foreldrepenger.inngangsvilkaar.regelmodell.medisinsk;

import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.IkkeOppfylt;
import no.nav.fpsak.nare.RuleService;
import no.nav.fpsak.nare.Ruleset;
import no.nav.fpsak.nare.doc.RuleDocumentation;
import no.nav.fpsak.nare.evaluation.Evaluation;
import no.nav.fpsak.nare.specification.Specification;

@RuleDocumentation(value = Medisinskvilkår.ID, specificationReference = "https://confluence.adeo.no/pages/viewpage.action?pageId=173827808")
public class Medisinskvilkår implements RuleService<MedisinskvilkårGrunnlag> {

    public static final String ID = "PSB_VK 9.10";

    @Override
    public Evaluation evaluer(MedisinskvilkårGrunnlag grunnlag, Object resultatStruktur) {
        final var mellomregningData = new MedisinskMellomregningData(grunnlag);

        final var evaluate = getSpecification().evaluate(mellomregningData);

        mellomregningData.oppdaterResultat((MedisinskVilkårResultat) resultatStruktur);

        return evaluate;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Specification<MedisinskMellomregningData> getSpecification() {
        Ruleset<MedisinskMellomregningData> rs = new Ruleset<>();
        return rs.hvisRegel(HarBarnetEnSykdomSkadeEllerLyteDokumenterFraRettOrgan.ID, "Har barnet en sykdom, skade eller lyte dokumentert fra rett organ?")
            .hvis(new HarBarnetEnSykdomSkadeEllerLyteDokumenterFraRettOrgan(), rs.hvisRegel(BeregnBehovForTilsynOgPleieOgAntallTilsynsPersoner.ID, "Beregn behov for kontinuerlig tilsyn og pleie. Og avklar antall pleiepersoner.")
                .hvis(new BeregnBehovForTilsynOgPleieOgAntallTilsynsPersoner(), new HarBarnetBehovForKontinuerligTilsynOgPleie())
                .ellers(new IkkeOppfylt(MedisinskeVilkårAvslagsårsaker.IKKE_BEHOV_FOR_KONTINUERLIG_PLEIE.toRuleReason())))
            .ellers(new IkkeOppfylt(MedisinskeVilkårAvslagsårsaker.IKKE_DOKUMENTERT_SYKDOM_SKADE_ELLER_LYTE.toRuleReason()));
    }
}
