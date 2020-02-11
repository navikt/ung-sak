package no.nav.foreldrepenger.web.app.tjenester.behandling.vedtak;

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
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.web.server.abac.AbacAttributtSupplier;
import no.nav.k9.sak.kontrakt.behandling.BehandlingIdDto;
import no.nav.k9.sak.kontrakt.behandling.BehandlingUuidDto;
import no.nav.k9.sak.kontrakt.vedtak.TotrinnskontrollSkjermlenkeContextDto;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessurs;
import no.nav.vedtak.sikkerhet.abac.TilpassetAbacAttributt;

@Path("")
@Produces(MediaType.APPLICATION_JSON)
@ApplicationScoped
@Transactional
public class TotrinnskontrollRestTjeneste {

    public static final String ARSAKER_PATH = "/behandling/totrinnskontroll/arsaker";
    public static final String ARSAKER_READ_ONLY_PATH = "/behandling/totrinnskontroll/arsaker_read_only";
    public static final String BEKREFT_AKSJONSPUNKT_PATH = "/behandling/aksjonspunkt";

    private BehandlingRepository behandlingRepository;
    private TotrinnskontrollAksjonspunkterTjeneste totrinnskontrollTjeneste;

    public TotrinnskontrollRestTjeneste() {
        //
    }

    @Inject
    public TotrinnskontrollRestTjeneste(BehandlingRepository behandlingRepository, TotrinnskontrollAksjonspunkterTjeneste totrinnskontrollTjeneste) {
        this.behandlingRepository = behandlingRepository;
        this.totrinnskontrollTjeneste = totrinnskontrollTjeneste;
    }

    @POST
    @Path(ARSAKER_PATH)
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Hent aksjonspunkter som skal til totrinnskontroll.", tags = "totrinnskontroll", responses = {
            @ApiResponse(responseCode = "200", content = @Content(array = @ArraySchema(arraySchema = @Schema(implementation = List.class), schema = @Schema(implementation = TotrinnskontrollSkjermlenkeContextDto.class)), mediaType = MediaType.APPLICATION_JSON))
    })
    @BeskyttetRessurs(action = READ, ressurs = FAGSAK)
    @Deprecated
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public List<TotrinnskontrollSkjermlenkeContextDto> hentTotrinnskontrollSkjermlenkeContext(@NotNull @Parameter(description = "BehandlingId for aktuell behandling") @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) BehandlingIdDto behandlingIdDto) {
        Long behandlingId = behandlingIdDto.getBehandlingId();
        Behandling behandling = behandlingId != null
            ? behandlingRepository.hentBehandling(behandlingId)
            : behandlingRepository.hentBehandling(behandlingIdDto.getBehandlingUuid());
        return totrinnskontrollTjeneste.hentTotrinnsSkjermlenkeContext(behandling);
    }

    @POST
    @Path(ARSAKER_READ_ONLY_PATH)
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Hent aksjonspunkter som skal til totrinnskontroll.", tags = "totrinnskontroll", responses = {
            @ApiResponse(responseCode = "200", content = @Content(array = @ArraySchema(arraySchema = @Schema(implementation = List.class), schema = @Schema(implementation = TotrinnskontrollSkjermlenkeContextDto.class)), mediaType = MediaType.APPLICATION_JSON))
    })
    @BeskyttetRessurs(action = READ, ressurs = FAGSAK)
    @Deprecated
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public List<TotrinnskontrollSkjermlenkeContextDto> hentTotrinnskontrollvurderingSkjermlenkeContext(@NotNull @Parameter(description = "BehandlingId for aktuell behandling") @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) BehandlingIdDto behandlingIdDto) {
        Long behandlingId = behandlingIdDto.getBehandlingId();
        Behandling behandling = behandlingId != null
            ? behandlingRepository.hentBehandling(behandlingId)
            : behandlingRepository.hentBehandling(behandlingIdDto.getBehandlingUuid());
        return totrinnskontrollTjeneste.hentTotrinnsvurderingSkjermlenkeContext(behandling);
    }

    @GET
    @Path(ARSAKER_PATH)
    @Operation(description = "Hent aksjonspunkter som skal til totrinnskontroll.", tags = "totrinnskontroll", responses = {
            @ApiResponse(responseCode = "200", content = @Content(array = @ArraySchema(arraySchema = @Schema(implementation = List.class), schema = @Schema(implementation = TotrinnskontrollSkjermlenkeContextDto.class)), mediaType = MediaType.APPLICATION_JSON))
    })
    @BeskyttetRessurs(action = READ, ressurs = FAGSAK)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public List<TotrinnskontrollSkjermlenkeContextDto> hentTotrinnskontrollSkjermlenkeContext(@NotNull @QueryParam(BehandlingUuidDto.NAME) @Parameter(description = BehandlingUuidDto.DESC) @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) BehandlingUuidDto behandlingUuid) {
        var behandling = behandlingRepository.hentBehandling(behandlingUuid.getBehandlingUuid());
        return totrinnskontrollTjeneste.hentTotrinnsSkjermlenkeContext(behandling);
    }

    @GET
    @Path(ARSAKER_READ_ONLY_PATH)
    @Operation(description = "Hent aksjonspunkter som skal til totrinnskontroll.", tags = "totrinnskontroll", responses = {
            @ApiResponse(responseCode = "200", content = @Content(array = @ArraySchema(arraySchema = @Schema(implementation = List.class), schema = @Schema(implementation = TotrinnskontrollSkjermlenkeContextDto.class)), mediaType = MediaType.APPLICATION_JSON))
    })
    @BeskyttetRessurs(action = READ, ressurs = FAGSAK)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public List<TotrinnskontrollSkjermlenkeContextDto> hentTotrinnskontrollvurderingSkjermlenkeContext(@NotNull @QueryParam(BehandlingUuidDto.NAME) @Parameter(description = BehandlingUuidDto.DESC) @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) BehandlingUuidDto behandlingUuid) {
        var behandling = behandlingRepository.hentBehandling(behandlingUuid.getBehandlingUuid());
        return totrinnskontrollTjeneste.hentTotrinnsvurderingSkjermlenkeContext(behandling);
    }
}
