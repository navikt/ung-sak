package no.nav.k9.sak.metrikker;

import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import no.nav.k9.kodeverk.behandling.FagsakStatus;
import no.nav.k9.sak.behandling.FagsakStatusEvent;
import no.nav.vedtak.felles.integrasjon.sensu.SensuEvent;
import no.nav.vedtak.felles.integrasjon.sensu.SensuKlient;

@ApplicationScoped
class FagsakStatusEventObserver {

    private SensuKlient sensuKlient;

    FagsakStatusEventObserver() {
        // for proxy
    }

    @Inject
    FagsakStatusEventObserver(SensuKlient sensuKlient) {
        this.sensuKlient = sensuKlient;
    }

    void observer(@Observes FagsakStatusEvent event) {
        if (FagsakStatus.OPPRETTET.equals(event.getNyStatus())) {
            var sensuEvent = SensuEvent.createSensuEvent(
                "antall_fagsak_opprettet",
                Map.of("ytelse_type", event.getYtelseType().getKode()),
                Map.of("antall", 1));

            sensuKlient.logMetrics(sensuEvent);
        } else if (FagsakStatus.UNDER_BEHANDLING.equals(event.getNyStatus())) {
            var sensuEvent = SensuEvent.createSensuEvent(
                "antall_fagsak_under_behandling",
                Map.of("ytelse_type", event.getYtelseType().getKode()),
                Map.of("antall", 1));

            sensuKlient.logMetrics(sensuEvent);
        } else if (FagsakStatus.UNDER_BEHANDLING.equals(event.getForrigeStatus())) {
            var sensuEvent = SensuEvent.createSensuEvent(
                "antall_fagsak_under_behandling",
                Map.of("ytelse_type", event.getYtelseType().getKode()),
                Map.of("antall", -1));

            sensuKlient.logMetrics(sensuEvent);
        }
    }
}
