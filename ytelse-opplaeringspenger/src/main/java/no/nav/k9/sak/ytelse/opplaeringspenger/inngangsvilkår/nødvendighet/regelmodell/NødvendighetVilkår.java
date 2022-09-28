package no.nav.k9.sak.ytelse.opplaeringspenger.inngangsvilkår.nødvendighet.regelmodell;

import java.util.Objects;

import no.nav.fpsak.nare.RuleService;
import no.nav.fpsak.nare.Ruleset;
import no.nav.fpsak.nare.doc.RuleDocumentation;
import no.nav.fpsak.nare.evaluation.Evaluation;
import no.nav.fpsak.nare.specification.Specification;
import no.nav.k9.sak.inngangsvilkår.IkkeOppfylt;
import no.nav.k9.sak.inngangsvilkår.Oppfylt;

@RuleDocumentation(value = NødvendighetVilkår.ID, specificationReference = "")
public class NødvendighetVilkår implements RuleService<NødvendighetVilkårGrunnlag> {

    public static final String ID = "OLP_VK 9.14.1";

    @Override
    public Evaluation evaluer(NødvendighetVilkårGrunnlag grunnlag, Object resultatStruktur) {
        final NødvendighetMellomregningData mellomregningData = new NødvendighetMellomregningData(grunnlag);

        final Evaluation evaluate = getSpecification().evaluate(mellomregningData);

        oppdaterResultat((NødvendighetVilkårResultat) resultatStruktur, mellomregningData);

        return evaluate;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Specification<NødvendighetMellomregningData> getSpecification() {
        Ruleset<NødvendighetMellomregningData> rs = new Ruleset<>();
        return rs.hvisRegel(ErSykdomsvilkårOppfylt.ID, "Hvis sykdomsvilkåret er godkjent...")
            .hvis(new ErSykdomsvilkårOppfylt(),
                rs.hvisRegel(ErGodkjentInstitusjon.ID, "Hvis opplæringsinstitusjonen er godkjent...")
                    .hvis(new ErGodkjentInstitusjon(),
                        rs.hvisRegel(ErNødvendigOpplæring.ID, "Hvis opplæringen er vurdert som nødvendig...")
                            .hvis(new ErNødvendigOpplæring(), new Oppfylt())
                            .ellers(new IkkeOppfylt(NødvendighetVilkårAvslagsårsaker.IKKE_NØDVENDIG.toRuleReason())))
                    .ellers(new IkkeOppfylt(NødvendighetVilkårAvslagsårsaker.IKKE_GODKJENT_INSTITUSJON.toRuleReason())))
            .ellers(new IkkeOppfylt(NødvendighetVilkårAvslagsårsaker.IKKE_GODKJENT_SYKDOMSVILKÅR.toRuleReason()));
    }

    private void oppdaterResultat(NødvendighetVilkårResultat resultat, NødvendighetMellomregningData mellomregningData) {
        Objects.requireNonNull(resultat);
        resultat.setNødvendigOpplæringPerioder(mellomregningData.getOpplæringVurderingPerioder());
        resultat.setGodkjentInstitusjonPerioder(mellomregningData.getInstitusjonVurderingPerioder());
        resultat.setGodkjentSykdomPerioder(mellomregningData.getSykdomVurderingPerioder());
    }
}
