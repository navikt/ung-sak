package no.nav.k9.sak.web.app.tjenester.behandling.sykdom;

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
import no.nav.k9.sak.kontrakt.sykdom.SykdomAksjonspunktDto;
import no.nav.k9.sak.web.server.abac.AbacAttributtSupplier;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomAksjonspunkt;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomVurderingService;

@Produces(MediaType.APPLICATION_JSON)
@ApplicationScoped
@Path(SykdomRestTjeneste.BASE_PATH)
@Transactional
public class SykdomRestTjeneste {

    static final String BASE_PATH = "/behandling/sykdom";
    private static final String SYKDOM_AKSJONSPUNKT = "/aksjonspunkt";
    public static final String SYKDOM_AKSJONSPUNKT_PATH = BASE_PATH + SYKDOM_AKSJONSPUNKT;

    private SykdomVurderingService sykdomVurderingService;
    private BehandlingRepository behandlingRepository;

    public SykdomRestTjeneste() {
    }

    @Inject
    public SykdomRestTjeneste(SykdomVurderingService sykdomVurderingService, BehandlingRepository behandlingRepository) {
        this.sykdomVurderingService = sykdomVurderingService;
        this.behandlingRepository = behandlingRepository;
    }

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
            aksjonspunkt.isManglerVurderingAvToOmsorgspersoner(),
            aksjonspunkt.isManglerVurderingAvILivetsSluttfase(),
            aksjonspunkt.isHarDataSomIkkeHarBlittTattMedIBehandling(),
            aksjonspunkt.isNyttDokumentHarIkkekontrollertEksisterendeVurderinger());
    }
}
