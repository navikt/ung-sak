package no.nav.ung.ytelse.aktivitetspenger.medlemskap;

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

import static no.nav.ung.kodeverk.behandling.BehandlingStegType.VURDER_FORUTGÅENDE_MEDLEMSKAPVILKÅR;

@ApplicationScoped
@BehandlingStegRef(value = VURDER_FORUTGÅENDE_MEDLEMSKAPVILKÅR)
@BehandlingTypeRef
@FagsakYtelseTypeRef(FagsakYtelseType.AKTIVITETSPENGER)
public class ForutgåendeMedlemskapsvilkårSteg implements BehandlingSteg {

    private BehandlingRepository behandlingRepository;
    private VilkårResultatRepository vilkårResultatRepository;


    @Inject
    public ForutgåendeMedlemskapsvilkårSteg(BehandlingRepository behandlingRepository,
                                            VilkårResultatRepository vilkårResultatRepository) {
        this.behandlingRepository = behandlingRepository;
        this.vilkårResultatRepository = vilkårResultatRepository;
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        var vilkårene = vilkårResultatRepository.hent(kontekst.getBehandlingId());

        var medlemskapsvilkår = vilkårene.getVilkår(VilkårType.FORUTGÅENDE_MEDLEMSKAPSVILKÅRET);
        if (medlemskapsvilkår.map(vilkår -> vilkår.getPerioder().stream().anyMatch(periode -> Utfall.IKKE_VURDERT.equals(periode.getUtfall()))).orElse(true)) {
            return BehandleStegResultat.utførtMedAksjonspunkter(List.of(AksjonspunktDefinisjon.AVKLAR_GYLDIG_MEDLEMSKAPSPERIODE));
        }

        return BehandleStegResultat.utførtUtenAksjonspunkter();
    }
}
