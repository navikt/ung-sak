package no.nav.k9.sak.web.app.tasks;

import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskTjeneste;
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

@ApplicationScoped
public class OpprettRevurderingService {

    private static final Logger logger = LoggerFactory.getLogger(OpprettRevurderingService.class);

    private BehandlingsoppretterTjeneste behandlingsoppretterTjeneste;
    private BehandlingsprosessApplikasjonTjeneste behandlingsprosessTjeneste;
    private FagsakTjeneste fagsakTjeneste;
    private ProsessTaskTjeneste taskTjeneste;
    private BehandlingRepository behandlingRepository;

    protected OpprettRevurderingService() {

    }

    @Inject
    public OpprettRevurderingService(BehandlingsoppretterTjeneste behandlingsoppretterTjeneste,
                                     BehandlingsprosessApplikasjonTjeneste behandlingsprosessTjeneste,
                                     FagsakTjeneste fagsakTjeneste,
                                     ProsessTaskTjeneste taskTjeneste,
                                     BehandlingRepository behandlingRepository) {
        this.behandlingsoppretterTjeneste = behandlingsoppretterTjeneste;
        this.behandlingsprosessTjeneste = behandlingsprosessTjeneste;
        this.fagsakTjeneste = fagsakTjeneste;
        this.taskTjeneste = taskTjeneste;
        this.behandlingRepository = behandlingRepository;
    }

    public void opprettManuellRevurdering(Saksnummer saksnummer, BehandlingÅrsakType behandlingÅrsakType) {
        revurder(saksnummer, behandlingÅrsakType, true);
    }

    public void opprettAutomatiskRevurdering(Saksnummer saksnummer, BehandlingÅrsakType behandlingÅrsakType) {
        revurder(saksnummer, behandlingÅrsakType, false);
    }

    private void revurder(Saksnummer saksnummer, BehandlingÅrsakType behandlingÅrsakType, boolean manuell) {
        final Optional<Fagsak> funnetFagsak = fagsakTjeneste.finnFagsakGittSaksnummer(saksnummer, true);
        final Fagsak fagsak = funnetFagsak.get();

        final RevurderingTjeneste revurderingTjeneste = FagsakYtelseTypeRef.Lookup.find(RevurderingTjeneste.class, fagsak.getYtelseType()).orElseThrow();
        if (revurderingTjeneste.kanRevurderingOpprettes(fagsak)) {
            final Behandling behandling = manuell
                ? behandlingsoppretterTjeneste.opprettManuellRevurdering(fagsak, behandlingÅrsakType)
                : behandlingsoppretterTjeneste.opprettAutomatiskRevurdering(fagsak, behandlingÅrsakType);
            behandlingsprosessTjeneste.asynkStartBehandlingsprosess(behandling);
        } else {
            final Behandling behandling = finnBehandlingSomKanSendesTilbakeTilStart(saksnummer);
            if (behandling == null) {
                logger.warn("Kunne ikke finne åpen behandling for: {}", saksnummer.getVerdi());
                return;
            }

            final ProsessTaskData prosessTaskData = ProsessTaskData.forProsessTask(TilbakeTilStartBehandlingTask.class);
            prosessTaskData.setCallIdFraEksisterende();
            prosessTaskData.setBehandling(fagsak.getId(), behandling.getId(), fagsak.getAktørId().getId());
            prosessTaskData.setProperty(TilbakeTilStartBehandlingTask.PROPERTY_MANUELT_OPPRETTET, Boolean.toString(manuell));
            taskTjeneste.lagre(prosessTaskData);
        }
    }

    private Behandling finnBehandlingSomKanSendesTilbakeTilStart(Saksnummer saksnummer) {
        final List<Behandling> behandlinger = behandlingRepository.hentAbsoluttAlleBehandlingerForSaksnummer(saksnummer)
            .stream()
            .filter(Behandling::erYtelseBehandling)
            .filter(b -> !b.erStatusFerdigbehandlet())
            .toList();

        if (behandlinger.isEmpty()) {
            return null;
        }
        if (behandlinger.size() > 1) {
            throw new IllegalStateException("Flere åpne behandlinger på én fagsak er ikke støttet i denne tasken ennå.");
        }
        return behandlinger.get(0);
    }
}
