package no.nav.ung.fordel.auth;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;

@Dependent
public class SystembrukerClientCredentials {
    private final String clientId;
    private final String clientSecret;

    @Inject
    public SystembrukerClientCredentials(@KonfigVerdi("systembruker.username") String clientId,
                                         @KonfigVerdi("systembruker.password") String clientSecret) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }

    public String getClientId() {
        return clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }
}
