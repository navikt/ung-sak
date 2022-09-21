package no.nav.k9.sak.web.app.tjenester.behandling.vedtak;

import static no.nav.k9.abac.BeskyttetRessursKoder.FAGSAK;
import static no.nav.k9.felles.sikkerhet.abac.BeskyttetRessursActionAttributt.READ;

import java.util.Optional;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import no.nav.k9.felles.sikkerhet.abac.BeskyttetRessurs;
import no.nav.k9.felles.sikkerhet.abac.TilpassetAbacAttributt;
import no.nav.k9.sak.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.k9.sak.behandlingslager.behandling.vedtak.BehandlingVedtakRepository;
import no.nav.k9.sak.kontrakt.behandling.BehandlingUuidDto;
import no.nav.k9.sak.kontrakt.vedtak.LosVedtaksdataDto;
import no.nav.k9.sak.web.server.abac.AbacAttributtSupplier;

@Path("")
@Produces(MediaType.APPLICATION_JSON)
@ApplicationScoped
class LosCallbackRestTjeneste {
    private BehandlingVedtakRepository behandlingVedtakRepository;

    @Inject
    LosCallbackRestTjeneste(BehandlingVedtakRepository behandlingVedtakRepository) {
        this.behandlingVedtakRepository = behandlingVedtakRepository;
    }

    @GET
    @Path("vedtak/loscallback")
    @Operation(description = "Callback fra k9-los for å hente vedtaksdato og -Id etter iverksetting av vedtak.", tags = "vedtak", responses = {
        @ApiResponse(responseCode = "200",
            description = "Returnerer tomt objekt hvis behandlingen ikke er vedtatt",
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = LosVedtaksdataDto.class)))
    })
    @BeskyttetRessurs(action = READ, resource = FAGSAK)
    public LosVedtaksdataDto hentLosVedtaksdata(
            @NotNull
            @QueryParam(BehandlingUuidDto.NAME)
            @Parameter(description = BehandlingUuidDto.DESC)
            @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class)
            BehandlingUuidDto behandlingUuid) {
        Optional<BehandlingVedtak> behandlingVedtak = behandlingVedtakRepository.hentBehandlingVedtakFor(behandlingUuid.getBehandlingUuid());
        return map(behandlingVedtak).orElse(null);
    }

    private Optional<LosVedtaksdataDto> map(Optional<BehandlingVedtak> behandlingVedtakOpt) {
        if (behandlingVedtakOpt.isPresent()) {
            BehandlingVedtak vedtak = behandlingVedtakOpt.get();
            return Optional.of(new LosVedtaksdataDto(vedtak.getVedtakstidspunkt(), vedtak.getId()));
        } else {
            return Optional.empty();
        }
    }
}
