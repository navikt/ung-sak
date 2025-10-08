package no.nav.ung.sak.datavarehus.observer;

import java.util.List;

import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskTjeneste;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.kodeverk.vedtak.IverksettingStatus;
import no.nav.ung.sak.metrikker.PubliserKontrollerteInntektperioderMetrikkTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import no.nav.ung.sak.behandling.FagsakStatusEvent;
import no.nav.ung.sak.behandling.hendelse.BehandlingEnhetEvent;
import no.nav.ung.sak.behandlingskontroll.events.AksjonspunktStatusEvent;
import no.nav.ung.sak.behandlingskontroll.events.BehandlingStatusEvent;
import no.nav.ung.sak.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.ung.sak.behandlingslager.behandling.vedtak.BehandlingVedtakEvent;

@ApplicationScoped
public class DatavarehusEventObserver {
    protected final Logger log = LoggerFactory.getLogger(this.getClass());

    private ProsessTaskTjeneste taskTjeneste;
    private boolean kontrollerInntektMetrikkPubliseringEnabled;

    public DatavarehusEventObserver() {
        // for CDI proxy
    }


    @Inject
    public DatavarehusEventObserver(ProsessTaskTjeneste taskTjeneste,
                                    @KonfigVerdi(value = "PUBLISER_KONTROLLERT_INNTEKT_METRIKK_ENABLED", required = false, defaultVerdi = "false") boolean kontrollerInntektMetrikkPubliseringEnabled) {
        this.taskTjeneste = taskTjeneste;
        this.kontrollerInntektMetrikkPubliseringEnabled = kontrollerInntektMetrikkPubliseringEnabled;
    }

    public void observerAksjonspunktStatusEvent(@Observes AksjonspunktStatusEvent event) {
        List<Aksjonspunkt> aksjonspunkter = event.getAksjonspunkter();
        log.debug("Lagrer {} aksjonspunkter i DVH datavarehus, for behandling {} og steg {}", aksjonspunkter.size(), event.getBehandlingId(), event.getBehandlingStegType());//NOSONAR
    }

    public void observerFagsakStatus(@Observes FagsakStatusEvent event) {
        log.debug("Lagrer fagsak {} i DVH mellomalger", event.getFagsakId());//NOSONAR
    }

    public void observerBehandlingEnhetEvent(@Observes BehandlingEnhetEvent event) {
        log.debug("Lagrer behandling {} i DVH datavarehus", event.getBehandlingId());//NOSONAR
    }

    public void observerBehandlingOpprettetEvent(@Observes BehandlingStatusEvent.BehandlingOpprettetEvent event) {
        log.debug("Lagrer behandling {} i DVH datavarehus", event.getBehandlingId());//NOSONAR
    }

    public void observerBehandlingAvsluttetEvent(@Observes BehandlingStatusEvent.BehandlingAvsluttetEvent event) {
        log.debug("Lagrer behandling {} i DVH datavarehus", event.getBehandlingId());//NOSONAR
    }

    public void observerBehandlingVedtakEvent(@Observes BehandlingVedtakEvent event) {
        if (kontrollerInntektMetrikkPubliseringEnabled && event.getVedtak().getIverksettingStatus().equals(IverksettingStatus.IVERKSATT) && erInntektkontrollBehandling(event)) {
            taskTjeneste.lagre(opprettTaskForInntektkontrollMetrikkPublisering(event));
        }
    }

    private ProsessTaskData opprettTaskForInntektkontrollMetrikkPublisering(BehandlingVedtakEvent event) {
        ProsessTaskData prosessTaskData = ProsessTaskData.forProsessTask(PubliserKontrollerteInntektperioderMetrikkTask.class);
        prosessTaskData.setBehandling(event.getFagsakId(), event.getBehandlingId());
        return prosessTaskData;
    }

    private boolean erInntektkontrollBehandling(BehandlingVedtakEvent event) {
        return event.getBehandling().getBehandlingÅrsakerTyper().contains(BehandlingÅrsakType.RE_KONTROLL_REGISTER_INNTEKT);
    }

}
