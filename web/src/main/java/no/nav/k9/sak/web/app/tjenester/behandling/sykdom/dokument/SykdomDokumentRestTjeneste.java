package no.nav.k9.sak.web.app.tjenester.behandling.sykdom.dokument;

import static no.nav.k9.abac.BeskyttetRessursKoder.FAGSAK;
import static no.nav.vedtak.sikkerhet.abac.BeskyttetRessursActionAttributt.READ;
import static no.nav.vedtak.sikkerhet.abac.BeskyttetRessursActionAttributt.UPDATE;

import java.io.ByteArrayInputStream;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

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
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import no.nav.k9.kodeverk.behandling.BehandlingStatus;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.dokument.arkiv.DokumentArkivTjeneste;
import no.nav.k9.sak.kontrakt.behandling.BehandlingUuidDto;
import no.nav.k9.sak.web.app.tjenester.behandling.sykdom.SykdomDokumentDto;
import no.nav.k9.sak.web.app.tjenester.dokument.DokumentRestTjenesteFeil;
import no.nav.k9.sak.web.server.abac.AbacAttributtSupplier;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomDiagnosekoder;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomDokument;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomDokumentRepository;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomDokumentType;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomInnleggelser;
import no.nav.vedtak.exception.ManglerTilgangException;
import no.nav.vedtak.exception.TekniskException;
import no.nav.vedtak.sikkerhet.abac.AbacDataAttributter;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessurs;
import no.nav.vedtak.sikkerhet.abac.TilpassetAbacAttributt;
import no.nav.vedtak.sikkerhet.context.SubjectHandler;

@Produces(MediaType.APPLICATION_JSON)
@ApplicationScoped
@Path(SykdomDokumentRestTjeneste.BASE_PATH)
@Transactional
public class SykdomDokumentRestTjeneste {

    private static final Logger log = LoggerFactory.getLogger(SykdomDokumentRestTjeneste.class);

    public static final String BASE_PATH = "/behandling/sykdom/dokument";
    private static final String DOKUMENT = "/";
    private static final String DOKUMENT_NY = "/ny";
    private static final String DOKUMENT_INNHOLD = "/innhold";
    private static final String SYKDOM_INNLEGGELSE = "/innleggelse";
    public static final String SYKDOM_INNLEGGELSE_PATH = BASE_PATH + SYKDOM_INNLEGGELSE;
    private static final String SYKDOM_DIAGNOSEKODER = "/diagnosekoder";
    public static final String SYKDOM_DIAGNOSEKODER_PATH = BASE_PATH + SYKDOM_DIAGNOSEKODER;
    public static final String DOKUMENT_PATH = BASE_PATH + DOKUMENT;
    public static final String DOKUMENT_INNHOLD_PATH = BASE_PATH + DOKUMENT_INNHOLD;
    private static final String DOKUMENT_OVERSIKT = "/oversikt";
    public static final String DOKUMENT_OVERSIKT_PATH = BASE_PATH + DOKUMENT_OVERSIKT;
    private static final String DOKUMENT_LISTE = "/liste";
    public static final String DOKUMENT_LISTE_PATH = BASE_PATH + DOKUMENT_LISTE;
    
    private BehandlingRepository behandlingRepository;
    private SykdomDokumentOversiktMapper sykdomDokumentOversiktMapper;
    private SykdomDokumentRepository sykdomDokumentRepository;
    private DokumentArkivTjeneste dokumentArkivTjeneste;


    public SykdomDokumentRestTjeneste() {
    }


    @Inject
    public SykdomDokumentRestTjeneste(BehandlingRepository behandlingRepository, SykdomDokumentOversiktMapper sykdomDokumentOversiktMapper, SykdomDokumentRepository sykdomDokumentRepository, DokumentArkivTjeneste dokumentArkivTjeneste) {
        this.behandlingRepository = behandlingRepository;
        this.sykdomDokumentOversiktMapper = sykdomDokumentOversiktMapper;
        this.sykdomDokumentRepository = sykdomDokumentRepository;
        this.dokumentArkivTjeneste = dokumentArkivTjeneste;
    }

    @GET
    @Path(DOKUMENT_LISTE)
    @Operation(description = "Henter en liste over dokumenter som kan brukes i vurdering.",
        summary = "Henter en liste over dokumenter som kan brukes i vurdering.",
        tags = "sykdom",
        responses = {
            @ApiResponse(responseCode = "200",
                description = "",
                content = @Content(mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = SykdomDokumentDto.class)))
        })
    @BeskyttetRessurs(action = READ, resource = FAGSAK)
    public List<SykdomDokumentDto> hentSykdomsdokumenter(
            @QueryParam(BehandlingUuidDto.NAME)
            @Parameter(description = BehandlingUuidDto.DESC)
            @NotNull
            @Valid
            @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class)
            BehandlingUuidDto behandlingUuid) {
        final var behandling = behandlingRepository.hentBehandlingHvisFinnes(behandlingUuid.getBehandlingUuid()).orElseThrow();

        final List<SykdomDokument> dokumenter = sykdomDokumentRepository.henDokumenterSomErRelevanteForSykdom(behandling.getFagsak().getPleietrengendeAktørId());
        return sykdomDokumentOversiktMapper.mapDokumenter(behandling.getUuid(), dokumenter, Collections.emptySet());
    }
    
    @GET
    @Path(SYKDOM_INNLEGGELSE)
    @Operation(description = "Henter alle perioder den pleietrengende er innlagt på sykehus og liknende.",
        summary = "Henter alle perioder den pleietrengende er innlagt på sykehus og liknende.",
        tags = "sykdom",
        responses = {
            @ApiResponse(responseCode = "200",
                description = "",
                content = @Content(mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = SykdomInnleggelseDto.class)))
        })
    @BeskyttetRessurs(action = READ, resource = FAGSAK)
    public SykdomInnleggelseDto hentSykdomInnleggelse(
            @NotNull @QueryParam(BehandlingUuidDto.NAME)
            @Parameter(description = BehandlingUuidDto.DESC)
            @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class)
            BehandlingUuidDto behandlingUuid) {

        final var behandling = behandlingRepository.hentBehandlingHvisFinnes(behandlingUuid.getBehandlingUuid()).orElseThrow();

        final SykdomInnleggelser innleggelser = sykdomDokumentRepository.hentInnleggelse(behandling.getFagsak().getPleietrengendeAktørId());

        return sykdomDokumentOversiktMapper.toSykdomInnleggelseDto(innleggelser, behandling);
    }

    @POST
    @Path(SYKDOM_INNLEGGELSE)
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Oppdaterer perioder den pleietrengende er innlagt på sykehus og liknende.",
        summary = "Oppdaterer perioder den pleietrengende er innlagt på sykehus og liknende.",
        tags = "sykdom",
        responses = {
                @ApiResponse(responseCode = "201",
                        description = "Dokumentet har blitt opprettet.")
        })
    @BeskyttetRessurs(action = UPDATE, resource = FAGSAK)
    public void oppdaterSykdomInnleggelse(
            @Parameter
            @NotNull
            @Valid
            @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class)
            SykdomInnleggelseDto sykdomInnleggelse) {

        final var behandling = behandlingRepository.hentBehandlingHvisFinnes(sykdomInnleggelse.getBehandlingUuid()).orElseThrow();
        if (behandling.getStatus().erFerdigbehandletStatus() || behandling.getStatus().equals(BehandlingStatus.FATTER_VEDTAK)) {
            throw new IllegalStateException("Behandlingen er ikke åpen for endringer.");
        }

        final SykdomInnleggelser innleggelser = sykdomDokumentOversiktMapper.toSykdomInnleggelser(sykdomInnleggelse, SubjectHandler.getSubjectHandler().getUid());

        sykdomDokumentRepository.opprettEllerOppdaterInnleggelser(innleggelser, behandling.getFagsak().getPleietrengendeAktørId());
    }

    @GET
    @Path(SYKDOM_DIAGNOSEKODER)
    @Operation(description = "Henter alle registrerte diagnosekoder på den pleietrengende.",
        summary = "Henter alle registrerte diagnosekoder på den pleietrengende..",
        tags = "sykdom",
        responses = {
            @ApiResponse(responseCode = "200",
                description = "",
                content = @Content(mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = SykdomDiagnosekoderDto.class)))
        })
    @BeskyttetRessurs(action = READ, resource = FAGSAK)
    public SykdomDiagnosekoderDto hentDiagnosekoder(
            @NotNull @QueryParam(BehandlingUuidDto.NAME)
            @Parameter(description = BehandlingUuidDto.DESC)
            @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class)
            BehandlingUuidDto behandlingUuid) {

        final var behandling = behandlingRepository.hentBehandlingHvisFinnes(behandlingUuid.getBehandlingUuid()).orElseThrow();

        final SykdomDiagnosekoder diagnosekoder = sykdomDokumentRepository.hentDiagnosekoder(behandling.getFagsak().getPleietrengendeAktørId());

        return sykdomDokumentOversiktMapper.toSykdomDiagnosekoderDto(diagnosekoder, behandling);
    }

    @POST
    @Path(SYKDOM_DIAGNOSEKODER)
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Oppdaterer diagnosekoder på den pleietrengende.",
        summary = "Oppdaterer diagnosekoder på den pleietrengende.",
        tags = "sykdom",
        responses = {
                @ApiResponse(responseCode = "201",
                        description = "Dokumentet har blitt opprettet.")
        })
    @BeskyttetRessurs(action = UPDATE, resource = FAGSAK)
    public void oppdaterDiagnosekoder(
            @Parameter
            @NotNull
            @Valid
            @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class)
            SykdomDiagnosekoderDto sykdomDiagnosekoderDto) {

        final var behandling = behandlingRepository.hentBehandlingHvisFinnes(sykdomDiagnosekoderDto.getBehandlingUuid()).orElseThrow();
        if (behandling.getStatus().erFerdigbehandletStatus() || behandling.getStatus().equals(BehandlingStatus.FATTER_VEDTAK)) {
            throw new IllegalStateException("Behandlingen er ikke åpen for endringer.");
        }
        
        final SykdomDiagnosekoder diagnosekoder = sykdomDokumentOversiktMapper.toSykdomDiagnosekoder(sykdomDiagnosekoderDto, SubjectHandler.getSubjectHandler().getUid());
        sykdomDokumentRepository.opprettEllerOppdaterDiagnosekoder(diagnosekoder, behandling.getFagsak().getPleietrengendeAktørId());
    }

    @GET
    @Path(DOKUMENT_OVERSIKT)
    @Operation(description = "En oversikt over alle dokumenter som er koblet på den pleietrengende behandlingen refererer til.",
        summary = "En oversikt over alle dokumenter som er koblet på den pleietrengende behandlingen refererer til.",
        tags = "sykdom",
        responses = {
            @ApiResponse(responseCode = "200",
                description = "",
                content = @Content(mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = SykdomDokumentOversikt.class)))
        })
    @BeskyttetRessurs(action = READ, resource = FAGSAK)
    public SykdomDokumentOversikt hentDokumentoversikt(
            @NotNull @QueryParam(BehandlingUuidDto.NAME)
            @Parameter(description = BehandlingUuidDto.DESC)
            @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class)
            BehandlingUuidDto behandlingUuid) {

        final var behandling = behandlingRepository.hentBehandlingHvisFinnes(behandlingUuid.getBehandlingUuid()).orElseThrow();
        final List<SykdomDokument> dokumenter = sykdomDokumentRepository.hentAlleDokumenterFor(behandling.getFagsak().getPleietrengendeAktørId());
        return sykdomDokumentOversiktMapper.map(behandling.getUuid().toString(), dokumenter);
    }

    @POST
    @Path(DOKUMENT)
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Oppdaterer metainformasjonen om et dokument.",
        summary = "Oppdaterer metainformasjonen om et dokument.",
        tags = "sykdom",
        responses = {
            @ApiResponse(responseCode = "201",
                description = "Dokumentet har blitt opprettet.")
        })
    @BeskyttetRessurs(action = UPDATE, resource = FAGSAK)
    public void oppdaterDokument(
            @Parameter
            @NotNull
            @Valid
            @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class)
            SykdomDokumentEndringDto sykdomDokumentEndringDto) {
        final var behandling = behandlingRepository.hentBehandlingHvisFinnes(sykdomDokumentEndringDto.getBehandlingUuid()).orElseThrow();
        if (behandling.getStatus().erFerdigbehandletStatus() || behandling.getStatus().equals(BehandlingStatus.FATTER_VEDTAK)) {
            throw new IllegalStateException("Behandlingen er ikke åpen for endringer.");
        }

        final var dokument = sykdomDokumentRepository.hentDokument(Long.valueOf(sykdomDokumentEndringDto.getId()), behandling.getFagsak().getPleietrengendeAktørId()).get();
        dokument.setDatert(sykdomDokumentEndringDto.getDatert());
        dokument.setType(sykdomDokumentEndringDto.getType());
        // TODO: Håndtering av versjoner/historikk.

        sykdomDokumentRepository.oppdater(dokument);
    }

    /**
     * TODO: DETTE ER KUN ET TESTENDEPUNKT. FJERN!
     */
    @POST
    @Path(DOKUMENT_NY)
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "TEST TEST TEST Oppretter et dokument.",
        summary = "TEST TEST TEST Oppretter et dokument.",
        tags = "sykdom",
        responses = {
            @ApiResponse(responseCode = "201",
                description = "Dokumentet har blitt opprettet.")
        })
    @BeskyttetRessurs(action = UPDATE, resource = FAGSAK)
    public void leggTilNyttDokument(
            @Parameter
            @NotNull
            @Valid
            @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class)
            SykdomDokumentOpprettelseDto sykdomDokumentOpprettelseDto) {
        final var behandling = behandlingRepository.hentBehandlingHvisFinnes(sykdomDokumentOpprettelseDto.getBehandlingUuid()).orElseThrow();
        if (behandling.getStatus().erFerdigbehandletStatus() || behandling.getStatus().equals(BehandlingStatus.FATTER_VEDTAK)) {
            throw new IllegalStateException("Behandlingen er ikke åpen for endringer.");
        }

        final LocalDateTime nå = LocalDateTime.now();
        final SykdomDokument dokument = new SykdomDokument(SykdomDokumentType.UKLASSIFISERT, sykdomDokumentOpprettelseDto.getJournalpostId(), null, getCurrentUserId(), nå, getCurrentUserId(), nå);

        sykdomDokumentRepository.lagre(dokument, behandling.getFagsak().getPleietrengendeAktørId());
    }

    @GET
    @Path(DOKUMENT_INNHOLD)
    @Operation(description = "Laster ned selve dokumentet (innholdet).", summary = ("Laster ned selve dokumentet (innholdet)."), tags = "dokument")
    @BeskyttetRessurs(action = READ, resource = FAGSAK)
    public Response hentDokumentinnhold(
            @NotNull @QueryParam(BehandlingUuidDto.NAME)
            @Parameter(description = BehandlingUuidDto.DESC)
            @Valid
            @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class)
            BehandlingUuidDto behandlingUuid,

            @QueryParam(SykdomDokumentIdDto.NAME)
            @Parameter(description = SykdomDokumentIdDto.DESC)
            @NotNull
            @Valid
            @TilpassetAbacAttributt(supplierClass = AbacDataSupplier.class)
            SykdomDokumentIdDto dokumentId) {
        final var behandling = behandlingRepository.hentBehandlingHvisFinnes(behandlingUuid.getBehandlingUuid()).orElseThrow();
        final var dokument = sykdomDokumentRepository.hentDokument(Long.valueOf(dokumentId.getSykdomDokumentId()), behandling.getFagsak().getPleietrengendeAktørId()).get();
        try {
            ResponseBuilder responseBuilder = Response.ok(
                new ByteArrayInputStream(dokumentArkivTjeneste.hentDokumnet(dokument.getJournalpostId(), dokument.getDokumentInfoId())));
            responseBuilder.type("application/pdf");
            responseBuilder.header("Content-Disposition", "filename=dokument.pdf");
            return responseBuilder.build();
        } catch (TekniskException e) {
            throw DokumentRestTjenesteFeil.FACTORY.dokumentIkkeFunnet(dokument.getJournalpostId(), dokument.getDokumentInfoId(), e).toException();
        } catch (ManglerTilgangException e) {
            throw DokumentRestTjenesteFeil.FACTORY.applikasjonHarIkkeTilgangTilHentDokumentTjeneste(e).toException();
        }
    }

    public static class AbacDataSupplier implements Function<Object, AbacDataAttributter> {
        @Override
        public AbacDataAttributter apply(Object obj) {
            return AbacDataAttributter.opprett();
        }
    }

    private static String getCurrentUserId() {
        return SubjectHandler.getSubjectHandler().getUid();
    }
}
