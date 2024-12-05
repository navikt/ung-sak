package no.nav.ung.sak.web.app.tjenester.microsoftgraph;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.core.UriBuilder;
import no.nav.k9.felles.exception.TekniskException;
import no.nav.k9.felles.integrasjon.rest.ScopedRestIntegration;
import no.nav.k9.felles.integrasjon.rest.SystemUserOidcRestClient;
import no.nav.k9.felles.konfigurasjon.env.Environment;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import org.apache.http.Header;
import org.apache.http.message.BasicHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

@ApplicationScoped
@ScopedRestIntegration(scopeKey = "microsofth.graph.scope", defaultScope = "https://graph.microsoft.com/.default")
public class MicrosoftGraphRestKlient {

    private static final Logger logger = LoggerFactory.getLogger(MicrosoftGraphRestKlient.class);
    private ObjectMapper mapper;
    private SystemUserOidcRestClient sysemuserRestClient;
    private URI endpoint;

    MicrosoftGraphRestKlient() {
        //for CDI proxy
    }

    @Inject
    public MicrosoftGraphRestKlient(SystemUserOidcRestClient sysemuserRestClient, @KonfigVerdi(value = "microsoft.graph.url", defaultVerdi = "https://graph.microsoft.com/v1.0") URI endpoint) {
        this.sysemuserRestClient = sysemuserRestClient;
        this.endpoint = endpoint;
        this.mapper = sysemuserRestClient.getMapper();
    }

    public Optional<String> hentNavnForNavBruker(String identNavBruker) {
        if (!NAVIDENT_PATTERN.matcher(identNavBruker).matches()) {
            throw new IllegalArgumentException("Ugyldig format på identNavBruker: " + identNavBruker);
        }

        // henter navn på én bruker
        // https://learn.microsoft.com/en-us/graph/api/user-list

        URI requestUri = UriBuilder.fromUri(toUri("/users"))
            .queryParam("$select", "id,onPremisesSamAccountName,displayName")
            .queryParam("$filter", "onPremisesSamAccountName eq '" + identNavBruker + "'")
            //.queryParam("$top", 500) bør sette denne om vi skal hente grupper også, for å slippe paging
            .queryParam("$count", true) // må være satt til å få filter til å virke
            .build();

        Set<Header> headers = Set.of(
            new BasicHeader("ConsistencyLevel", "eventual")
        );
        String stringResponse = sysemuserRestClient.get(requestUri, headers);
        if (stringResponse == null) {
            logger.info("Fant ikke bruker med ident: {}", identNavBruker); //skjer når bruker ikke lenger jobber i nav
            return Optional.empty();
        }
        if (Environment.current().isDev()) {
            logger.info("Fant bruker: " + stringResponse);
        }
        UsersResponse users = fromJson(stringResponse, UsersResponse.class);
        if (users.users.isEmpty()) {
            return Optional.empty();
        } else if (users.users.size() > 1) {
            logger.warn("Fant flere brukere for ident: {}", identNavBruker);
            return Optional.empty();
        } else {
            return Optional.of(users.users.getFirst().displayName());
        }
    }

    record User(@NotNull UUID id,
                @NotNull String onPremisesSamAccountName,
                @NotNull String displayName) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record UsersResponse(@NotNull @JsonProperty("value") List<User> users) {
    }

    private URI toUri(String relativeUri) {
        String uri = endpoint.toString() + relativeUri;
        try {
            return new URI(uri);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Ugyldig uri: " + uri, e);
        }
    }

    protected <T> T fromJson(String json, Class<T> clazz) {
        try {
            return mapper.readValue(json, clazz);
        } catch (IOException e) {
            throw new TekniskException("F-919328", "Fikk IO exception ved parsing av JSON", e);
        }
    }


    private static final String NAVIDENT_REGEX = "^[a-zA-Z]\\d{6}$";
    public static final Pattern NAVIDENT_PATTERN = Pattern.compile(NAVIDENT_REGEX);
}
