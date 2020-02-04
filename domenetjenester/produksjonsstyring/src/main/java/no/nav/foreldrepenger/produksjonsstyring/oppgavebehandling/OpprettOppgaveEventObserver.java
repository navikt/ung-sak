package no.nav.foreldrepenger.produksjonsstyring.oppgavebehandling;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingskontroll.events.BehandlingskontrollEvent;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.produksjonsstyring.oppgavebehandling.task.OpprettOppgaveForBehandlingTask;
import no.nav.foreldrepenger.produksjonsstyring.oppgavebehandling.task.OpprettOppgaveGodkjennVedtakTask;
import no.nav.foreldrepenger.produksjonsstyring.totrinn.TotrinnTjeneste;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktType;
import no.nav.k9.kodeverk.produksjonsstyring.OppgaveÅrsak;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskRepository;

/**
 * Observerer behandlinger med åpne aksjonspunkter og oppretter deretter oppgave i Gsak.
 */
@ApplicationScoped
public class OpprettOppgaveEventObserver {

    private OppgaveBehandlingKoblingRepository oppgaveBehandlingKoblingRepository;
    private OppgaveTjeneste oppgaveTjeneste;
    private ProsessTaskRepository prosessTaskRepository;
    private TotrinnTjeneste totrinnTjeneste;
    private BehandlingRepository behandlingRepository;

    @Inject
    public OpprettOppgaveEventObserver(OppgaveBehandlingKoblingRepository oppgaveBehandlingKoblingRepository
                                       , OppgaveTjeneste oppgaveTjeneste
                                       , ProsessTaskRepository prosessTaskRepository
                                       , BehandlingRepository behandlingRepository
                                       , TotrinnTjeneste totrinnTjeneste) {
        this.oppgaveBehandlingKoblingRepository = oppgaveBehandlingKoblingRepository;
        this.oppgaveTjeneste = oppgaveTjeneste;
        this.prosessTaskRepository = prosessTaskRepository;
        this.behandlingRepository = behandlingRepository;
        this.totrinnTjeneste = totrinnTjeneste;
    }

    /**
     * Håndterer oppgave etter at behandlingskontroll er kjørt ferdig.
     */
    public void opprettOppgaveDersomDetErÅpneAksjonspunktForAktivtBehandlingSteg(@Observes BehandlingskontrollEvent.StoppetEvent event) {
        Behandling behandling = behandlingRepository.hentBehandling(event.getBehandlingId());
        if (behandling.isBehandlingPåVent()) {
            oppgaveTjeneste.opprettTaskAvsluttOppgave(behandling);
            return;
        }
        List<Aksjonspunkt> åpneAksjonspunkt = filterAksjonspunkt(behandling.getÅpneAksjonspunkter(AksjonspunktType.MANUELL), event);

        totrinnTjeneste.hentTotrinnaksjonspunktvurderinger(behandling);
        //TODO(OJR) kunne informasjonen om hvilken oppgaveårsak som skal opprettes i GSAK være knyttet til AksjonspunktDef?
        if (!åpneAksjonspunkt.isEmpty()) {
            if (harAksjonspunkt(åpneAksjonspunkt, AksjonspunktDefinisjon.FATTER_VEDTAK)) {
                oppgaveTjeneste.avsluttOppgaveOgStartTask(behandling, behandling.erRevurdering() ? OppgaveÅrsak.REVURDER : OppgaveÅrsak.BEHANDLE_SAK, OpprettOppgaveGodkjennVedtakTask.TASKTYPE);
            } else {
                opprettOppgaveVedBehov(behandling);
            }
        }
    }

    private void opprettOppgaveVedBehov(Behandling behandling) {
        List<OppgaveBehandlingKobling> oppgaveBehandlingKoblinger = oppgaveBehandlingKoblingRepository.hentOppgaverRelatertTilBehandling(behandling.getId());
        Optional<OppgaveBehandlingKobling> aktivOppgave = OppgaveBehandlingKobling.getAktivOppgaveMedÅrsak(behandling.erRevurdering() ? OppgaveÅrsak.REVURDER : OppgaveÅrsak.BEHANDLE_SAK, oppgaveBehandlingKoblinger);
        if (!aktivOppgave.isPresent()) {
            ProsessTaskData enkeltTask = opprettProsessTaskData(behandling, OpprettOppgaveForBehandlingTask.TASKTYPE);
            enkeltTask.setCallIdFraEksisterende();
            prosessTaskRepository.lagre(enkeltTask);
        }
    }

    private boolean harAksjonspunkt(List<Aksjonspunkt> åpneAksjonspunkt, AksjonspunktDefinisjon aksjonspunktDefinisjon) {
        return åpneAksjonspunkt.stream().anyMatch(apListe -> apListe.getAksjonspunktDefinisjon().equals(aksjonspunktDefinisjon));
    }

    private ProsessTaskData opprettProsessTaskData(Behandling behandling, String prosesstaskType) {
        ProsessTaskData prosessTaskData = new ProsessTaskData(prosesstaskType);
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
