package no.nav.ung.sak.web.app.tjenester.microsoftgraph;

import com.fasterxml.jackson.annotation.*;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import no.nav.k9.felles.konfigurasjon.env.Environment;
import no.nav.k9.felles.sikkerhet.abac.BeskyttetRessurs;
import no.nav.k9.felles.sikkerhet.abac.TilpassetAbacAttributt;
import no.nav.ung.sak.web.server.abac.AbacAttributtSupplier;

import java.util.Optional;

import static no.nav.k9.felles.sikkerhet.abac.BeskyttetRessursActionAttributt.READ;
import static no.nav.ung.abac.BeskyttetRessursKoder.APPLIKASJON;


@Path("/forvaltning/saksbehandler")
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
public class ForvaltningTestRestTjeneste {

    private MicrosoftGraphTjeneste microsoftGraphTjeneste;

    public ForvaltningTestRestTjeneste() {
    }

    @Inject
    public ForvaltningTestRestTjeneste(MicrosoftGraphTjeneste microsoftGraphTjeneste) {
        this.microsoftGraphTjeneste = microsoftGraphTjeneste;
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("/saksbehandlernavn")
    @BeskyttetRessurs(action = READ, resource = APPLIKASJON)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public Response finnNavnPåSakbehandler(@Parameter(description = "saksbehandleIdent") @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) @Valid SaksbehandlerIdentDto saksbehandler) {
        if (Environment.current().isProd()) {
            throw new IllegalArgumentException("Kun tiltenkt brukt i test");
        }
        Optional<String> resultat = microsoftGraphTjeneste.navnPåNavAnsatt(saksbehandler.ident);
        if (resultat.isPresent()) {
            return Response.ok(resultat.get()).build();
        }
        return Response.noContent().build();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonFormat(shape = JsonFormat.Shape.OBJECT)
    @JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
    public static class SaksbehandlerIdentDto {

        @JsonProperty("ident")
        @NotNull
        @Valid
        @Pattern(regexp = "^[A-Z][0-9]{6}$")
        @Size(min = 7, max = 7)
        private String ident;

        public SaksbehandlerIdentDto() {
            //
        }

        @JsonCreator
        public SaksbehandlerIdentDto(@JsonProperty("ident") @NotNull @Valid String ident) {
            this.ident = ident;
        }

        public String getIdent() {
            return ident;
        }

        public void setIdent(String ident) {
            this.ident = ident;
        }
    }

}
