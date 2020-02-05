package no.nav.foreldrepenger.web.app.tjenester.behandling;

import static no.nav.vedtak.feil.LogLevel.ERROR;
import static no.nav.vedtak.sikkerhet.abac.BeskyttetRessursActionAttributt.CREATE;
import static no.nav.vedtak.sikkerhet.abac.BeskyttetRessursActionAttributt.READ;
import static no.nav.vedtak.sikkerhet.abac.BeskyttetRessursActionAttributt.UPDATE;
import static no.nav.vedtak.sikkerhet.abac.BeskyttetRessursResourceAttributt.FAGSAK;

import java.net.URISyntaxException;
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
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import no.nav.foreldrepenger.behandling.BehandlingIdDto;
import no.nav.foreldrepenger.behandling.FagsakTjeneste;
import no.nav.foreldrepenger.behandling.UuidDto;
import no.nav.foreldrepenger.behandling.steg.iverksettevedtak.HenleggBehandlingTjeneste;
import no.nav.foreldrepenger.behandlingslager.aktør.OrganisasjonsEnhet;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.web.app.tjenester.behandling.aksjonspunkt.BehandlingsoppretterApplikasjonTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.aksjonspunkt.BehandlingsprosessApplikasjonTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.aksjonspunkt.BehandlingsutredningApplikasjonTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.AsyncPollingStatus;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.BehandlingRettigheterDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.ByttBehandlendeEnhetDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.GjenopptaBehandlingDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.HenleggBehandlingDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.NyBehandlingDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.Redirect;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.ReåpneBehandlingDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.SettBehandlingPaVentDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.behandling.BehandlingDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.behandling.BehandlingDtoTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.behandling.ProsessTaskGruppeIdDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.behandling.UtvidetBehandlingDto;
import no.nav.foreldrepenger.web.app.tjenester.fagsak.dto.SaksnummerDto;
import no.nav.k9.kodeverk.behandling.BehandlingResultatType;
import no.nav.k9.kodeverk.behandling.BehandlingType;
import no.nav.k9.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.k9.kodeverk.historikk.HistorikkAktør;
import no.nav.k9.sak.typer.Saksnummer;
import no.nav.vedtak.feil.Feil;
import no.nav.vedtak.feil.FeilFactory;
import no.nav.vedtak.feil.deklarasjon.DeklarerteFeil;
import no.nav.vedtak.feil.deklarasjon.FunksjonellFeil;
import no.nav.vedtak.feil.deklarasjon.TekniskFeil;
import no.nav.vedtak.felles.jpa.TomtResultatException;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessurs;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessursResourceAttributt;

@ApplicationScoped
@Transactional
@Path("")
@Produces(MediaType.APPLICATION_JSON)
public class BehandlingRestTjeneste {

    public static final String BEHANDLING_PATH = "/behandling";
    public static final String BEHANDLINGER_ALLE = "/behandlinger/alle";
    public static final String BEHANDLINGER_PATH = "/behandlinger";
    public static final String BEHANDLINGER_STATUS = "/behandlinger/status";
    public static final String FAGSAK_BEHANDLING_PATH = "/fagsak/behandling";
    public static final String REVURDERING_ORGINAL_PATH = "/behandling/revurdering-original";
    public static final String STATUS_PATH = "/behandling/status";
    static public final String BYTT_ENHET_PATH = "/behandlinger/bytt-enhet";
    static public final String GJENOPPTA_PATH = "/behandlinger/gjenoppta";
    static public final String HENLEGG_PATH = "/behandlinger/henlegg";
    static public final String OPNE_FOR_ENDRINGER_PATH = "/behandlinger/opne-for-endringer";
    static public final String SETT_PA_VENT_PATH = "/behandlinger/sett-pa-vent";
    static public final String HANDLING_RETTIGHETER = "/behandlinger/handling-rettigheter";

    private BehandlingsutredningApplikasjonTjeneste behandlingsutredningApplikasjonTjeneste;
    private BehandlingsoppretterApplikasjonTjeneste behandlingsoppretterApplikasjonTjeneste;
    private BehandlingsprosessApplikasjonTjeneste behandlingsprosessTjeneste;
    private FagsakTjeneste fagsakTjeneste;
    private HenleggBehandlingTjeneste henleggBehandlingTjeneste;
    private BehandlingDtoTjeneste behandlingDtoTjeneste;

    public BehandlingRestTjeneste() {
        // for resteasy
    }

    @Inject
    public BehandlingRestTjeneste(BehandlingsutredningApplikasjonTjeneste behandlingsutredningApplikasjonTjeneste, // NOSONAR
                                  BehandlingsoppretterApplikasjonTjeneste behandlingsoppretterApplikasjonTjeneste,
                                  BehandlingsprosessApplikasjonTjeneste behandlingsprosessTjeneste,
                                  FagsakTjeneste fagsakTjeneste,
                                  HenleggBehandlingTjeneste henleggBehandlingTjeneste,
                                  BehandlingDtoTjeneste behandlingDtoTjeneste) {
        this.behandlingsutredningApplikasjonTjeneste = behandlingsutredningApplikasjonTjeneste;
        this.behandlingsoppretterApplikasjonTjeneste = behandlingsoppretterApplikasjonTjeneste;
        this.behandlingsprosessTjeneste = behandlingsprosessTjeneste;
        this.fagsakTjeneste = fagsakTjeneste;
        this.henleggBehandlingTjeneste = henleggBehandlingTjeneste;
        this.behandlingDtoTjeneste = behandlingDtoTjeneste;
    }

    @POST
    @Path(BEHANDLINGER_PATH)
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Init hent behandling",
        tags = "behandlinger",
        responses = {
            @ApiResponse(responseCode = "202", description = "Hent behandling initiert, Returnerer link til å polle på fremdrift",
                headers = @Header(name = HttpHeaders.LOCATION)
            ),
            @ApiResponse(responseCode = "303", description = "Behandling tilgjenglig (prosesstasks avsluttet)",
                headers = @Header(name = HttpHeaders.LOCATION)
            )
        })
    @BeskyttetRessurs(action = READ, ressurs = FAGSAK)
    @Deprecated
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public Response hentBehandling(@NotNull @Valid BehandlingIdDto behandlingIdDto) {
        var behandlingId = behandlingIdDto.getBehandlingId();
        var behandling = behandlingId != null
            ? behandlingsprosessTjeneste.hentBehandling(behandlingId)
            : behandlingsprosessTjeneste.hentBehandling(behandlingIdDto.getBehandlingUuid());

        Optional<String> gruppeOpt = behandlingsprosessTjeneste.sjekkOgForberedAsynkInnhentingAvRegisteropplysningerOgKjørProsess(behandling);

        // sender alltid til poll status slik at vi får sjekket på utestående prosess tasks også.
        return Redirect.tilBehandlingPollStatus(behandling.getUuid(), gruppeOpt);
    }

    @GET
    @Path(BEHANDLINGER_STATUS)
    @Deprecated
    @Operation(description = "Url for å polle på behandling mens behandlingprosessen pågår i bakgrunnen(asynkront)",
        summary = ("Returnerer link til enten samme (hvis ikke ferdig) eller redirecter til /behandlinger dersom asynkrone operasjoner er ferdig."),
        tags = "behandlinger",
        responses = {
            @ApiResponse(responseCode = "200",
                description = "Returnerer Status",
                content = @Content(
                    mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = AsyncPollingStatus.class)
                )
            ),
            @ApiResponse(responseCode = "303",
                description = "Behandling tilgjenglig (prosesstasks avsluttet)",
                headers = @Header(name = HttpHeaders.LOCATION)
            ),
            @ApiResponse(responseCode = "418",
                description = "ProsessTasks har feilet",
                headers = @Header(name = HttpHeaders.LOCATION),
                content = @Content(
                    mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = AsyncPollingStatus.class)
                )
            )
        })
    @BeskyttetRessurs(action = READ, ressurs = FAGSAK)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public Response hentBehandlingMidlertidigStatus(@NotNull @QueryParam("behandlingId") @Valid BehandlingIdDto behandlingIdDto,
                                                    @QueryParam("gruppe") @Valid ProsessTaskGruppeIdDto gruppeDto)
        throws URISyntaxException {
        var behandlingId = behandlingIdDto.getBehandlingId();
        var behandling = behandlingId != null
            ? behandlingsprosessTjeneste.hentBehandling(behandlingId)
            : behandlingsprosessTjeneste.hentBehandling(behandlingIdDto.getBehandlingUuid());
        String gruppe = gruppeDto == null ? null : gruppeDto.getGruppe();
        Optional<AsyncPollingStatus> prosessTaskGruppePågår = behandlingsprosessTjeneste.sjekkProsessTaskPågårForBehandling(behandling, gruppe);
        return Redirect.tilBehandlingEllerPollStatus(behandling.getUuid(), prosessTaskGruppePågår.orElse(null));
    }

    @GET
    @Path(STATUS_PATH)
    @Operation(description = "Url for å polle på behandling mens behandlingprosessen pågår i bakgrunnen(asynkront)",
        summary = ("Returnerer link til enten samme (hvis ikke ferdig) eller redirecter til /behandlinger dersom asynkrone operasjoner er ferdig."),
        tags = "behandlinger",
        responses = {
            @ApiResponse(responseCode = "200",
                description = "Returnerer Status",
                content = @Content(
                    mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = AsyncPollingStatus.class)
                )
            ),
            @ApiResponse(responseCode = "303",
                description = "Behandling tilgjenglig (prosesstasks avsluttet)",
                headers = @Header(name = HttpHeaders.LOCATION)
            ),
            @ApiResponse(responseCode = "418",
                description = "ProsessTasks har feilet",
                headers = @Header(name = HttpHeaders.LOCATION),
                content = @Content(
                    mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = AsyncPollingStatus.class)
                )
            )
        })
    @BeskyttetRessurs(action = READ, ressurs = FAGSAK)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public Response hentBehandlingMidlertidigStatus(
        @NotNull @QueryParam(UuidDto.NAME) @Parameter(description = UuidDto.DESC) @Valid UuidDto uuidDto,
        @QueryParam("gruppe") @Valid ProsessTaskGruppeIdDto gruppeDto
    ) throws URISyntaxException {
        return hentBehandlingMidlertidigStatus(new BehandlingIdDto(uuidDto), gruppeDto);
    }

    @GET
    @Path(BEHANDLINGER_PATH)
    @Deprecated
    @Operation(description = "Hent behandling gitt id",
        summary = ("Returnerer behandlingen som er tilknyttet id. Dette er resultat etter at asynkrone operasjoner er utført."),
        tags = "behandlinger",
        responses = {
            @ApiResponse(responseCode = "200",
                description = "Returnerer Behandling",
                content = @Content(
                    mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = UtvidetBehandlingDto.class)
                )
            )
        }
    )
    @BeskyttetRessurs(action = READ, ressurs = FAGSAK)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public Response hentBehandlingResultat(@NotNull @QueryParam("behandlingId") @Valid BehandlingIdDto behandlingIdDto) {
        var behandlingId = behandlingIdDto.getBehandlingId();
        var behandling = behandlingId != null
            ? behandlingsprosessTjeneste.hentBehandling(behandlingId)
            : behandlingsprosessTjeneste.hentBehandling(behandlingIdDto.getBehandlingUuid());
        AsyncPollingStatus taskStatus = behandlingsprosessTjeneste.sjekkProsessTaskPågårForBehandling(behandling, null).orElse(null);
        UtvidetBehandlingDto dto = behandlingDtoTjeneste.lagUtvidetBehandlingDto(behandling, taskStatus);
        ResponseBuilder responseBuilder = Response.ok().entity(dto);
        return responseBuilder.build();
    }

    @GET
    @Path(BEHANDLING_PATH)
    @Operation(description = "Hent behandling gitt id",
        summary = ("Returnerer behandlingen som er tilknyttet id. Dette er resultat etter at asynkrone operasjoner er utført."),
        tags = "behandlinger",
        responses = {
            @ApiResponse(responseCode = "200",
                description = "Returnerer Behandling",
                content = @Content(
                    mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = UtvidetBehandlingDto.class)
                )
            )
        }
    )
    @BeskyttetRessurs(action = READ, ressurs = FAGSAK)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public Response hentBehandlingResultat(@NotNull @QueryParam(UuidDto.NAME) @Parameter(description = UuidDto.DESC) @Valid UuidDto uuidDto) {
        return hentBehandlingResultat(new BehandlingIdDto(uuidDto));
    }


    @GET
    @Path("/behandlinger/revurdering-original")
    @Operation(description = "Hent avsluttet førstegangsbehandling gitt id",
        summary = ("Henter førstegangngsbehandlingen som er/blir revurdert"),
        tags = "behandlinger",
        responses = {
            @ApiResponse(responseCode = "200",
                description = "Returnerer avsluttet førstegangsbehandling",
                content = @Content(
                    mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = UtvidetBehandlingDto.class)
                )
            )
        }
    )
    @BeskyttetRessurs(action = READ, ressurs = FAGSAK)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public Response hentRevurderingensOriginalBehandling(@NotNull @QueryParam("behandlingId") @Valid BehandlingIdDto behandlingIdDto) {
        var behandlingId = behandlingIdDto.getBehandlingId();
        var behandling = behandlingId != null
            ? behandlingsprosessTjeneste.hentBehandling(behandlingId)
            : behandlingsprosessTjeneste.hentBehandling(behandlingIdDto.getBehandlingUuid());

        UtvidetBehandlingDto dto = behandlingDtoTjeneste.lagUtvidetBehandlingDtoForRevurderingensOriginalBehandling(behandling);
        ResponseBuilder responseBuilder = Response.ok().entity(dto);
        return responseBuilder.build();
    }

    @GET
    @Path(REVURDERING_ORGINAL_PATH)
    @Operation(description = "Hent avsluttet førstegangsbehandling gitt id",
        summary = ("Henter førstegangngsbehandlingen som er/blir revurdert"),
        tags = "behandlinger",
        responses = {
            @ApiResponse(responseCode = "200",
                description = "Returnerer avsluttet førstegangsbehandling",
                content = @Content(
                    mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = UtvidetBehandlingDto.class)
                )
            )
        }
    )
    @BeskyttetRessurs(action = READ, ressurs = FAGSAK)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public Response hentRevurderingensOriginalBehandling(@NotNull @QueryParam(UuidDto.NAME) @Parameter(description = UuidDto.DESC) @Valid UuidDto uuidDto) {
        return hentRevurderingensOriginalBehandling(new BehandlingIdDto(uuidDto));
    }

    @POST
    @Path(SETT_PA_VENT_PATH)
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Setter behandling på vent", tags = "behandlinger")
    @BeskyttetRessurs(action = UPDATE, ressurs = FAGSAK)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public void settBehandlingPaVent(@Parameter(description = "Frist for behandling på vent") @Valid SettBehandlingPaVentDto dto) {
        behandlingsutredningApplikasjonTjeneste.kanEndreBehandling(dto.getBehandlingId(), dto.getBehandlingVersjon());
        behandlingsutredningApplikasjonTjeneste.settBehandlingPaVent(dto.getBehandlingId(), dto.getFrist(), dto.getVentearsak());
    }

    @POST
    @Path("/behandlinger/endre-pa-vent")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Endrer ventefrist for behandling på vent", tags = "behandlinger")
    @BeskyttetRessurs(action = UPDATE, ressurs = BeskyttetRessursResourceAttributt.VENTEFRIST)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public void endreFristForBehandlingPaVent(
        @Parameter(description = "Frist for behandling på vent") @Valid SettBehandlingPaVentDto dto) {
        behandlingsutredningApplikasjonTjeneste.kanEndreBehandling(dto.getBehandlingId(), dto.getBehandlingVersjon());
        behandlingsutredningApplikasjonTjeneste.endreBehandlingPaVent(dto.getBehandlingId(), dto.getFrist(), dto.getVentearsak());
    }

    @POST
    @Path(HENLEGG_PATH)
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Henlegger behandling", tags = "behandlinger")
    @BeskyttetRessurs(action = UPDATE, ressurs = FAGSAK)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public void henleggBehandling(@Parameter(description = "Henleggelsesårsak") @Valid HenleggBehandlingDto dto) {
        Long behandlingId = dto.getBehandlingId();
        behandlingsutredningApplikasjonTjeneste.kanEndreBehandling(behandlingId, dto.getBehandlingVersjon());
        BehandlingResultatType årsakKode = tilHenleggBehandlingResultatType(dto.getÅrsakKode());
        henleggBehandlingTjeneste.henleggBehandling(String.valueOf(behandlingId), årsakKode, dto.getBegrunnelse());
    }

    @POST
    @Path(GJENOPPTA_PATH)
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Gjenopptar behandling som er satt på vent",
        tags = "behandlinger",
        responses = {
            @ApiResponse(responseCode = "200",
                description = "Gjenoppta behandling påstartet i bakgrunnen",
                headers = @Header(name = HttpHeaders.LOCATION)
            )
        }
    )
    @BeskyttetRessurs(action = UPDATE, ressurs = FAGSAK)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public Response gjenopptaBehandling(@Parameter(description = "BehandlingId for behandling som skal gjenopptas") @Valid GjenopptaBehandlingDto dto)
        throws URISyntaxException {
        Long behandlingId = dto.getBehandlingId();
        Long behandlingVersjon = dto.getBehandlingVersjon();

        // precondition - sjekk behandling versjon/lås
        behandlingsutredningApplikasjonTjeneste.kanEndreBehandling(behandlingId, behandlingVersjon);

        // gjenoppta behandling ( sparkes i gang asynkront, derav redirect til status url under )
        var behandling = behandlingsprosessTjeneste.hentBehandling(behandlingId);
        Optional<String> gruppeOpt = behandlingsprosessTjeneste.gjenopptaBehandling(behandling);
        return Redirect.tilBehandlingPollStatus(behandling.getUuid(), gruppeOpt);
    }

    @POST
    @Path(BYTT_ENHET_PATH)
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Bytte behandlende enhet", tags = "behandlinger")
    @BeskyttetRessurs(action = UPDATE, ressurs = FAGSAK)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public void byttBehandlendeEnhet(@Parameter(description = "Ny enhet som skal byttes") @Valid ByttBehandlendeEnhetDto dto) {
        Long behandlingId = dto.getBehandlingId();
        Long behandlingVersjon = dto.getBehandlingVersjon();
        behandlingsutredningApplikasjonTjeneste.kanEndreBehandling(behandlingId, behandlingVersjon);

        String enhetId = dto.getEnhetId();
        String enhetNavn = dto.getEnhetNavn();
        String begrunnelse = dto.getBegrunnelse();
        behandlingsutredningApplikasjonTjeneste.byttBehandlendeEnhet(behandlingId, new OrganisasjonsEnhet(enhetId, enhetNavn), begrunnelse, HistorikkAktør.SAKSBEHANDLER);
    }

    @PUT
    @Path(BEHANDLINGER_PATH)
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Opprette ny behandling",
        tags = "behandlinger",
        responses = {
            @ApiResponse(responseCode = "202",
                description = "Opprett ny behandling pågår",
                headers = @Header(name = HttpHeaders.LOCATION)
            )
        }
    )
    @BeskyttetRessurs(action = CREATE, ressurs = FAGSAK)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public Response opprettNyBehandling(@Parameter(description = "Saksnummer og flagg om det er ny behandling etter klage") @Valid NyBehandlingDto dto)
        throws URISyntaxException {
        Saksnummer saksnummer = dto.getSaksnummer();
        Optional<Fagsak> funnetFagsak = fagsakTjeneste.finnFagsakGittSaksnummer(saksnummer, true);
        String kode = dto.getBehandlingType().getKode();

        if (funnetFagsak.isEmpty()) {
            throw BehandlingRestTjenesteFeil.FACTORY.fantIkkeFagsak(saksnummer).toException();
        }

        Fagsak fagsak = funnetFagsak.get();

        if (BehandlingType.REVURDERING.getKode().equals(kode)) {
            BehandlingÅrsakType behandlingÅrsakType = BehandlingÅrsakType.fraKode(dto.getBehandlingArsakType().getKode());
            Behandling behandling = behandlingsoppretterApplikasjonTjeneste.opprettRevurdering(fagsak, behandlingÅrsakType);
            String gruppe = behandlingsprosessTjeneste.asynkStartBehandlingsprosess(behandling);
            return Redirect.tilBehandlingPollStatus(behandling.getUuid(), Optional.of(gruppe));

        } else if (BehandlingType.FØRSTEGANGSSØKNAD.getKode().equals(kode)) {
            behandlingsoppretterApplikasjonTjeneste.opprettNyFørstegangsbehandling(fagsak.getId(), saksnummer, dto.getNyBehandlingEtterKlage());
            // ved førstegangssønad opprettes egen task for vurdere denne,
            // sender derfor ikke viderer til prosesser behandling (i motsetning til de andre).
            // må også oppfriske hele sakskomplekset, så sender til fagsak poll url
            return Redirect.tilFagsakPollStatus(fagsak.getSaksnummer(), Optional.empty());
        } else {
            throw new IllegalArgumentException("Støtter ikke opprette ny behandling for behandlingType:" + kode);
        }

    }

    private BehandlingResultatType tilHenleggBehandlingResultatType(String årsak) {
        return BehandlingResultatType.getAlleHenleggelseskoder().stream().filter(k -> k.getKode().equals(årsak))
            .findFirst().orElse(null);
    }

    @GET
    @Path(BEHANDLINGER_ALLE)
    @Operation(description = "Henter alle behandlinger basert på saksnummer", summary = ("Returnerer alle behandlinger som er tilknyttet saksnummer."), tags = "behandlinger")
    @BeskyttetRessurs(action = READ, ressurs = FAGSAK)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public List<BehandlingDto> hentBehandlinger(@NotNull @QueryParam("saksnummer") @Parameter(description = "Saksnummer må være et eksisterende saksnummer") @Valid SaksnummerDto s) {
        Saksnummer saksnummer = new Saksnummer(s.getVerdi());
        List<Behandling> behandlinger = behandlingsutredningApplikasjonTjeneste.hentBehandlingerForSaksnummer(saksnummer);
        return behandlingDtoTjeneste.lagBehandlingDtoer(behandlinger);
    }

    @GET
    @Path(FAGSAK_BEHANDLING_PATH)
    @Operation(description = "Henter alle behandlinger basert på saksnummer", summary = ("Returnerer alle behandlinger som er tilknyttet saksnummer."), tags = "behandlinger")
    @BeskyttetRessurs(action = READ, ressurs = FAGSAK)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public List<BehandlingDto> hentAlleBehandlinger(@NotNull
                                                    @QueryParam("saksnummer")
                                                    @Parameter(description = "Saksnummer må være et eksisterende saksnummer") @Valid SaksnummerDto s) {
        Saksnummer saksnummer = new Saksnummer(s.getVerdi());
        List<Behandling> behandlinger = behandlingsutredningApplikasjonTjeneste.hentBehandlingerForSaksnummer(saksnummer);
        return behandlingDtoTjeneste.lagBehandlingDtoer(behandlinger);
    }

    @POST
    @Path(OPNE_FOR_ENDRINGER_PATH)
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Åpner behandling for endringer",
        tags = "behandlinger",
        responses = {
            @ApiResponse(responseCode = "200",
                description = "Åpning av behandling for endringer påstartet i bakgrunnen",
                headers = @Header(name = HttpHeaders.LOCATION)
            )
        }
    )
    @BeskyttetRessurs(action = UPDATE, ressurs = FAGSAK)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public Response åpneBehandlingForEndringer(@Parameter(description = "BehandlingId for behandling som skal åpnes for endringer") @Valid ReåpneBehandlingDto dto) {
        Long behandlingId = dto.getBehandlingId();
        Long behandlingVersjon = dto.getBehandlingVersjon();

        // precondition - sjekk behandling versjon/lås
        behandlingsutredningApplikasjonTjeneste.kanEndreBehandling(behandlingId, behandlingVersjon);
        var behandling = behandlingsprosessTjeneste.hentBehandling(behandlingId);
        if (behandling.isBehandlingPåVent()) {
            throw BehandlingRestTjenesteFeil.FACTORY.måTaAvVent(behandlingId).toException();
        }
        if (behandling.harBehandlingÅrsak(BehandlingÅrsakType.BERØRT_BEHANDLING)) {
            throw BehandlingRestTjenesteFeil.FACTORY.erBerørtBehandling(behandlingId).toException();
        }
        behandlingsprosessTjeneste.asynkTilbakestillOgÅpneBehandlingForEndringer(behandlingId);
        behandling = behandlingsprosessTjeneste.hentBehandling(behandlingId);
        return Redirect.tilBehandlingPollStatus(behandling.getUuid(), Optional.empty());
    }

    @GET
    @Path(HANDLING_RETTIGHETER)
    @Operation(description = "Henter rettigheter for lovlige behandlingsoperasjoner", tags = "behandlinger")
    @BeskyttetRessurs(action = READ, ressurs = FAGSAK)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public BehandlingRettigheterDto hentBehandlingOperasjonRettigheter(@NotNull @QueryParam(UuidDto.NAME) @Parameter(description = UuidDto.DESC) @Valid UuidDto uuidDto) {
        Behandling behandling = behandlingsprosessTjeneste.hentBehandling(uuidDto.getBehandlingUuid());
        Boolean harSoknad = behandlingDtoTjeneste.finnBehandlingOperasjonRettigheter(behandling);
        //TODO (TOR) Denne skal etterkvart returnere rettighetene knytta til behandlingsmeny i frontend
        return new BehandlingRettigheterDto(harSoknad);
    }


    private interface BehandlingRestTjenesteFeil extends DeklarerteFeil {
        BehandlingRestTjenesteFeil FACTORY = FeilFactory.create(BehandlingRestTjenesteFeil.class); // NOSONAR

        @TekniskFeil(feilkode = "FP-760410", feilmelding = "Fant ikke fagsak med saksnummer %s", logLevel = ERROR, exceptionClass = TomtResultatException.class)
        Feil fantIkkeFagsak(Saksnummer saksnummer);

        @FunksjonellFeil(feilkode = "FP-722320", feilmelding = "Behandling må tas av vent før den kan åpnes", løsningsforslag = "Ta behandling av vent")
        Feil måTaAvVent(Long behandlingId);

        @FunksjonellFeil(feilkode = "FP-722321", feilmelding = "Behandling er berørt må gjennomføres", løsningsforslag = "Behandle ferdig berørt og opprett revurdering")
        Feil erBerørtBehandling(Long behandlingId);
    }
}
