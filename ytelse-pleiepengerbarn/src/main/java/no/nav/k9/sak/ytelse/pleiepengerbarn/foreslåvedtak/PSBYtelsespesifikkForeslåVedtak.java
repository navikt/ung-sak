package no.nav.k9.sak.ytelse.pleiepengerbarn.foreslåvedtak;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.k9.kodeverk.behandling.BehandlingStegType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandling.prosessering.BehandlingProsesseringTjeneste;
import no.nav.k9.sak.behandlingskontroll.BehandleStegResultat;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.domene.behandling.steg.foreslåvedtak.YtelsespesifikkForeslåVedtak;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.SamtidigUttakTjeneste;

@ApplicationScoped
@FagsakYtelseTypeRef("PSB")
public class PSBYtelsespesifikkForeslåVedtak implements YtelsespesifikkForeslåVedtak {
    
    private SamtidigUttakTjeneste samtidigUttakTjeneste;
    private BehandlingProsesseringTjeneste behandlingProsesseringTjeneste;
    private BehandlingRepository behandlingRepository;
    
    @Inject
    public PSBYtelsespesifikkForeslåVedtak(SamtidigUttakTjeneste samtidigUttakTjeneste, BehandlingProsesseringTjeneste behandlingProsesseringTjeneste, BehandlingRepository behandlingRepository) {
        this.samtidigUttakTjeneste = samtidigUttakTjeneste;
        this.behandlingProsesseringTjeneste = behandlingProsesseringTjeneste;
        this.behandlingRepository = behandlingRepository;
    }
    
    @Override
    public BehandleStegResultat run(BehandlingReferanse ref) {
        if (samtidigUttakTjeneste.isSkalHaTilbakehopp(ref)) {
            var behandling = behandlingRepository.hentBehandling(ref.getBehandlingId());
            behandlingProsesseringTjeneste.opprettTasksForFortsettBehandling(behandling);
            return BehandleStegResultat.tilbakeførtTilStegUtenVidereKjøring(BehandlingStegType.VURDER_UTTAK);
        }
        
        return null;
    }

}
