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

@ApplicationScoped
public class KvitterSykdomsvurderingerVedtakEventObserver {

    public static final String TASKTYPE = "iverksetteVedtak.kvitterSykdomsvurderinger"; //TODO: Fjerne?

    private static final Logger log = LoggerFactory.getLogger(no.nav.k9.sak.ytelse.pleiepengerbarn.iverksett.KvitterSykdomsvurderingerVedtakEventObserver.class);

    private BehandlingRepository behandlingRepository;
    private FagsakRepository fagsakRepository;

    private SykdomGrunnlagService sykdomGrunnlagService;
    private SykdomVurderingRepository sykdomVurderingRepository;

    KvitterSykdomsvurderingerVedtakEventObserver() {
        // for CDI proxy
    }

    @Inject
    public KvitterSykdomsvurderingerVedtakEventObserver(BehandlingRepositoryProvider repositoryProvider, FagsakRepository fagsakRepository, SykdomGrunnlagService sykdomGrunnlagService, SykdomVurderingRepository sykdomVurderingRepository) {
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.fagsakRepository = fagsakRepository;
        this.sykdomGrunnlagService = sykdomGrunnlagService;
        this.sykdomVurderingRepository = sykdomVurderingRepository;
    }

    public void observerBehandlingVedtak(@Observes BehandlingVedtakEvent event) {
        Fagsak fagsak = fagsakRepository.finnEksaktFagsak(event.getFagsakId());
        if (fagsak.getYtelseType() != FagsakYtelseType.PSB) {
            return;
        }
        if (IverksettingStatus.IVERKSATT.equals(event.getVedtak().getIverksettingStatus())) {
            var behandlingId = event.getBehandlingId();
            var behandling = behandlingRepository.hentBehandling(behandlingId);
            //logContext(behandling); //TODO: antar jeg burde ha mdc-logging her også?

            SykdomGrunnlagBehandling sykdomGrunnlagBehandling = sykdomGrunnlagService.hentGrunnlag(behandling.getUuid());

            String endretAv = behandling.isToTrinnsBehandling() ? behandling.getAnsvarligBeslutter() : behandling.getAnsvarligSaksbehandler();
            LocalDateTime nå = LocalDateTime.now();

            sykdomGrunnlagBehandling.getGrunnlag().getVurderinger()
                .stream()
                .forEach(v -> {
                    sykdomVurderingRepository.lagre(
                        new SykdomVurderingVersjonBesluttet(
                            endretAv,
                            nå,
                            v)); //On conflict do nothing
                });

            log.info("Utført for behandling: {}", behandlingId);
        }
    }
}
