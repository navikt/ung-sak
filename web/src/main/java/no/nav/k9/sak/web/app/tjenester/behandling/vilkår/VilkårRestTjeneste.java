package no.nav.k9.sak.web.app.tjenester.behandling.vilkår;

import static no.nav.k9.abac.BeskyttetRessursKoder.FAGSAK;
import static no.nav.vedtak.sikkerhet.abac.BeskyttetRessursActionAttributt.READ;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
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
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.k9.sak.domene.vedtak.intern.SendVedtaksbrev;
import no.nav.k9.sak.kontrakt.behandling.BehandlingIdDto;
import no.nav.k9.sak.kontrakt.behandling.BehandlingUuidDto;
import no.nav.k9.sak.kontrakt.vilkår.VilkårDto;
import no.nav.k9.sak.kontrakt.vilkår.VilkårMedPerioderDto;
import no.nav.k9.sak.perioder.VilkårsPerioderTilVurderingTjeneste;
import no.nav.k9.sak.web.server.abac.AbacAttributtSupplier;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessurs;
import no.nav.vedtak.sikkerhet.abac.TilpassetAbacAttributt;

@Path("")
@Produces(MediaType.APPLICATION_JSON)
@ApplicationScoped
@Transactional
public class VilkårRestTjeneste {

    public static final String PATH = "/behandling/vilkar";
    public static final String FULL_PATH = "/behandling/vilkar/full";
    public static final String V2_PATH = "/behandling/vilkar-v2";
    public static final String FULL_V2_PATH = "/behandling/vilkar/full-v2";
    public static final String V3_PATH = "/behandling/vilkar-v3";
    public static final String FULL_V3_PATH = "/behandling/vilkar/full-v3";
    public static final String TIL_VURDERING_PATH = "/behandling/vilkar-til-vurdering";

    private BehandlingRepository behandlingRepository;
    private VilkårResultatRepository vilkårResultatRepository;
    private Instance<VilkårsPerioderTilVurderingTjeneste> vilkårsPerioderTilVurderingTjenesteInstances;

    public VilkårRestTjeneste() {
        // for CDI proxy
    }

    @Inject
    public VilkårRestTjeneste(BehandlingRepositoryProvider behandlingRepositoryProvider,
                              @Any Instance<VilkårsPerioderTilVurderingTjeneste> vilkårsPerioderTilVurderingTjenesteInstance) {
        this.behandlingRepository = behandlingRepositoryProvider.getBehandlingRepository();
        this.vilkårResultatRepository = behandlingRepositoryProvider.getVilkårResultatRepository();
        this.vilkårsPerioderTilVurderingTjenesteInstances = vilkårsPerioderTilVurderingTjenesteInstance;
    }

    @GET
    @Path(PATH)
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Hent informasjon om vilkår for en behandling", tags = "vilkår", responses = {
        @ApiResponse(responseCode = "200", description = "Returnerer vilkår på behandling, tom liste hvis ikke eksisterer (GUI støtter ikke NOT_FOUND p.t.)", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = VilkårDto.class)))
    })
    @BeskyttetRessurs(action = READ, resource = FAGSAK)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public Response getVilkår(@NotNull @QueryParam("behandlingId") @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) BehandlingIdDto behandlingIdDto) {
        Long behandlingId = behandlingIdDto.getBehandlingId();
        Behandling behandling = behandlingId != null
            ? behandlingRepository.hentBehandling(behandlingId)
            : behandlingRepository.hentBehandling(behandlingIdDto.getBehandlingUuid());

        final var vilkårene = vilkårResultatRepository.hentHvisEksisterer(behandlingId).orElse(null);
        List<VilkårDto> dto = VilkårDtoMapper.lagVilkarDto(behandling, false, vilkårene);
        CacheControl cc = new CacheControl();
        cc.setNoCache(true);
        cc.setNoStore(true);
        cc.setMaxAge(0);
        return Response.ok(dto).cacheControl(cc).build();
    }

    @GET
    @Path(FULL_PATH)
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Hent informasjon om vilkår for en behandling", summary = ("Returnerer info om vilkår, inkludert hvordan eventuelt kjørt (input og evaluering)."), tags = "vilkår", responses = {
        @ApiResponse(responseCode = "200", description = "Returnerer vilkår på behandling, tom liste hvis ikke eksisterer (GUI støtter ikke NOT_FOUND p.t.)", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = VilkårDto.class)))
    })
    @BeskyttetRessurs(action = READ, resource = FAGSAK)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public Response getVilkårFull(@NotNull @QueryParam("behandlingId") @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) BehandlingIdDto behandlingIdDto) {
        Long behandlingId = behandlingIdDto.getBehandlingId();
        Behandling behandling = behandlingId != null
            ? behandlingRepository.hentBehandling(behandlingId)
            : behandlingRepository.hentBehandling(behandlingIdDto.getBehandlingUuid());

        final var vilkårene = vilkårResultatRepository.hentHvisEksisterer(behandlingId).orElse(null);
        List<VilkårDto> dto = VilkårDtoMapper.lagVilkarDto(behandling, true, vilkårene);
        CacheControl cc = new CacheControl();
        cc.setNoCache(true);
        cc.setNoStore(true);
        cc.setMaxAge(0);
        return Response.ok(dto).cacheControl(cc).build();
    }

    @GET
    @Path(V2_PATH)
    @Operation(description = "Hent informasjon om vilkår for en behandling", tags = "vilkår", responses = {
        @ApiResponse(responseCode = "200", description = "Returnerer vilkår på behandling, tom liste hvis ikke eksisterer (GUI støtter ikke NOT_FOUND p.t.)", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = VilkårDto.class)))
    })
    @BeskyttetRessurs(action = READ, resource = FAGSAK)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public Response getVilkår(@NotNull @QueryParam(BehandlingUuidDto.NAME) @Parameter(description = BehandlingUuidDto.DESC) @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) BehandlingUuidDto behandlingUuid) {
        var behandling = behandlingRepository.hentBehandling(behandlingUuid.getBehandlingUuid());
        final var vilkårene = vilkårResultatRepository.hentHvisEksisterer(behandling.getId()).orElse(null);
        List<VilkårDto> dto = VilkårDtoMapper.lagVilkarDto(behandling, false, vilkårene);
        CacheControl cc = new CacheControl();
        cc.setNoCache(true);
        cc.setNoStore(true);
        cc.setMaxAge(0);
        return Response.ok(dto).cacheControl(cc).build();
    }

    @GET
    @Path(FULL_V2_PATH)
    @Operation(description = "Hent informasjon om vilkår for en behandling", summary = ("Returnerer info om vilkår, inkludert hvordan eventuelt kjørt (input og evaluering)."), tags = "vilkår", responses = {
        @ApiResponse(responseCode = "200", description = "Returnerer vilkår på behandling, tom liste hvis ikke eksisterer (GUI støtter ikke NOT_FOUND p.t.)", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = VilkårDto.class)))
    })
    @BeskyttetRessurs(action = READ, resource = FAGSAK)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public Response getVilkårFull(@NotNull @QueryParam(BehandlingUuidDto.NAME) @Parameter(description = BehandlingUuidDto.DESC) @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) BehandlingUuidDto behandlingUuid) {
        var behandling = behandlingRepository.hentBehandling(behandlingUuid.getBehandlingUuid());
        final var vilkårene = vilkårResultatRepository.hentHvisEksisterer(behandling.getId()).orElse(null);
        List<VilkårDto> dto = VilkårDtoMapper.lagVilkarDto(behandling, true, vilkårene);
        CacheControl cc = new CacheControl();
        cc.setNoCache(true);
        cc.setNoStore(true);
        cc.setMaxAge(0);
        return Response.ok(dto).cacheControl(cc).build();
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
        final var vilkårene = vilkårResultatRepository.hentHvisEksisterer(behandling.getId()).orElse(null);
        var dto = VilkårDtoMapper.lagVilkarMedPeriodeDto(behandling, false, vilkårene);
        CacheControl cc = new CacheControl();
        cc.setNoCache(true);
        cc.setNoStore(true);
        cc.setMaxAge(0);
        return Response.ok(dto).cacheControl(cc).build();
    }

    @GET
    @Path(FULL_V3_PATH)
    @Operation(description = "Hent informasjon om vilkår for en behandling", summary = ("Returnerer info om vilkår, inkludert hvordan eventuelt kjørt (input og evaluering)."), tags = "vilkår", responses = {
        @ApiResponse(responseCode = "200", description = "Returnerer vilkår på behandling, tom liste hvis ikke eksisterer (GUI støtter ikke NOT_FOUND p.t.)", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = VilkårMedPerioderDto.class)))
    })
    @BeskyttetRessurs(action = READ, resource = FAGSAK)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public Response getVilkårFullV3(@NotNull @QueryParam(BehandlingUuidDto.NAME) @Parameter(description = BehandlingUuidDto.DESC) @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) BehandlingUuidDto behandlingUuid) {
        var behandling = behandlingRepository.hentBehandling(behandlingUuid.getBehandlingUuid());
        final var vilkårene = vilkårResultatRepository.hentHvisEksisterer(behandling.getId()).orElse(null);
        var dto = VilkårDtoMapper.lagVilkarMedPeriodeDto(behandling, true, vilkårene);
        CacheControl cc = new CacheControl();
        cc.setNoCache(true);
        cc.setNoStore(true);
        cc.setMaxAge(0);
        return Response.ok(dto).cacheControl(cc).build();
    }

    @GET
    @Path(TIL_VURDERING_PATH)
    @Operation(description = "Hent beregningsvilkår med perioder som vurderes i denne behandlingen", summary = ("Returnerer info om vilkår, inkludert hvordan eventuelt kjørt (input og evaluering)."), tags = "vilkår", responses = {
        @ApiResponse(responseCode = "200", description = "Returnerer kun beregningsvilkårperioder som er vurdert i denne behandlingen, ikke i tidligere. Tom liste hvis ikke eksisterer (GUI støtter ikke NOT_FOUND p.t.)", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = VilkårMedPerioderDto.class)))
    })
    @BeskyttetRessurs(action = READ, resource = FAGSAK)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public Response getVilkårTilVurdering(@NotNull @QueryParam(BehandlingUuidDto.NAME) @Parameter(description = BehandlingUuidDto.DESC) @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) BehandlingUuidDto behandlingUuid) {
        var behandling = behandlingRepository.hentBehandling(behandlingUuid.getBehandlingUuid());
        var tjeneste = FagsakYtelseTypeRef.Lookup.find(VilkårsPerioderTilVurderingTjeneste.class, vilkårsPerioderTilVurderingTjenesteInstances, behandling.getFagsakYtelseType())
            .orElseThrow(() -> new UnsupportedOperationException("Har ikke " + VilkårsPerioderTilVurderingTjeneste.class.getSimpleName() + " for " + behandling.getId()));
        final var vilkårsperioderIDenneBehandlingen = tjeneste.utled(behandling.getId());
        final var alleVilkår = vilkårResultatRepository.hentHvisEksisterer(behandling.getId()).orElse(null);

        var dto = VilkårDtoMapper.lagVilkarMedPerioderTilVurdering(behandling, alleVilkår, vilkårsperioderIDenneBehandlingen);
        CacheControl cc = new CacheControl();
        cc.setNoCache(true);
        cc.setNoStore(true);
        cc.setMaxAge(0);
        return Response.ok(dto).cacheControl(cc).build();
    }


}
