package no.nav.k9.sak.ytelse.pleiepengerbarn.iverksett;

import java.util.ArrayList;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakProsessTaskRepository;
import no.nav.k9.sak.hendelse.vedtak.VurderOmVedtakPåvirkerSakerTjeneste;
import no.nav.k9.sak.kontrakt.vedtak.VedtakHendelse;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.Saksnummer;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomGrunnlagBehandling;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomGrunnlagRepository;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomVurderingRepository;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomVurderingService;

@ApplicationScoped
@FagsakYtelseTypeRef("PSB")
public class VurderOmPSBVedtakPåvirkerAndreSakerTjeneste implements VurderOmVedtakPåvirkerSakerTjeneste {

    private BehandlingRepository behandlingRepository;
    private SykdomGrunnlagRepository sykdomGrunnlagRepository;
    private SykdomVurderingRepository sykdomVurderingRepository;
    private SykdomVurderingService sykdomVurderingService;
    private FagsakProsessTaskRepository fagsakProsessTaskRepository;

    VurderOmPSBVedtakPåvirkerAndreSakerTjeneste() {
    }

    @Inject
    public VurderOmPSBVedtakPåvirkerAndreSakerTjeneste(BehandlingRepository behandlingRepository,
                                                       SykdomGrunnlagRepository sykdomGrunnlagRepository,
                                                       SykdomVurderingRepository sykdomVurderingRepository,
                                                       SykdomVurderingService sykdomVurderingService,
                                                       FagsakProsessTaskRepository fagsakProsessTaskRepository) {
        this.behandlingRepository = behandlingRepository;
        this.sykdomGrunnlagRepository = sykdomGrunnlagRepository;
        this.sykdomVurderingRepository = sykdomVurderingRepository;
        this.sykdomVurderingService = sykdomVurderingService;
        this.fagsakProsessTaskRepository = fagsakProsessTaskRepository;
    }

    @Override
    public List<Saksnummer> utledSakerSomErKanVærePåvirket(VedtakHendelse vedtakHendelse) {
        var behandlingId = vedtakHendelse.getBehandlingId();
        Behandling vedtattBehandling = behandlingRepository.hentBehandling(behandlingId);
        SykdomGrunnlagBehandling vedtattSykdomGrunnlagBehandling = sykdomGrunnlagRepository.hentGrunnlagForBehandling(vedtattBehandling.getUuid()).orElseThrow();

        AktørId pleietrengende = vedtattBehandling.getFagsak().getPleietrengendeAktørId();
        List<Saksnummer> alleSaksnummer = sykdomVurderingRepository.hentAlleSaksnummer(pleietrengende);

        var result = new ArrayList<Saksnummer>();
        for (Saksnummer kandidatsaksnummer : alleSaksnummer) {
            if (!kandidatsaksnummer.equals(vedtattSykdomGrunnlagBehandling.getSaksnummer())) {
                SykdomGrunnlagBehandling kandidatSykdomBehandling = sykdomGrunnlagRepository.hentSisteBehandling(kandidatsaksnummer)
                    .flatMap(uuid -> sykdomGrunnlagRepository.hentGrunnlagForBehandling(uuid))
                    .orElseThrow();

                final LocalDateTimeline<Boolean> endringerISøktePerioder = sykdomVurderingService.utledRelevanteEndringerSidenForrigeBehandling(
                    kandidatsaksnummer, kandidatSykdomBehandling.getBehandlingUuid(), pleietrengende, List.of()).getDiffPerioder();

                if (!endringerISøktePerioder.isEmpty()) {
                    result.add(kandidatsaksnummer);
                }
            }
        }

        return result;
    }
}
