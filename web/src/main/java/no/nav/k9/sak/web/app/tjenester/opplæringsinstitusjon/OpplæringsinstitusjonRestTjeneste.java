package no.nav.k9.sak.web.app.tjenester.opplæringsinstitusjon;

import static no.nav.k9.abac.BeskyttetRessursKoder.FAGSAK;
import static no.nav.k9.felles.sikkerhet.abac.BeskyttetRessursActionAttributt.READ;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import no.nav.k9.felles.sikkerhet.abac.BeskyttetRessurs;
import no.nav.k9.felles.sikkerhet.abac.TilpassetAbacAttributt;
import no.nav.k9.sak.typer.Periode;
import no.nav.k9.sak.web.server.abac.AbacAttributtSupplier;
import no.nav.k9.sak.ytelse.opplaeringspenger.repo.GodkjentInstitusjon;
import no.nav.k9.sak.ytelse.opplaeringspenger.repo.GodkjentInstitusjonRepository;

@ApplicationScoped
@Transactional
@Path("/opplæringsinstitusjon")
public class OpplæringsinstitusjonRestTjeneste {

    private GodkjentInstitusjonRepository godkjentInstitusjonRepository;

    public OpplæringsinstitusjonRestTjeneste() {
    }

    @Inject
    public OpplæringsinstitusjonRestTjeneste(GodkjentInstitusjonRepository godkjentInstitusjonRepository) {
        this.godkjentInstitusjonRepository = godkjentInstitusjonRepository;
    }

    @GET
    @Path("")
    @Operation(description = "Hent opplæringsinstitusjon med navn", tags = "opplæringsinstitusjon", responses = {
        @ApiResponse(responseCode = "200", description = "Returnerer opplæringsinstitusjon", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = GodkjentInstitusjonDto.class))),
        @ApiResponse(responseCode = "204", description = "Opplæringsinstitusjon ikke funnet")
    })
    @BeskyttetRessurs(action = READ, resource = FAGSAK)
    public Response hentMedNavn(@NotNull @QueryParam("navn") @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) String navn) {
        Optional<GodkjentInstitusjon> godkjentInstitusjon = godkjentInstitusjonRepository.hentMedNavn(navn);
        if (godkjentInstitusjon.isEmpty()) {
            return Response.noContent().build();
        }
        return Response.ok(mapTilDto(godkjentInstitusjon.get())).build();
    }

    @GET
    @Path("/aktiv")
    @Operation(description = "Hent aktiv opplæringsinstitusjon med navn", tags = "opplæringsinstitusjon", responses = {
        @ApiResponse(responseCode = "200", description = "Returnerer aktiv opplæringsinstitusjon", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = GodkjentInstitusjonDto.class))),
        @ApiResponse(responseCode = "204", description = "Opplæringsinstitusjon ikke funnet")
    })
    @BeskyttetRessurs(action = READ, resource = FAGSAK)
    public Response hentAktivMedNavn(@NotNull @QueryParam("navn") @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) String navn) {
        Optional<GodkjentInstitusjon> godkjentInstitusjon = godkjentInstitusjonRepository.hentMedNavn(navn);
        if (godkjentInstitusjon.isEmpty() || !erAktiv(godkjentInstitusjon.get())) {
            return Response.noContent().build();
        }
        return Response.ok(mapTilDto(godkjentInstitusjon.get())).build();
    }

    @GET
    @Path("/alle")
    @Operation(description = "Hent opplæringsinstitusjoner", tags = "opplæringsinstitusjon", responses = {
        @ApiResponse(responseCode = "200", description = "Returnerer opplæringsinstitusjoner", content = @Content(mediaType = MediaType.APPLICATION_JSON, array = @ArraySchema(uniqueItems = true, arraySchema = @Schema(implementation = List.class), schema = @Schema(implementation = GodkjentInstitusjonDto.class))))
    })
    @BeskyttetRessurs(action = READ, resource = FAGSAK)
    public Response hentAlle() {
        List<GodkjentInstitusjon> godkjenteInstitusjoner = godkjentInstitusjonRepository.hentAlle();
        return Response.ok(mapTilDto(godkjenteInstitusjoner)).build();
    }

    @GET
    @Path("/aktive")
    @Operation(description = "Hent aktive opplæringsinstitusjoner", tags = "opplæringsinstitusjon", responses = {
        @ApiResponse(responseCode = "200", description = "Returnerer aktive opplæringsinstitusjoner", content = @Content(mediaType = MediaType.APPLICATION_JSON, array = @ArraySchema(uniqueItems = true, arraySchema = @Schema(implementation = List.class), schema = @Schema(implementation = GodkjentInstitusjonDto.class))))
    })
    @BeskyttetRessurs(action = READ, resource = FAGSAK)
    public Response hentAktive() {
        List<GodkjentInstitusjon> godkjenteInstitusjoner = godkjentInstitusjonRepository.hentAlle();
        List<GodkjentInstitusjon> aktiveInstitusjoner = godkjenteInstitusjoner.stream()
            .filter(this::erAktiv)
            .collect(Collectors.toList());
        return Response.ok(mapTilDto(aktiveInstitusjoner)).build();
    }

    @GET
    @Path("/erAktiv")
    @Operation(description = "Sjekk om opplæringsinstitusjon er aktiv", tags = "opplæringsinstitusjon", responses = {
        @ApiResponse(responseCode = "200", description = "Returnerer om opplæringsinstitusjon er aktiv", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = Boolean.class)))
    })
    @BeskyttetRessurs(action = READ, resource = FAGSAK)
    public Boolean erAktiv(@NotNull @QueryParam("navn") @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) String navn) {
        Optional<GodkjentInstitusjon> godkjentInstitusjon = godkjentInstitusjonRepository.hentMedNavn(navn);
        return godkjentInstitusjon.isPresent() && erAktiv(godkjentInstitusjon.get());
    }

    private boolean erAktiv(GodkjentInstitusjon godkjentInstitusjon) {
        Periode aktivPeriode = new Periode(godkjentInstitusjon.getFomDato(), godkjentInstitusjon.getTomDato());
        Periode idag = new Periode(LocalDate.now(), LocalDate.now());
        return aktivPeriode.overlaps(idag);
    }

    private List<GodkjentInstitusjonDto> mapTilDto(List<GodkjentInstitusjon> godkjenteInstitusjoner) {
        return godkjenteInstitusjoner.stream().map(this::mapTilDto).collect(Collectors.toList());
    }
    private GodkjentInstitusjonDto mapTilDto(GodkjentInstitusjon godkjentInstitusjon) {
        return new GodkjentInstitusjonDto(godkjentInstitusjon.getNavn(), godkjentInstitusjon.getFomDato(), godkjentInstitusjon.getTomDato());
    }
}
