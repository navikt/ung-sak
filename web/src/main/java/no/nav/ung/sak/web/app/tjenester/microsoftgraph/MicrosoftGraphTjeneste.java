package no.nav.ung.sak.web.app.tjenester.microsoftgraph;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.k9.felles.util.LRUCache;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

@Dependent
public class MicrosoftGraphTjeneste {

    private final MicrosoftGraphRestKlient microsoftGraphRestKlient;

    private final LRUCache<String, Optional<String>> cache = new LRUCache<>(1000, Duration.ofDays(7).toMillis());

    @Inject
    public MicrosoftGraphTjeneste(MicrosoftGraphRestKlient microsoftGraphRestKlient) {
        this.microsoftGraphRestKlient = microsoftGraphRestKlient;
    }

    public Map<String, String> navnPåNavAnsatte(Collection<String> identer) {
        return identer.stream()
                .distinct()
                .parallel()
                .map(ident -> new IdentOgNavn(ident, navnPåNavAnsatt(ident)))
                .filter(it -> it.navn.isPresent())
                .collect(Collectors.toMap(IdentOgNavn::ident, it -> it.navn.get()));
    }

    public Optional<String> navnPåNavAnsatt(String ident) {
        Optional<String> navnFraCache = cache.get(ident);
        if (navnFraCache != null) {
            return navnFraCache;
        }
        Optional<String> navn = microsoftGraphRestKlient.hentNavnForNavBruker(ident);
        cache.put(ident, navn);
        return navn;
    }

    private record IdentOgNavn(String ident, Optional<String> navn) {
    }
}
