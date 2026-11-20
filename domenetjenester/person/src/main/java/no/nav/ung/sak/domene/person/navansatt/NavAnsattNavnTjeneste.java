package no.nav.ung.sak.domene.person.navansatt;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.felles.integrasjon.microsoftgraph.MicrosoftGraphRestKlient;
import no.nav.k9.felles.konfigurasjon.env.Environment;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.k9.felles.util.LRUCache;

import java.time.Duration;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@ApplicationScoped
public class NavAnsattNavnTjeneste {

    private EntraProxyKlient entraProxyKlient;
    private MicrosoftGraphRestKlient microsoftGraphRestKlient;
    private boolean brukEntraProxy;

    private final LRUCache<String, Optional<String>> cache = new LRUCache<>(1000, Duration.ofDays(1).toMillis());

    NavAnsattNavnTjeneste() {
        // for CDI proxy
    }

    @Inject
    public NavAnsattNavnTjeneste(EntraProxyKlient entraProxyKlient,
                                 MicrosoftGraphRestKlient microsoftGraphRestKlient,
                                 @KonfigVerdi(value = "ENTRA_PROXY_ENABLED", defaultVerdi = "true") boolean entraProxyEnabled) {
        this.entraProxyKlient = entraProxyKlient;
        this.microsoftGraphRestKlient = microsoftGraphRestKlient;
        this.brukEntraProxy = entraProxyEnabled && !Environment.current().isLocal(); // VTP mocker kun Microsoft Graph
    }

    public Map<String, String> navnPåNavAnsatte(Collection<String> identer) {
        return identer.stream()
            .distinct()
            .map(ident -> new IdentOgNavn(ident, navnPåNavAnsatt(ident)))
            .filter(it -> it.navn().isPresent())
            .collect(Collectors.toMap(IdentOgNavn::ident, it -> it.navn().get()));
    }

    public Optional<String> navnPåNavAnsatt(String ident) {
        Optional<String> navnFraCache = cache.get(ident);
        if (navnFraCache != null) {
            return navnFraCache;
        }
        Optional<String> navn = brukEntraProxy
            ? entraProxyKlient.hentNavnForNavAnsatt(ident)
            : microsoftGraphRestKlient.hentNavnForNavBruker(ident);
        cache.put(ident, navn);
        return navn;
    }

    private record IdentOgNavn(String ident, Optional<String> navn) {
    }
}
