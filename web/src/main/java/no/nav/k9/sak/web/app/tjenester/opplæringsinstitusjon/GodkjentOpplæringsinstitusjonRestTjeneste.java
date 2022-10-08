package no.nav.k9.sak.web.app.tjenester.opplæringsinstitusjon;

import static no.nav.k9.abac.BeskyttetRessursKoder.FAGSAK;
import static no.nav.k9.felles.sikkerhet.abac.BeskyttetRessursActionAttributt.READ;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
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
import jakarta.ws.rs.core.Response;
import no.nav.k9.felles.sikkerhet.abac.BeskyttetRessurs;
import no.nav.k9.felles.sikkerhet.abac.TilpassetAbacAttributt;
import no.nav.k9.sak.kontrakt.opplæringspenger.GodkjentOpplæringsinstitusjonDto;
import no.nav.k9.sak.kontrakt.opplæringspenger.GodkjentOpplæringsinstitusjonIdDto;
import no.nav.k9.sak.typer.Periode;
import no.nav.k9.sak.web.server.abac.AbacAttributtEmptySupplier;
import no.nav.k9.sak.ytelse.opplaeringspenger.inngangsvilkår.nødvendighet.GodkjentOpplæringsinstitusjonTjeneste;
import no.nav.k9.sak.ytelse.opplaeringspenger.repo.GodkjentOpplæringsinstitusjon;

@ApplicationScoped
@Transactional
@Path("/opplæringsinstitusjon")
@Produces(MediaType.APPLICATION_JSON)
public class GodkjentOpplæringsinstitusjonRestTjeneste {

    private GodkjentOpplæringsinstitusjonTjeneste godkjentOpplæringsinstitusjonTjeneste;

    public GodkjentOpplæringsinstitusjonRestTjeneste() {
    }

    @Inject
    public GodkjentOpplæringsinstitusjonRestTjeneste(GodkjentOpplæringsinstitusjonTjeneste godkjentOpplæringsinstitusjonTjeneste) {
        this.godkjentOpplæringsinstitusjonTjeneste = godkjentOpplæringsinstitusjonTjeneste;
    }

    @GET
    @Operation(description = "Hent opplæringsinstitusjon med uuid", tags = "opplæringsinstitusjon", responses = {
        @ApiResponse(responseCode = "200", description = "Returnerer opplæringsinstitusjon", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = GodkjentOpplæringsinstitusjonDto.class))),
        @ApiResponse(responseCode = "204", description = "Opplæringsinstitusjon ikke funnet")
    })
    @BeskyttetRessurs(action = READ, resource = FAGSAK)
    public Response hentMedUuid(@NotNull @QueryParam("id") @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtEmptySupplier.class) GodkjentOpplæringsinstitusjonIdDto id) {
        Optional<GodkjentOpplæringsinstitusjon> institusjon = godkjentOpplæringsinstitusjonTjeneste.hentMedUuid(id.getUuid());
        if (institusjon.isPresent()) {
            return Response.ok(mapTilDto(institusjon.get())).build();
        }
        return Response.noContent().build();
    }

    @GET
    @Path("/aktiv")
    @Operation(description = "Hent aktiv opplæringsinstitusjon med uuid for oppgitt periode", tags = "opplæringsinstitusjon", responses = {
        @ApiResponse(responseCode = "200", description = "Returnerer aktiv opplæringsinstitusjon", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = GodkjentOpplæringsinstitusjonDto.class))),
        @ApiResponse(responseCode = "204", description = "Opplæringsinstitusjon ikke funnet")
    })
    @BeskyttetRessurs(action = READ, resource = FAGSAK)
    public Response hentAktivMedUuid(@NotNull @QueryParam("id") @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtEmptySupplier.class) GodkjentOpplæringsinstitusjonIdDto id,
                                     @NotNull @QueryParam("aktivPeriode") @Parameter(description = "Format: YYYY-MM-DD/YYYY-MM-DD") @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtEmptySupplier.class) Periode aktivPeriode) {
        Optional<GodkjentOpplæringsinstitusjon> aktivInstitusjon = godkjentOpplæringsinstitusjonTjeneste.hentAktivMedUuid(id.getUuid(), aktivPeriode);
        if (aktivInstitusjon.isPresent()) {
            return Response.ok(mapTilDto(aktivInstitusjon.get())).build();
        }
        return Response.noContent().build();
    }

    @GET
    @Path("/alle")
    @Operation(description = "Hent opplæringsinstitusjoner", tags = "opplæringsinstitusjon", responses = {
        @ApiResponse(responseCode = "200", description = "Returnerer opplæringsinstitusjoner", content = @Content(mediaType = MediaType.APPLICATION_JSON, array = @ArraySchema(uniqueItems = true, arraySchema = @Schema(implementation = List.class), schema = @Schema(implementation = GodkjentOpplæringsinstitusjonDto.class))))
    })
    @BeskyttetRessurs(action = READ, resource = FAGSAK)
    public Response hentAlle() {
        List<GodkjentOpplæringsinstitusjon> alle = godkjentOpplæringsinstitusjonTjeneste.hentAlle();
        return Response.ok(mapTilDto(alle)).build();
    }

    @GET
    @Path("/aktive")
    @Operation(description = "Hent aktive opplæringsinstitusjoner for oppgitt periode", tags = "opplæringsinstitusjon", responses = {
        @ApiResponse(responseCode = "200", description = "Returnerer aktive opplæringsinstitusjoner for oppgitt periode", content = @Content(mediaType = MediaType.APPLICATION_JSON, array = @ArraySchema(uniqueItems = true, arraySchema = @Schema(implementation = List.class), schema = @Schema(implementation = GodkjentOpplæringsinstitusjonDto.class))))
    })
    @BeskyttetRessurs(action = READ, resource = FAGSAK)
    public Response hentAktive(@NotNull @QueryParam("aktivPeriode") @Parameter(description = "Format: YYYY-MM-DD/YYYY-MM-DD") @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtEmptySupplier.class) Periode aktivPeriode) {
        List<GodkjentOpplæringsinstitusjon> aktive = godkjentOpplæringsinstitusjonTjeneste.hentAktive(aktivPeriode);
        return Response.ok(mapTilDto(aktive)).build();
    }

    @GET
    @Path("/erAktiv")
    @Operation(description = "Sjekk om opplæringsinstitusjon er aktiv i oppgitt periode", tags = "opplæringsinstitusjon", responses = {
        @ApiResponse(responseCode = "200", description = "Returnerer om opplæringsinstitusjon er aktiv i oppgitt periode", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = Boolean.class)))
    })
    @BeskyttetRessurs(action = READ, resource = FAGSAK)
    public Boolean erAktiv(@NotNull @QueryParam("id") @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtEmptySupplier.class) GodkjentOpplæringsinstitusjonIdDto id,
                           @NotNull @QueryParam("aktivPeriode") @Parameter(description = "Format: YYYY-MM-DD/YYYY-MM-DD") @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtEmptySupplier.class) Periode aktivPeriode) {
        Optional<GodkjentOpplæringsinstitusjon> aktivInstitusjon = godkjentOpplæringsinstitusjonTjeneste.hentAktivMedUuid(id.getUuid(), aktivPeriode);
        return aktivInstitusjon.isPresent();
    }

    private List<GodkjentOpplæringsinstitusjonDto> mapTilDto(List<GodkjentOpplæringsinstitusjon> godkjenteOpplæringsinstitusjoner) {
        return godkjenteOpplæringsinstitusjoner.stream().map(this::mapTilDto).collect(Collectors.toList());
    }

    private GodkjentOpplæringsinstitusjonDto mapTilDto(GodkjentOpplæringsinstitusjon godkjentOpplæringsInstitusjon) {
        return new GodkjentOpplæringsinstitusjonDto(godkjentOpplæringsInstitusjon.getUuid(), godkjentOpplæringsInstitusjon.getNavn(), godkjentOpplæringsInstitusjon.getFomDato(), godkjentOpplæringsInstitusjon.getTomDato());
    }
}
