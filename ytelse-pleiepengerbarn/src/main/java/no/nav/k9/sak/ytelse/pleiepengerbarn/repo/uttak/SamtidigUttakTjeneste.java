package no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.k9.kodeverk.behandling.BehandlingStegType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.BehandlingModell;
import no.nav.k9.sak.behandlingskontroll.impl.BehandlingModellRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.ytelse.pleiepengerbarn.uttak.input.MapInputTilUttakTjeneste;
import no.nav.k9.sak.ytelse.pleiepengerbarn.uttak.tjeneste.UttakTjeneste;
import no.nav.pleiepengerbarn.uttak.kontrakter.Simulering;
import no.nav.pleiepengerbarn.uttak.kontrakter.Uttaksgrunnlag;

@Dependent
public class SamtidigUttakTjeneste {

    private static final Logger log = LoggerFactory.getLogger(SamtidigUttakTjeneste.class);
    private MapInputTilUttakTjeneste mapInputTilUttakTjeneste;
    private UttakTjeneste uttakTjeneste;
    private BehandlingRepository behandlingRepository;
    private BehandlingModellRepository behandlingModellRepository;

    
    @Inject
    public SamtidigUttakTjeneste(MapInputTilUttakTjeneste mapInputTilUttakTjeneste,
            UttakTjeneste uttakTjeneste,
            BehandlingRepository behandlingRepository,
            BehandlingModellRepository behandlingModellRepository) {
        this.mapInputTilUttakTjeneste = mapInputTilUttakTjeneste;
        this.uttakTjeneste = uttakTjeneste;
        this.behandlingRepository = behandlingRepository;
        this.behandlingModellRepository = behandlingModellRepository;
    }
    
    
    public boolean isSkalHaTilbakehopp(BehandlingReferanse ref) {
        final var behandling = behandlingRepository.hentBehandling(ref.getBehandlingId());
        final BehandlingStegType steg = behandling.getAktivtBehandlingSteg();
        final BehandlingModell modell = behandlingModellRepository.getModell(behandling.getType(), behandling.getFagsakYtelseType());
        if (modell.erStegAFørStegB(steg, BehandlingStegType.VURDER_UTTAK)) {
            log.info("For tidlig i prosessen til å vurdere uttak, er på steg={}", steg);
            return false;
        }
        final Simulering simulering = simulerUttak(ref);
        return simulering.getUttakplanEndret();
    }
    
    
    private Simulering simulerUttak(BehandlingReferanse ref) {
        final Uttaksgrunnlag request = mapInputTilUttakTjeneste.hentUtOgMapRequest(ref, true);
        final Simulering simulering = uttakTjeneste.simulerUttaksplan(request);

        return simulering;
    }
    
    public boolean isEndringerMedUbesluttedeData(BehandlingReferanse ref) {
        final Simulering simulering = simulerUttak(ref);
        // Hvis en sak ikke har kommet til uttak betyr det at true returneres her.
        log.info("Simulering harForrigeUttaksplan={} endret={}", (simulering.getForrigeUttaksplan() != null), simulering.getUttakplanEndret());
        return simulering.getUttakplanEndret();
    }
}
