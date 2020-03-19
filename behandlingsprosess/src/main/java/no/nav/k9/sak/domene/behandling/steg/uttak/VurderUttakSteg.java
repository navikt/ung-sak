package no.nav.k9.sak.domene.behandling.steg.uttak;

import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingskontroll.BehandleStegResultat;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingSteg;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingTypeRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandling.revurdering.ytelse.UttakInputTjeneste;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.domene.uttak.UttakTjeneste;
import no.nav.k9.sak.skjæringstidspunkt.SkjæringstidspunktTjeneste;

@BehandlingStegRef(kode = "VURDER_UTTAK")
@BehandlingTypeRef
@FagsakYtelseTypeRef
public class VurderUttakSteg implements BehandlingSteg {

    private UttakTjeneste uttakTjeneste;
    private BehandlingRepository behandlingRepository;
    private UttakInputTjeneste uttakInputTjeneste;
    private SkjæringstidspunktTjeneste stpTjeneste;
    
    VurderUttakSteg(){
        // for proxy
    }

    @Inject
    public VurderUttakSteg(BehandlingRepository behandlingRepository, 
                           SkjæringstidspunktTjeneste stpTjeneste,
                           UttakTjeneste uttakTjeneste, 
                           UttakInputTjeneste uttakInputTjeneste){
        this.behandlingRepository = behandlingRepository;
        this.stpTjeneste = stpTjeneste;
        this.uttakTjeneste = uttakTjeneste;
        this.uttakInputTjeneste = uttakInputTjeneste;
    }
    
    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        var behandlingId = kontekst.getBehandlingId();
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        var stp = stpTjeneste.getSkjæringstidspunkter(behandlingId);
        var ref = BehandlingReferanse.fra(behandling, stp);
        var uttakInput = uttakInputTjeneste.lagInput(ref);
        uttakTjeneste.opprettUttaksplan(uttakInput);
        
        return BehandleStegResultat.utførtUtenAksjonspunkter();
    }

}
