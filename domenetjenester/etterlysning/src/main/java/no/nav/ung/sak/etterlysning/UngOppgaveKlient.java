package no.nav.ung.sak.etterlysning;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.k9.felles.integrasjon.rest.OidcRestClient;
import no.nav.k9.felles.integrasjon.rest.ScopedRestIntegration;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.ung.deltakelseopplyser.kontrakt.oppgave.registerinntekt.RegisterInntektOppgaveDTO;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.UUID;

@Dependent
@ScopedRestIntegration(scopeKey = "ungdomsprogramregister.scope", defaultScope = "api://prod-gcp.k9saksbehandling.ung-deltakelse-opplyser/.default")
public class UngOppgaveKlient {
    private final OidcRestClient restClient;
    private final URI opprettURI;
    private final URI avbrytURI;

    @Inject
    public UngOppgaveKlient(
        OidcRestClient restClient,
        @KonfigVerdi(value = "ungdomsprogramregister.url", defaultVerdi = "http://ung-deltakelse-opplyser.k9saksbehandling") String url) {
        this.restClient = restClient;
        opprettURI = tilUri(url, "oppgave/opprett");
        avbrytURI = tilUri(url, "oppgave/avbryt");

    }

    public void avbrytOppgave(UUID eksternRef) {
        try {
            restClient.post(avbrytURI, eksternRef);
        } catch (Exception e) {
            throw UngOppgavetjenesteFeil.FACTORY.feilVedKallTilUngOppgaveTjeneste(e).toException();
        }

    }

    public void opprettOppgave(RegisterInntektOppgaveDTO oppgaver) {
        try {
            restClient.post(opprettURI, oppgaver);
        } catch (Exception e) {
            throw UngOppgavetjenesteFeil.FACTORY.feilVedKallTilUngOppgaveTjeneste(e).toException();
        }

    }





    private static URI tilUri(String baseUrl, String path) {
        try {
            return new URI(baseUrl + "/" + path);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Ugyldig konfigurasjon for ungdomsprogram.register.url", e);
        }
    }

}

