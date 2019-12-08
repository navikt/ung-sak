package no.nav.foreldrepenger.økonomi.simulering.klient;

import java.net.URI;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.vedtak.felles.integrasjon.rest.SystemUserOidcRestClient;

@ApplicationScoped
public class FpoppdragSystembrukerRestKlientImpl implements FpoppdragSystembrukerRestKlient {

    private static final String FPOPPDRAG_KANSELLER_SIMULERING = "/simulering/kanseller";

    private SystemUserOidcRestClient restClient;
    private URI uriKansellerSimulering;

    public FpoppdragSystembrukerRestKlientImpl() {
        //for cdi proxy
    }

    @Inject
    public FpoppdragSystembrukerRestKlientImpl(SystemUserOidcRestClient restClient) {
        this.restClient = restClient;
        String fpoppdragBaseUrl = FpoppdragFelles.getFpoppdragBaseUrl();

        uriKansellerSimulering = URI.create(fpoppdragBaseUrl + FPOPPDRAG_KANSELLER_SIMULERING);
    }

    @Override
    public void kansellerSimulering(Long behandlingId) {
        restClient.post(uriKansellerSimulering, new BehandlingIdDto(behandlingId));
    }

}
