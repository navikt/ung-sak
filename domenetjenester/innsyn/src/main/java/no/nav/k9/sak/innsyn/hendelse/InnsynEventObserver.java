package no.nav.k9.sak.innsyn.hendelse;

import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import no.nav.k9.kodeverk.behandling.BehandlingStatus;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskTjeneste;
import no.nav.k9.sak.behandlingskontroll.events.AksjonspunktStatusEvent;
import no.nav.k9.sak.behandlingskontroll.events.BehandlingStatusEvent;

@ApplicationScoped
public class InnsynEventObserver {

    protected final Logger log = LoggerFactory.getLogger(this.getClass());

    private ProsessTaskTjeneste prosessTaskTjeneste;

    public InnsynEventObserver() {
    }

    @Inject
    public InnsynEventObserver(ProsessTaskTjeneste prosessTaskTjeneste) {
        this.prosessTaskTjeneste = prosessTaskTjeneste;
    }


    public void observerBehandlingStartet(@Observes BehandlingStatusEvent event) {
        if ((event.getGammelStatus() == BehandlingStatus.OPPRETTET || event.getGammelStatus() == null)
            && event.getNyStatus() == BehandlingStatus.UTREDES) {
            log.info("Publiserer melding til brukerdialog for behandling startet");
            lagProsessTask(event.getFagsakId(), event.getBehandlingId());
        }
    }

    public void observerBehandlingAvsluttetEvent(@Observes BehandlingStatusEvent.BehandlingAvsluttetEvent event)  {
        log.info("Publiserer melding til brukerdialog for behandling avsluttet");
        lagProsessTask(event.getFagsakId(), event.getBehandlingId());
    }

    public void observerAksjonspunkterFunnetEvent(@Observes AksjonspunktStatusEvent event) {
        //TODO spisse sånn at det ikke sendes for mange eventer til innsyn
        // må fange opp nye dokumenter på en eller annen måte
        log.info("Publiserer melding til brukerdialog for aksjonspunkt");
        lagProsessTask(event.getFagsakId(), event.getBehandlingId());
    }

    private void lagProsessTask(Long fagsakId, Long behandlingId) {
        var pd = ProsessTaskData.forProsessTask(PubliserInnsynEventTask.class);
        pd.setBehandling(fagsakId, behandlingId);
        prosessTaskTjeneste.lagre(pd);
    }

    private void debugObservasjon(AksjonspunktStatusEvent event) {
        var collect = event.getAksjonspunkter().stream()
            .filter(it -> it.erÅpentAksjonspunkt())
            .map(it -> it.getAksjonspunktDefinisjon().getKode() +":"+ it.getVenteårsak().getKode())
            .collect(Collectors.toSet());
        String join = String.join(", ", collect);
        log.info("################ -> Steg {} aksjonspunkter {}", event.getBehandlingStegType(), join);
    }


}
