package no.nav.k9.sak.web.app.tjenester.behandling.omsorg;

import static no.nav.k9.abac.BeskyttetRessursKoder.FAGSAK;
import static no.nav.k9.felles.sikkerhet.abac.BeskyttetRessursActionAttributt.READ;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
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
import no.nav.k9.felles.sikkerhet.abac.BeskyttetRessurs;
import no.nav.k9.felles.sikkerhet.abac.TilpassetAbacAttributt;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.kontrakt.behandling.BehandlingUuidDto;
import no.nav.k9.sak.kontrakt.omsorg.OmsorgenForOversiktDto;
import no.nav.k9.sak.web.server.abac.AbacAttributtSupplier;
import no.nav.k9.sak.ytelse.pleiepengerbarn.inngangsvilkår.omsorgenfor.OmsorgenForDtoMapper;

@Produces(MediaType.APPLICATION_JSON)
@ApplicationScoped
@Path(OmsorgenForRestTjeneste.BASE_PATH)
@Transactional
public class OmsorgenForRestTjeneste {

    public static final String BASE_PATH = "/behandling/omsorg-for";
    public static final String OMSORGEN_FOR_OPPLYSNINGER_PATH = BASE_PATH;
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
    @Operation(description = "Hent omsorgen for opplysninger",
        summary = ("Returnerer informasjon saksbehandler har skrevet inn fra legeerklæring " +
            "og vurderinger vedrørende kontinuerlig tilsyn & pleie"),
        tags = "omsorgen-for",
        responses = {
            @ApiResponse(responseCode = "200",
                description = "null hvis ikke eksisterer",
                content = @Content(mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = OmsorgenForOversiktDto.class)))
        })
    @BeskyttetRessurs(action = READ, resource = FAGSAK)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public OmsorgenForOversiktDto hentOmsorgenForInformasjon(
            @NotNull @QueryParam(BehandlingUuidDto.NAME)
            @Parameter(description = BehandlingUuidDto.DESC)
            @Valid
            @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class)
            BehandlingUuidDto behandlingUuid) {
        final var behandling = behandlingRepository.hentBehandlingHvisFinnes(behandlingUuid.getBehandlingUuid());
        return behandling.map(it -> dtoMapper.map(it.getId(), it.getAktørId(), it.getFagsak().getPleietrengendeAktørId())).orElse(null);
    }
}
