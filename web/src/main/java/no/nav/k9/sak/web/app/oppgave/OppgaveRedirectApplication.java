package no.nav.k9.sak.web.app.oppgave;

import javax.ws.rs.ApplicationPath;

import org.glassfish.jersey.server.ResourceConfig;

import no.nav.k9.sak.web.app.exceptions.RedirectExceptionMapper;
import no.nav.k9.sak.web.app.jackson.JacksonJsonConfig;

@ApplicationPath("oppgaveredirect")
public class OppgaveRedirectApplication extends ResourceConfig {

    public OppgaveRedirectApplication() {
        registerClasses(OppgaveRedirectTjeneste.class);
        register(new JacksonJsonConfig());
        register(new RedirectExceptionMapper());

        property(org.glassfish.jersey.server.ServerProperties.PROCESSING_RESPONSE_ERRORS_ENABLED, true);
    }

}
