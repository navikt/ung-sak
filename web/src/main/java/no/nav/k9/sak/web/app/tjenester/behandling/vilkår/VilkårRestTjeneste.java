package no.nav.k9.sak.web.app.tjenester.behandling.vilkår;

import static no.nav.k9.abac.BeskyttetRessursKoder.FAGSAK;
import static no.nav.k9.felles.sikkerhet.abac.BeskyttetRessursActionAttributt.READ;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.felles.sikkerhet.abac.BeskyttetRessurs;
import no.nav.k9.felles.sikkerhet.abac.TilpassetAbacAttributt;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.kontrakt.behandling.BehandlingUuidDto;
import no.nav.k9.sak.kontrakt.vilkår.VilkårDto;
import no.nav.k9.sak.kontrakt.vilkår.VilkårMedPerioderDto;
import no.nav.k9.sak.kontrakt.vilkår.VilkårUtfallSamlet;
import no.nav.k9.sak.vilkår.VilkårTjeneste;
import no.nav.k9.sak.web.server.abac.AbacAttributtSupplier;

@Path("")
@Produces(MediaType.APPLICATION_JSON)
@ApplicationScoped
@Transactional
public class VilkårRestTjeneste {

    public static final String V3_PATH = "/behandling/vilkar-v3";
    public static final String VILKÅR_SAMLET_PATH = "/behandling/vilkar/samlet";
    public static final String FULL_V3_PATH = "/behandling/vilkar/full-v3";
    private BehandlingRepository behandlingRepository;
    private VilkårTjeneste vilkårTjeneste;

    public VilkårRestTjeneste() {
        // for CDI proxy
    }

    @Inject
    public VilkårRestTjeneste(BehandlingRepository behandlingRepository,
                              VilkårTjeneste vilkårTjeneste) {
        this.behandlingRepository = behandlingRepository;
        this.vilkårTjeneste = vilkårTjeneste;
    }

    @GET
    @Path(V3_PATH)
    @Operation(description = "Hent informasjon om vilkår for en behandling", tags = "vilkår", responses = {
            @ApiResponse(responseCode = "200", description = "Returnerer vilkår på behandling, tom liste hvis ikke eksisterer (GUI støtter ikke NOT_FOUND p.t.)", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = VilkårDto.class)))
    })
    @BeskyttetRessurs(action = READ, resource = FAGSAK)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public Response getVilkårV3(@NotNull @QueryParam(BehandlingUuidDto.NAME) @Parameter(description = BehandlingUuidDto.DESC) @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) BehandlingUuidDto behandlingUuid) {
        var behandling = behandlingRepository.hentBehandling(behandlingUuid.getBehandlingUuid());
        final var vilkårene = vilkårTjeneste.hentHvisEksisterer(behandling.getId()).orElse(null);
        var dto = VilkårDtoMapper.lagVilkarMedPeriodeDto(behandling, false, vilkårene);
        CacheControl cc = new CacheControl();
        cc.setNoCache(true);
        cc.setNoStore(true);
        cc.setMaxAge(0);
        return Response.ok(dto).cacheControl(cc).build();
    }

    @GET
    @Path(VILKÅR_SAMLET_PATH)
    @Operation(description = "Hent informasjon om vilkår samlet for en behandling", tags = "vilkår", responses = {
            @ApiResponse(responseCode = "200", description = "Returnerer vilkår på behandling, tom liste hvis ikke eksisterer (GUI støtter ikke NOT_FOUND p.t.)", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = VilkårDto.class)))
    })
    @BeskyttetRessurs(action = READ, resource = FAGSAK)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public Response getVilkårSamlet(@NotNull @QueryParam(BehandlingUuidDto.NAME) @Parameter(description = BehandlingUuidDto.DESC) @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) BehandlingUuidDto behandlingUuid) {
        var behandling = behandlingRepository.hentBehandling(behandlingUuid.getBehandlingUuid());
        var samletVilkårResultat = vilkårTjeneste.samletVilkårsresultat(behandling.getId());

        var dto = new VilkårResultatContainer(samletVilkårResultat);
        CacheControl cc = new CacheControl();
        cc.setNoCache(true);
        cc.setNoStore(true);
        cc.setMaxAge(0);
        return Response.ok(dto).cacheControl(cc).build();
    }

    @GET
    @Path(FULL_V3_PATH)
    @Operation(description = "Forvaltning : Hent informasjon om vilkår for en behandling", summary = ("Returnerer info om vilkår, inkludert hvordan eventuelt kjørt (input og evaluering)."), tags = {
            "vilkår", "forvaltning" }, responses = {
                    @ApiResponse(responseCode = "200", description = "Returnerer vilkår på behandling, tom liste hvis ikke eksisterer (GUI støtter ikke NOT_FOUND p.t.)", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = VilkårMedPerioderDto.class)))
            })
    @BeskyttetRessurs(action = READ, resource = FAGSAK)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public Response getVilkårFullV3(@NotNull @QueryParam(BehandlingUuidDto.NAME) @Parameter(description = BehandlingUuidDto.DESC) @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) BehandlingUuidDto behandlingUuid) {
        var behandling = behandlingRepository.hentBehandling(behandlingUuid.getBehandlingUuid());
        final var vilkårene = vilkårTjeneste.hentHvisEksisterer(behandling.getId()).orElse(null);
        var dto = VilkårDtoMapper.lagVilkarMedPeriodeDto(behandling, true, vilkårene);
        CacheControl cc = new CacheControl();
        cc.setNoCache(true);
        cc.setNoStore(true);
        cc.setMaxAge(0);
        return Response.ok(dto).cacheControl(cc).build();
    }

    public static class VilkårResultatContainer {

        @JsonProperty(value = "vilkårTidslinje")
        private LocalDateTimeline<VilkårUtfallSamlet> vilkårTidslinje;

        public VilkårResultatContainer(LocalDateTimeline<VilkårUtfallSamlet> vilkårTidslinje) {
            this.vilkårTidslinje = vilkårTidslinje;
        }

    }
}
