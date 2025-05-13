package no.nav.ung.sak.hendelse.vedtak;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.k9.prosesstask.api.ProsessTask;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskHandler;
import no.nav.ung.kodeverk.behandling.BehandlingType;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.sak.behandling.revurdering.OpprettKontrollBehandlingEllerDiffTask;
import no.nav.ung.sak.behandling.revurdering.OpprettRevurderingEllerOpprettDiffTask;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.fagsak.FagsakProsessTaskRepository;
import no.nav.ung.sak.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.ung.sak.perioder.ProsessTriggerPeriodeUtleder;
import no.nav.ung.sak.ytelse.kontroll.ManglendeKontrollperioderTjeneste;

import java.util.NavigableSet;
import java.util.stream.Collectors;

@ApplicationScoped
@ProsessTask(VurderOmVedtakPåvirkerKontrollbehandlingTask.TASKNAME)
@FagsakProsesstaskRekkefølge(gruppeSekvens = false)
// TODO: Denne kan fjernes
public class VurderOmVedtakPåvirkerKontrollbehandlingTask implements ProsessTaskHandler {

    public static final String TASKNAME = "iverksetteVedtak.vurderPavirketKontrollbehandling";

    private BehandlingRepository behandlingRepository;
    private FagsakProsessTaskRepository fagsakProsessTaskRepository;
    private ProsessTriggerPeriodeUtleder prosessTriggerPeriodeUtleder;
    private ManglendeKontrollperioderTjeneste manglendeKontrollperioderTjeneste;

    VurderOmVedtakPåvirkerKontrollbehandlingTask() {
    }

    @Inject
    public VurderOmVedtakPåvirkerKontrollbehandlingTask(BehandlingRepository behandlingRepository,
                                                        FagsakProsessTaskRepository fagsakProsessTaskRepository,
                                                        ProsessTriggerPeriodeUtleder prosessTriggerPeriodeUtleder, ManglendeKontrollperioderTjeneste manglendeKontrollperioderTjeneste) {
        this.behandlingRepository = behandlingRepository;
        this.fagsakProsessTaskRepository = fagsakProsessTaskRepository;
        this.prosessTriggerPeriodeUtleder = prosessTriggerPeriodeUtleder;
        this.manglendeKontrollperioderTjeneste = manglendeKontrollperioderTjeneste;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        final var behandlingId = Long.parseLong(prosessTaskData.getBehandlingId());
        final var åpenKontrollBehandling = behandlingRepository.hentÅpneBehandlingerForFagsakId(prosessTaskData.getFagsakId(), BehandlingType.KONTROLLBEHANDLING);

        if (!åpenKontrollBehandling.isEmpty()) {
            if (åpenKontrollBehandling.size() != 1) {
                throw new IllegalStateException("Kan ikke ha mer enn en åpen kontrollbehandling på en fagsak");
            }
            var kontrollBehandling = åpenKontrollBehandling.getFirst();

            final var tidslinjeTilVurdering = prosessTriggerPeriodeUtleder.utledTidslinje(behandlingId);
            final var tidslinjeTilKontroll = prosessTriggerPeriodeUtleder.utledTidslinje(kontrollBehandling.getId());

            final var endretTidslinjeTilKontroll = tidslinjeTilVurdering.intersection(tidslinjeTilKontroll);

            if (!endretTidslinjeTilKontroll.isEmpty()) {
                var taskData = ProsessTaskData.forProsessTask(OpprettKontrollBehandlingEllerDiffTask.class);
                taskData.setProperty(OpprettRevurderingEllerOpprettDiffTask.BEHANDLING_ÅRSAK, BehandlingÅrsakType.RE_ENDRET_SATS_ELLER_VILKÅRSRESULTAT.getKode());
                taskData.setProperty(OpprettRevurderingEllerOpprettDiffTask.PERIODER, utledPerioder(endretTidslinjeTilKontroll.getLocalDateIntervals()));
                taskData.setBehandling(kontrollBehandling.getFagsakId(), kontrollBehandling.getId(), kontrollBehandling.getAktørId().getId());
                fagsakProsessTaskRepository.lagreNyGruppe(taskData);
            }

        } else {
            manglendeKontrollperioderTjeneste.lagProsesstaskForRevurderingGrunnetManglendeKontrollAvInntekt(behandlingId).ifPresent(fagsakProsessTaskRepository::lagreNyGruppe);
        }

    }

    private String utledPerioder(NavigableSet<LocalDateInterval> perioder) {
        return perioder.stream().map(it -> it.getFomDato() + "/" + it.getTomDato())
            .collect(Collectors.joining("|"));
    }

}
