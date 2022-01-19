package no.nav.k9.sak.web.app.tjenester.punsj;

import static no.nav.k9.abac.BeskyttetRessursKoder.FAGSAK;
import static no.nav.k9.felles.sikkerhet.abac.BeskyttetRessursActionAttributt.READ;

import java.util.Optional;

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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import no.nav.k9.felles.sikkerhet.abac.BeskyttetRessurs;
import no.nav.k9.felles.sikkerhet.abac.TilpassetAbacAttributt;
import no.nav.k9.sak.behandling.prosessering.BehandlingsprosessApplikasjonTjeneste;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.kontrakt.behandling.BehandlingUuidDto;
import no.nav.k9.sak.kontrakt.dokument.JournalpostIderDto;
import no.nav.k9.sak.punsj.PunsjRestKlient;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.web.server.abac.AbacAttributtSupplier;

@ApplicationScoped
@Transactional
@Path("/punsj")
@Produces(MediaType.APPLICATION_JSON)
public class PunsjRestTjeneste {

    private PunsjRestKlient klient;
    private BehandlingsprosessApplikasjonTjeneste behandlingsprosessTjeneste;

    public PunsjRestTjeneste() {
        // for CDI proxy
    }

    @Inject
    public PunsjRestTjeneste(PunsjRestKlient klient, BehandlingsprosessApplikasjonTjeneste behandlingsprosessTjeneste) {
        this.klient = klient;
        this.behandlingsprosessTjeneste = behandlingsprosessTjeneste;
    }

    @GET
    @Path("/journalpost/uferdig")
    @Operation(description = "Henter uferdig journalposter fra punsj for en gitt behandlingUuid", tags = "journalposter", responses = {
        @ApiResponse(responseCode = "200", description = "Returnerer en liste med uferdig journalpostIder som ligger i punsj på gitt behandlingUuid.",
            content = @Content(mediaType = MediaType.APPLICATION_JSON,
                schema = @Schema(implementation = JournalpostIderDto.class)))
    })
    @BeskyttetRessurs(action = READ, resource = FAGSAK)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public Response getUferdigJournalpostIderPrAktoer(@NotNull @QueryParam(BehandlingUuidDto.NAME) @Parameter(description = BehandlingUuidDto.DESC) @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) BehandlingUuidDto behandlingUuid) {
        Behandling behandling = behandlingsprosessTjeneste.hentBehandling(behandlingUuid.getBehandlingUuid());
        AktørId søker = behandling.getAktørId();
        AktørId barnet = behandling.getFagsak().getPleietrengendeAktørId();
        Optional<JournalpostIderDto> uferdigJournalpostIderPåAktør = klient.getUferdigJournalpostIderPåAktør(søker.getAktørId(), barnet.getAktørId());
        if (uferdigJournalpostIderPåAktør.isPresent()) {
            return Response.ok(uferdigJournalpostIderPåAktør.get()).build();
        }
        return Response.ok().build();
    }
}
