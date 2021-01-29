package no.nav.k9.sak.web.app.tjenester.behandling.sykdom.dokument;

import static no.nav.k9.abac.BeskyttetRessursKoder.FAGSAK;
import static no.nav.vedtak.sikkerhet.abac.BeskyttetRessursActionAttributt.READ;
import static no.nav.vedtak.sikkerhet.abac.BeskyttetRessursActionAttributt.UPDATE;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.kontrakt.behandling.BehandlingUuidDto;
import no.nav.k9.sak.web.server.abac.AbacAttributtSupplier;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomDokument;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomDokumentRepository;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomDokumentType;
import no.nav.vedtak.sikkerhet.abac.AbacDataAttributter;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessurs;
import no.nav.vedtak.sikkerhet.abac.TilpassetAbacAttributt;
import no.nav.vedtak.sikkerhet.context.SubjectHandler;

@Produces(MediaType.APPLICATION_JSON)
@ApplicationScoped
@Path(SykdomDokumentRestTjeneste.BASE_PATH)
@Transactional
public class SykdomDokumentRestTjeneste {

    public static final String BASE_PATH = "/behandling/sykdom/dokument";
    private static final String DOKUMENT = "/";
    private static final String DOKUMENT_NY = "/";
    private static final String SYKDOM_INNLEGGELSE = "/innleggelse/";
    public static final String SYKDOM_INNLEGGELSE_PATH = BASE_PATH + SYKDOM_INNLEGGELSE;
    private static final String DIAGNOSEKODER = "/diagnosekoder/";
    public static final String DIAGNOSEKODER_PATH = BASE_PATH + DIAGNOSEKODER;
    public static final String DOKUMENT_PATH = BASE_PATH + DOKUMENT;
    private static final String DOKUMENT_OVERSIKT = "/oversikt";
    public static final String DOKUMENT_OVERSIKT_PATH = BASE_PATH + DOKUMENT_OVERSIKT;

    private BehandlingRepository behandlingRepository;
    private SykdomDokumentOversiktMapper sykdomDokumentOversiktMapper;
    private SykdomDokumentRepository sykdomDokumentRepository;
    

    public SykdomDokumentRestTjeneste() {
    }
    

    @Inject
    public SykdomDokumentRestTjeneste(BehandlingRepository behandlingRepository, SykdomDokumentOversiktMapper sykdomDokumentOversiktMapper, SykdomDokumentRepository sykdomDokumentRepository) {
        this.behandlingRepository = behandlingRepository;
        this.sykdomDokumentOversiktMapper = sykdomDokumentOversiktMapper;
        this.sykdomDokumentRepository = sykdomDokumentRepository;
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
        
        // TODO: Mapping av sykdominnleggelse:
        return new SykdomInnleggelseDto(behandling.getUuid(), "0", Collections.emptyList());
    }
    
    @POST
    @Path(SYKDOM_INNLEGGELSE)
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
        // TODO: Mapping av sykdominnleggelse:
    }
    
    @GET
    @Path(DIAGNOSEKODER)
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
        
        // TODO: Mapping av diagnosekoder:
        return new SykdomDiagnosekoderDto(behandling.getUuid(), "0", Collections.emptyList());
    }
    
    @POST
    @Path(DIAGNOSEKODER)
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
            SykdomDiagnosekoderDto sykdomDiagnosekoder) {
        
        final var behandling = behandlingRepository.hentBehandlingHvisFinnes(sykdomDiagnosekoder.getBehandlingUuid()).orElseThrow();
        // TODO: Mapping av diagnosekoder:
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
        if (behandling.getStatus().erFerdigbehandletStatus()) {
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
        if (behandling.getStatus().erFerdigbehandletStatus()) {
            throw new IllegalStateException("Behandlingen er ikke åpen for endringer.");
        }

        final LocalDateTime nå = LocalDateTime.now();
        final SykdomDokument dokument = new SykdomDokument(SykdomDokumentType.UKLASSIFISERT, sykdomDokumentOpprettelseDto.getJournalpostId(), null, getCurrentUserId(), nå, getCurrentUserId(), nå);
        
        sykdomDokumentRepository.lagre(dokument, behandling.getFagsak().getPleietrengendeAktørId());
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
