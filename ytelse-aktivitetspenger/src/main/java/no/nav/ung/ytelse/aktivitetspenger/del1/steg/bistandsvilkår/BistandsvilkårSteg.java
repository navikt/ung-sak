package no.nav.ung.ytelse.aktivitetspenger.del1.steg.bistandsvilkår;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.ung.kodeverk.vilkår.Utfall;
import no.nav.ung.kodeverk.vilkår.VilkårType;
import no.nav.ung.sak.behandlingskontroll.*;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;

import java.util.List;

import static no.nav.ung.kodeverk.behandling.BehandlingStegType.VURDER_BISTANDSVILKÅR;

@ApplicationScoped
@BehandlingStegRef(value = VURDER_BISTANDSVILKÅR)
@BehandlingTypeRef
@FagsakYtelseTypeRef(FagsakYtelseType.AKTIVITETSPENGER)
public class BistandsvilkårSteg implements BehandlingSteg {

    private BehandlingRepository behandlingRepository;
    private VilkårResultatRepository vilkårResultatRepository;


    @Inject
    public BistandsvilkårSteg(BehandlingRepository behandlingRepository,
                              VilkårResultatRepository vilkårResultatRepository) {
        this.behandlingRepository = behandlingRepository;
        this.vilkårResultatRepository = vilkårResultatRepository;
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        var vilkårene = vilkårResultatRepository.hent(kontekst.getBehandlingId());

        var bistandsvilkår = vilkårene.getVilkår(VilkårType.BISTANDSVILKÅR);
        if (bistandsvilkår.map(vilkår -> vilkår.getPerioder().stream().anyMatch(periode -> Utfall.IKKE_VURDERT.equals(periode.getUtfall()))).orElse(true)) {
            return BehandleStegResultat.utførtMedAksjonspunkter(List.of(AksjonspunktDefinisjon.VURDER_BISTANDSVILKÅR));
        }

        return BehandleStegResultat.utførtUtenAksjonspunkter();
    }
}
