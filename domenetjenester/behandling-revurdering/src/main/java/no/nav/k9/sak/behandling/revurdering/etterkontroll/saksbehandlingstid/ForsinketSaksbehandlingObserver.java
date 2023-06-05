package no.nav.k9.sak.behandling.revurdering.etterkontroll.saksbehandlingstid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.k9.sak.behandlingskontroll.events.BehandlingStatusEvent;

@ApplicationScoped
public class ForsinketSaksbehandlingObserver {

    private static final Logger log = LoggerFactory.getLogger(ForsinketSaksbehandlingObserver.class);

    private ForsinketSaksbehandlingEtterkontrollOppretter oppretter;
    private boolean isEnabled;

    public ForsinketSaksbehandlingObserver() {}

    @Inject
    public ForsinketSaksbehandlingObserver(
        ForsinketSaksbehandlingEtterkontrollOppretter oppretter,
        @KonfigVerdi(value = "ENABLE_ETTERKONTROLL_FORSINKET_SAKB", defaultVerdi = "false") boolean isEnabled
    ) {
        this.oppretter = oppretter;
        this.isEnabled = isEnabled;
    }

    public void observerBehandlingOpprettet(@Observes BehandlingStatusEvent.BehandlingOpprettetEvent event) {
        if (isEnabled) {
            log.info("Vurderer behov for etterkontroll for forsinket saksbehandling");
            oppretter.opprettEtterkontroll(event.getBehandlingId());
        }
    }
}
