package no.nav.ung.sak.web.app.tjenester.integrasjonstatus;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.k9.felles.sikkerhet.abac.BeskyttetRessurs;
import no.nav.k9.felles.sikkerhet.abac.BeskyttetRessursActionType;
import no.nav.k9.felles.sikkerhet.abac.BeskyttetRessursResourceType;
import no.nav.ung.sak.kontrakt.SystemNedeDto;
import org.apache.commons.lang3.BooleanUtils;

import java.util.Collections;
import java.util.List;

@Path("/integrasjon")
@ApplicationScoped
@Transactional
@Produces(MediaType.APPLICATION_JSON)
public class IntegrasjonstatusRestTjeneste {

    private boolean skalViseDetaljerteFeilmeldinger;

    public IntegrasjonstatusRestTjeneste() {
        // CDI
    }

    @Inject
    public IntegrasjonstatusRestTjeneste(@KonfigVerdi(value = "vise.detaljerte.feilmeldinger", defaultVerdi = "true") Boolean viseDetaljerteFeilmeldinger) {
        this.skalViseDetaljerteFeilmeldinger = BooleanUtils.toBoolean(viseDetaljerteFeilmeldinger);
    }

    @GET
    @Path("/status")
    @Operation(description = "Gir en oversikt over systemer som er nede", summary = ("Inneholder også detaljer og evt kjent tidspunkt for når systemet er oppe igjen."), tags = "integrasjon")
    @BeskyttetRessurs(action = BeskyttetRessursActionType.READ, resource = BeskyttetRessursResourceType.APPLIKASJON)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public List<SystemNedeDto> finnSystemerSomErNede() {
        return Collections.emptyList();
    }

    @GET
    @Path("/status/vises")
    @Operation(description = "Returnerer en boolean som angir om detaljerte feilmeldinger skal vises av GUI", tags = "integrasjon")
    @BeskyttetRessurs(action = BeskyttetRessursActionType.READ, resource = BeskyttetRessursResourceType.APPLIKASJON)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public boolean skalViseDetaljerteFeilmeldinger() {
        return skalViseDetaljerteFeilmeldinger;
    }
}
