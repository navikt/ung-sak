package no.nav.k9.sak.web.app.jackson;

import com.fasterxml.jackson.jakarta.rs.cfg.JakartaRSFeature;
import com.fasterxml.jackson.jakarta.rs.json.JacksonJsonProvider;

/**
 * Overrides default JacksonJsonProvider to disable the caching of resolved ObjectMapper so that we can resolve to
 * different ObjectMappers based on incoming http request header.
 */
public class CustomJacksonJsonProvider extends JacksonJsonProvider {
    public CustomJacksonJsonProvider() {
        super();
        // Disable endpoint writer caching
        this.disable(JakartaRSFeature.CACHE_ENDPOINT_WRITERS);
    }
}

