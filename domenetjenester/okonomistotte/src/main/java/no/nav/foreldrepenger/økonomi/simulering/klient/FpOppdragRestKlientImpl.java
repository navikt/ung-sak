package no.nav.foreldrepenger.økonomi.simulering.klient;

import java.net.URI;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.k9.sak.kontrakt.økonomi.tilbakekreving.BehandlingIdDto;
import no.nav.k9.sak.kontrakt.økonomi.tilbakekreving.SimulerOppdragDto;
import no.nav.k9.sak.kontrakt.økonomi.tilbakekreving.SimuleringResultatDto;
import no.nav.vedtak.felles.integrasjon.rest.OidcRestClient;

@ApplicationScoped
public class FpOppdragRestKlientImpl implements FpOppdragRestKlient {

    private static final String FPOPPDRAG_START_SIMULERING = "/simulering/start";
    private static final String FPOPPDRAG_HENT_RESULTAT = "/simulering/resultat";


    private OidcRestClient restClient;
    private URI uriStartSimulering;
    private URI uriHentResultat;

    public FpOppdragRestKlientImpl() {
        //for cdi proxy
    }

    @Inject
    public FpOppdragRestKlientImpl(OidcRestClient restClient) {
        this.restClient = restClient;
        String fpoppdragBaseUrl = FpoppdragFelles.getFpoppdragBaseUrl();
        uriStartSimulering = URI.create(fpoppdragBaseUrl + FPOPPDRAG_START_SIMULERING);
        uriHentResultat = URI.create(fpoppdragBaseUrl + FPOPPDRAG_HENT_RESULTAT);
    }

    @Override
    public void startSimulering(SimulerOppdragDto request) {
        restClient.post(uriStartSimulering, request);
    }

    @Override
    public Optional<SimuleringResultatDto> hentResultat(Long behandlingId) {
        return restClient.postReturnsOptional(uriHentResultat, new BehandlingIdDto(behandlingId), SimuleringResultatDto.class);
    }

}
