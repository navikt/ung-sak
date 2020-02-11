package no.nav.foreldrepenger.web.app.tjenester.behandling.kontroll;

import static no.nav.vedtak.sikkerhet.abac.BeskyttetRessursActionAttributt.READ;
import static no.nav.vedtak.sikkerhet.abac.BeskyttetRessursResourceAttributt.FAGSAK;

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
import javax.ws.rs.core.MediaType;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.web.server.abac.AbacAttributtSupplier;
import no.nav.k9.sak.kontrakt.behandling.BehandlingIdDto;
import no.nav.k9.sak.kontrakt.behandling.BehandlingUuidDto;
import no.nav.k9.sak.kontrakt.kontroll.KontrollresultatDto;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessurs;
import no.nav.vedtak.sikkerhet.abac.TilpassetAbacAttributt;

@Path("")
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
@Transactional
public class KontrollRestTjeneste {

    public static final String KONTROLLRESULTAT_PATH = "/behandling/kontrollresultat";
    public static final String KONTROLLRESULTAT_V2_PATH = "/behandling/kontrollresultat/resultat";
    private KontrollDtoTjeneste kontrollDtoTjeneste;
    private BehandlingRepository behandlingRepository;

    public KontrollRestTjeneste() {
        // resteasy
    }

    @Inject
    public KontrollRestTjeneste(KontrollDtoTjeneste kontrollDtoTjeneste, BehandlingRepository behandlingRepository) {
        this.kontrollDtoTjeneste = kontrollDtoTjeneste;
        this.behandlingRepository = behandlingRepository;
    }

    @GET
    @Path(KONTROLLRESULTAT_PATH)
    @Operation(description = "Hent kontrollresultatet for en behandling", tags = "kontroll", responses = {
            @ApiResponse(responseCode = "200", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = KontrollresultatDto.class)))
    })
    @BeskyttetRessurs(action = READ, ressurs = FAGSAK)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public KontrollresultatDto hentKontrollresultat(@NotNull @QueryParam(BehandlingUuidDto.NAME) @Parameter(description = BehandlingUuidDto.DESC) @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) BehandlingUuidDto behandlingUuid) {
        Behandling behandling = behandlingRepository.hentBehandling(behandlingUuid.getBehandlingUuid());
        return kontrollDtoTjeneste.lagKontrollresultatForBehandling(behandling).orElse(null);
    }

    @GET
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Hent kontrollresultatet for en behandling", tags = "kontroll", responses = {
            @ApiResponse(responseCode = "200", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = KontrollresultatDto.class)))
    })
    @BeskyttetRessurs(action = READ, ressurs = FAGSAK)
    @Path(KONTROLLRESULTAT_V2_PATH)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public KontrollresultatDto hentKontrollresultatV2(@NotNull @QueryParam("behandlingId") @Parameter(description = "BehandlingId for aktuell behandling") @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) BehandlingIdDto behandlingIdDto) {
        Long behandlingId = behandlingIdDto.getBehandlingId();
        Behandling behandling = behandlingId != null
            ? behandlingRepository.hentBehandling(behandlingId)
            : behandlingRepository.hentBehandling(behandlingIdDto.getBehandlingUuid());

        return kontrollDtoTjeneste.lagKontrollresultatForBehandling(behandling)
            .orElse(null);
    }

    @GET
    @Operation(description = "Hent kontrollresultatet for en behandling", tags = "kontroll", responses = {
            @ApiResponse(responseCode = "200", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = KontrollresultatDto.class)))
    })
    @BeskyttetRessurs(action = READ, ressurs = FAGSAK)
    @Path(KONTROLLRESULTAT_V2_PATH)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public KontrollresultatDto hentKontrollresultatV2(@NotNull @QueryParam(BehandlingUuidDto.NAME) @Parameter(description = BehandlingUuidDto.DESC) @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) BehandlingUuidDto behandlingUuid) {
        Behandling behandling = behandlingRepository.hentBehandling(behandlingUuid.getBehandlingUuid());
        return kontrollDtoTjeneste.lagKontrollresultatForBehandling(behandling).orElse(null);
    }

}
