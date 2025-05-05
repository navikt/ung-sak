package no.nav.ung.sak.web.app.tjenester.behandling.vilkår;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.felles.sikkerhet.abac.BeskyttetRessurs;
import no.nav.k9.felles.sikkerhet.abac.TilpassetAbacAttributt;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.kontrakt.behandling.BehandlingUuidDto;
import no.nav.ung.sak.kontrakt.vilkår.VilkårMedPerioderDto;
import no.nav.ung.sak.kontrakt.vilkår.VilkårUtfallSamlet;
import no.nav.ung.sak.vilkår.VilkårTjeneste;
import no.nav.ung.sak.web.server.abac.AbacAttributtSupplier;
import no.nav.ung.sak.web.server.caching.CacheControl;

import java.util.Collections;
import java.util.List;

import static no.nav.k9.felles.sikkerhet.abac.BeskyttetRessursActionAttributt.READ;
import static no.nav.ung.abac.BeskyttetRessursKoder.FAGSAK;

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
        @ApiResponse(description = "Returnerer vilkår på behandling, tom liste hvis ikke eksisterer")
    })
    @BeskyttetRessurs(action = READ, resource = FAGSAK)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    @CacheControl()
    public List<VilkårMedPerioderDto> getVilkårV3(@NotNull @QueryParam(BehandlingUuidDto.NAME) @Parameter(description = BehandlingUuidDto.DESC) @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) BehandlingUuidDto behandlingUuid) {
        return getVilkårV3(behandlingUuid, false);
    }

    @GET
    @Path(FULL_V3_PATH)
    @Operation(description = "Forvaltning : Hent informasjon om vilkår for en behandling", summary = ("Returnerer info om vilkår, inkludert hvordan eventuelt kjørt (input og evaluering)."), tags = {"vilkår", "forvaltning"}, responses = {
        @ApiResponse(responseCode = "200", description = "Returnerer vilkår på behandling, tom liste hvis ikke eksisterer")
    })
    @BeskyttetRessurs(action = READ, resource = FAGSAK)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    @CacheControl()
    public List<VilkårMedPerioderDto> getVilkårFullV3(@NotNull @QueryParam(BehandlingUuidDto.NAME) @Parameter(description = BehandlingUuidDto.DESC) @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) BehandlingUuidDto behandlingUuid) {
        return getVilkårV3(behandlingUuid, true);
    }

    private List<VilkårMedPerioderDto> getVilkårV3(BehandlingUuidDto behandlingUuid, boolean inkluderVilkårkjøring) {
        var behandling = behandlingRepository.hentBehandling(behandlingUuid.getBehandlingUuid());
        var vilkåreneOpt = vilkårTjeneste.hentHvisEksisterer(behandling.getId());
        var dto = vilkåreneOpt.map(vilkårene -> VilkårDtoMapper.lagVilkarMedPeriodeDto(behandling, inkluderVilkårkjøring, vilkårene)).orElse(Collections.emptyList());
        return dto;
    }

    @GET
    @Path(VILKÅR_SAMLET_PATH)
    @Operation(description = "Hent informasjon om vilkår samlet for en behandling", tags = "vilkår", responses = {
        @ApiResponse(description = "Returnerer vilkår på behandling, tom liste hvis ikke eksisterer")
    })
    @BeskyttetRessurs(action = READ, resource = FAGSAK)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    @CacheControl()
    public VilkårResultatContainer getVilkårSamlet(@NotNull @QueryParam(BehandlingUuidDto.NAME) @Parameter(description = BehandlingUuidDto.DESC) @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) BehandlingUuidDto behandlingUuid) {
        var behandling = behandlingRepository.hentBehandling(behandlingUuid.getBehandlingUuid());
        var samletVilkårResultat = vilkårTjeneste.samletVilkårsresultat(behandling.getId());

        return new VilkårResultatContainer(samletVilkårResultat);
    }

    @Schema
    public static class VilkårResultatContainer {


        @JsonProperty(value = "vilkårTidslinje")
        @Valid
        private LocalDateTimeline<VilkårUtfallSamlet> vilkårTidslinje;

        public VilkårResultatContainer(LocalDateTimeline<VilkårUtfallSamlet> vilkårTidslinje) {
            this.vilkårTidslinje = vilkårTidslinje;
        }

    }
}
