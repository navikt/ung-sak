package no.nav.k9.sak.ytelse.omsorgspenger.utvidetrett.prosess.steg;

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
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;

@FagsakYtelseTypeRef("OMP_KS")
@FagsakYtelseTypeRef("OMP_MA")
@BehandlingStegRef(kode = "MANUELL_VILKÅRSVURDERING")
@BehandlingTypeRef
@ApplicationScoped
public class UtvidetRettManuellVilkårsvurderingSteg implements BehandlingSteg {

    @SuppressWarnings("unused")
    private BehandlingRepository behandlingRepository;
    private VilkårResultatRepository vilkårResultatRepository;

    public UtvidetRettManuellVilkårsvurderingSteg() {
        // CDO
    }

    @Inject
    public UtvidetRettManuellVilkårsvurderingSteg(BehandlingRepository behandlingRepository,
                                                  VilkårResultatRepository vilkårResultatRepository) {
        this.behandlingRepository = behandlingRepository;
        this.vilkårResultatRepository = vilkårResultatRepository;
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        Long behandlingId = kontekst.getBehandlingId();
        var vilkårene = vilkårResultatRepository.hent(behandlingId);
        var vilkår = vilkårene.getVilkår(VilkårType.UTVIDETRETT);
        if (vilkår.get().getPerioder().stream().anyMatch(v -> v.getUtfall() == Utfall.IKKE_VURDERT)) {
            return BehandleStegResultat.utførtMedAksjonspunkter(List.of(AksjonspunktDefinisjon.VURDER_OMS_UTVIDET_RETT));
        } else {
            return BehandleStegResultat.utførtUtenAksjonspunkter();
        }

    }

}
