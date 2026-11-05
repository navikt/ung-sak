package no.nav.ung.sak.domene.person.navansatt;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.felles.integrasjon.rest.ScopedRestIntegration;
import no.nav.k9.felles.integrasjon.rest.SystemUserOidcRestClient;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;

import java.net.URI;
import java.util.Optional;

import static no.nav.k9.felles.integrasjon.microsoftgraph.MicrosoftGraphRestKlient.NAVIDENT_PATTERN;

@ApplicationScoped
@ScopedRestIntegration(scopeKey = "entra.proxy.scope", defaultScope = "api://prod-gcp.sikkerhetstjenesten.entra-proxy/.default")
public class EntraProxyKlient {

    private SystemUserOidcRestClient restClient;
    private URI baseUri;

    EntraProxyKlient() {
        // for CDI proxy
    }

    @Inject
    public EntraProxyKlient(SystemUserOidcRestClient restClient,
                            @KonfigVerdi(value = "entra.proxy.url", defaultVerdi = "http://entra-proxy.sikkerhetstjenesten/api/v1") URI baseUri) {
        this.restClient = restClient;
        this.baseUri = baseUri;
    }

    public Optional<String> hentNavnForNavAnsatt(String navIdent) {
        if (!NAVIDENT_PATTERN.matcher(navIdent).matches()) {
            throw new IllegalArgumentException("Ugyldig format på navIdent: " + navIdent);
        }
        URI uri = URI.create(baseUri + "/ansatt/" + navIdent);
        return restClient.getReturnsOptional(uri, AnsattDto.class)
            .map(AnsattDto::visningNavn);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record AnsattDto(String navIdent, String visningNavn) {
    }
}
