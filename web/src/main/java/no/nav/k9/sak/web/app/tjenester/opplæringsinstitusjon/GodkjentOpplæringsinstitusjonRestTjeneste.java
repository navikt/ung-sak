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
import no.nav.k9.sak.typer.Periode;
import no.nav.k9.sak.web.server.abac.AbacAttributtEmptySupplier;
import no.nav.k9.sak.ytelse.opplaeringspenger.repo.GodkjentOpplæringsinstitusjon;
import no.nav.k9.sak.ytelse.opplaeringspenger.repo.GodkjentOpplæringsinstitusjonRepository;

@ApplicationScoped
@Transactional
@Path("/opplæringsinstitusjon")
@Produces(MediaType.APPLICATION_JSON)
public class GodkjentOpplæringsinstitusjonRestTjeneste {

    private GodkjentOpplæringsinstitusjonRepository godkjentOpplæringsinstitusjonRepository;

    public GodkjentOpplæringsinstitusjonRestTjeneste() {
    }

    @Inject
    public GodkjentOpplæringsinstitusjonRestTjeneste(GodkjentOpplæringsinstitusjonRepository godkjentOpplæringsinstitusjonRepository) {
        this.godkjentOpplæringsinstitusjonRepository = godkjentOpplæringsinstitusjonRepository;
    }

    @GET
    @Operation(description = "Hent opplæringsinstitusjon med uuid", tags = "opplæringsinstitusjon", responses = {
        @ApiResponse(responseCode = "200", description = "Returnerer opplæringsinstitusjon", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = GodkjentOpplæringsinstitusjonDto.class))),
        @ApiResponse(responseCode = "204", description = "Opplæringsinstitusjon ikke funnet")
    })
    @BeskyttetRessurs(action = READ, resource = FAGSAK)
    public Response hentMedNavn(@NotNull @QueryParam("id") @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtEmptySupplier.class) GodkjentOpplæringsinstitusjonIdDto id) {
        Optional<GodkjentOpplæringsinstitusjon> godkjentInstitusjon = godkjentOpplæringsinstitusjonRepository.hentMedUuid(id.getUuid());
        if (godkjentInstitusjon.isEmpty()) {
            return Response.noContent().build();
        }
        return Response.ok(mapTilDto(godkjentInstitusjon.get())).build();
    }

    @GET
    @Path("/aktiv")
    @Operation(description = "Hent aktiv opplæringsinstitusjon med uuid", tags = "opplæringsinstitusjon", responses = {
        @ApiResponse(responseCode = "200", description = "Returnerer aktiv opplæringsinstitusjon", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = GodkjentOpplæringsinstitusjonDto.class))),
        @ApiResponse(responseCode = "204", description = "Opplæringsinstitusjon ikke funnet")
    })
    @BeskyttetRessurs(action = READ, resource = FAGSAK)
    public Response hentAktivMedNavn(@NotNull @QueryParam("id") @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtEmptySupplier.class) GodkjentOpplæringsinstitusjonIdDto id,
                                     @NotNull @QueryParam("aktivPeriode") @Parameter(description = "Format: YYYY-MM-DD/YYYY-MM-DD") @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtEmptySupplier.class) Periode aktivPeriode) {
        Optional<GodkjentOpplæringsinstitusjon> godkjentInstitusjon = godkjentOpplæringsinstitusjonRepository.hentMedUuid(id.getUuid());
        if (godkjentInstitusjon.isEmpty() || !erAktiv(godkjentInstitusjon.get(), aktivPeriode)) {
            return Response.noContent().build();
        }
        return Response.ok(mapTilDto(godkjentInstitusjon.get())).build();
    }

    @GET
    @Path("/alle")
    @Operation(description = "Hent opplæringsinstitusjoner", tags = "opplæringsinstitusjon", responses = {
        @ApiResponse(responseCode = "200", description = "Returnerer opplæringsinstitusjoner", content = @Content(mediaType = MediaType.APPLICATION_JSON, array = @ArraySchema(uniqueItems = true, arraySchema = @Schema(implementation = List.class), schema = @Schema(implementation = GodkjentOpplæringsinstitusjonDto.class))))
    })
    @BeskyttetRessurs(action = READ, resource = FAGSAK)
    public Response hentAlle() {
        List<GodkjentOpplæringsinstitusjon> godkjenteInstitusjoner = godkjentOpplæringsinstitusjonRepository.hentAlle();
        return Response.ok(mapTilDto(godkjenteInstitusjoner)).build();
    }

    @GET
    @Path("/aktive")
    @Operation(description = "Hent aktive opplæringsinstitusjoner", tags = "opplæringsinstitusjon", responses = {
        @ApiResponse(responseCode = "200", description = "Returnerer aktive opplæringsinstitusjoner", content = @Content(mediaType = MediaType.APPLICATION_JSON, array = @ArraySchema(uniqueItems = true, arraySchema = @Schema(implementation = List.class), schema = @Schema(implementation = GodkjentOpplæringsinstitusjonDto.class))))
    })
    @BeskyttetRessurs(action = READ, resource = FAGSAK)
    public Response hentAktive(@NotNull @QueryParam("aktivPeriode") @Parameter(description = "Format: YYYY-MM-DD/YYYY-MM-DD") @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtEmptySupplier.class) Periode aktivPeriode) {
        List<GodkjentOpplæringsinstitusjon> alle = godkjentOpplæringsinstitusjonRepository.hentAlle();
        List<GodkjentOpplæringsinstitusjon> aktive = alle.stream()
            .filter(godkjentOpplæringsinstitusjon -> erAktiv(godkjentOpplæringsinstitusjon, aktivPeriode))
            .collect(Collectors.toList());
        return Response.ok(mapTilDto(aktive)).build();
    }

    @GET
    @Path("/erAktiv")
    @Operation(description = "Sjekk om opplæringsinstitusjon er aktiv", tags = "opplæringsinstitusjon", responses = {
        @ApiResponse(responseCode = "200", description = "Returnerer om opplæringsinstitusjon er aktiv", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = Boolean.class)))
    })
    @BeskyttetRessurs(action = READ, resource = FAGSAK)
    public Boolean erAktiv(@NotNull @QueryParam("id") @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtEmptySupplier.class) GodkjentOpplæringsinstitusjonIdDto id,
                           @NotNull @QueryParam("aktivPeriode") @Parameter(description = "Format: YYYY-MM-DD/YYYY-MM-DD") @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtEmptySupplier.class) Periode aktivPeriode) {
        Optional<GodkjentOpplæringsinstitusjon> godkjentInstitusjon = godkjentOpplæringsinstitusjonRepository.hentMedUuid(id.getUuid());
        return godkjentInstitusjon.isPresent() && erAktiv(godkjentInstitusjon.get(), aktivPeriode);
    }

    private boolean erAktiv(GodkjentOpplæringsinstitusjon godkjentOpplæringsInstitusjon, Periode periode) {
        Periode aktivPeriode = new Periode(godkjentOpplæringsInstitusjon.getFomDato(), godkjentOpplæringsInstitusjon.getTomDato());
        return aktivPeriode.overlaps(periode);
    }

    private List<GodkjentOpplæringsinstitusjonDto> mapTilDto(List<GodkjentOpplæringsinstitusjon> godkjenteOpplæringsinstitusjoner) {
        return godkjenteOpplæringsinstitusjoner.stream().map(this::mapTilDto).collect(Collectors.toList());
    }
    private GodkjentOpplæringsinstitusjonDto mapTilDto(GodkjentOpplæringsinstitusjon godkjentOpplæringsInstitusjon) {
        return new GodkjentOpplæringsinstitusjonDto(godkjentOpplæringsInstitusjon.getUuid(), godkjentOpplæringsInstitusjon.getNavn(), godkjentOpplæringsInstitusjon.getFomDato(), godkjentOpplæringsInstitusjon.getTomDato());
    }
}
