package no.nav.k9.sak.web.app.tjenester.behandling.sykdom;

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
import javax.ws.rs.core.MediaType;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.kontrakt.behandling.BehandlingUuidDto;
import no.nav.k9.sak.kontrakt.sykdom.SykdomAksjonspunktDto;
import no.nav.k9.sak.web.server.abac.AbacAttributtSupplier;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomAksjonspunkt;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomVurderingService;
import no.nav.k9.felles.sikkerhet.abac.BeskyttetRessurs;
import no.nav.k9.felles.sikkerhet.abac.TilpassetAbacAttributt;

@Produces(MediaType.APPLICATION_JSON)
@ApplicationScoped
@Path(SykdomRestTjeneste.BASE_PATH)
@Transactional
public class SykdomRestTjeneste {

    static final String BASE_PATH = "/behandling/sykdom";
    private static final String SYKDOM_AKSJONSPUNKT = "/aksjonspunkt";
    public static final String SYKDOM_AKSJONSPUNKT_PATH = BASE_PATH + SYKDOM_AKSJONSPUNKT;

    //private SykdomDtoMapper dtoMapper;
    private SykdomVurderingService sykdomVurderingService;
    private BehandlingRepository behandlingRepository;

    public SykdomRestTjeneste() {
    }

    @Inject
    public SykdomRestTjeneste(SykdomVurderingService sykdomVurderingService, BehandlingRepository behandlingRepository) {
        this.sykdomVurderingService = sykdomVurderingService;
        this.behandlingRepository = behandlingRepository;
    }

    /*
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
    */

    @GET
    @Path(SYKDOM_AKSJONSPUNKT)
    @Operation(description = "Hent informasjon om sykdomsaksjonspunkt",
        summary = ("Henter informasjon om sykdomsaksjonspunkt"),
        tags = "sykdom",
        responses = {
            @ApiResponse(responseCode = "200",
                description = "Informasjon om sykdomsaksjonspunkt",
                content = @Content(mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = SykdomAksjonspunktDto.class)))
        })
    @BeskyttetRessurs(action = READ, resource = FAGSAK)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public SykdomAksjonspunktDto hentSykdomAksjonspunkt(@NotNull @QueryParam(BehandlingUuidDto.NAME)
                @Parameter(description = BehandlingUuidDto.DESC)
                @Valid
                @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class)
                BehandlingUuidDto behandlingUuid) {
        final var behandling = behandlingRepository.hentBehandlingHvisFinnes(behandlingUuid.getBehandlingUuid()).get();
        final var aksjonspunkt = sykdomVurderingService.vurderAksjonspunkt(behandling);

        return toSykdomAksjonspunktDto(aksjonspunkt);
    }

    private SykdomAksjonspunktDto toSykdomAksjonspunktDto(final SykdomAksjonspunkt aksjonspunkt) {
        return new SykdomAksjonspunktDto(aksjonspunkt.isKanLøseAksjonspunkt(),
                aksjonspunkt.isHarUklassifiserteDokumenter(),
                aksjonspunkt.isManglerDiagnosekode(),
                aksjonspunkt.isManglerGodkjentLegeerklæring(),
                aksjonspunkt.isManglerVurderingAvKontinuerligTilsynOgPleie(),
                aksjonspunkt.isManglerVurderingAvToOmsorgspersoner());
    }
}
