package no.nav.ung.ytelse.aktivitetspenger.del1.steg.søknadsfrist;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.kodeverk.vilkår.Utfall;
import no.nav.ung.kodeverk.vilkår.VilkårType;
import no.nav.ung.sak.behandlingskontroll.BehandleStegResultat;
import no.nav.ung.sak.behandlingskontroll.BehandlingSteg;
import no.nav.ung.sak.behandlingskontroll.BehandlingStegRef;
import no.nav.ung.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.ung.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.ung.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;

import static no.nav.ung.kodeverk.behandling.BehandlingStegType.VURDER_SØKNADSFRIST;

@ApplicationScoped
@BehandlingStegRef(value = VURDER_SØKNADSFRIST)
@BehandlingTypeRef
@FagsakYtelseTypeRef(FagsakYtelseType.AKTIVITETSPENGER)
public class VurderSøknadsfristSteg implements BehandlingSteg {

    private VilkårResultatRepository vilkårResultatRepository;

    VurderSøknadsfristSteg() {
        // for CDI proxy
    }

    @Inject
    public VurderSøknadsfristSteg(VilkårResultatRepository vilkårResultatRepository) {
        this.vilkårResultatRepository = vilkårResultatRepository;
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        Long behandlingId = kontekst.getBehandlingId();
        vilkårResultatRepository.settUtfallForAllePerioder(behandlingId, VilkårType.SØKNADSFRIST, Utfall.OPPFYLT);
        return BehandleStegResultat.utførtUtenAksjonspunkter();
    }
}
