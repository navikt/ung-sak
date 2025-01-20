package no.nav.ung.domenetjenester.arkiv.dok;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.util.Objects;

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.AbstractResponseHandler;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.k9.felles.integrasjon.rest.OidcRestClient;
import no.nav.k9.felles.integrasjon.rest.ScopedRestIntegration;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.k9.søknad.JsonUtils;
import no.nav.ung.domenetjenester.arkiv.dok.model.FerdigstillJournalpostRequest;
import no.nav.ung.domenetjenester.arkiv.dok.model.OppdaterJournalpostRequest;
import no.nav.ung.sak.typer.JournalpostId;

@Dependent
@ScopedRestIntegration(scopeKey = "DOKARKIV_SCOPE", defaultScope = "api://prod-fss.teamdokumenthandtering.dokarkiv/.default")
public class DokTjeneste {

    public static final String MASKINELL_JOURNALFØRENDE_ENHET = "9999";
    private static final Logger LOG = LoggerFactory.getLogger(DokTjeneste.class);

    private OidcRestClient restKlient;
    private String ferdigstillJournalpostEndpoint;
    private String oppdaterJournalpostEndpoint;

    @Inject
    public DokTjeneste(
        @KonfigVerdi("DOKARKIV_BASE_URL") URI endpoint,
        OidcRestClient restKlient) {
        this.restKlient = restKlient;
        this.ferdigstillJournalpostEndpoint = endpoint.toString() + "/rest/journalpostapi/v1/journalpost/%s/ferdigstill";
        this.oppdaterJournalpostEndpoint = endpoint.toString() + "/rest/journalpostapi/v1/journalpost/%s";
    }

    public void ferdigstillJournalpost(JournalpostId journalpostId) {
        Objects.requireNonNull(journalpostId);
        valider(journalpostId);
        var request = new FerdigstillJournalpostRequest(MASKINELL_JOURNALFØRENDE_ENHET);
        var requestUri = URI.create(String.format(ferdigstillJournalpostEndpoint, journalpostId.getVerdi()));
        var httpPatch = new HttpPatch(requestUri);
        try {
            httpPatch.setEntity(new StringEntity(JsonUtils.toString(request)));
        } catch (UnsupportedEncodingException e) {
            throw new DokarkivException("Serialisering av request feilet", e);
        }

        utførForespørsel(httpPatch, ForventetStatusCodeResponseHandler.of(200));
    }

    public void oppdaterJournalpost(JournalpostId journalpostId, OppdaterJournalpostRequest request) {
        Objects.requireNonNull(journalpostId);
        valider(journalpostId);
        var httpPut = new HttpPut(String.format(oppdaterJournalpostEndpoint, journalpostId.getVerdi()));
        try {
            var string = JsonUtils.toString(request);
            LOG.debug("Oppdater journalpost request=\n{}", string);
            httpPut.setEntity(new StringEntity(string));
        } catch (UnsupportedEncodingException e) {
            throw new DokarkivException("Serialisering av request feilet", e);
        }

        utførForespørsel(httpPut, ForventetStatusCodeResponseHandler.of(200));
    }

    public void valider(JournalpostId journalpostId) {
        if (!journalpostId.getVerdi().matches("^\\p{Alnum}+$")) {
            throw new IllegalArgumentException("Jounalpostid passet ikke i kjent mønster, gjelder " + journalpostId.getVerdi());
        }
    }

    private <T> T utførForespørsel(HttpRequestBase request, AbstractResponseHandler<T> responseHandler) {
        try (var httpResponse = restKlient.execute(request)) {
            int responseCode = httpResponse.getStatusLine().getStatusCode();
            if (responseCode == HttpStatus.SC_OK) {
                return responseHandler.handleResponse(httpResponse);
            } else {
                String responseBody = EntityUtils.toString(httpResponse.getEntity());
                String feilmelding = "Kunne ikke utføre request mot dokarkiv: " + request.getURI()
                    + ", HTTP status=" + httpResponse.getStatusLine() + ". HTTP Errormessage=" + responseBody;
                throw new DokarkivException(feilmelding);
            }
        } catch (IOException e) {
            throw new DokarkivException("Kunne ikke utføre request mot dokariv", e);
        }
    }
}
