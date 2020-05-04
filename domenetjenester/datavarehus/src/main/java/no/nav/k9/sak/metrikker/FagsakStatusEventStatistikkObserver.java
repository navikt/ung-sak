package no.nav.k9.sak.metrikker;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import no.nav.k9.sak.behandling.FagsakStatusEvent;
import no.nav.vedtak.felles.integrasjon.sensu.SensuEvent;
import no.nav.vedtak.felles.integrasjon.sensu.SensuKlient;

@ApplicationScoped
class FagsakStatusEventStatistikkObserver {

    private SensuKlient sensuKlient;

    FagsakStatusEventStatistikkObserver() {
        // for proxy
    }

    @Inject
    FagsakStatusEventStatistikkObserver(SensuKlient sensuKlient) {
        this.sensuKlient = sensuKlient;
    }

    void observer(@Observes FagsakStatusEvent event) {
        if (Objects.equals(event.getForrigeStatus(), event.getNyStatus())) {
            return;
        }

        List<SensuEvent> events = new ArrayList<>();
        if (event.getNyStatus() != null) {
            var sensuEvent = SensuEvent.createSensuEvent(
                "antall_fagsak",
                Map.of("ytelse_type", event.getYtelseType().getKode(),
                    "fagsak_status", event.getNyStatus().getKode()),
                Map.of("antall", 1));
            events.add(sensuEvent);
        }

        if (event.getForrigeStatus() != null) {
            var sensuEvent = SensuEvent.createSensuEvent(
                "antall_fagsak",
                Map.of("ytelse_type", event.getYtelseType().getKode(),
                    "fagsak_status", event.getForrigeStatus().getKode()),
                Map.of("antall", -1));
            events.add(sensuEvent);
        }

        sensuKlient.logMetrics(events);

    }
}
