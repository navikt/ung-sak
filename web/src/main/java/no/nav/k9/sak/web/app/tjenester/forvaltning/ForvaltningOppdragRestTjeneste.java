package no.nav.k9.sak.web.app.tjenester.forvaltning;


import static no.nav.k9.abac.BeskyttetRessursKoder.FAGSAK;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import no.nav.k9.oppdrag.kontrakt.tilkjentytelse.TilkjentYtelseOppdrag;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.kontrakt.behandling.BehandlingIdDto;
import no.nav.k9.sak.web.server.abac.AbacAttributtSupplier;
import no.nav.k9.sak.økonomi.tilkjentytelse.TilkjentYtelseTjeneste;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessurs;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessursActionAttributt;
import no.nav.vedtak.sikkerhet.abac.TilpassetAbacAttributt;


/**
 * DENNE TJENESTEN ER BARE FOR MIDLERTIDIG BEHOV, OG SKAL AVVIKLES SÅ RASKT SOM MULIG.
 * ENDRINGER I DENNE KLASSEN SKAL KLARERES OG KODE-REVIEWES MED ANSVARLIG APPLIKASJONSARKITEKT.
 */
@Path("/oppdrag/forvaltning")
@ApplicationScoped
@Transactional
public class ForvaltningOppdragRestTjeneste {

    private static final String JSON_UTF8 = "application/json; charset=UTF-8";

    private TilkjentYtelseTjeneste tilkjentYtelseTjeneste;
    private BehandlingRepository behandlingRepository;

    public ForvaltningOppdragRestTjeneste() {
        // For Rest-CDI
    }

    @Inject
    public ForvaltningOppdragRestTjeneste(TilkjentYtelseTjeneste tilkjentYtelseTjeneste, BehandlingRepository behandlingRepository) {
        this.tilkjentYtelseTjeneste = tilkjentYtelseTjeneste;
        this.behandlingRepository = behandlingRepository;
    }

    @GET
    @Path("hent-iverksetting-data")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Henter data som k9-sak sender til k9-oppdrag ved iverksetting", summary = ("Henter data som k9-sak sender til k9-oppdrag ved iverksetting"), tags = "oppdrag", responses = {
        @ApiResponse(responseCode = "200", description = "Returnerer data som k9-sak sender tli k9-oppdrag ved iverksetting", content = {
            @Content(mediaType = "application/json", schema = @Schema(implementation = TilkjentYtelseOppdrag.class))
        })})
    @Produces(JSON_UTF8)
    @BeskyttetRessurs(action = BeskyttetRessursActionAttributt.READ, resource = FAGSAK)
    public TilkjentYtelseOppdrag hentIverksettingData(@NotNull @QueryParam("behandlingId") @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) BehandlingIdDto behandlingIdDto) {
        Behandling behandling = behandlingRepository.hentBehandling(behandlingIdDto.getId());
        return tilkjentYtelseTjeneste.hentTilkjentYtelseOppdrag(behandling);
    }

}
