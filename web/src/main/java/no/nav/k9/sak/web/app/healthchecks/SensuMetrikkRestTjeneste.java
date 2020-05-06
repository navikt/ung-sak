package no.nav.k9.sak.web.app.healthchecks;

import static no.nav.vedtak.sikkerhet.abac.BeskyttetRessursActionAttributt.READ;
import static no.nav.vedtak.sikkerhet.abac.BeskyttetRessursResourceAttributt.DRIFT;

import java.time.LocalDate;
import java.util.List;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import no.nav.k9.sak.metrikker.StatistikkRepository;
import no.nav.k9.sak.web.server.abac.AbacAttributtEmptySupplier;
import no.nav.vedtak.felles.integrasjon.sensu.SensuEvent;
import no.nav.vedtak.felles.integrasjon.sensu.SensuEvent.SensuRequest;
import no.nav.vedtak.felles.integrasjon.sensu.SensuKlient;
import no.nav.vedtak.felles.prosesstask.rest.dto.ProsessTaskDataDto;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessurs;
import no.nav.vedtak.sikkerhet.abac.TilpassetAbacAttributt;

@OpenAPIDefinition(tags = @Tag(name = "prosesstask", description = "Håndtering av asynkrone oppgaver i form av prosesstask"))
@Path("/sensu")
@RequestScoped
@Transactional
public class SensuMetrikkRestTjeneste {

    private SensuKlient sensuKlient;

    private StatistikkRepository statistikkRepository;

    public SensuMetrikkRestTjeneste() {
        // REST CDI
    }

    @Inject
    public SensuMetrikkRestTjeneste(SensuKlient sensuKlient, StatistikkRepository statistikkRepository) {
        this.sensuKlient = sensuKlient;
        this.statistikkRepository = statistikkRepository;
    }

    @POST
    @Path("/statistikk")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Hent SensuEvent statistikk for alt", tags = "sensu", responses = {
            @ApiResponse(responseCode = "200", description = "Liste over SensuEvents (tilsvarer hva som publiseres av task)", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProsessTaskDataDto.class)))
    })
    @BeskyttetRessurs(action = READ, ressurs = DRIFT)
    public SensuRequest hentStatistikk(@Parameter(description = "Angitt dato for fagsak opprettet") @TilpassetAbacAttributt(supplierClass = AbacAttributtEmptySupplier.class) @Valid LocalDate fagsakOpprettetDato) {
        List<SensuEvent> events = statistikkRepository.hentAlle(fagsakOpprettetDato);
        return SensuEvent.createBatchSensuRequest(events);
    }

    @POST
    @Path("/publiser")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Publiserer SensuEvents direkte til sensuklient. Brukes til å korrigere grafan boards with endring av tidsserie/tellemåte. (USE WITH CAUTION)", tags = "sensu")
    @BeskyttetRessurs(action = READ, ressurs = DRIFT)
    public Response finnProsessTaskInkludertPayload(@NotNull @Parameter(description = "Publiserer SensuEvents direkte.") @TilpassetAbacAttributt(supplierClass = AbacAttributtEmptySupplier.class) @Valid SensuRequest sensuRequest) {
        sensuKlient.logMetrics(sensuRequest);
        return Response.ok().build();
    }

}
