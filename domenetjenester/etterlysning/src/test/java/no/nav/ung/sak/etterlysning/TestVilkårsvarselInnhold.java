package no.nav.ung.sak.etterlysning;

import no.nav.ung.kodeverk.vilkår.Avklaringtype;
import no.nav.ung.kodeverk.vilkår.IkkeOppfyltDetaljertÅrsak;
import no.nav.ung.kodeverk.vilkår.VilkårType;
import no.nav.ung.sak.behandlingslager.vilkårsavklaring.VilkårPeriodeAvklaring;
import no.nav.ung.sak.typer.Periode;

import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Testhjelper som pakker en {@link VilkårPeriodeAvklaring} inn i en {@link VilkårsvarselInnhold} for bruk mot
 * {@link VilkårsavklaringEtterlysningTjeneste#oppdaterEtterlysninger}. I produksjonskode gjøres denne mappingen
 * av vilkårsspesifikke innholdstyper (f.eks. {@code BistandAvklaringInnhold}); her trengs kun en generisk variant.
 */
record TestVilkårsvarselInnhold(Periode periode,
                                IkkeOppfyltDetaljertÅrsak ikkeOppfyltÅrsak,
                                boolean skalSendeVarsel,
                                String fritekstTilVarsel,
                                String kildeKode,
                                String kildeFritekst,
                                Avklaringtype avklaringtype) implements VilkårsvarselInnhold {

    static TestVilkårsvarselInnhold fra(VilkårType vilkårType, VilkårPeriodeAvklaring avklaring) {
        return new TestVilkårsvarselInnhold(
            avklaring.getPeriode().tilPeriode(),
            IkkeOppfyltDetaljertÅrsak.fraKode(vilkårType, avklaring.getIkkeOppfyltÅrsakKode()),
            avklaring.skalSendeVarsel(),
            avklaring.getFritekstTilVarsel(),
            avklaring.getKildeKode(),
            avklaring.getKildeFritekst(),
            avklaring.getAvklaringtype()
        );
    }

    static Map<VilkårsvarselInnhold, UUID> tilMap(VilkårType vilkårType, VilkårPeriodeAvklaring... avklaringer) {
        return java.util.Arrays.stream(avklaringer)
            .collect(Collectors.toMap(avklaring -> fra(vilkårType, avklaring), VilkårPeriodeAvklaring::getReferanse));
    }
}
