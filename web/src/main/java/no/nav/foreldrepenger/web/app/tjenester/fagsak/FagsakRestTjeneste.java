package no.nav.foreldrepenger.web.app.tjenester.fagsak;

import static no.nav.vedtak.sikkerhet.abac.BeskyttetRessursActionAttributt.READ;
import static no.nav.vedtak.sikkerhet.abac.BeskyttetRessursResourceAttributt.FAGSAK;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import no.nav.foreldrepenger.behandling.revurdering.RevurderingTjeneste;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.aktør.Personinfo;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.web.app.rest.Redirect;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.ProsessTaskGruppeIdDto;
import no.nav.foreldrepenger.web.server.abac.AbacAttributtSupplier;
import no.nav.k9.sak.kontrakt.AsyncPollingStatus;
import no.nav.k9.sak.kontrakt.behandling.FagsakDto;
import no.nav.k9.sak.kontrakt.behandling.SaksnummerDto;
import no.nav.k9.sak.kontrakt.person.PersonDto;
import no.nav.k9.sak.kontrakt.produksjonstyring.SøkeSakEllerBrukerDto;
import no.nav.k9.sak.typer.Saksnummer;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessurs;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessursActionAttributt;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessursResourceAttributt;
import no.nav.vedtak.sikkerhet.abac.TilpassetAbacAttributt;

@Path("")
@ApplicationScoped
@Transactional
public class FagsakRestTjeneste {

    public static final String PATH = "/fagsak";
    public static final String STATUS_PATH = "/fagsak/status";
    public static final String SOK_PATH = "/fagsak/sok";
    private FagsakApplikasjonTjeneste fagsakApplikasjonTjeneste;

    public FagsakRestTjeneste() {
        // For Rest-CDI
    }

    @Inject
    public FagsakRestTjeneste(FagsakApplikasjonTjeneste fagsakApplikasjonTjeneste) {
        this.fagsakApplikasjonTjeneste = fagsakApplikasjonTjeneste;
    }

    @GET
    @Path(STATUS_PATH)
    @Operation(description = "Url for å polle på fagsak mens behandlingprosessen pågår i bakgrunnen(asynkront)", summary = "Returnerer link til enten samme (hvis ikke ferdig) eller redirecter til /fagsak dersom asynkrone operasjoner er ferdig.", tags = "fagsak", responses = {
            @ApiResponse(responseCode = "200", description = "Returnerer Status", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = AsyncPollingStatus.class))),
            @ApiResponse(responseCode = "303", description = "Pågående prosesstasks avsluttet", headers = @Header(name = HttpHeaders.LOCATION)),
            @ApiResponse(responseCode = "418", description = "ProsessTasks har feilet", headers = @Header(name = HttpHeaders.LOCATION), content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = AsyncPollingStatus.class)))
    })
    @BeskyttetRessurs(action = READ, ressurs = FAGSAK)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public Response hentFagsakMidlertidigStatus(@NotNull @QueryParam("saksnummer") @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) SaksnummerDto idDto,
                                                @QueryParam("gruppe") @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) ProsessTaskGruppeIdDto gruppeDto)
            throws URISyntaxException {
        Saksnummer saksnummer = idDto.getVerdi();
        String gruppe = gruppeDto == null ? null : gruppeDto.getGruppe();
        Optional<AsyncPollingStatus> prosessTaskGruppePågår = fagsakApplikasjonTjeneste.sjekkProsessTaskPågår(saksnummer, gruppe);
        return Redirect.tilFagsakEllerPollStatus(saksnummer, prosessTaskGruppePågår.orElse(null));
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path(PATH)
    @Operation(description = "Hent fagsak for saksnummer", tags = "fagsak", responses = {
            @ApiResponse(responseCode = "200", description = "Returnerer fagsak", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = FagsakDto.class))),
            @ApiResponse(responseCode = "404", description = "Fagsak ikke tilgjengelig")
    })
    @BeskyttetRessurs(action = READ, ressurs = FAGSAK)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public Response hentFagsak(@NotNull @QueryParam("saksnummer") @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) SaksnummerDto s) {

        Saksnummer saksnummer = s.getVerdi();
        FagsakSamlingForBruker view = fagsakApplikasjonTjeneste.hentFagsakForSaksnummer(saksnummer);
        List<FagsakDto> list = tilDtoer(view);
        if (list.isEmpty()) {
            // return 403 Forbidden istdf 404 Not Found (sikkerhet - ikke avslør for mye)
            return Response.status(Response.Status.FORBIDDEN).build();
        } else if (list.size() == 1) {
            return Response.ok(list.get(0)).build();
        } else {
            throw new IllegalStateException(
                "Utvikler-feil: fant mer enn en fagsak for saksnummer [" + saksnummer + "], skal ikke være mulig: fant " + list.size());
        }
    }

    @POST
    @Path(SOK_PATH)
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Søk etter saker på saksnummer eller fødselsnummer", tags = "fagsak", summary = ("Spesifikke saker kan søkes via saksnummer. " +
        "Oversikt over saker knyttet til en bruker kan søkes via fødselsnummer eller d-nummer."))
    @BeskyttetRessurs(action = BeskyttetRessursActionAttributt.READ, ressurs = BeskyttetRessursResourceAttributt.FAGSAK)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public List<FagsakDto> søkFagsaker(@Parameter(description = "Søkestreng kan være saksnummer, fødselsnummer eller D-nummer.") @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) SøkeSakEllerBrukerDto søkestreng) {
        FagsakSamlingForBruker view = fagsakApplikasjonTjeneste.hentSaker(søkestreng.getSearchString());
        return tilDtoer(view);
    }

    private List<FagsakDto> tilDtoer(FagsakSamlingForBruker view) {
        if (view.isEmpty()) {
            return new ArrayList<>();
        }
        Personinfo brukerInfo = view.getBrukerInfo();

        PersonDto personDto = new PersonDto(brukerInfo.getNavn(), brukerInfo.getAlder(), String.valueOf(brukerInfo.getPersonIdent().getIdent()),
            brukerInfo.erKvinne(), brukerInfo.getPersonstatus(), brukerInfo.getDiskresjonskode(), brukerInfo.getDødsdato());

        List<FagsakDto> dtoer = new ArrayList<>();
        for (var info : view.getFagsakInfoer()) {
            Fagsak fagsak = info.getFagsak();
            Boolean kanRevurderingOpprettes = FagsakYtelseTypeRef.Lookup.find(RevurderingTjeneste.class, fagsak.getYtelseType()).orElseThrow()
                .kanRevurderingOpprettes(fagsak);
            dtoer.add(new FagsakDto(fagsak.getSaksnummer(), fagsak.getYtelseType(), fagsak.getStatus(), personDto,
                kanRevurderingOpprettes, fagsak.getSkalTilInfotrygd(),
                fagsak.getOpprettetTidspunkt(), fagsak.getEndretTidspunkt()));
        }
        return dtoer;
    }

}
