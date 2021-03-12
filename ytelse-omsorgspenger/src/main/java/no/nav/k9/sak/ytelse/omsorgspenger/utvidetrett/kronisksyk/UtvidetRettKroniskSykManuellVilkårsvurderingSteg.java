package no.nav.k9.sak.ytelse.omsorgspenger.utvidetrett.kronisksyk;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.k9.kodeverk.vilkår.Utfall;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandlingskontroll.BehandleStegResultat;
import no.nav.k9.sak.behandlingskontroll.BehandlingSteg;
import no.nav.k9.sak.behandlingskontroll.BehandlingStegRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;

@FagsakYtelseTypeRef("OMP_KS")
@BehandlingStegRef(kode = "MANUELL_VILKÅRSVURDERING")
@BehandlingTypeRef
@ApplicationScoped
public class UtvidetRettKroniskSykManuellVilkårsvurderingSteg implements BehandlingSteg {

    private VilkårResultatRepository vilkårResultatRepository;

    private AksjonspunktDefinisjon aksjonspunktDef = AksjonspunktDefinisjon.VURDER_OMS_UTVIDET_RETT;

    public UtvidetRettKroniskSykManuellVilkårsvurderingSteg() {
        // CDO
    }

    @Inject
    public UtvidetRettKroniskSykManuellVilkårsvurderingSteg(VilkårResultatRepository vilkårResultatRepository) {
        this.vilkårResultatRepository = vilkårResultatRepository;
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        Long behandlingId = kontekst.getBehandlingId();
        var vilkårene = vilkårResultatRepository.hent(behandlingId);
        var vilkår = vilkårene.getVilkår(VilkårType.UTVIDETRETT);

        if (vilkår.get().getPerioder().stream().anyMatch(v -> v.getUtfall() == Utfall.IKKE_VURDERT)) {
            return BehandleStegResultat.utførtMedAksjonspunkter(List.of(aksjonspunktDef));
        } else {
            return BehandleStegResultat.utførtUtenAksjonspunkter();
        }

    }

}
