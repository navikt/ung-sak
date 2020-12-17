package no.nav.k9.sak.web.app.tjenester.behandling;

import static no.nav.k9.abac.BeskyttetRessursKoder.FAGSAK;
import static no.nav.k9.abac.BeskyttetRessursKoder.VENTEFRIST;
import static no.nav.vedtak.feil.LogLevel.ERROR;
import static no.nav.vedtak.sikkerhet.abac.BeskyttetRessursActionAttributt.CREATE;
import static no.nav.vedtak.sikkerhet.abac.BeskyttetRessursActionAttributt.READ;
import static no.nav.vedtak.sikkerhet.abac.BeskyttetRessursActionAttributt.UPDATE;

import java.net.URISyntaxException;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

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
import no.nav.k9.kodeverk.behandling.BehandlingResultatType;
import no.nav.k9.kodeverk.behandling.BehandlingType;
import no.nav.k9.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.k9.kodeverk.historikk.HistorikkAktør;
import no.nav.k9.kodeverk.produksjonsstyring.OrganisasjonsEnhet;
import no.nav.k9.sak.behandling.FagsakTjeneste;
import no.nav.k9.sak.behandling.prosessering.BehandlingsprosessApplikasjonTjeneste;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.domene.behandling.steg.iverksettevedtak.HenleggBehandlingTjeneste;
import no.nav.k9.sak.kontrakt.AsyncPollingStatus;
import no.nav.k9.sak.kontrakt.ProsessTaskGruppeIdDto;
import no.nav.k9.sak.kontrakt.behandling.BehandlingDto;
import no.nav.k9.sak.kontrakt.behandling.BehandlingIdDto;
import no.nav.k9.sak.kontrakt.behandling.BehandlingRettigheterDto;
import no.nav.k9.sak.kontrakt.behandling.BehandlingUuidDto;
import no.nav.k9.sak.kontrakt.behandling.ByttBehandlendeEnhetDto;
import no.nav.k9.sak.kontrakt.behandling.GjenopptaBehandlingDto;
import no.nav.k9.sak.kontrakt.behandling.HenleggBehandlingDto;
import no.nav.k9.sak.kontrakt.behandling.NyBehandlingDto;
import no.nav.k9.sak.kontrakt.behandling.ReåpneBehandlingDto;
import no.nav.k9.sak.kontrakt.behandling.SaksnummerDto;
import no.nav.k9.sak.kontrakt.behandling.SettBehandlingPaVentDto;
import no.nav.k9.sak.typer.Saksnummer;
import no.nav.k9.sak.web.app.rest.Redirect;
import no.nav.k9.sak.web.app.tjenester.behandling.aksjonspunkt.BehandlingsutredningApplikasjonTjeneste;
import no.nav.k9.sak.web.server.abac.AbacAttributtSupplier;
import no.nav.vedtak.feil.Feil;
import no.nav.vedtak.feil.FeilFactory;
import no.nav.vedtak.feil.deklarasjon.DeklarerteFeil;
import no.nav.vedtak.feil.deklarasjon.FunksjonellFeil;
import no.nav.vedtak.feil.deklarasjon.TekniskFeil;
import no.nav.vedtak.felles.jpa.TomtResultatException;
import no.nav.vedtak.konfig.KonfigVerdi;
import no.nav.vedtak.sikkerhet.abac.AbacDataAttributter;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessurs;
import no.nav.vedtak.sikkerhet.abac.TilpassetAbacAttributt;

@ApplicationScoped
@Transactional
@Path("")
@Produces(MediaType.APPLICATION_JSON)
public class BehandlingRestTjeneste {

    public static final String BEHANDLING_PATH = "/behandling";
    public static final String BEHANDLINGER_ALLE = "/behandlinger/alle";
    public static final String BEHANDLINGER_PATH = "/behandlinger";
    public static final String BEHANDLINGER_UNNTAK_PATH = "/behandlinger/unntak";
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
    private BehandlingsprosessApplikasjonTjeneste behandlingsprosessTjeneste;
    private BehandlingsoppretterTjeneste behandlingsoppretterTjeneste;
    private FagsakTjeneste fagsakTjeneste;
    private HenleggBehandlingTjeneste henleggBehandlingTjeneste;
    private BehandlingDtoTjeneste behandlingDtoTjeneste;
    private SjekkProsessering sjekkProsessering;
    private Boolean unntaksbehandlingTogglet;

    public BehandlingRestTjeneste() {
        // for resteasy
    }

    @Inject
    public BehandlingRestTjeneste(BehandlingsutredningApplikasjonTjeneste behandlingsutredningApplikasjonTjeneste, // NOSONAR
                                  BehandlingsoppretterTjeneste behandlingsoppretterTjeneste,
                                  BehandlingsprosessApplikasjonTjeneste behandlingsprosessTjeneste,
                                  FagsakTjeneste fagsakTjeneste,
                                  HenleggBehandlingTjeneste henleggBehandlingTjeneste,
                                  BehandlingDtoTjeneste behandlingDtoTjeneste,
                                  SjekkProsessering sjekkProsessering,
                                  @KonfigVerdi(value = "UNNTAKSBEHANDLING", defaultVerdi = "true") Boolean unntaksbehandlingTogglet) {
        this.behandlingsutredningApplikasjonTjeneste = behandlingsutredningApplikasjonTjeneste;
        this.behandlingsprosessTjeneste = behandlingsprosessTjeneste;
        this.behandlingsoppretterTjeneste = behandlingsoppretterTjeneste;
        this.fagsakTjeneste = fagsakTjeneste;
        this.henleggBehandlingTjeneste = henleggBehandlingTjeneste;
        this.behandlingDtoTjeneste = behandlingDtoTjeneste;
        this.sjekkProsessering = sjekkProsessering;
        this.unntaksbehandlingTogglet = unntaksbehandlingTogglet;
    }

    @POST
    @Path(BEHANDLINGER_PATH)
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Init hent behandling", tags = "behandlinger", responses = {
            @ApiResponse(responseCode = "202", description = "Hent behandling initiert, Returnerer link til å polle på fremdrift", headers = @Header(name = HttpHeaders.LOCATION)),
            @ApiResponse(responseCode = "303", description = "Behandling tilgjenglig (prosesstasks avsluttet)", headers = @Header(name = HttpHeaders.LOCATION))
    })
    @BeskyttetRessurs(action = READ, resource = FAGSAK)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public Response hentBehandling(@NotNull @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) BehandlingIdDto behandlingIdDto) {
        var behandlingId = behandlingIdDto.getBehandlingId();
        var behandling = behandlingId != null
            ? behandlingsprosessTjeneste.hentBehandling(behandlingId)
            : behandlingsprosessTjeneste.hentBehandling(behandlingIdDto.getBehandlingUuid());

        Optional<String> gruppeOpt = sjekkProsessering.sjekkOgForberedAsynkInnhentingAvRegisteropplysningerOgKjørProsess(behandling, true);

        // sender alltid til poll status slik at vi får sjekket på utestående prosess tasks også.
        return Redirect.tilBehandlingPollStatus(behandling.getUuid(), gruppeOpt);
    }

    @GET
    @Path(BEHANDLINGER_STATUS)
    @Operation(description = "Url for å polle på behandling mens behandlingprosessen pågår i bakgrunnen(asynkront)", summary = ("Returnerer link til enten samme (hvis ikke ferdig) eller redirecter til /behandlinger dersom asynkrone operasjoner er ferdig."), tags = "behandlinger", responses = {
            @ApiResponse(responseCode = "200", description = "Returnerer Status", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = AsyncPollingStatus.class))),
            @ApiResponse(responseCode = "303", description = "Behandling tilgjenglig (prosesstasks avsluttet)", headers = @Header(name = HttpHeaders.LOCATION)),
            @ApiResponse(responseCode = "418", description = "ProsessTasks har feilet", headers = @Header(name = HttpHeaders.LOCATION), content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = AsyncPollingStatus.class)))
    })
    @BeskyttetRessurs(action = READ, resource = FAGSAK)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public Response hentBehandlingMidlertidigStatus(@NotNull @QueryParam("behandlingId") @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) BehandlingIdDto behandlingIdDto,
                                                    @QueryParam("gruppe") @Valid @TilpassetAbacAttributt(supplierClass = IngenTilgangsAttributter.class) ProsessTaskGruppeIdDto gruppeDto)
            throws URISyntaxException {
        var behandlingId = behandlingIdDto.getBehandlingId();
        var behandling = behandlingId != null
            ? behandlingsprosessTjeneste.hentBehandling(behandlingId)
            : behandlingsprosessTjeneste.hentBehandling(behandlingIdDto.getBehandlingUuid());
        String gruppe = gruppeDto == null ? null : gruppeDto.getGruppe();
        Optional<AsyncPollingStatus> prosessTaskGruppePågår = sjekkProsessering.sjekkProsessTaskPågårForBehandling(behandling, gruppe);
        return Redirect.tilBehandlingEllerPollStatus(behandling.getUuid(), prosessTaskGruppePågår.orElse(null));
    }

    @GET
    @Path(STATUS_PATH)
    @Operation(description = "Url for å polle på behandling mens behandlingprosessen pågår i bakgrunnen(asynkront)", summary = ("Returnerer link til enten samme (hvis ikke ferdig) eller redirecter til /behandlinger dersom asynkrone operasjoner er ferdig."), tags = "behandlinger", responses = {
            @ApiResponse(responseCode = "200", description = "Returnerer Status", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = AsyncPollingStatus.class))),
            @ApiResponse(responseCode = "303", description = "Behandling tilgjenglig (prosesstasks avsluttet)", headers = @Header(name = HttpHeaders.LOCATION)),
            @ApiResponse(responseCode = "418", description = "ProsessTasks har feilet", headers = @Header(name = HttpHeaders.LOCATION), content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = AsyncPollingStatus.class)))
    })
    @BeskyttetRessurs(action = READ, resource = FAGSAK)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public Response hentBehandlingMidlertidigStatus(
                                                    @NotNull @QueryParam(BehandlingUuidDto.NAME) @Parameter(description = BehandlingUuidDto.DESC) @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) BehandlingUuidDto behandlingUuid,
                                                    @QueryParam("gruppe") @Valid @TilpassetAbacAttributt(supplierClass = IngenTilgangsAttributter.class) ProsessTaskGruppeIdDto gruppeDto)
            throws URISyntaxException {
        var behandling = behandlingsprosessTjeneste.hentBehandling(behandlingUuid.getBehandlingUuid());
        String gruppe = gruppeDto == null ? null : gruppeDto.getGruppe();
        Optional<AsyncPollingStatus> prosessTaskGruppePågår = sjekkProsessering.sjekkProsessTaskPågårForBehandling(behandling, gruppe);
        return Redirect.tilBehandlingEllerPollStatus(behandling.getUuid(), prosessTaskGruppePågår.orElse(null));
    }

    @GET
    @Path(BEHANDLINGER_PATH)
    @Operation(description = "Hent behandling gitt id", summary = ("Returnerer behandlingen som er tilknyttet id. Dette er resultat etter at asynkrone operasjoner er utført."), tags = "behandlinger", responses = {
            @ApiResponse(responseCode = "200", description = "Returnerer Behandling", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = BehandlingDto.class)))
    })
    @BeskyttetRessurs(action = READ, resource = FAGSAK)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public Response hentBehandlingData(@NotNull @QueryParam("behandlingId") @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) BehandlingIdDto behandlingIdDto) {
        var behandlingId = behandlingIdDto.getBehandlingId();
        var behandling = behandlingId != null
            ? behandlingsprosessTjeneste.hentBehandling(behandlingId)
            : behandlingsprosessTjeneste.hentBehandling(behandlingIdDto.getBehandlingUuid());
        AsyncPollingStatus taskStatus = sjekkProsessering.sjekkProsessTaskPågårForBehandling(behandling, null).orElse(null);
        BehandlingDto dto = behandlingDtoTjeneste.lagUtvidetBehandlingDto(behandling, taskStatus);
        ResponseBuilder responseBuilder = Response.ok().entity(dto);
        return responseBuilder.build();
    }

    @GET
    @Path(BEHANDLING_PATH)
    @Operation(description = "Hent behandling gitt id", summary = ("Returnerer behandlingen som er tilknyttet id. Dette er resultat etter at asynkrone operasjoner er utført."), tags = "behandlinger", responses = {
            @ApiResponse(responseCode = "200", description = "Returnerer Behandling", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = BehandlingDto.class)))
    })
    @BeskyttetRessurs(action = READ, resource = FAGSAK)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public Response hentBehandlingData(@NotNull @QueryParam(BehandlingUuidDto.NAME) @Parameter(description = BehandlingUuidDto.DESC) @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) BehandlingUuidDto behandlingUuid) {
        var behandling = sjekkProsessering.hentBehandling(behandlingUuid.getBehandlingUuid());
        AsyncPollingStatus taskStatus = sjekkProsessering.sjekkProsessTaskPågårForBehandling(behandling, null).orElse(null);
        var dto = behandlingDtoTjeneste.lagUtvidetBehandlingDto(behandling, taskStatus);
        ResponseBuilder responseBuilder = Response.ok().entity(dto);
        return responseBuilder.build();
    }

    @GET
    @Path("/behandlinger/revurdering-original")
    @Operation(description = "Hent avsluttet førstegangsbehandling gitt id", summary = ("Henter førstegangngsbehandlingen som er/blir revurdert"), tags = "behandlinger", responses = {
            @ApiResponse(responseCode = "200", description = "Returnerer avsluttet førstegangsbehandling", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = BehandlingDto.class)))
    })
    @BeskyttetRessurs(action = READ, resource = FAGSAK)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public Response hentRevurderingensOriginalBehandling(@NotNull @QueryParam("behandlingId") @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) BehandlingIdDto behandlingIdDto) {
        var behandlingId = behandlingIdDto.getBehandlingId();
        var behandling = behandlingId != null
            ? behandlingsprosessTjeneste.hentBehandling(behandlingId)
            : behandlingsprosessTjeneste.hentBehandling(behandlingIdDto.getBehandlingUuid());

        BehandlingDto dto = behandlingDtoTjeneste.lagUtvidetBehandlingDtoForRevurderingensOriginalBehandling(behandling);
        ResponseBuilder responseBuilder = Response.ok().entity(dto);
        return responseBuilder.build();
    }

    @GET
    @Path(REVURDERING_ORGINAL_PATH)
    @Operation(description = "Hent avsluttet førstegangsbehandling gitt id", summary = ("Henter førstegangngsbehandlingen som er/blir revurdert"), tags = "behandlinger", responses = {
            @ApiResponse(responseCode = "200", description = "Returnerer avsluttet førstegangsbehandling", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = BehandlingDto.class)))
    })
    @BeskyttetRessurs(action = READ, resource = FAGSAK)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public Response hentRevurderingensOriginalBehandling(@NotNull @QueryParam(BehandlingUuidDto.NAME) @Parameter(description = BehandlingUuidDto.DESC) @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) BehandlingUuidDto behandlingUuid) {
        var behandling = behandlingsprosessTjeneste.hentBehandling(behandlingUuid.getBehandlingUuid());
        BehandlingDto dto = behandlingDtoTjeneste.lagUtvidetBehandlingDtoForRevurderingensOriginalBehandling(behandling);
        ResponseBuilder responseBuilder = Response.ok().entity(dto);
        return responseBuilder.build();
    }

    @POST
    @Path(SETT_PA_VENT_PATH)
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Setter behandling på vent", tags = "behandlinger")
    @BeskyttetRessurs(action = UPDATE, resource = FAGSAK)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public void settBehandlingPaVent(@Parameter(description = "Frist for behandling på vent") @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) SettBehandlingPaVentDto dto) {
        behandlingsutredningApplikasjonTjeneste.kanEndreBehandling(dto.getBehandlingId(), dto.getBehandlingVersjon());
        behandlingsutredningApplikasjonTjeneste.settBehandlingPaVent(dto.getBehandlingId(), dto.getFrist(), dto.getVentearsak(), dto.getVentearsakVariant());
    }

    @POST
    @Path("/behandlinger/endre-pa-vent")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Endrer ventefrist for behandling på vent", tags = "behandlinger")
    @BeskyttetRessurs(action = UPDATE, resource = VENTEFRIST)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public void endreFristForBehandlingPaVent(
                                              @Parameter(description = "Frist for behandling på vent") @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) SettBehandlingPaVentDto dto) {
        behandlingsutredningApplikasjonTjeneste.kanEndreBehandling(dto.getBehandlingId(), dto.getBehandlingVersjon());
        behandlingsutredningApplikasjonTjeneste.endreBehandlingPaVent(dto.getBehandlingId(), dto.getFrist(), dto.getVentearsak(), dto.getVentearsakVariant());
    }

    @POST
    @Path(HENLEGG_PATH)
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Henlegger behandling", tags = "behandlinger")
    @BeskyttetRessurs(action = UPDATE, resource = FAGSAK)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public void henleggBehandling(@Parameter(description = "Henleggelsesårsak") @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) HenleggBehandlingDto dto) {
        Long behandlingId = dto.getBehandlingId();
        behandlingsutredningApplikasjonTjeneste.kanEndreBehandling(behandlingId, dto.getBehandlingVersjon());
        BehandlingResultatType årsakKode = tilHenleggBehandlingResultatType(dto.getÅrsakKode());
        henleggBehandlingTjeneste.henleggBehandlingAvSaksbehandler(String.valueOf(behandlingId), årsakKode, dto.getBegrunnelse());
    }

    @POST
    @Path(GJENOPPTA_PATH)
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Gjenopptar behandling som er satt på vent", tags = "behandlinger", responses = {
            @ApiResponse(responseCode = "200", description = "Gjenoppta behandling påstartet i bakgrunnen", headers = @Header(name = HttpHeaders.LOCATION))
    })
    @BeskyttetRessurs(action = UPDATE, resource = FAGSAK)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public Response gjenopptaBehandling(@Parameter(description = "BehandlingId for behandling som skal gjenopptas") @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) GjenopptaBehandlingDto dto)
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
    @BeskyttetRessurs(action = UPDATE, resource = FAGSAK)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public void byttBehandlendeEnhet(@Parameter(description = "Ny enhet som skal byttes") @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) ByttBehandlendeEnhetDto dto) {
        Long behandlingId = dto.getBehandlingId();
        Long behandlingVersjon = dto.getBehandlingVersjon();
        behandlingsutredningApplikasjonTjeneste.kanEndreBehandling(behandlingId, behandlingVersjon);

        String enhetId = dto.getEnhetId();
        String enhetNavn = dto.getEnhetNavn();
        String begrunnelse = dto.getBegrunnelse();
        behandlingsutredningApplikasjonTjeneste.byttBehandlendeEnhet(behandlingId, new OrganisasjonsEnhet(enhetId, enhetNavn), begrunnelse,
            HistorikkAktør.SAKSBEHANDLER);
    }

    @PUT
    @Path(BEHANDLINGER_PATH)
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Opprette ny behandling", tags = "behandlinger", responses = {
            @ApiResponse(responseCode = "202", description = "Opprett ny behandling pågår", headers = @Header(name = HttpHeaders.LOCATION))
    })
    @BeskyttetRessurs(action = CREATE, resource = FAGSAK)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public Response opprettNyBehandling(@Parameter(description = "Saksnummer og flagg om det er ny behandling etter klage") @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) NyBehandlingDto dto)
            throws URISyntaxException {
        Saksnummer saksnummer = dto.getSaksnummer();
        BehandlingType behandlingType = BehandlingType.fraKode(dto.getBehandlingType().getKode());
        Optional<Fagsak> funnetFagsak = fagsakTjeneste.finnFagsakGittSaksnummer(saksnummer, true);

        if (funnetFagsak.isEmpty()) {
            throw BehandlingRestTjenesteFeil.FACTORY.fantIkkeFagsak(saksnummer).toException();
        }

        Fagsak fagsak = funnetFagsak.get();
        if (BehandlingType.REVURDERING.getKode().equals(behandlingType.getKode())) {
            BehandlingÅrsakType behandlingÅrsakType = BehandlingÅrsakType.fraKode(dto.getBehandlingArsakType().getKode());
            Behandling behandling = behandlingsoppretterTjeneste.opprettRevurdering(fagsak, behandlingÅrsakType);
            String gruppe = behandlingsprosessTjeneste.asynkStartBehandlingsprosess(behandling);
            return Redirect.tilBehandlingPollStatus(behandling.getUuid(), Optional.of(gruppe));

        } else if (BehandlingType.FØRSTEGANGSSØKNAD.getKode().equals(behandlingType.getKode())) {
            throw new UnsupportedOperationException("Ikke implementert støtte for å opprette ny førstegangsbehandling for " + fagsak);
            // ved førstegangssønad opprettes egen task for vurdere denne,
            // sender derfor ikke viderer til prosesser behandling (i motsetning til de andre).
            // må også oppfriske hele sakskomplekset, så sender til fagsak poll url
            // return Redirect.tilFagsakPollStatus(fagsak.getSaksnummer(), Optional.empty());
        } else {
            throw new IllegalArgumentException("Støtter ikke opprette ny behandling for behandlingType:" + behandlingType);
        }

    }

    @PUT
    @Path(BEHANDLINGER_UNNTAK_PATH)
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Opprette ny unntaksbehandling", tags = "behandlinger", responses = {
            @ApiResponse(responseCode = "202", description = "Opprett ny unntaksbehandling pågår", headers = @Header(name = HttpHeaders.LOCATION))
    })
    @BeskyttetRessurs(action = CREATE, resource = FAGSAK)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public Response opprettNyUnntaksbehandling(@Parameter(description = "Saksnummer") @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) NyBehandlingDto dto) {
        if (!unntaksbehandlingTogglet) {
            throw new UnsupportedOperationException("Unntaksbehandling er ikke aktivert");
        }
        Saksnummer saksnummer = dto.getSaksnummer();
        Optional<Fagsak> funnetFagsak = fagsakTjeneste.finnFagsakGittSaksnummer(saksnummer, true);
        BehandlingType kode = BehandlingType.fraKode(dto.getBehandlingType().getKode());

        if (funnetFagsak.isEmpty()) {
            throw BehandlingRestTjenesteFeil.FACTORY.fantIkkeFagsak(saksnummer).toException();
        }
        Fagsak fagsak = funnetFagsak.get();
        if (BehandlingType.UNNTAKSBEHANDLING.getKode().equals(kode.getKode())) {
            BehandlingÅrsakType behandlingÅrsakType = BehandlingÅrsakType.fraKode(dto.getBehandlingArsakType().getKode());
            Behandling behandling = behandlingsoppretterTjeneste.opprettUnntaksbehandling(fagsak, behandlingÅrsakType);
            String gruppe = behandlingsprosessTjeneste.asynkStartBehandlingsprosess(behandling);
            return Redirect.tilBehandlingPollStatus(behandling.getUuid(), Optional.of(gruppe));

        } else {
            throw new IllegalArgumentException("Støtter ikke opprette ny unntaksbehandling for behandlingType:" + kode);
        }

    }

    private BehandlingResultatType tilHenleggBehandlingResultatType(String årsak) {
        return BehandlingResultatType.getAlleHenleggelseskoder().stream().filter(k -> k.getKode().equals(årsak))
            .findFirst().orElse(null);
    }

    @GET
    @Path(BEHANDLINGER_ALLE)
    @Operation(description = "Henter alle behandlinger basert på saksnummer", summary = ("Returnerer alle behandlinger som er tilknyttet saksnummer."), tags = "behandlinger")
    @BeskyttetRessurs(action = READ, resource = FAGSAK)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public List<BehandlingDto> hentBehandlinger(@NotNull @QueryParam("saksnummer") @Parameter(description = "Saksnummer må være et eksisterende saksnummer") @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) SaksnummerDto s) {
        Saksnummer saksnummer = s.getVerdi();
        List<Behandling> behandlinger = behandlingsutredningApplikasjonTjeneste.hentBehandlingerForSaksnummer(saksnummer);
        return behandlingDtoTjeneste.lagBehandlingDtoer(behandlinger);
    }

    @GET
    @Path(FAGSAK_BEHANDLING_PATH)
    @Operation(description = "Henter alle behandlinger basert på saksnummer", summary = ("Returnerer alle behandlinger som er tilknyttet saksnummer."), tags = "behandlinger")
    @BeskyttetRessurs(action = READ, resource = FAGSAK)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public List<BehandlingDto> hentAlleBehandlinger(@NotNull @QueryParam("saksnummer") @Parameter(description = "Saksnummer må være et eksisterende saksnummer") @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) SaksnummerDto s) {
        Saksnummer saksnummer = s.getVerdi();
        List<Behandling> behandlinger = behandlingsutredningApplikasjonTjeneste.hentBehandlingerForSaksnummer(saksnummer);
        return behandlingDtoTjeneste.lagBehandlingDtoer(behandlinger);
    }

    @POST
    @Path(OPNE_FOR_ENDRINGER_PATH)
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Åpner behandling for endringer", tags = "behandlinger", responses = {
            @ApiResponse(responseCode = "200", description = "Åpning av behandling for endringer påstartet i bakgrunnen", headers = @Header(name = HttpHeaders.LOCATION))
    })
    @BeskyttetRessurs(action = UPDATE, resource = FAGSAK)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public Response åpneBehandlingForEndringer(@Parameter(description = "BehandlingId for behandling som skal åpnes for endringer") @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) ReåpneBehandlingDto dto) {
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
    @BeskyttetRessurs(action = READ, resource = FAGSAK)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public BehandlingRettigheterDto hentBehandlingOperasjonRettigheter(@NotNull @QueryParam(BehandlingUuidDto.NAME) @Parameter(description = BehandlingUuidDto.DESC) @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) BehandlingUuidDto behandlingUuid) {
        Behandling behandling = behandlingsprosessTjeneste.hentBehandling(behandlingUuid.getBehandlingUuid());
        Boolean harSoknad = behandlingDtoTjeneste.finnBehandlingOperasjonRettigheter(behandling);
        // TODO (TOR) Denne skal etterkvart returnere rettighetene knytta til behandlingsmeny i frontend
        return new BehandlingRettigheterDto(harSoknad);
    }

    private interface BehandlingRestTjenesteFeil extends DeklarerteFeil {
        BehandlingRestTjenesteFeil FACTORY = FeilFactory.create(BehandlingRestTjenesteFeil.class); // NOSONAR

        @TekniskFeil(feilkode = "K9-760410", feilmelding = "Fant ikke fagsak med saksnummer %s", logLevel = ERROR, exceptionClass = TomtResultatException.class)
        Feil fantIkkeFagsak(Saksnummer saksnummer);

        @FunksjonellFeil(feilkode = "K9-722320", feilmelding = "Behandling må tas av vent før den kan åpnes", løsningsforslag = "Ta behandling av vent")
        Feil måTaAvVent(Long behandlingId);

        @FunksjonellFeil(feilkode = "K9-722321", feilmelding = "Behandling er berørt må gjennomføres", løsningsforslag = "Behandle ferdig berørt og opprett revurdering")
        Feil erBerørtBehandling(Long behandlingId);

    }

    public static class IngenTilgangsAttributter implements Function<Object, AbacDataAttributter> {
        @Override
        public AbacDataAttributter apply(Object obj) {
            return AbacDataAttributter.opprett();
        }
    }
}
