package no.nav.k9.sak.web.app.tjenester.behandling.død;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import no.nav.k9.felles.sikkerhet.abac.BeskyttetRessurs;
import no.nav.k9.felles.sikkerhet.abac.TilpassetAbacAttributt;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.kontrakt.behandling.BehandlingUuidDto;
import no.nav.k9.sak.kontrakt.død.VurderingRettPleiepengerVedDødDto;
import no.nav.k9.sak.web.server.abac.AbacAttributtSupplier;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.pleietrengende.død.RettPleiepengerVedDødRepository;

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
import jakarta.ws.rs.core.Response;

import static no.nav.k9.abac.BeskyttetRessursKoder.FAGSAK;
import static no.nav.k9.felles.sikkerhet.abac.BeskyttetRessursActionAttributt.READ;

@Produces(MediaType.APPLICATION_JSON)
@ApplicationScoped
@Path("")
@Transactional
public class RettVedDødRestTjeneste {

    public static final String BASEPATH = "/behandling/pleietrengende/dod";

    private RettPleiepengerVedDødRepository rettPleiepengerVedDødRepository;
    private BehandlingRepository behandlingRepository;

    RettVedDødRestTjeneste() {
        // CDI
    }

    @Inject
    RettVedDødRestTjeneste(RettPleiepengerVedDødRepository rettPleiepengerVedDødRepository, BehandlingRepository behandlingRepository) {
        this.rettPleiepengerVedDødRepository = rettPleiepengerVedDødRepository;
        this.behandlingRepository = behandlingRepository;
    }

    @GET
    @Operation(description = "Hent vurdering av rett til pleiepenger ved død",
        summary = "Hent rett pleiepenger ved død",
        tags = "død",
        responses = {
            @ApiResponse(
                responseCode = "200",
                description = "rett til pleiepenger ved død",
                content = @Content(
                    mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = VurderingRettPleiepengerVedDødDto.class)
                )
            ),
            @ApiResponse(
                responseCode = "204",
                description = "ingen rett til pleiepenger ved død vurdert"
            )
        }
    )
    @BeskyttetRessurs(action = READ, resource = FAGSAK)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    @Path(BASEPATH)
    public Response hentVurderingRettPleiepengerVedDød(
        @NotNull
        @QueryParam(BehandlingUuidDto.NAME)
        @Parameter(description = BehandlingUuidDto.DESC)
        @Valid
        @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class)
            BehandlingUuidDto behandlingUuidDto
    ) {
        var behandling = behandlingRepository.hentBehandling(behandlingUuidDto.getBehandlingUuid());
        var grunnlag = rettPleiepengerVedDødRepository.hentHvisEksisterer(behandling.getId());
        if (grunnlag.isPresent()) {
            var rettVedDød = grunnlag.get().getRettVedPleietrengendeDød();
            var responseDto = new VurderingRettPleiepengerVedDødDto(rettVedDød.getVurdering(), rettVedDød.getRettVedDødType());
            return Response.ok(responseDto).build();
        }
        return Response.noContent().build();
    }

}
