package no.nav.k9.sak.metrikker;

import java.util.Map;
import java.util.Objects;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import no.nav.k9.kodeverk.behandling.FagsakStatus;
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
        if(Objects.equals(event.getForrigeStatus(), event.getNyStatus())) {
            return;
        }
        
        if (event.getNyStatus() != null) {
            var sensuEvent = SensuEvent.createSensuEvent(
                "antall_fagsak_under_behandling",
                Map.of("ytelse_type", event.getYtelseType().getKode()),
                Map.of("antall", 1));

            sensuKlient.logMetrics(sensuEvent);
        }

        if (event.getForrigeStatus() != null) {
            var sensuEvent = SensuEvent.createSensuEvent(
                "antall_fagsak_under_behandling",
                Map.of("ytelse_type", event.getYtelseType().getKode()),
                Map.of("antall", -1));

            sensuKlient.logMetrics(sensuEvent);
        }

    }
}
