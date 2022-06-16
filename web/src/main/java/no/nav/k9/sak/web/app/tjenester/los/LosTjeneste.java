package no.nav.k9.sak.web.app.tjenester.los;

import static no.nav.k9.abac.BeskyttetRessursKoder.FAGSAK;
import static no.nav.k9.felles.sikkerhet.abac.BeskyttetRessursActionAttributt.READ;
import static no.nav.k9.felles.sikkerhet.abac.BeskyttetRessursActionAttributt.UPDATE;
import static no.nav.k9.sak.web.app.tjenester.los.LosTjeneste.BASE_PATH;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import no.nav.k9.felles.sikkerhet.abac.BeskyttetRessurs;
import no.nav.k9.felles.sikkerhet.abac.TilpassetAbacAttributt;
import no.nav.k9.sak.kontrakt.behandling.BehandlingUuidDto;
import no.nav.k9.sak.web.server.abac.AbacAttributtSupplier;
import no.nav.k9.sikkerhet.context.SubjectHandler;

@ApplicationScoped
@Path(BASE_PATH)
@Produces(MediaType.APPLICATION_JSON)
public class LosTjeneste {

    public static final String BASE_PATH = "/los";
    public static final String MERKNAD = "/merknad";
    public static final String MERKNAD_PATH = BASE_PATH + MERKNAD;

    private LosSystemUserKlient losKlient;

    public LosTjeneste() {
        // For Rest-CDI
    }

    @Inject
    public LosTjeneste(LosSystemUserKlient losKlient) {
        this.losKlient = losKlient;
    }

    @GET
    @Path(MERKNAD)
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Henter merknad på oppgave i los", tags = "merknad")
    @BeskyttetRessurs(action = READ, resource = FAGSAK)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public Response getMerknad(@NotNull @QueryParam(BehandlingUuidDto.NAME) @Parameter(description = BehandlingUuidDto.DESC) @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) BehandlingUuidDto behandlingUuid) {
        var merknad = losKlient.hentMerknad(behandlingUuid.getBehandlingUuid());
        var response = (merknad == null) ? Response.noContent() : Response.ok(merknad);
        return response.build();
    }

    @POST
    @Path(MERKNAD)
    @Operation(description = "Lagrer merknad på oppgave i los", tags = "merknad")
    @BeskyttetRessurs(action = UPDATE, resource = FAGSAK)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public Response postMerknad(@Parameter(description = BehandlingUuidDto.DESC) @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) MerknadEndretDto merknadEndret) {
        var merknad = losKlient.lagreMerknad(merknadEndret.overstyrSaksbehandlerIdent(getCurrentUserId()));
        var response = (merknad == null) ? Response.noContent() : Response.ok(merknad);
        return response.build();
    }

    private static String getCurrentUserId() {
        return SubjectHandler.getSubjectHandler().getUid();
    }

}
