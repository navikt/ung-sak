package no.nav.k9.sak.ytelse.pleiepengerbarn.uttak.død;

import java.util.Set;
import java.util.TreeSet;

import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandlingslager.behandling.personopplysning.PersonopplysningEntitet;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatBuilder;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.Vilkårene;
import no.nav.k9.sak.domene.behandling.steg.inngangsvilkår.alder.VurderAldersVilkårTjeneste;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;

class VilkårForlengingTjeneste {

    private final VurderAldersVilkårTjeneste vurderAldersVilkårTjeneste = new VurderAldersVilkårTjeneste();

    public void forlengOgVurderAldersvilkåret(VilkårResultatBuilder resultatBuilder, DatoIntervallEntitet periode, PersonopplysningEntitet brukerPersonopplysninger) {
        var aldersvilkår = VilkårType.ALDERSVILKÅR;
        var vilkårBuilder = resultatBuilder.hentBuilderFor(aldersvilkår);
        var fødselsdato = brukerPersonopplysninger.getFødselsdato();

        var set = new TreeSet<DatoIntervallEntitet>();
        set.add(periode);
        vurderAldersVilkårTjeneste.vurderPerioder(vilkårBuilder, set, fødselsdato);
    }

    public void forlengeVilkårMedPeriode(Set<VilkårType> vilkår, VilkårResultatBuilder resultatBuilder, Vilkårene vilkårene, DatoIntervallEntitet periode) {
        for (VilkårType vilkårType : vilkår) {
            var vilkårBuilder = resultatBuilder.hentBuilderFor(vilkårType);
            var dødsdato = periode.getFomDato().minusDays(1); //periode begynner 1 dag etter dødsdato
            var eksisterendeResultat = vilkårene.getVilkår(vilkårType).orElseThrow().finnPeriodeSomInneholderDato(dødsdato).orElseThrow();

            vilkårBuilder.leggTil(vilkårBuilder.hentBuilderFor(periode).forlengelseAv(eksisterendeResultat));
            resultatBuilder.leggTil(vilkårBuilder);
        }
    }
}


