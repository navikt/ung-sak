package no.nav.k9.sak.ytelse.pleiepengerbarn.iverksett;

import java.util.List;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.k9.prosesstask.api.ProsessTask;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskHandler;
import no.nav.k9.prosesstask.api.ProsessTaskRepository;
import no.nav.k9.sak.behandling.FagsakTjeneste;
import no.nav.k9.sak.behandling.prosessering.BehandlingsprosessApplikasjonTjeneste;
import no.nav.k9.sak.behandling.prosessering.task.TilbakeTilStartBehandlingTask;
import no.nav.k9.sak.behandling.revurdering.OpprettRevurderingEllerOpprettDiffTask;
import no.nav.k9.sak.behandling.revurdering.RevurderingTjeneste;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakProsessTaskRepository;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;

import no.nav.k9.sak.mottak.Behandlingsoppretter;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.Saksnummer;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomGrunnlagBehandling;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomGrunnlagRepository;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomVurderingRepository;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomVurderingService;

@ApplicationScoped
@ProsessTask(VurderRevurderingAndreSøknaderTask.TASKNAME)
@FagsakProsesstaskRekkefølge(gruppeSekvens = true)
public class VurderRevurderingAndreSøknaderTask implements ProsessTaskHandler {
    private BehandlingRepository behandlingRepository;
    private SykdomGrunnlagRepository sykdomGrunnlagRepository;
    private SykdomVurderingRepository sykdomVurderingRepository;
    private SykdomVurderingService sykdomVurderingService;
    private FagsakTjeneste fagsakTjeneste;
    private Behandlingsoppretter behandlingsoppretter;
    private BehandlingsprosessApplikasjonTjeneste behandlingsprosessApplikasjonTjeneste;
    private ProsessTaskRepository prosessTaskRepository;
    private FagsakProsessTaskRepository fagsakProsessTaskRepository;



    public static final String TASKNAME = "iverksetteVedtak.vurderRevurderingAndreSøknader";

    @Inject
    public VurderRevurderingAndreSøknaderTask(BehandlingRepository behandlingRepository, SykdomGrunnlagRepository sykdomGrunnlagRepository, SykdomVurderingRepository sykdomVurderingRepository, SykdomVurderingService sykdomVurderingService, FagsakTjeneste fagsakTjeneste, Behandlingsoppretter behandlingsoppretter, BehandlingsprosessApplikasjonTjeneste behandlingsprosessApplikasjonTjeneste, ProsessTaskRepository prosessTaskRepository, FagsakProsessTaskRepository fagsakProsessTaskRepository) {
        this.behandlingRepository = behandlingRepository;
        this.sykdomGrunnlagRepository = sykdomGrunnlagRepository;
        this.sykdomVurderingRepository = sykdomVurderingRepository;
        this.sykdomVurderingService = sykdomVurderingService;
        this.fagsakTjeneste = fagsakTjeneste;
        this.behandlingsoppretter = behandlingsoppretter;
        this.behandlingsprosessApplikasjonTjeneste = behandlingsprosessApplikasjonTjeneste;
        this.prosessTaskRepository = prosessTaskRepository;
        this.fagsakProsessTaskRepository = fagsakProsessTaskRepository;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        Long behandlingId = Long.valueOf(prosessTaskData.getBehandlingId());
        Behandling vedtattBehandling = behandlingRepository.hentBehandling(behandlingId);
        SykdomGrunnlagBehandling vedtattSykdomGrunnlagBehandling = sykdomGrunnlagRepository.hentGrunnlagForBehandling(vedtattBehandling.getUuid()).get();

        AktørId pleietrengende = vedtattBehandling.getFagsak().getPleietrengendeAktørId();
        List<Saksnummer> alleSaksnummer = sykdomVurderingRepository.hentAlleSaksnummer(pleietrengende);

        for (Saksnummer kandidatsaksnummer : alleSaksnummer) {
            if (!kandidatsaksnummer.equals(vedtattSykdomGrunnlagBehandling.getSaksnummer())) {
                SykdomGrunnlagBehandling kandidatSykdomBehandling = sykdomGrunnlagRepository.hentSisteBehandling(kandidatsaksnummer).map(uuid -> sykdomGrunnlagRepository.hentGrunnlagForBehandling(uuid)).get().get();

                final LocalDateTimeline<Boolean> endringerISøktePerioder = sykdomVurderingService.utledRelevanteEndringerSidenForrigeBehandling(
                    kandidatsaksnummer, kandidatSykdomBehandling.getBehandlingUuid(), pleietrengende, List.of()).getDiffPerioder();

                if (!endringerISøktePerioder.isEmpty()) {
                    ProsessTaskData tilRevurderingTaskData = new ProsessTaskData(OpprettRevurderingEllerOpprettDiffTask.TASKNAME);
                    Behandling tilRevurdering = behandlingRepository.hentBehandling(kandidatSykdomBehandling.getBehandlingUuid());
                    tilRevurderingTaskData.setBehandling(tilRevurdering.getFagsakId(), tilRevurdering.getId(), tilRevurdering.getAktørId().getId());

                    fagsakProsessTaskRepository.lagreNyGruppe(tilRevurderingTaskData);
                }
            }
        }
    }
}
