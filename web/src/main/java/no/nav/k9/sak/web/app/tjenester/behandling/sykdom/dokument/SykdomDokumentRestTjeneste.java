package no.nav.k9.sak.web.app.tjenester.behandling.sykdom.dokument;

import static no.nav.k9.abac.BeskyttetRessursKoder.FAGSAK;
import static no.nav.vedtak.sikkerhet.abac.BeskyttetRessursActionAttributt.READ;
import static no.nav.vedtak.sikkerhet.abac.BeskyttetRessursActionAttributt.UPDATE;

import java.time.LocalDateTime;
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
