package no.nav.k9.sak.ytelse.omsorgspenger.utvidetrett.prosess.steg;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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
import no.nav.k9.sak.behandlingslager.behandling.vilkår.Vilkår;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;

@FagsakYtelseTypeRef("OMP_KS")
@FagsakYtelseTypeRef("OMP_MA")
@BehandlingStegRef(kode = "KOFAK")
@BehandlingTypeRef()
@ApplicationScoped
public class UtvidetRettKontrollerFaktaSteg implements BehandlingSteg {

    private VilkårResultatRepository vilkårResultatRepository;

    // Dummy-steg - for å støtte tilbakehopp ved registeroppdateringer

    public UtvidetRettKontrollerFaktaSteg() {
    }

    @Inject
    public UtvidetRettKontrollerFaktaSteg(VilkårResultatRepository vilkårResultatRepository) {
        this.vilkårResultatRepository = vilkårResultatRepository;
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        List<AksjonspunktDefinisjon> aksjonspunkter = new ArrayList<>();

        Long behandlingId = kontekst.getBehandlingId();
        var vilkårene = vilkårResultatRepository.hent(behandlingId);

        // TODO: bør mulig sjekke bosted å barna før dette blir lagt på?
        måVurdereVilkår(vilkårene.getVilkår(VilkårType.OMSORGEN_FOR).get()).ifPresent(a -> aksjonspunkter.add(AksjonspunktDefinisjon.VURDER_OMSORGEN_FOR));
        
        måVurdereVilkår(vilkårene.getVilkår(VilkårType.UTVIDETRETT).get()).ifPresent(a -> aksjonspunkter.add(AksjonspunktDefinisjon.VURDER_OMS_UTVIDET_RETT));

        return BehandleStegResultat.utførtMedAksjonspunkter(aksjonspunkter);
    }

    private Optional<Vilkår> måVurdereVilkår(Vilkår vilkår) {
        if (vilkår.getPerioder().stream().anyMatch(v -> v.getUtfall() == Utfall.IKKE_VURDERT)) {
            return Optional.of(vilkår);
        } else {
            return Optional.empty();
        }
    }

}
