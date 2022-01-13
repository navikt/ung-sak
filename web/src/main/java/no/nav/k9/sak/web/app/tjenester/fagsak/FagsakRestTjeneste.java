package no.nav.k9.sak.web.app.tjenester.fagsak;

import static no.nav.k9.abac.BeskyttetRessursKoder.FAGSAK;
import static no.nav.k9.felles.sikkerhet.abac.BeskyttetRessursActionAttributt.READ;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import no.nav.k9.felles.sikkerhet.abac.AbacDataAttributter;
import no.nav.k9.felles.sikkerhet.abac.BeskyttetRessurs;
import no.nav.k9.felles.sikkerhet.abac.BeskyttetRessursActionAttributt;
import no.nav.k9.felles.sikkerhet.abac.StandardAbacAttributtType;
import no.nav.k9.felles.sikkerhet.abac.TilpassetAbacAttributt;
import no.nav.k9.kodeverk.behandling.BehandlingType;
import no.nav.k9.sak.behandling.FagsakTjeneste;
import no.nav.k9.sak.behandling.revurdering.RevurderingTjeneste;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.aktør.Personinfo;
import no.nav.k9.sak.behandlingslager.aktør.PersoninfoBasis;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.k9.sak.domene.person.pdl.PersoninfoAdapter;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.kontrakt.AsyncPollingStatus;
import no.nav.k9.sak.kontrakt.ProsessTaskGruppeIdDto;
import no.nav.k9.sak.kontrakt.behandling.BehandlingOpprettingDto;
import no.nav.k9.sak.kontrakt.behandling.BehandlingUuidDto;
import no.nav.k9.sak.kontrakt.behandling.SakRettigheterDto;
import no.nav.k9.sak.kontrakt.behandling.SaksnummerDto;
import no.nav.k9.sak.kontrakt.fagsak.FagsakDto;
import no.nav.k9.sak.kontrakt.fagsak.FagsakInfoDto;
import no.nav.k9.sak.kontrakt.fagsak.MatchFagsak;
import no.nav.k9.sak.kontrakt.fagsak.RelatertSakDto;
import no.nav.k9.sak.kontrakt.fagsak.RelatertSøkerDto;
import no.nav.k9.sak.kontrakt.mottak.FinnSak;
import no.nav.k9.sak.kontrakt.person.PersonDto;
import no.nav.k9.sak.kontrakt.produksjonsstyring.SøkeSakEllerBrukerDto;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.Periode;
import no.nav.k9.sak.typer.PersonIdent;
import no.nav.k9.sak.typer.Saksnummer;
import no.nav.k9.sak.web.app.rest.Redirect;
import no.nav.k9.sak.web.app.tjenester.behandling.BehandlingsoppretterTjeneste;
import no.nav.k9.sak.web.server.abac.AbacAttributtSupplier;

@Path("")
@ApplicationScoped
@Transactional
public class FagsakRestTjeneste {

    public static final String PATH = "/fagsak";
    public static final String STATUS_PATH = PATH + "/status";
    public static final String SISTE_FAGSAK_PATH = PATH + "/siste";
    public static final String SOK_PATH = PATH + "/sok";
    public static final String MATCH_PATH = PATH + "/match";
    public static final String RELATERTE_SAKER_PATH = PATH + "/relatertesaker";

    public static final String BRUKER_PATH = PATH + "/bruker";
    public static final String RETTIGHETER_PATH = PATH + "/rettigheter";

    private FagsakApplikasjonTjeneste fagsakApplikasjonTjeneste;
    private FagsakTjeneste fagsakTjeneste;
    private BehandlingsoppretterTjeneste behandlingsoppretterTjeneste;
    private PersoninfoAdapter personinfoAdapter;
    private BehandlingRepository behandlingRepository;
    private FagsakRepository fagsakRepository;

    public FagsakRestTjeneste() {
        // For Rest-CDI
    }

    @Inject
    public FagsakRestTjeneste(FagsakApplikasjonTjeneste fagsakApplikasjonTjeneste, FagsakTjeneste fagsakTjeneste, BehandlingsoppretterTjeneste behandlingsoppretterTjeneste, PersoninfoAdapter personinfoAdapter, BehandlingRepository behandlingRepository, FagsakRepository fagsakRepository) {
        this.fagsakApplikasjonTjeneste = fagsakApplikasjonTjeneste;
        this.fagsakTjeneste = fagsakTjeneste;
        this.behandlingsoppretterTjeneste = behandlingsoppretterTjeneste;
        this.personinfoAdapter = personinfoAdapter;
        this.behandlingRepository = behandlingRepository;
        this.fagsakRepository = fagsakRepository;
    }

    @GET
    @Path(STATUS_PATH)
    @Operation(description = "Url for å polle på fagsak mens behandlingprosessen pågår i bakgrunnen(asynkront)", summary = "Returnerer link til enten samme (hvis ikke ferdig) eller redirecter til /fagsak dersom asynkrone operasjoner er ferdig.", tags = "fagsak", responses = {
        @ApiResponse(responseCode = "200", description = "Returnerer Status", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = AsyncPollingStatus.class))),
        @ApiResponse(responseCode = "303", description = "Pågående prosesstasks avsluttet", headers = @Header(name = HttpHeaders.LOCATION)),
        @ApiResponse(responseCode = "418", description = "ProsessTasks har feilet", headers = @Header(name = HttpHeaders.LOCATION), content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = AsyncPollingStatus.class)))
    })
    @Produces(MediaType.APPLICATION_JSON)
    @BeskyttetRessurs(action = READ, resource = FAGSAK)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public Response hentFagsakMidlertidigStatus(@Context HttpServletRequest request,
                                                @NotNull @QueryParam("saksnummer") @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) SaksnummerDto idDto,
                                                @QueryParam("gruppe") @Valid @TilpassetAbacAttributt(supplierClass = IngenTilgangsAttributter.class) ProsessTaskGruppeIdDto gruppeDto)
        throws URISyntaxException {
        Saksnummer saksnummer = idDto.getVerdi();
        String gruppe = gruppeDto == null ? null : gruppeDto.getGruppe();
        Optional<AsyncPollingStatus> prosessTaskGruppePågår = fagsakApplikasjonTjeneste.sjekkProsessTaskPågår(saksnummer, gruppe);
        return Redirect.tilFagsakEllerPollStatus(request, saksnummer, prosessTaskGruppePågår.orElse(null));
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path(PATH)
    @Operation(description = "Hent fagsak for saksnummer", tags = "fagsak", responses = {
        @ApiResponse(responseCode = "200", description = "Returnerer fagsak", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = FagsakDto.class))),
        @ApiResponse(responseCode = "404", description = "Fagsak ikke tilgjengelig")
    })
    @BeskyttetRessurs(action = READ, resource = FAGSAK)
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

    @SuppressWarnings("resource")
    @POST
    @Path(SISTE_FAGSAK_PATH)
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(description = "Finn siste eksisterende fagsak.", summary = ("Finn siste fagsak som matcher søkekriteriene"), tags = "fordel")
    @BeskyttetRessurs(action = BeskyttetRessursActionAttributt.READ, resource = FAGSAK)
    public Response finnSisteFagsak(@Parameter(description = "Oppretter fagsak") @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) FinnSak finnSakDto) {
        var ytelseType = finnSakDto.getYtelseType();

        AktørId bruker = finnSakDto.getAktørId();
        AktørId pleietrengendeAktørId = finnSakDto.getPleietrengendeAktørId();
        AktørId relatertPersonAktørId = finnSakDto.getRelatertPersonAktørId();
        var periode = finnSakDto.getPeriode();

        var fagsak = fagsakTjeneste.finnFagsakerForAktør(bruker)
            .stream()
            .filter(f -> pleietrengendeAktørId == null || Objects.equals(f.getPleietrengendeAktørId(), pleietrengendeAktørId))
            .filter(f -> relatertPersonAktørId == null || Objects.equals(f.getRelatertPersonAktørId(), relatertPersonAktørId))
            .filter(f -> Objects.equals(f.getYtelseType(), ytelseType))
            .filter(f -> periode == null || f.getPeriode().overlapper(DatoIntervallEntitet.fra(periode)))
            .sorted(Comparator.comparing(Fagsak::getPeriode).thenComparing(Fagsak::getOpprettetTidspunkt).reversed())
            .findFirst();

        return fagsak.isPresent()
            ? Response.ok(tilFagsakDto(null, fagsak.get())).build()
            : Response.status(Status.NO_CONTENT).build();
    }

    @GET
    @Path(BRUKER_PATH)
    @Operation(description = "Hent brukerdata for aktørId", tags = "fagsak", responses = {
        @ApiResponse(responseCode = "200", description = "Returnerer person", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = PersonDto.class))),
        @ApiResponse(responseCode = "404", description = "Person ikke tilgjengelig")
    })
    @Produces(MediaType.APPLICATION_JSON)
    @BeskyttetRessurs(action = READ, resource = FAGSAK)
    public Response hentBrukerForFagsak(@NotNull @QueryParam("saksnummer") @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) SaksnummerDto s) {
        var personInfo = fagsakApplikasjonTjeneste.hentBruker(s.getVerdi());
        if (personInfo.isEmpty()) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        var dto = mapFraPersoninfoBasis(personInfo.get());
        return Response.ok(dto).build();
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path(RETTIGHETER_PATH)
    @Operation(description = "Hent rettigheter for saksnummer", tags = "fagsak", responses = {
        @ApiResponse(responseCode = "200", description = "Returnerer rettigheter", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = SakRettigheterDto.class))),
        @ApiResponse(responseCode = "404", description = "Fagsak ikke tilgjengelig")
    })
    @BeskyttetRessurs(action = READ, resource = FAGSAK)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public Response hentRettigheter(@NotNull @QueryParam("saksnummer") @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) SaksnummerDto s) {
        var fagsak = fagsakTjeneste.finnFagsakGittSaksnummer(s.getVerdi().getSaksnummer(), false);
        if (fagsak.isEmpty()) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        var fagsakId = fagsak.map(Fagsak::getId).orElseThrow();
        var oppretting = BehandlingType.getYtelseBehandlingTyper().stream()
            .map(bt -> new BehandlingOpprettingDto(bt, behandlingsoppretterTjeneste.kanOppretteNyBehandlingAvType(fagsakId, bt)))
            .collect(Collectors.toList());

        var dto = new SakRettigheterDto(fagsak.map(Fagsak::getSkalTilInfotrygd).orElse(false), oppretting, List.of());
        return Response.ok(dto).build();
    }

    @POST
    @Path(SOK_PATH)
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(description = "Søk etter saker på saksnummer eller fødselsnummer", tags = "fagsak", summary = ("Spesifikke saker kan søkes via saksnummer. " +
        "Oversikt over saker knyttet til en bruker kan søkes via fødselsnummer eller d-nummer."))
    @BeskyttetRessurs(action = BeskyttetRessursActionAttributt.READ, resource = FAGSAK)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public List<FagsakDto> søkFagsaker(@Parameter(description = "Søkestreng kan være saksnummer, fødselsnummer eller D-nummer.") @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) SøkeSakEllerBrukerDto søkestreng) {
        FagsakSamlingForBruker view = fagsakApplikasjonTjeneste.hentSaker(søkestreng.getSearchString());
        return tilDtoer(view);
    }

    @POST
    @Path(MATCH_PATH)
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(description = "Søk etter saker på ytelse, bruker, og evt. pleieptrengende/relatertPerson", tags = "fagsak", summary = ("Finner matchende fagsaker for angitt ytelse, bruker, etc.. "))
    @BeskyttetRessurs(action = BeskyttetRessursActionAttributt.READ, resource = FAGSAK)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public List<FagsakInfoDto> matchFagsaker(@Parameter(description = "Match kritierer for å lete opp fagsaker") @Valid @TilpassetAbacAttributt(supplierClass = MatchFagsakAttributter.class) MatchFagsak matchFagsak) {
        List<FagsakInfoDto> fagsaker = fagsakApplikasjonTjeneste.matchFagsaker(matchFagsak.getYtelseType(),
            matchFagsak.getBruker(),
            matchFagsak.getPeriode(),
            matchFagsak.getPleietrengendeIdenter(),
            matchFagsak.getRelatertPersonIdenter());
        return fagsaker;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path(RELATERTE_SAKER_PATH)
    @Operation(description = "Hent liste over saket tilknyttet en pleietrengende"
        , tags = "fagsak"
        , responses = {
        @ApiResponse(responseCode = "200",
            content = @Content(mediaType = MediaType.APPLICATION_JSON,
                schema = @Schema(implementation = RelatertSakDto.class)))
    })
    @BeskyttetRessurs(action = READ, resource = FAGSAK)
    public RelatertSakDto hentRelaterteSaker(
        @QueryParam(BehandlingUuidDto.NAME)
        @Parameter(description = BehandlingUuidDto.DESC)
        @NotNull
        @Valid
        @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class)
            BehandlingUuidDto behandlingUuid) {
        Behandling behandling = behandlingRepository.hentBehandling(behandlingUuid.getBehandlingUuid()); //TODO: utvide for andre ytelsestyper
        if (behandling.getFagsak().getPleietrengendeAktørId() == null) {
            return new RelatertSakDto(List.of());
        }
        List<Fagsak> fagsaker = fagsakRepository.finnFagsakRelatertTil(behandling.getFagsakYtelseType(), behandling.getFagsak().getPleietrengendeAktørId(), null, null, null);

        return new RelatertSakDto(fagsaker.stream()
            .filter(f -> !f.getAktørId().equals(behandling.getAktørId()))
            .map(f -> {
                Personinfo personinfo = personinfoAdapter.hentKjerneinformasjon(f.getAktørId());
                var åpenBehandling = behandlingRepository.hentSisteYtelsesBehandlingForFagsakId(f.getId())
                    .map(Behandling::erStatusFerdigbehandlet)
                    .map(it -> !it)
                    .orElse(false);
                return new RelatertSøkerDto(personinfo.getPersonIdent(), personinfo.getNavn(), f.getSaksnummer(), åpenBehandling);
            }).collect(Collectors.toList()));
    }

    private List<FagsakDto> tilDtoer(FagsakSamlingForBruker view) {
        if (view.isEmpty()) {
            return new ArrayList<>();
        }
        Personinfo brukerInfo = view.getBrukerInfo();

        PersonDto personDto = new PersonDto(
            brukerInfo.getNavn(),
            brukerInfo.getAlder(),
            String.valueOf(brukerInfo.getPersonIdent().getIdent()),
            brukerInfo.erKvinne(),
            brukerInfo.getPersonstatus(),
            brukerInfo.getDiskresjonskode(),
            brukerInfo.getDødsdato(),
            brukerInfo.getAktørId());

        List<FagsakDto> dtoer = new ArrayList<>();
        for (var info : view.getFagsakInfoer()) {
            dtoer.add(tilFagsakDto(personDto, info.getFagsak()));
        }
        return dtoer;
    }

    private FagsakDto tilFagsakDto(PersonDto personDto, Fagsak fagsak) {
        Boolean kanRevurderingOpprettes = FagsakYtelseTypeRef.Lookup.find(RevurderingTjeneste.class, fagsak.getYtelseType()).orElseThrow()
            .kanRevurderingOpprettes(fagsak);
        var periode = new Periode(fagsak.getPeriode().getFomDato(), fagsak.getPeriode().getTomDato());
        return new FagsakDto(
            fagsak.getSaksnummer(),
            fagsak.getYtelseType(),
            fagsak.getStatus(),
            periode,
            personDto,
            fagsak.getPleietrengendeAktørId(),
            fagsak.getRelatertPersonAktørId(),
            kanRevurderingOpprettes,
            fagsak.getSkalTilInfotrygd(),
            fagsak.getOpprettetTidspunkt(),
            fagsak.getEndretTidspunkt());
    }

    private PersonDto mapFraPersoninfoBasis(PersoninfoBasis pi) {
        return new PersonDto(pi.getNavn(), pi.getAlder(), String.valueOf(pi.getPersonIdent().getIdent()),
            pi.erKvinne(), pi.getPersonstatus(), pi.getDiskresjonskode(), pi.getDødsdato(), pi.getAktørId());
    }

    public static class MatchFagsakAttributter implements Function<Object, AbacDataAttributter> {
        private static final StandardAbacAttributtType AKTØR_ID_TYPE = StandardAbacAttributtType.AKTØR_ID;
        private static final StandardAbacAttributtType FNR_TYPE = StandardAbacAttributtType.FNR;

        @Override
        public AbacDataAttributter apply(Object obj) {
            var m = (MatchFagsak) obj;
            var abac = AbacDataAttributter.opprett();

            Optional.ofNullable(m.getBruker()).map(PersonIdent::getIdent).ifPresent(v -> abac.leggTil(FNR_TYPE, v));
            Optional.ofNullable(m.getBruker()).map(PersonIdent::getAktørId).ifPresent(v -> abac.leggTil(AKTØR_ID_TYPE, v));

            Optional.ofNullable(m.getPleietrengendeIdenter()).stream()
                .flatMap(List::stream)
                .map(PersonIdent::getIdent)
                .filter(Objects::nonNull)
                .forEach(v -> abac.leggTil(FNR_TYPE, v));

            Optional.ofNullable(m.getPleietrengendeIdenter()).stream()
                .flatMap(List::stream)
                .map(PersonIdent::getAktørId)
                .filter(Objects::nonNull)
                .forEach(v -> abac.leggTil(AKTØR_ID_TYPE, v));

            Optional.ofNullable(m.getRelatertPersonIdenter()).stream()
                .flatMap(List::stream)
                .map(PersonIdent::getIdent)
                .filter(Objects::nonNull)
                .forEach(v -> abac.leggTil(FNR_TYPE, v));

            Optional.ofNullable(m.getRelatertPersonIdenter()).stream()
                .flatMap(List::stream)
                .map(PersonIdent::getAktørId)
                .filter(Objects::nonNull)
                .forEach(v -> abac.leggTil(AKTØR_ID_TYPE, v));

            // må ha minst en aktørid
            if (abac.getVerdier(FNR_TYPE).isEmpty() && abac.getVerdier(AKTØR_ID_TYPE).isEmpty()) {
                throw new IllegalArgumentException("Må ha minst en aktørid eller fnr oppgitt");
            }
            return abac;
        }
    }

    public static class IngenTilgangsAttributter implements Function<Object, AbacDataAttributter> {
        @Override
        public AbacDataAttributter apply(Object obj) {
            return AbacDataAttributter.opprett();
        }
    }
}
