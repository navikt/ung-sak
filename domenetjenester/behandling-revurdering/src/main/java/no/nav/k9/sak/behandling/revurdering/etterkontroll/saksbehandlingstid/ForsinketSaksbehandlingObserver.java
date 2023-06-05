package no.nav.k9.sak.behandling.revurdering.etterkontroll.saksbehandlingstid;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import no.nav.k9.sak.behandlingskontroll.events.BehandlingStatusEvent;

@ApplicationScoped
public class ForsinketSaksbehandlingObserver {

    private ForsinketSaksbehandlingEtterkontrollOppretter oppretter;

    public ForsinketSaksbehandlingObserver() {}

    @Inject
    public ForsinketSaksbehandlingObserver(ForsinketSaksbehandlingEtterkontrollOppretter oppretter) {
        this.oppretter = oppretter;
    }

    public void observerBehandlingOpprettet(@Observes BehandlingStatusEvent.BehandlingOpprettetEvent event) {
        oppretter.opprettEtterkontroll(event.getBehandlingId());
    }
}
