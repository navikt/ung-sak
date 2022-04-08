package no.nav.k9.sak.ytelse.pleiepengerbarn.iverksett;

import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.vedtak.IverksettingStatus;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.behandlingslager.behandling.vedtak.BehandlingVedtakEvent;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomGrunnlagBehandling;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomGrunnlagService;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomVurderingRepository;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomVurderingVersjonBesluttet;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.søknadsperiode.SøknadsperiodeTjeneste;

@ApplicationScoped
public class KvitterSykdomsvurderingerVedtakEventObserver {

    private static final Logger log = LoggerFactory.getLogger(no.nav.k9.sak.ytelse.pleiepengerbarn.iverksett.KvitterSykdomsvurderingerVedtakEventObserver.class);

    private BehandlingRepository behandlingRepository;
    private FagsakRepository fagsakRepository;

    private SykdomGrunnlagService sykdomGrunnlagService;
    private SykdomVurderingRepository sykdomVurderingRepository;

    private SøknadsperiodeTjeneste søknadsperiodeTjeneste;

    KvitterSykdomsvurderingerVedtakEventObserver() {
        // for CDI proxy
    }

    @Inject
    public KvitterSykdomsvurderingerVedtakEventObserver(BehandlingRepositoryProvider repositoryProvider, FagsakRepository fagsakRepository, SykdomGrunnlagService sykdomGrunnlagService, SykdomVurderingRepository sykdomVurderingRepository, SøknadsperiodeTjeneste søknadsperiodeTjeneste) {
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.fagsakRepository = fagsakRepository;
        this.sykdomGrunnlagService = sykdomGrunnlagService;
        this.sykdomVurderingRepository = sykdomVurderingRepository;
        this.søknadsperiodeTjeneste = søknadsperiodeTjeneste;
    }

    public void observerBehandlingVedtak(@Observes BehandlingVedtakEvent event) {
        Fagsak fagsak = fagsakRepository.finnEksaktFagsak(event.getFagsakId());
        if (fagsak.getYtelseType() != FagsakYtelseType.PSB) {
            return;
        }
        if (!IverksettingStatus.IVERKSATT.equals(event.getVedtak().getIverksettingStatus())) {
            return;
        }
        var behandlingId = event.getBehandlingId();

        if (søknadsperiodeTjeneste.utledFullstendigPeriode(behandlingId).isEmpty()) {
            return;
        }

        var behandling = behandlingRepository.hentBehandling(behandlingId);

        SykdomGrunnlagBehandling sykdomGrunnlagBehandling = sykdomGrunnlagService.hentGrunnlag(behandling.getUuid());

        if (!behandling.isToTrinnsBehandling()) {
            if (harUbesluttet(sykdomGrunnlagBehandling)) {
                log.warn("Har ubesluttede sykdomsvurderinger uten at AP9001 har blitt generert");
            }
            return;
        }

        String endretAv = behandling.getAnsvarligBeslutter();
        LocalDateTime nå = LocalDateTime.now();

        sykdomGrunnlagBehandling.getGrunnlag().getVurderinger()
            .stream()
            .filter(v -> !v.isBesluttet())
            .forEach(v -> {
                sykdomVurderingRepository.lagre(
                    new SykdomVurderingVersjonBesluttet(
                        endretAv,
                        nå,
                        v)); //On conflict do nothing
            });

        log.info("Utført for behandling: {}", behandlingId);
    }

    private boolean harUbesluttet(SykdomGrunnlagBehandling sykdomGrunnlagBehandling) {
        return sykdomGrunnlagBehandling.getGrunnlag().getVurderinger()
            .stream()
            .anyMatch(v -> !sykdomVurderingRepository.hentErBesluttet(v));
    }
}
