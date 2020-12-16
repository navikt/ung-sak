package no.nav.k9.sak.web.app;

import java.util.List;
import java.util.Optional;
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
import no.nav.k9.sak.web.app.tjenester.behandling.BehandlingsoppretterTjeneste;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskRepository;

@ApplicationScoped
@ProsessTask(OpprettManuellRevurderingTask.TASKTYPE)
public class OpprettManuellRevurderingTask implements ProsessTaskHandler {
    public static final String TASKTYPE = "forvaltning.opprettManuellRevurdering";

    private static final Logger logger = LoggerFactory.getLogger(OpprettManuellRevurderingTask.class);

    private BehandlingsoppretterTjeneste behandlingsoppretterTjeneste;
    private BehandlingsprosessApplikasjonTjeneste behandlingsprosessTjeneste;
    private FagsakTjeneste fagsakTjeneste;
    private ProsessTaskRepository prosessTaskRepository;
    private BehandlingRepository behandlingRepository;

    protected OpprettManuellRevurderingTask() {
        // CDI proxy
    }

    @Inject
    public OpprettManuellRevurderingTask(BehandlingsoppretterTjeneste behandlingsoppretterTjeneste,
                                         BehandlingsprosessApplikasjonTjeneste behandlingsprosessTjeneste,
                                         FagsakTjeneste fagsakTjeneste,
                                         ProsessTaskRepository prosessTaskRepository,
                                         BehandlingRepository behandlingRepository) {
        this.behandlingsoppretterTjeneste = behandlingsoppretterTjeneste;
        this.behandlingsprosessTjeneste = behandlingsprosessTjeneste;
        this.fagsakTjeneste = fagsakTjeneste;
        this.prosessTaskRepository = prosessTaskRepository;
        this.behandlingRepository = behandlingRepository;
    }

    @Override
    public void doTask(ProsessTaskData pd) {
        var saksnummer = pd.getSaksnummer();
        if (saksnummer == null) {
            final String[] saksnumre = pd.getPayloadAsString().split("\\s+");
            if (saksnumre.length != 1) {
                throw new IllegalStateException("Kan ikke håndtere forespørsel med flere saksnummer grunnet feil i Abakus. Antall: " + saksnumre.length);
            }
            revurder(new Saksnummer(saksnumre[0]));
        } else {
            revurder(new Saksnummer(saksnummer));
        }
    }

    public void revurder(Saksnummer saksnummer) {
        final Optional<Fagsak> funnetFagsak = fagsakTjeneste.finnFagsakGittSaksnummer(saksnummer, true);
        final Fagsak fagsak = funnetFagsak.get();
        final BehandlingÅrsakType behandlingÅrsakType = BehandlingÅrsakType.RE_ANNET;

        final RevurderingTjeneste revurderingTjeneste = FagsakYtelseTypeRef.Lookup.find(RevurderingTjeneste.class, fagsak.getYtelseType()).orElseThrow();
        if (revurderingTjeneste.kanRevurderingOpprettes(fagsak)) {
            final Behandling behandling = behandlingsoppretterTjeneste.opprettRevurdering(fagsak, behandlingÅrsakType);
            behandlingsprosessTjeneste.asynkStartBehandlingsprosess(behandling);
        } else {
            final Behandling behandling = finnBehandlingSomKanSendesTilbakeTilStart(saksnummer);
            if (behandling == null) {
                logger.warn("Kunne ikke finne åpen behandling for: {}", saksnummer.getVerdi());
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
