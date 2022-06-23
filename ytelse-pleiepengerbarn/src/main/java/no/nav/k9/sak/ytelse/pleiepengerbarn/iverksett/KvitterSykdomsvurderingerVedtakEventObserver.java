package no.nav.k9.sak.ytelse.pleiepengerbarn.iverksett;

import java.time.LocalDateTime;
import java.util.Set;

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
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.MedisinskGrunnlag;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomGrunnlagService;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomVurderingRepository;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.PleietrengendeSykdomVurderingVersjonBesluttet;
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
        if (!Set.of(FagsakYtelseType.PSB, FagsakYtelseType.PPN).contains(fagsak.getYtelseType())) {
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

        MedisinskGrunnlag medisinskGrunnlag = sykdomGrunnlagService.hentGrunnlag(behandling.getUuid());

        if (!behandling.isToTrinnsBehandling()) {
            if (harUbesluttet(medisinskGrunnlag)) {
                log.warn("Har ubesluttede sykdomsvurderinger uten at AP9001 har blitt generert");
            }
            return;
        }

        String endretAv = behandling.getAnsvarligBeslutter();
        LocalDateTime nå = LocalDateTime.now();

        medisinskGrunnlag.getGrunnlagsdata().getVurderinger()
            .stream()
            .filter(v -> !v.isBesluttet())
            .forEach(v -> {
                sykdomVurderingRepository.lagre(
                    new PleietrengendeSykdomVurderingVersjonBesluttet(
                        endretAv,
                        nå,
                        v)); //On conflict do nothing
            });

        log.info("Utført for behandling: {}", behandlingId);
    }

    private boolean harUbesluttet(MedisinskGrunnlag medisinskGrunnlag) {
        return medisinskGrunnlag.getGrunnlagsdata().getVurderinger()
            .stream()
            .anyMatch(v -> !sykdomVurderingRepository.hentErBesluttet(v));
    }
}
