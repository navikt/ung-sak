package no.nav.k9.sak.web.app.tjenester.behandling.omsorg;

import static no.nav.vedtak.sikkerhet.abac.BeskyttetRessursActionAttributt.READ;
import static no.nav.vedtak.sikkerhet.abac.BeskyttetRessursResourceAttributt.FAGSAK;

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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.kontrakt.behandling.BehandlingUuidDto;
import no.nav.k9.sak.kontrakt.medisinsk.OmsorgenForDto;
import no.nav.k9.sak.kontrakt.medisinsk.SykdomsDto;
import no.nav.k9.sak.web.server.abac.AbacAttributtSupplier;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessurs;
import no.nav.vedtak.sikkerhet.abac.TilpassetAbacAttributt;

@Produces(MediaType.APPLICATION_JSON)
@ApplicationScoped
@Path(OmsorgenForRestTjeneste.BASE_PATH)
@Transactional
public class OmsorgenForRestTjeneste {

    public static final String BASE_PATH = "/behandling/omsorg-for";
    public static final String OMSORGEN_FOR_OPPLYSNINGER = "";
    public static final String OMSORGEN_FOR_OPPLYSNINGER_PATH = BASE_PATH + OMSORGEN_FOR_OPPLYSNINGER;
    private OmsorgenForDtoMapper dtoMapper;
    private BehandlingRepository behandlingRepository;

    public OmsorgenForRestTjeneste() {
    }

    @Inject
    public OmsorgenForRestTjeneste(OmsorgenForDtoMapper dtoMapper, BehandlingRepository behandlingRepository) {
        this.dtoMapper = dtoMapper;
        this.behandlingRepository = behandlingRepository;
    }

    @GET
    @Path(OMSORGEN_FOR_OPPLYSNINGER)
    @Operation(description = "Hent omsorgen for opplysninger",
        summary = ("Returnerer informasjon saksbehandler har skrevet inn fra legeerklæring " +
            "og vurderinger vedrørende kontinuerlig tilsyn & pleie"),
        tags = "omsorgen-for",
        responses = {
            @ApiResponse(responseCode = "200",
                description = "null hvis ikke eksisterer",
                content = @Content(mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = SykdomsDto.class)))
        })
    @BeskyttetRessurs(action = READ, ressurs = FAGSAK)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public OmsorgenForDto hentSykdomsInformasjonFor(@NotNull @QueryParam(BehandlingUuidDto.NAME)
                                                    @Parameter(description = BehandlingUuidDto.DESC)
                                                    @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class)
                                                        BehandlingUuidDto behandlingUuid) {
        final var behandling = behandlingRepository.hentBehandlingHvisFinnes(behandlingUuid.getBehandlingUuid());
        return behandling.map(it -> dtoMapper.map(it.getId(), it.getAktørId())).orElse(null);
    }
}
