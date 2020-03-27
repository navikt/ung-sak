package no.nav.k9.sak.ytelse.omsorgspenger.årskvantum;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.BehandleStegResultat;
import no.nav.k9.sak.behandlingskontroll.BehandlingSteg;
import no.nav.k9.sak.behandlingskontroll.BehandlingStegRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.skjæringstidspunkt.SkjæringstidspunktTjeneste;

@ApplicationScoped
@BehandlingStegRef(kode = "VURDER_UTTAK")
@BehandlingTypeRef
@FagsakYtelseTypeRef("OMP")
public class VurderÅrskvantumUttakSteg implements BehandlingSteg {

    private BehandlingRepository behandlingRepository;
    private SkjæringstidspunktTjeneste stpTjeneste;
    
    VurderÅrskvantumUttakSteg(){
        // for proxy
    }

    @Inject
    public VurderÅrskvantumUttakSteg(BehandlingRepository behandlingRepository, 
                           SkjæringstidspunktTjeneste stpTjeneste){
        this.behandlingRepository = behandlingRepository;
        this.stpTjeneste = stpTjeneste;
    }
    
    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        var behandlingId = kontekst.getBehandlingId();
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        var stp = stpTjeneste.getSkjæringstidspunkter(behandlingId);
        var ref = BehandlingReferanse.fra(behandling, stp);
        
        // K9 TODO: 
        // 1. kalle årskvantum for å få vurdet fraværet
        //    a. hvis Ok gå videre uten aksjonspunkter
        //    b. hvis Ikke Ok opprett aksjonspunkt som må løses i dette steget (fosterforeldre, delt bosted, etc.)
        // 2. Lag REST tjeneste for GUI - vise hvor mye brukt (basert på samme tjeneste som kalles her
        // 3. Lag AksjonspunktOppdaterer for å skrive ned oppdatert kvantum til Årskvantum og la steget kjøre på nytt.
        return BehandleStegResultat.utførtUtenAksjonspunkter();
    }

}
