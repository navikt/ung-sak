package no.nav.k9.sak.web.app.tasks;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.BeregningsgrunnlagTjeneste;
import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.ManglerBeregningsgrunnlagException;
import no.nav.k9.kodeverk.behandling.BehandlingStegType;
import no.nav.k9.prosesstask.api.ProsessTask;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskHandler;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.BehandlingModell;
import no.nav.k9.sak.behandlingskontroll.impl.BehandlingModellRepository;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.k9.sak.typer.Saksnummer;

@ApplicationScoped
@ProsessTask(PsbFeilretteTask.TASKTYPE)
public class PsbFeilretteTask implements ProsessTaskHandler {
    public static final String TASKTYPE = "fix.psbFeilrette";
    private static final Logger logger = LoggerFactory.getLogger(PsbFeilretteTask.class);

    private BehandlingRepository behandlingRepository;
    private BeregningsgrunnlagTjeneste beregningsgrunnlagTjeneste;
    private OpprettManuellRevurderingService opprettManuellRevurderingService;
    private FagsakRepository fagsakRepository;
    private BehandlingModellRepository behandlingModellRepository;
    
    
    public PsbFeilretteTask() {}

    @Inject
    public PsbFeilretteTask(BehandlingRepository behandlingRepository,
            BeregningsgrunnlagTjeneste beregningsgrunnlagTjeneste,
            OpprettManuellRevurderingService opprettManuellRevurderingService,
            FagsakRepository fagsakRepository,
            BehandlingModellRepository behandlingModellRepository) {
        this.behandlingRepository = behandlingRepository;
        this.beregningsgrunnlagTjeneste = beregningsgrunnlagTjeneste;
        this.opprettManuellRevurderingService = opprettManuellRevurderingService;
        this.fagsakRepository = fagsakRepository;
        this.behandlingModellRepository = behandlingModellRepository;
    }

    
    @Override
    public void doTask(ProsessTaskData pd) {
        final Saksnummer saksnummer = new Saksnummer(pd.getSaksnummer());
        final Fagsak fagsak = fagsakRepository.hentSakGittSaksnummer(saksnummer).orElseThrow();
        final Optional<Behandling> behandlingSomSkalSjekkes = finnBehandlingSomSkalSjekkes(fagsak.getId());
        
        if (behandlingSomSkalSjekkes.isEmpty()) {
            return;
        }
        
        final BehandlingReferanse ref = BehandlingReferanse.fra(behandlingSomSkalSjekkes.get());
        
        try {
            beregningsgrunnlagTjeneste.hentEksaktFastsattForAllePerioder(ref);
        } catch (ManglerBeregningsgrunnlagException e) {
            logger.info("Mangler beregningsgrunnlag i behandling " + ref.getBehandlingUuid().toString() + " sak: " + saksnummer.getVerdi());
            opprettManuellRevurderingService.revurder(saksnummer);
        }
    }
    
    private Optional<Behandling> finnBehandlingSomSkalSjekkes(Long fagsakId) {
        final Optional<Behandling> sisteBehandling = behandlingRepository.hentSisteYtelsesBehandlingForFagsakId(fagsakId);
        if (sisteBehandling.isEmpty()) {
            return Optional.empty();
        }
        
        if (harKommetTilBekreftUttak(BehandlingReferanse.fra(sisteBehandling.get()))) {
            return sisteBehandling;
        }
        
        if (sisteBehandling.get().getOriginalBehandlingId().isEmpty()) {
            return Optional.empty();
        }
        
        return Optional.of(behandlingRepository.hentBehandling(sisteBehandling.get().getOriginalBehandlingId().get()));
    }
    
    private boolean harKommetTilBekreftUttak(BehandlingReferanse ref) {
        final var behandling = behandlingRepository.hentBehandling(ref.getBehandlingId());
        final BehandlingStegType steg = behandling.getAktivtBehandlingSteg();
        final BehandlingModell modell = behandlingModellRepository.getModell(behandling.getType(), behandling.getFagsakYtelseType());
        return !modell.erStegAFørStegB(steg, BehandlingStegType.BEKREFT_UTTAK);
    }
}