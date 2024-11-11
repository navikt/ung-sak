package no.nav.ung.sak.web.app.oppgave;

import jakarta.ws.rs.ApplicationPath;

import org.glassfish.jersey.server.ResourceConfig;

import no.nav.ung.sak.web.app.exceptions.RedirectExceptionMapper;
import no.nav.ung.sak.web.app.jackson.ObjectMapperResolver;

@ApplicationPath("oppgaveredirect")
public class OppgaveRedirectApplication extends ResourceConfig {

    public OppgaveRedirectApplication() {
        registerClasses(OppgaveRedirectTjeneste.class);
        register(new ObjectMapperResolver());
        register(new RedirectExceptionMapper());

        property(org.glassfish.jersey.server.ServerProperties.PROCESSING_RESPONSE_ERRORS_ENABLED, true);
    }

}
