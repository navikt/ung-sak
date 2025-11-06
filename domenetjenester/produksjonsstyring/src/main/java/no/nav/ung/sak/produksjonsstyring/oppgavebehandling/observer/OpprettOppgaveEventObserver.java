package no.nav.ung.sak.produksjonsstyring.oppgavebehandling.observer;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskTjeneste;
import no.nav.k9.prosesstask.api.TaskType;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.AksjonspunktType;
import no.nav.ung.kodeverk.produksjonsstyring.OppgaveÅrsak;
import no.nav.ung.sak.behandlingskontroll.events.BehandlingStatusEvent;
import no.nav.ung.sak.behandlingskontroll.events.BehandlingskontrollEvent;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.ung.sak.behandlingslager.behandling.klage.KlageRepository;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.kontrakt.klage.KlageresultatEndretEvent;
import no.nav.ung.sak.produksjonsstyring.oppgavebehandling.OppgaveBehandlingKobling;
import no.nav.ung.sak.produksjonsstyring.oppgavebehandling.OppgaveBehandlingKoblingRepository;
import no.nav.ung.sak.produksjonsstyring.oppgavebehandling.OppgaveTjeneste;
import no.nav.ung.sak.produksjonsstyring.oppgavebehandling.task.OpprettOppgaveForBehandlingTask;
import no.nav.ung.sak.produksjonsstyring.oppgavebehandling.task.OpprettOppgaveGodkjennVedtakTask;
import no.nav.ung.sak.produksjonsstyring.totrinn.TotrinnTjeneste;
import no.nav.ung.sak.produksjonsstyring.totrinn.Totrinnsvurdering;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Observerer behandlinger med åpne aksjonspunkter og oppretter deretter oppgave i Gsak.
 */
@ApplicationScoped
public class OpprettOppgaveEventObserver {

    private final OppgaveBehandlingKoblingRepository oppgaveBehandlingKoblingRepository;
    private final OppgaveTjeneste oppgaveTjeneste;
    private final ProsessTaskTjeneste prosessTaskRepository;
    private final BehandlingRepository behandlingRepository;
    private final KlageRepository klageRepository;

    @Inject
    public OpprettOppgaveEventObserver(OppgaveBehandlingKoblingRepository oppgaveBehandlingKoblingRepository,
                                       OppgaveTjeneste oppgaveTjeneste,
                                       ProsessTaskTjeneste prosessTaskRepository,
                                       BehandlingRepository behandlingRepository,
                                       KlageRepository klageRepository) {
        this.oppgaveBehandlingKoblingRepository = oppgaveBehandlingKoblingRepository;
        this.oppgaveTjeneste = oppgaveTjeneste;
        this.prosessTaskRepository = prosessTaskRepository;
        this.behandlingRepository = behandlingRepository;
        this.klageRepository = klageRepository;
    }

    /**
     * Håndterer oppgave etter at behandlingskontroll er kjørt ferdig.
     */
    public void opprettOppgaveDersomDetErÅpneAksjonspunktForAktivtBehandlingSteg(@Observes BehandlingskontrollEvent.StoppetEvent event) {
        Behandling behandling = behandlingRepository.hentBehandling(event.getBehandlingId());
        if (behandling.erYtelseBehandling()) {
            return;
        }

        if (behandling.isBehandlingPåVent()) {
            oppgaveTjeneste.opprettTaskAvsluttOppgave(behandling);
            return;
        }
        List<Aksjonspunkt> åpneAksjonspunkt = filterAksjonspunkt(behandling.getÅpneAksjonspunkter(AksjonspunktType.MANUELL), event);
        if (!åpneAksjonspunkt.isEmpty()) {
            if (harAksjonspunkt(åpneAksjonspunkt, AksjonspunktDefinisjon.FATTER_VEDTAK)) {
                oppgaveTjeneste.avsluttOppgaveOgStartTask(behandling, behandling.erRevurdering() ? OppgaveÅrsak.REVURDER : OppgaveÅrsak.BEHANDLE_SAK, OpprettOppgaveGodkjennVedtakTask.TASKTYPE);
            } else {
                opprettOppgaveVedBehov(behandling);
            }
        }
    }

    public void opprettOppgaveVedKlagevurderingEndret(@Observes KlageresultatEndretEvent event) {
        var klage = klageRepository.hentKlageUtredning(event.behandlingId());
        if (klage.erKlageHjemsendt()) {
            opprettOppgaveVedBehov(behandlingRepository.hentBehandling(event.behandlingId()));
        }
    }

    private void opprettOppgaveVedBehov(Behandling behandling) {
        List<OppgaveBehandlingKobling> oppgaveBehandlingKoblinger = oppgaveBehandlingKoblingRepository.hentOppgaverRelatertTilBehandling(behandling.getId());
        Optional<OppgaveBehandlingKobling> aktivOppgave = OppgaveBehandlingKobling.getAktivOppgaveMedÅrsak(behandling.erRevurdering() ? OppgaveÅrsak.REVURDER : OppgaveÅrsak.BEHANDLE_SAK, oppgaveBehandlingKoblinger);
        if (!aktivOppgave.isPresent()) {
            ProsessTaskData enkeltTask = opprettProsessTaskData(behandling, new TaskType(OpprettOppgaveForBehandlingTask.TASKTYPE));
            enkeltTask.setCallIdFraEksisterende();
            prosessTaskRepository.lagre(enkeltTask);
        }
    }

    private boolean harAksjonspunkt(List<Aksjonspunkt> åpneAksjonspunkt, AksjonspunktDefinisjon aksjonspunktDefinisjon) {
        return åpneAksjonspunkt.stream().anyMatch(apListe -> apListe.getAksjonspunktDefinisjon().equals(aksjonspunktDefinisjon));
    }

    private ProsessTaskData opprettProsessTaskData(Behandling behandling, TaskType taskType) {
        ProsessTaskData prosessTaskData = ProsessTaskData.forTaskType(taskType);
        prosessTaskData.setBehandling(behandling.getFagsakId(), behandling.getId(), behandling.getAktørId().getId());
        return prosessTaskData;
    }

    private List<Aksjonspunkt> filterAksjonspunkt(List<Aksjonspunkt> åpneAksjonspunkter, BehandlingskontrollEvent event) {
        Set<String> aksjonspunktForSteg = event.getBehandlingModell().finnAksjonspunktDefinisjoner(event.getStegType());
        return åpneAksjonspunkter.stream()
                .filter(ad -> aksjonspunktForSteg.contains(ad.getAksjonspunktDefinisjon().getKode()))
                .collect(Collectors.toList());
    }
}
