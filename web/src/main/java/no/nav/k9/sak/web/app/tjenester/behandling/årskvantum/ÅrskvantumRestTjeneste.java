package no.nav.k9.sak.web.app.tjenester.behandling.årskvantum;

import static no.nav.k9.abac.BeskyttetRessursKoder.FAGSAK;
import static no.nav.vedtak.sikkerhet.abac.BeskyttetRessursActionAttributt.READ;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import no.nav.k9.aarskvantum.kontrakter.*;
import no.nav.k9.sak.kontrakt.behandling.BehandlingUuidDto;
import no.nav.k9.sak.kontrakt.behandling.SaksnummerDto;
import no.nav.k9.sak.web.server.abac.AbacAttributtSupplier;
import no.nav.k9.sak.ytelse.omsorgspenger.årskvantum.tjenester.ÅrskvantumTjeneste;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessurs;
import no.nav.vedtak.sikkerhet.abac.TilpassetAbacAttributt;

@Path(ÅrskvantumRestTjeneste.BASE_PATH)
@Produces(MediaType.APPLICATION_JSON)
@Transactional
@ApplicationScoped
public class ÅrskvantumRestTjeneste {

    public static final String FORBRUKTEDAGER_PATH = "/forbruktedager";
    public static final String FULL_UTTAKSPLAN_PATH = "/uttaksplan";
    public static final String INPUT_PATH = "/input";
    public static final String UTBETALINGSGRUNNLAG = "/utbetalingsgrunnlag";
    public static final String UTTREKK_PATH = "/uttrekk";
    static final String BASE_PATH = "/behandling/aarskvantum";
    public static final String FORBRUKTEDAGER = BASE_PATH + FORBRUKTEDAGER_PATH;
    public static final String FULL_UTTAKSPLAN = BASE_PATH + FULL_UTTAKSPLAN_PATH;
    private ÅrskvantumTjeneste årskvantumTjeneste;

    public ÅrskvantumRestTjeneste() {
        // for proxying
    }

    @Inject
    public ÅrskvantumRestTjeneste(ÅrskvantumTjeneste årskvantumTjeneste) {
        this.årskvantumTjeneste = årskvantumTjeneste;
    }

    /**
     * Hent oppgitt uttak for angitt behandling.
     */
    @GET
    @Consumes(MediaType.APPLICATION_JSON)
    @Path(FORBRUKTEDAGER_PATH)
    @Operation(description = "Hent forbrukte dager", tags = "behandling - årskvantum/uttak", responses = {
        @ApiResponse(responseCode = "200", description = "Returnerer forbrukte dager av totalt årskvantum", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ÅrskvantumForbrukteDager.class)))
    })
    @BeskyttetRessurs(action = READ, resource = FAGSAK)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public ÅrskvantumForbrukteDager getForbrukteDager(@NotNull @QueryParam(BehandlingUuidDto.NAME) @Parameter(description = BehandlingUuidDto.DESC) @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) BehandlingUuidDto behandlingIdDto) {
        return årskvantumTjeneste.hentÅrskvantumForBehandling(behandlingIdDto.getBehandlingUuid());
    }

    /**
     * Hent den totale uttaksplanen for en sak.
     */
    @GET
    @Consumes(MediaType.APPLICATION_JSON)
    @Path(FULL_UTTAKSPLAN_PATH)
    @Operation(description = "Hent full uttaksplan", tags = "behandling - årskvantum/uttak", responses = {
        @ApiResponse(responseCode = "200", description = "Returnerer full uttaksplan hittil i år", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = FullUttaksplan.class)))
    })
    @BeskyttetRessurs(action = READ, resource = FAGSAK)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public FullUttaksplan getFullUttaksplan(@NotNull @QueryParam("saksnummer") @Parameter(description = "saksnummer") @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) SaksnummerDto saksnummerDto) {
        return årskvantumTjeneste.hentFullUttaksplan(saksnummerDto.getVerdi());
    }

    /**
     * Hent den totale uttaksplanen for en sak.
     */
    @GET
    @Consumes(MediaType.APPLICATION_JSON)
    @Path(FULL_UTTAKSPLAN_PATH)
    @Operation(description = "Hent full uttaksplan", tags = "behandling - årskvantum/uttak", responses = {
        @ApiResponse(responseCode = "200", description = "Returnerer full uttaksplan hittil i år", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = FullUttaksplan.class)))
    })
    @BeskyttetRessurs(action = READ, resource = FAGSAK)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public FullUttaksplanForBehandlinger getFullUttaksplan(@NotNull @QueryParam("saksnummer") @Parameter(description = "saksnummer") @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) SaksnummerDto saksnummerDto,
                                                           @NotNull @QueryParam(BehandlingUuidDto.NAME) @Parameter(description = BehandlingUuidDto.DESC) @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) BehandlingUuidDto behandlingIdDto) {
        return årskvantumTjeneste.hentUttaksplanForBehandling(saksnummerDto.getVerdi(), behandlingIdDto.getBehandlingUuid());
    }

    /**
     * Hent oppgitt uttak for angitt behandling.
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Path(INPUT_PATH)
    @Operation(description = "Hent input til beregning av årskvantum", tags = "behandling - årskvantum/uttak", responses = {
        @ApiResponse(responseCode = "200", description = "input til beregning av årskvantum", content = @Content(mediaType = MediaType.APPLICATION_JSON))
    })
    @BeskyttetRessurs(action = READ, resource = FAGSAK)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public Response inputTilÅrskvantumsBeregning(@NotNull @Parameter(description = BehandlingUuidDto.DESC) @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) BehandlingUuidDto behandlingIdDto) {

        var request = årskvantumTjeneste.hentInputTilBeregning(behandlingIdDto.getBehandlingUuid());

        return Response.ok(request).build();
    }

    /**
     * Hent utbetalingsgrunnlag fra årskvantum for angitt behandling.
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Path(UTBETALINGSGRUNNLAG)
    @Operation(description = "Hent utbetalingsgrunnlag fra årskvantum", tags = "behandling - årskvantum/uttak", responses = {
        @ApiResponse(responseCode = "200", description = "utbetalingsgrunnlag fra årskvantum", content = @Content(mediaType = MediaType.APPLICATION_JSON))
    })
    @BeskyttetRessurs(action = READ, resource = FAGSAK)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public ÅrskvantumUtbetalingGrunnlag hentUtbetalingsgrunnlagFraÅrskvantum(@NotNull @Parameter(description = BehandlingUuidDto.DESC) @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) BehandlingUuidDto behandlingIdDto) {
        return årskvantumTjeneste.hentUtbetalingGrunnlag(behandlingIdDto.getBehandlingUuid());
    }

    /**
     * Hent Uttrekk fra årskvantum.
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Path(UTTREKK_PATH)
    @Operation(description = "Hent uttrekk fra årskvantum", tags = "behandling - årskvantum/uttrekk", responses = {
        @ApiResponse(responseCode = "200", description = "uttrekk fra årskvantum", content = @Content(mediaType = MediaType.APPLICATION_JSON))
    })
    @BeskyttetRessurs(action = READ, resource = FAGSAK)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public ÅrskvantumUttrekk hentUttrekk() {
        return årskvantumTjeneste.hentUttrekk();
    }
}
