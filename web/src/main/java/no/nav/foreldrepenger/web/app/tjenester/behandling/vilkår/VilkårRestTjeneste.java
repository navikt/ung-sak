package no.nav.foreldrepenger.web.app.tjenester.behandling.vilkår;

import static no.nav.vedtak.sikkerhet.abac.BeskyttetRessursActionAttributt.READ;
import static no.nav.vedtak.sikkerhet.abac.BeskyttetRessursResourceAttributt.FAGSAK;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import no.nav.foreldrepenger.behandling.BehandlingIdDto;
import no.nav.foreldrepenger.behandling.UuidDto;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessurs;

@Path("")
@Produces(MediaType.APPLICATION_JSON)
@ApplicationScoped
@Transactional
public class VilkårRestTjeneste {

    public static final String PATH = "/behandling/vilkar";
    public static final String FULL_PATH = "/behandling/vilkar/full";
    public static final String V2_PATH = "/behandling/vilkar-v2";
    public static final String FULL_V2_PATH = "/behandling/vilkar/full-v2";
    private BehandlingRepository behandlingRepository;

    public VilkårRestTjeneste() {
        // for CDI proxy
    }

    @Inject
    public VilkårRestTjeneste(BehandlingRepositoryProvider behandlingRepositoryProvider) {
        this.behandlingRepository = behandlingRepositoryProvider.getBehandlingRepository();
    }

    @GET
    @Path(PATH)
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Hent informasjon om vilkår for en behandling",
        tags = "vilkår",
        responses = {
            @ApiResponse(responseCode = "200",
                description = "Returnerer vilkår på behandling, tom liste hvis ikke eksisterer (GUI støtter ikke NOT_FOUND p.t.)",
                content = @Content(
                    mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = VilkårDto.class)
                )
            )
        })
    @BeskyttetRessurs(action = READ, ressurs = FAGSAK)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public Response getVilkår(@NotNull @QueryParam("behandlingId") @Valid BehandlingIdDto behandlingIdDto) {
        Long behandlingId = behandlingIdDto.getBehandlingId();
        Behandling behandling = behandlingId != null
            ? behandlingRepository.hentBehandling(behandlingId)
            : behandlingRepository.hentBehandling(behandlingIdDto.getBehandlingUuid());

        List<VilkårDto> dto = VilkårDtoMapper.lagVilkarDto(behandling, false);
        CacheControl cc = new CacheControl();
        cc.setNoCache(true);
        cc.setNoStore(true);
        cc.setMaxAge(0);
        return Response.ok(dto).cacheControl(cc).build();
    }

    @GET
    @Path(FULL_PATH)
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Hent informasjon om vilkår for en behandling",
        summary = ("Returnerer info om vilkår, inkludert hvordan eventuelt kjørt (input og evaluering)."),
        tags = "vilkår",
        responses = {
            @ApiResponse(responseCode = "200",
                description = "Returnerer vilkår på behandling, tom liste hvis ikke eksisterer (GUI støtter ikke NOT_FOUND p.t.)",
                content = @Content(
                    mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = VilkårDto.class)
                )
            )
        })
    @BeskyttetRessurs(action = READ, ressurs = FAGSAK)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public Response getVilkårFull(@NotNull @QueryParam("behandlingId") @Valid BehandlingIdDto behandlingIdDto) {
        Long behandlingId = behandlingIdDto.getBehandlingId();
        Behandling behandling = behandlingId != null
            ? behandlingRepository.hentBehandling(behandlingId)
            : behandlingRepository.hentBehandling(behandlingIdDto.getBehandlingUuid());

        List<VilkårDto> dto = VilkårDtoMapper.lagVilkarDto(behandling, true);
        CacheControl cc = new CacheControl();
        cc.setNoCache(true);
        cc.setNoStore(true);
        cc.setMaxAge(0);
        return Response.ok(dto).cacheControl(cc).build();
    }

    @GET
    @Path(V2_PATH)
    @Operation(description = "Hent informasjon om vilkår for en behandling",
        tags = "vilkår",
        responses = {
            @ApiResponse(responseCode = "200",
                description = "Returnerer vilkår på behandling, tom liste hvis ikke eksisterer (GUI støtter ikke NOT_FOUND p.t.)",
                content = @Content(
                    mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = VilkårDto.class)
                )
            )
        })
    @BeskyttetRessurs(action = READ, ressurs = FAGSAK)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public Response getVilkår(@NotNull @QueryParam(UuidDto.NAME) @Parameter(description = UuidDto.DESC) @Valid UuidDto uuidDto) {
        return getVilkår(new BehandlingIdDto(uuidDto));
    }

    @GET
    @Path(FULL_V2_PATH)
    @Operation(description = "Hent informasjon om vilkår for en behandling",
        summary = ("Returnerer info om vilkår, inkludert hvordan eventuelt kjørt (input og evaluering)."),
        tags = "vilkår",
        responses = {
            @ApiResponse(responseCode = "200",
                description = "Returnerer vilkår på behandling, tom liste hvis ikke eksisterer (GUI støtter ikke NOT_FOUND p.t.)",
                content = @Content(
                    mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = VilkårDto.class)
                )
            )
        })
    @BeskyttetRessurs(action = READ, ressurs = FAGSAK)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public Response getVilkårFull(@NotNull @QueryParam(UuidDto.NAME) @Parameter(description = UuidDto.DESC) @Valid UuidDto uuidDto) {
        return getVilkårFull(new BehandlingIdDto(uuidDto));
    }

}
