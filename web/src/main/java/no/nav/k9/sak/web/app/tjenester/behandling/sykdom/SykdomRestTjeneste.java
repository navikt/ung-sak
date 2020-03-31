package no.nav.k9.sak.web.app.tjenester.behandling.sykdom;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.kontrakt.behandling.BehandlingUuidDto;
import no.nav.k9.sak.kontrakt.medisinsk.SykdomsDto;
import no.nav.k9.sak.web.server.abac.AbacAttributtSupplier;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessurs;
import no.nav.vedtak.sikkerhet.abac.TilpassetAbacAttributt;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import static no.nav.k9.abac.BeskyttetRessursKoder.FAGSAK;
import static no.nav.vedtak.sikkerhet.abac.BeskyttetRessursActionAttributt.READ;

@Produces(MediaType.APPLICATION_JSON)
@ApplicationScoped
@Path(SykdomRestTjeneste.BASE_PATH)
@Transactional
public class SykdomRestTjeneste {

    public static final String BASE_PATH = "/behandling/sykdom";
    public static final String SYKDOMS_DTO = "";
    public static final String SYKDOMS_OPPLYSNINGER_PATH = BASE_PATH + SYKDOMS_DTO;
    private SykdomDtoMapper dtoMapper;
    private BehandlingRepository behandlingRepository;

    public SykdomRestTjeneste() {
    }

    @Inject
    public SykdomRestTjeneste(SykdomDtoMapper dtoMapper, BehandlingRepository behandlingRepository) {
        this.dtoMapper = dtoMapper;
        this.behandlingRepository = behandlingRepository;
    }

    @GET
    @Path(SYKDOMS_DTO)
    @Operation(description = "Hent sykdoms opplysninger",
        summary = ("Returnerer informasjon saksbehandler har skrevet inn fra legeerklæring " +
            "og vurderinger vedrørende kontinuerlig tilsyn & pleie"),
        tags = "sykdom",
        responses = {
            @ApiResponse(responseCode = "200",
                description = "null hvis ikke eksisterer",
                content = @Content(mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = SykdomsDto.class)))
        })
    @BeskyttetRessurs(action = READ, resource = FAGSAK)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public SykdomsDto hentSykdomsInformasjonFor(@NotNull @QueryParam(BehandlingUuidDto.NAME)
                                                @Parameter(description = BehandlingUuidDto.DESC)
                                                @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class)
                                                    BehandlingUuidDto behandlingUuid) {
        final var behandling = behandlingRepository.hentBehandlingHvisFinnes(behandlingUuid.getBehandlingUuid()).map(Behandling::getId);
        return behandling.map(behandlingId -> dtoMapper.map(behandlingId)).orElse(null);
    }
}
