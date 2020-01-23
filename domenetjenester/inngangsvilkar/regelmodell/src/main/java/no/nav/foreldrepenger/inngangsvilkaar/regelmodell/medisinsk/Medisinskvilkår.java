package no.nav.foreldrepenger.inngangsvilkaar.regelmodell.medisinsk;

import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.IkkeOppfylt;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.Oppfylt;
import no.nav.fpsak.nare.RuleService;
import no.nav.fpsak.nare.Ruleset;
import no.nav.fpsak.nare.doc.RuleDocumentation;
import no.nav.fpsak.nare.evaluation.Evaluation;
import no.nav.fpsak.nare.specification.Specification;


/**
 * Denne implementerer regeltjenesten som validerer medlemskapsvilkåret (FP_VK_2)
 * <p>
 * Data underlag definisjoner:<br>
 * <p>
 * VilkårUtfall IKKE_OPPFYLT:<br>
 * - Bruker har ikke lovlig opphold<br>
 * - Bruker har ikke oppholdsrett<br>
 * - Bruker er utvandret<br>
 * - Bruker er avklart som ikke bosatt<br>
 * - Bruker er registrert som ikke medlem<br>
 * <p>
 * VilkårUtfall OPPFYLT:<br>
 * - Bruker er avklart som EU/EØS statsborger og har avklart oppholdsrett<br>
 * - Bruker har lovlig opphold<br>
 * - Bruker er nordisk statsborger<br>
 * - Bruker er pliktig eller frivillig medlem<br>
 */

@RuleDocumentation(value = Medisinskvilkår.ID, specificationReference = "https://confluence.adeo.no/pages/viewpage.action?pageId=173827808")
public class Medisinskvilkår implements RuleService<MedisinskvilkårGrunnlag> {

    public static final String ID = "FP_VK_2";

    @Override
    public Evaluation evaluer(MedisinskvilkårGrunnlag grunnlag) {
        return getSpecification().evaluate(grunnlag);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Specification<MedisinskvilkårGrunnlag> getSpecification() {
        Ruleset<MedisinskvilkårGrunnlag> rs = new Ruleset<>();

        return rs.hvisRegel(ErBehovForPleie.ID, "Er behov for pleie av pleietrengende")
            .hvis(new ErBehovForPleie(), new Oppfylt())
            .ellers(new IkkeOppfylt(MedisinkevilkårAvslagsårsaker.IKKE_BEHOV_FOR_PLEIE.toRuleReason()));
    }
}
