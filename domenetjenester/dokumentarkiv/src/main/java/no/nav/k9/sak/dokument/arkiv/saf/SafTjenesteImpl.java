package no.nav.k9.sak.dokument.arkiv.saf;


import static no.nav.k9.sak.dokument.arkiv.saf.SafTjenesteImpl.SafTjenesteFeil.FEILFACTORY;

import com.fasterxml.jackson.databind.ObjectReader;

import no.nav.k9.sak.dokument.arkiv.saf.graphql.DokumentoversiktFagsakQuery;
import no.nav.k9.sak.dokument.arkiv.saf.graphql.GrapQlData;
import no.nav.k9.sak.dokument.arkiv.saf.graphql.GraphQlError;
import no.nav.k9.sak.dokument.arkiv.saf.graphql.GraphQlRequest;
import no.nav.k9.sak.dokument.arkiv.saf.graphql.GraphQlResponse;
import no.nav.k9.sak.dokument.arkiv.saf.graphql.HentDokumentQuery;
import no.nav.k9.sak.dokument.arkiv.saf.graphql.JournalpostQuery;
import no.nav.k9.sak.dokument.arkiv.saf.graphql.Variables;
import no.nav.k9.sak.dokument.arkiv.saf.rest.model.DokumentoversiktFagsak;
import no.nav.k9.sak.dokument.arkiv.saf.rest.model.Journalpost;
import no.nav.k9.søknad.JsonUtils;
import no.nav.vedtak.feil.Feil;
import no.nav.vedtak.feil.FeilFactory;
import no.nav.vedtak.feil.LogLevel;
import no.nav.vedtak.feil.deklarasjon.DeklarerteFeil;
import no.nav.vedtak.feil.deklarasjon.TekniskFeil;
import no.nav.vedtak.felles.integrasjon.rest.OidcRestClient;
import no.nav.vedtak.felles.integrasjon.rest.OidcRestClientResponseHandler;
import no.nav.vedtak.konfig.KonfigVerdi;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.opensaml.xmlsec.signature.G;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Optional;

@Dependent
public class SafTjenesteImpl implements SafTjeneste {

    private URI graphqlEndpoint;
    private URI hentDokumentEndpoint;
    private CloseableHttpClient restKlient;

    private String journalpostQueryDef;
    private String dokumentoversiktFagsakQueryDef;

    private ObjectReader objectReader = JsonUtils.getObjectMapper().readerFor(GraphQlResponse.class);

    SafTjenesteImpl() {
        // CDI
    }

    @Inject
    public SafTjenesteImpl(@KonfigVerdi(value = "saf.base.url", defaultVerdi = "http://localhost:8060/rest/api/saf") URI endpoint,
                           OidcRestClient restKlient) {
        this.graphqlEndpoint = URI.create(endpoint.toString() + "/graphql");
        this.hentDokumentEndpoint = URI.create(endpoint.toString() + "/rest/hentdokument");
        this.restKlient = restKlient;

        this.journalpostQueryDef = ReadFileFromClassPathHelper.hent("saf/journalpostQuery.graphql");
        this.dokumentoversiktFagsakQueryDef = ReadFileFromClassPathHelper.hent("saf/dokumentoversiktFagsakQuery.graphql");
    }


    @Override
    public DokumentoversiktFagsak dokumentoversiktFagsak(DokumentoversiktFagsakQuery query) {
        var graphQlRequest = new GraphQlRequest(dokumentoversiktFagsakQueryDef, new Variables(query.getFagsakId(), query.getFagsaksystem()));
        var responseHandler = new OidcRestClientResponseHandler.ObjectReaderResponseHandler<GraphQlResponse>(graphqlEndpoint, objectReader);

        GraphQlResponse graphQlResponse;
        try {
            var httpPost = new HttpPost(graphqlEndpoint);
            httpPost.setEntity(new StringEntity(JsonUtils.toString(graphQlRequest)));
            graphQlResponse = utførForespørsel(httpPost, responseHandler);
        } catch (Exception e) {
            throw FEILFACTORY.dokumentoversiktFagsakRequestFeilet(query, e).toException();
        }

        if (graphQlResponse == null) {
            return null;
        }
        if (graphQlResponse.getErrors() != null && graphQlResponse.getErrors().size() > 0) {
            throw  FEILFACTORY.queryReturnerteFeil(graphQlResponse).toException();
        }
        return Optional.of(graphQlResponse)
            .map(GraphQlResponse::getData)
            .map(GrapQlData::getDokumentoversiktFagsak)
            .orElseThrow(() -> FEILFACTORY.dokumentoversiktFagsakResponseFeilet(query).toException());
    }

    @Override
    public Journalpost hentJournalpostInfo(JournalpostQuery query) {
        var graphQlRequest = new GraphQlRequest(journalpostQueryDef, new Variables(query.getJournalpostId()));
        var responseHandler = new OidcRestClientResponseHandler.ObjectReaderResponseHandler<GraphQlResponse>(graphqlEndpoint, objectReader);

        GraphQlResponse graphQlResponse;
        try {
            var httpPost = new HttpPost(graphqlEndpoint);
            httpPost.setEntity(new StringEntity(JsonUtils.toString(graphQlRequest)));
            graphQlResponse = utførForespørsel(httpPost, responseHandler);
        } catch (Exception e) {
            throw FEILFACTORY.hentJournalpostRequestFeilet(query, e).toException();
        }

        if (graphQlResponse == null) {
            return null;
        }
        if (graphQlResponse.getErrors() != null && graphQlResponse.getErrors().size() > 0) {
            throw  FEILFACTORY.queryReturnerteFeil(graphQlResponse).toException();
        }
        return Optional.of(graphQlResponse)
            .map(GraphQlResponse::getData)
            .map(GrapQlData::getJournalpost)
            .orElseThrow(() -> FEILFACTORY.hentJournalpostResponseFeilet(query).toException());
    }

    @Override
    public byte[] hentDokument(HentDokumentQuery query) {
        var uri = URI.create(hentDokumentEndpoint.toString() +
            String.format("/%s/%s/%s", query.getJournalpostId(), query.getDokumentInfoId(), query.getVariantFormat()));
        var getRequest = new HttpGet(uri);

        try {
            return utførForespørsel(getRequest);
        } catch (Exception e) {
            throw FEILFACTORY.hentDokumentRequestFeilet(query, e).toException();
        }
    }

    private <T> T utførForespørsel(HttpPost request, OidcRestClientResponseHandler.ObjectReaderResponseHandler<T> responseHandler) throws IOException {
        try (var httpResponse = restKlient.execute(request)) {
            int responseCode = httpResponse.getStatusLine().getStatusCode();
            if (responseCode == HttpStatus.SC_OK) {
                return responseHandler.handleResponse(httpResponse);
            } else {
                if (responseCode == HttpStatus.SC_NOT_MODIFIED) {
                    return null;
                }
                if (responseCode == HttpStatus.SC_NO_CONTENT) {
                    return null;
                }
                if (responseCode == HttpStatus.SC_ACCEPTED) {
                    return null;
                }
                String responseBody = EntityUtils.toString(httpResponse.getEntity());
                String feilmelding = "Kunne ikke hente informasjon for query mot SAF: " + request.getURI()
                    + ", HTTP request=" + request.getEntity()
                    + ", HTTP status=" + httpResponse.getStatusLine()
                    + ". HTTP Errormessage=" + responseBody;
                throw new SafException(feilmelding);
            }
        }
    }


    private <T> byte[] utførForespørsel(HttpGet request) throws IOException {
        try (var httpResponse = restKlient.execute(request)) {
            int responseCode = httpResponse.getStatusLine().getStatusCode();
            if (responseCode == HttpStatus.SC_OK) {
                return EntityUtils.toByteArray(httpResponse.getEntity());
            } else {
                if (responseCode == HttpStatus.SC_NOT_MODIFIED) {
                    return null;
                }
                if (responseCode == HttpStatus.SC_NO_CONTENT) {
                    return null;
                }
                if (responseCode == HttpStatus.SC_ACCEPTED) {
                    return null;
                }
                String responseBody = EntityUtils.toString(httpResponse.getEntity());
                String feilmelding = "Kunne ikke hente informasjon for query mot SAF: " + request.getURI()
                    + ", HTTP status=" + httpResponse.getStatusLine() + ". HTTP Errormessage=" + responseBody;
                throw new SafException(feilmelding);
            }
        }
    }

    interface SafTjenesteFeil extends DeklarerteFeil { // NOSONAR - internt interface er ok her
        SafTjenesteFeil FEILFACTORY = FeilFactory.create(SafTjenesteFeil.class); // NOSONAR ok med konstant

        @TekniskFeil(feilkode = "K9-240613", feilmelding = "Feil ved request dokumentoversiktFagsak: %s", logLevel = LogLevel.WARN)
        Feil dokumentoversiktFagsakRequestFeilet(DokumentoversiktFagsakQuery query, Throwable t);

        @TekniskFeil(feilkode = "K9-240614", feilmelding = "Feil ved respons dokumentoversiktFagsak: %s", logLevel = LogLevel.WARN)
        Feil dokumentoversiktFagsakResponseFeilet(DokumentoversiktFagsakQuery query);

        @TekniskFeil(feilkode = "K9-969997", feilmelding = "Feil ved request hentJournalpost: %s", logLevel = LogLevel.WARN)
        Feil hentJournalpostRequestFeilet(JournalpostQuery query, Throwable t);

        @TekniskFeil(feilkode = "K9-969998", feilmelding = "Feil ved respons hentJournalpost: %s", logLevel = LogLevel.WARN)
        Feil hentJournalpostResponseFeilet(JournalpostQuery query);

        @TekniskFeil(feilkode = "K9-588730", feilmelding = "Feil fra SAF ved utført query: %s", logLevel = LogLevel.WARN)
        Feil queryReturnerteFeil(GraphQlResponse response);

        @TekniskFeil(feilkode = "K9-969999", feilmelding = "Feil ved request hentDokument: %s", logLevel = LogLevel.WARN)
        Feil hentDokumentRequestFeilet(HentDokumentQuery query, Throwable t);
    }
}
