package no.nav.ung.sak.tilgangskontroll.rest.pdl;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.felles.integrasjon.pdl.PdlDefaultErrorHandler;
import no.nav.k9.felles.integrasjon.pdl.PdlKlient;
import no.nav.k9.felles.integrasjon.rest.ScopedRestIntegration;
import no.nav.k9.felles.integrasjon.rest.SystemUserOidcRestClient;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;

import java.net.URI;
import java.util.Optional;

@ApplicationScoped
@ScopedRestIntegration(scopeKey = "pdl.scope", defaultScope = "api://prod-fss.pdl.pdl-api/.default")
public class SystemUserPdlKlient {

    private PdlKlient systemUserPdlClient;

    SystemUserPdlKlient() {
        //for CDI proxy
    }

    @Inject
    public SystemUserPdlKlient(
        @KonfigVerdi(value = "pdl.base.url", defaultVerdi = "http://pdl-api.default/graphql") URI endpoint,
        @KonfigVerdi(value = "pdl.tema", defaultVerdi = "OMS") String tema,
        SystemUserOidcRestClient restClient) {

        systemUserPdlClient = new PdlKlient(endpoint, tema, restClient, new PdlDefaultErrorHandler());
    }

    public Optional<String> hentPersonIdentForAktørId(String aktørId) {
        return systemUserPdlClient.hentPersonIdentForAktørId(aktørId);
    }

}
