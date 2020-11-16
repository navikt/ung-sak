package no.nav.k9.sak.web.app;

import java.util.List;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.k9.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.k9.sak.behandling.FagsakTjeneste;
import no.nav.k9.sak.behandling.prosessering.BehandlingsprosessApplikasjonTjeneste;
import no.nav.k9.sak.behandling.prosessering.task.TilbakeTilStartBehandlingTask;
import no.nav.k9.sak.behandling.revurdering.RevurderingTjeneste;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.typer.Saksnummer;
import no.nav.k9.sak.web.app.tjenester.behandling.aksjonspunkt.BehandlingsoppretterApplikasjonTjeneste;
import no.nav.vedtak.exception.VLException;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskRepository;

@ApplicationScoped
@ProsessTask(OpprettManuellRevurderingTask.TASKTYPE)
public class OpprettManuellRevurderingTask implements ProsessTaskHandler {
    public static final String TASKTYPE = "forvaltning.opprettManuellRevurdering";

    private static final Logger logger = LoggerFactory.getLogger(OpprettManuellRevurderingTask.class);

    private BehandlingsoppretterApplikasjonTjeneste behandlingsoppretterApplikasjonTjeneste;
    private BehandlingsprosessApplikasjonTjeneste behandlingsprosessTjeneste;
    private FagsakTjeneste fagsakTjeneste;
    private ProsessTaskRepository prosessTaskRepository;
    private BehandlingRepository behandlingRepository;

    protected OpprettManuellRevurderingTask() {
        // CDI proxy
    }

    @Inject
    public OpprettManuellRevurderingTask(BehandlingsoppretterApplikasjonTjeneste behandlingsoppretterApplikasjonTjeneste,
                                         BehandlingsprosessApplikasjonTjeneste behandlingsprosessTjeneste,
                                         FagsakTjeneste fagsakTjeneste,
                                         ProsessTaskRepository prosessTaskRepository,
                                         BehandlingRepository behandlingRepository) {
        this.behandlingsoppretterApplikasjonTjeneste = behandlingsoppretterApplikasjonTjeneste;
        this.behandlingsprosessTjeneste = behandlingsprosessTjeneste;
        this.fagsakTjeneste = fagsakTjeneste;
        this.prosessTaskRepository = prosessTaskRepository;
        this.behandlingRepository = behandlingRepository;
    }


    @Override
    public void doTask(ProsessTaskData pd) {
        Fagsak f = fagsakTjeneste.finnEksaktFagsak(pd.getFagsakId(), true);
        try {
            revurder(f);
        } catch (VLException e) {
            logger.warn("Kunne ikke opprette manuell revurdering for: {}", f.getId());
        }
    }

    public void revurder(Fagsak fagsak) {
        final BehandlingÅrsakType behandlingÅrsakType = BehandlingÅrsakType.RE_ANNET;

        final RevurderingTjeneste revurderingTjeneste = FagsakYtelseTypeRef.Lookup.find(RevurderingTjeneste.class, fagsak.getYtelseType()).orElseThrow();
        if (revurderingTjeneste.kanRevurderingOpprettes(fagsak)) {
            final Behandling behandling = behandlingsoppretterApplikasjonTjeneste.opprettRevurdering(fagsak, behandlingÅrsakType);
            behandlingsprosessTjeneste.asynkStartBehandlingsprosess(behandling);
        } else {
            final Behandling behandling = finnBehandlingSomKanSendesTilbakeTilStart(fagsak.getSaksnummer());
            if (behandling == null) {
                logger.warn("Kunne ikke finne åpen behandling for: {}", fagsak.getSaksnummer());
                return;
            }

            final ProsessTaskData prosessTaskData = new ProsessTaskData(TilbakeTilStartBehandlingTask.TASKNAME);
            prosessTaskData.setCallIdFraEksisterende();
            prosessTaskData.setBehandling(fagsak.getId(), behandling.getId(), fagsak.getAktørId().getId());
            prosessTaskData.setProperty(TilbakeTilStartBehandlingTask.PROPERTY_MANUELT_OPPRETTET, Boolean.TRUE.toString());
            prosessTaskRepository.lagre(prosessTaskData);
        }
    }

    private Behandling finnBehandlingSomKanSendesTilbakeTilStart(Saksnummer saksnummer) {
        final List<Behandling> behandlinger = behandlingRepository.hentAbsoluttAlleBehandlingerForSaksnummer(saksnummer)
            .stream()
            .filter(Behandling::erYtelseBehandling)
            .filter(b -> !b.erStatusFerdigbehandlet())
            .collect(Collectors.toList());

        if (behandlinger.isEmpty()) {
            return null;
        }
        if (behandlinger.size() > 1) {
            throw new IllegalStateException("Flere åpne behandlinger på én fagsak er ikke støttet i denne tasken ennå.");
        }
        return behandlinger.get(0);
    }
}
