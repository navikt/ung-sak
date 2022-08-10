package no.nav.k9.sak.web.app.tjenester.behandling;

import static no.nav.k9.abac.BeskyttetRessursKoder.FAGSAK;
import static no.nav.k9.felles.sikkerhet.abac.BeskyttetRessursActionAttributt.READ;
import static no.nav.k9.felles.sikkerhet.abac.BeskyttetRessursActionAttributt.UPDATE;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.ResponseBuilder;
import jakarta.ws.rs.core.Response.Status;
import no.nav.k9.abac.BeskyttetRessursKoder;
import no.nav.k9.felles.sikkerhet.abac.AbacDataAttributter;
import no.nav.k9.felles.sikkerhet.abac.BeskyttetRessurs;
import no.nav.k9.felles.sikkerhet.abac.TilpassetAbacAttributt;
import no.nav.k9.sak.behandling.prosessering.BehandlingsprosessApplikasjonTjeneste;
import no.nav.k9.sak.kontrakt.AsyncPollingStatus;
import no.nav.k9.sak.kontrakt.behandling.BehandlingDto;
import no.nav.k9.sak.kontrakt.behandling.BehandlingIdListe;
import no.nav.k9.sak.kontrakt.behandling.BehandlingStatusListe;
import no.nav.k9.sak.kontrakt.behandling.BehandlingUuidDto;
import no.nav.k9.sak.web.server.abac.AbacAttributtSupplier;

@ApplicationScoped
@Transactional
@Path("")
@Produces(MediaType.APPLICATION_JSON)
public class BehandlingBackendRestTjeneste {

    static final String BASE_PATH = "/behandling";
    private static final String BACKEND_ROOT_PATH = BASE_PATH + "/backend-root";

    private BehandlingsprosessApplikasjonTjeneste behandlingsprosessTjeneste;

    private BehandlingDtoTjeneste behandlingDtoTjeneste;
    private SjekkProsessering sjekkProsessering;


    public BehandlingBackendRestTjeneste() {
        // for resteasy
    }

    @Inject
    public BehandlingBackendRestTjeneste(BehandlingsprosessApplikasjonTjeneste behandlingsprosessTjeneste,
                                         BehandlingDtoTjeneste behandlingDtoTjeneste,
                                         SjekkProsessering sjekkProsessering) {
        this.behandlingsprosessTjeneste = behandlingsprosessTjeneste;
        this.behandlingDtoTjeneste = behandlingDtoTjeneste;
        this.sjekkProsessering = sjekkProsessering;
    }

    @GET
    @Path(BACKEND_ROOT_PATH)
    @Operation(description = "Hent behandling gitt id for backend", summary = ("Returnerer behandlingen som er tilknyttet id. Dette er resultat etter at asynkrone operasjoner er utført."), tags = "behandlinger", responses = {
            @ApiResponse(responseCode = "200", description = "Returnerer behandling", content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = BehandlingDto.class))
            }),
    })
    @BeskyttetRessurs(action = READ, resource = FAGSAK)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public Response hentBehandlingResultatForBackend(@NotNull @QueryParam(BehandlingUuidDto.NAME) @Parameter(description = BehandlingUuidDto.DESC) @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) BehandlingUuidDto behandlingUuid) {
        var behandling = behandlingsprosessTjeneste.hentBehandling(behandlingUuid.getBehandlingUuid());
        AsyncPollingStatus taskStatus = sjekkProsessering.sjekkProsessTaskPågårForBehandling(behandling, null).orElse(null);
        BehandlingDto dto = behandlingDtoTjeneste.lagUtvidetBehandlingDto(behandling, taskStatus);
        ResponseBuilder responseBuilder = Response.ok().entity(dto);
        return responseBuilder.build();
    }

    @POST
    @Path(BACKEND_ROOT_PATH + "/refresh")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Init hent behandling", tags = "behandlinger", responses = {
            @ApiResponse(responseCode = "202", description = "Hent behandling initiert, Returnerer status på fremdrift/feil i backend", headers = @Header(name = HttpHeaders.LOCATION))
    })
    @BeskyttetRessurs(action = UPDATE, resource = BeskyttetRessursKoder.REFRESH_BEHANDLING_REGISTERDATA)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public Response oppfriskSaker(@NotNull @Valid @TilpassetAbacAttributt(supplierClass = BehandlingBackendRestTjeneste.AbacDataSupplier.class) BehandlingIdListe behandlinger) {

        List<BehandlingStatusListe.StatusDto> result = new ArrayList<>();
        int max = 100;
        for (var id : new LinkedHashSet<>(behandlinger.getBehandlinger())) {
            max--;
            if (max < 0) {
                // sjekker maks antall, og returnerer de
                break;
            }
            UUID behandlingUuid = id.getBehandlingUuid();
            var behandling = behandlingsprosessTjeneste.hentBehandling(behandlingUuid);
            if (behandling.erStatusFerdigbehandlet()) {
                result.add(new BehandlingStatusListe.StatusDto(behandlingUuid, null, behandling.getStatus()));
            } else if (!behandling.isBehandlingPåVent() && !sjekkProsessering.opprettTaskForOppfrisking(behandling, false)) {
                result.add(new BehandlingStatusListe.StatusDto(behandlingUuid, null, behandling.getStatus()));
            } else {
                var taskStatus = sjekkProsessering.sjekkProsessTaskPågårForBehandling(behandling, null);
                result.add(new BehandlingStatusListe.StatusDto(behandlingUuid, taskStatus.orElse(null), behandling.getStatus()));
            }

        }

        return Response.ok(new BehandlingStatusListe(result)).status(Status.ACCEPTED).build();
    }

    public static class AbacDataSupplier implements Function<Object, AbacDataAttributter> {
        @Override
        public AbacDataAttributter apply(Object obj) {
            return AbacDataAttributter.opprett();
        }
    }

}
