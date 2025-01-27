package no.nav.ung.sak.tilgangskontroll.integrasjon.pdl;

import io.opentelemetry.instrumentation.annotations.WithSpan;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.felles.integrasjon.rest.ScopedRestIntegration;
import no.nav.k9.felles.integrasjon.rest.SystemUserOidcRestClient;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.ung.sak.tilgangskontroll.integrasjon.pdl.dto.AdressebeskyttelseGradering;
import no.nav.ung.sak.tilgangskontroll.integrasjon.pdl.dto.PipPersondataResponse;
import no.nav.ung.sak.typer.AktørId;
import no.nav.ung.sak.typer.PersonIdent;
import org.apache.http.Header;
import org.apache.http.message.BasicHeader;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Set;

@ApplicationScoped
@ScopedRestIntegration(scopeKey = "pdl.pip.scope", defaultScope = "api://prod-fss.pdl.pdl-pip-api/.default")
public class PersonPipRestKlient {

    private SystemUserOidcRestClient restClient;
    private URI urlPdlPip;

    PersonPipRestKlient() {
    }

    @Inject
    public PersonPipRestKlient(SystemUserOidcRestClient restClient, @KonfigVerdi(value = "pdl.pip.url", defaultVerdi = "https://pdl-pip-api.intern.nav.no/api/v1/person") String urlPdlPip) {
        this.restClient = restClient;
        this.urlPdlPip = tilUri(urlPdlPip);
    }

    @WithSpan
    public PipPersondataResponse hentPersoninformasjon(AktørId aktørId) {
        Set<Header> headers = Set.of(
            new BasicHeader("ident", aktørId.getAktørId())
        );
        return restClient.get(urlPdlPip, headers, PipPersondataResponse.class);
    }

    @WithSpan
    public Set<AdressebeskyttelseGradering> hentAdressebeskyttelse(PersonIdent personIdent) {
        Set<Header> headers = Set.of(
            new BasicHeader("ident", personIdent.getIdent())
        );
        PipPersondataResponse response = restClient.get(urlPdlPip, headers, PipPersondataResponse.class);
        return response.getAdressebeskyttelseGradering();
    }

    private static URI tilUri(String url) {
        try {
            return new URI(url);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Ugyldig konfigurasjon for skjermet person url", e);
        }
    }


}
