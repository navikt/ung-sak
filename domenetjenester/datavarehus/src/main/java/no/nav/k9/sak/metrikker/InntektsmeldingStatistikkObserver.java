package no.nav.k9.sak.metrikker;

import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import no.nav.k9.sak.domene.iay.inntektsmelding.InntektsmeldingEvent;
import no.nav.vedtak.felles.integrasjon.sensu.SensuEvent;
import no.nav.vedtak.felles.integrasjon.sensu.SensuKlient;

@ApplicationScoped
class InntektsmeldingStatistikkObserver {

    private SensuKlient sensuKlient;

    InntektsmeldingStatistikkObserver() {
        // for proxy
    }

    @Inject
    InntektsmeldingStatistikkObserver(SensuKlient sensuKlient) {
        this.sensuKlient = sensuKlient;
    }

    void observer(@Observes InntektsmeldingEvent.Mottatt event) {
        var sensuEvent = SensuEvent.createSensuEvent(
            "antall_inntektsmelding_mottatt",
            Map.of("ytelse_type", event.getYtelseType().getKode()),
            Map.of("antall", 1));

        sensuKlient.logMetrics(sensuEvent);
    }
}
