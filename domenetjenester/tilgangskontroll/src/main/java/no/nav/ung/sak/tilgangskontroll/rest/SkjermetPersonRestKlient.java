package no.nav.ung.sak.tilgangskontroll.rest;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.felles.integrasjon.rest.ScopedRestIntegration;
import no.nav.k9.felles.integrasjon.rest.SystemUserOidcRestClient;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.ung.sak.typer.PersonIdent;

import java.net.URI;
import java.net.URISyntaxException;

@ApplicationScoped
@ScopedRestIntegration(scopeKey = "skjermet.person.scope", defaultScope = "api://prod-gcp.nom.skjermede-personer-pip/.default")
public class SkjermetPersonRestKlient {

    private SystemUserOidcRestClient restClient;
    private URI urlSkjermetPerson;

    SkjermetPersonRestKlient() {
    }

    @Inject
    public SkjermetPersonRestKlient(SystemUserOidcRestClient restClient, @KonfigVerdi(value = "skjermet.person.url", defaultVerdi = "https://skjermede-personer-pip.intern.nav.no/skjermet") String urlSkjermetPerson) {
        this.restClient = restClient;
        this.urlSkjermetPerson = tilUri(urlSkjermetPerson);
    }

    public Boolean personErSkjermet(PersonIdent personIdent) {
        String response = restClient.post(urlSkjermetPerson, new SkjermetRequestDto(personIdent.getIdent()), String.class);
        if ("true".equalsIgnoreCase(response)) {
            return true;
        }
        if ("false".equalsIgnoreCase(response)) {
            return false;
        }
        throw new IllegalArgumentException("Fikk ikke-forventet svar fra skjermingsløsningen.");
    }

    record SkjermetRequestDto(String personident) {
    }

    private static URI tilUri(String url) {
        try {
            return new URI(url);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Ugyldig konfigurasjon for skjermet person url", e);
        }
    }
}
