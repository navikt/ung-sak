package no.nav.k9.sak.domene.behandling.steg.uttak;

import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.revurdering.ytelse.UttakInputTjeneste;
import no.nav.foreldrepenger.behandlingskontroll.BehandleStegResultat;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingSteg;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingTypeRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.domene.uttak.UttakTjeneste;

@BehandlingStegRef(kode = "VURDER_UTTAK")
@BehandlingTypeRef
@FagsakYtelseTypeRef
public class VurderUttakSteg implements BehandlingSteg {

    private UttakTjeneste uttakTjeneste;
    private BehandlingRepository behandlingRepository;
    private UttakInputTjeneste uttakInputTjeneste;
    
    VurderUttakSteg(){
        // for proxy
    }

    @Inject
    public VurderUttakSteg(BehandlingRepository behandlingRepository, UttakTjeneste uttakTjeneste, UttakInputTjeneste uttakInputTjeneste){
        this.behandlingRepository = behandlingRepository;
        this.uttakTjeneste = uttakTjeneste;
        this.uttakInputTjeneste = uttakInputTjeneste;
    }
    
    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        var behandling = behandlingRepository.hentBehandling(kontekst.getBehandlingId());
        var ref = BehandlingReferanse.fra(behandling);
        var uttakInput = uttakInputTjeneste.lagInput(ref);
        uttakTjeneste.opprettUttaksplan(uttakInput);
        
        return BehandleStegResultat.utførtUtenAksjonspunkter();
    }

}
