package no.nav.k9.sak.web.server.mock;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Specializes;

import no.nav.vedtak.felles.integrasjon.sensu.SensuEvent;
import no.nav.vedtak.felles.integrasjon.sensu.SensuKlient;

@Specializes
@ApplicationScoped
//brukes ved kjøring lokalt for å unngå støy i loggen
public class SensuKlientMock extends SensuKlient {

    public SensuKlientMock() {
        super(null, 0);
    }

    @Override
    public void logMetrics(SensuEvent metrics) {
        //ingenting her
    }

    @Override
    public void logMetrics(List<SensuEvent> metrics) {
        //ingenting her
    }

    @Override
    public void logMetrics(SensuEvent.SensuRequest sensuRequest) {
        //ingenting her
    }

    @Override
    public synchronized void start() {
        //ingenting her
    }

    @Override
    public synchronized void stop() {
        //ingenting her
    }
}
