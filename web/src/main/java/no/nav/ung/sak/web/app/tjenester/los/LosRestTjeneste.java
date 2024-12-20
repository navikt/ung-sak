package no.nav.ung.sak.web.app.tjenester.los;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import no.nav.k9.felles.sikkerhet.abac.BeskyttetRessurs;
import no.nav.k9.felles.sikkerhet.abac.TilpassetAbacAttributt;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;
import no.nav.ung.sak.behandling.hendelse.produksjonsstyring.BehandlingProsessHendelseMapper;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.merknad.BehandlingMerknad;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.ung.sak.historikk.HistorikkTjenesteAdapter;
import no.nav.ung.sak.kontrakt.behandling.BehandlingUuidDto;
import no.nav.ung.sak.kontrakt.produksjonsstyring.los.BehandlingMedFagsakDto;
import no.nav.ung.sak.kontrakt.produksjonsstyring.los.LosOpplysningerSomManglerIKlageDto;
import no.nav.ung.sak.web.app.tjenester.los.dto.MerknadDto;
import no.nav.ung.sak.web.server.abac.AbacAttributtSupplier;
import no.nav.k9.sikkerhet.context.SubjectHandler;

import java.util.ArrayList;
import java.util.Optional;

import static no.nav.ung.abac.BeskyttetRessursKoder.FAGSAK;
import static no.nav.k9.felles.sikkerhet.abac.BeskyttetRessursActionAttributt.READ;
import static no.nav.ung.sak.web.app.tjenester.los.LosRestTjeneste.BASE_PATH;

@ApplicationScoped
@Path(BASE_PATH)
@Transactional
@Produces(MediaType.APPLICATION_JSON)
public class LosRestTjeneste {

    public static final String BASE_PATH = "/los";
    public static final String MERKNAD = "/merknad";
    public static final String BEHANDLING = "/behandling";
    public static final String BERIK_KLAGE = "/klage/berik";
    public static final String MERKNAD_PATH = BASE_PATH + MERKNAD;
    public static final String BEHANDLING_PATH = BASE_PATH + BEHANDLING;

    private LosMerknadTjeneste losMerknadTjeneste;
    private BehandlingRepository behandlingRepository;
    private BehandlingProsessHendelseMapper behandlingProsessHendelseMapper;


    public LosRestTjeneste() {
        // For Rest-CDI
    }

    @Inject
    public LosRestTjeneste(
        LosMerknadTjeneste losMerknadTjeneste,
        HistorikkTjenesteAdapter historikkTjenesteAdapter,
        BehandlingRepository behandlingRepository,
        BehandlingProsessHendelseMapper behandlingProsessHendelseMapper, FagsakRepository fagsakRepository) {
        this.losMerknadTjeneste = losMerknadTjeneste;
        this.behandlingRepository = behandlingRepository;
        this.behandlingProsessHendelseMapper = behandlingProsessHendelseMapper;
    }

    @GET
    @Path(BERIK_KLAGE)
    @Operation(
        description = "",
        summary = "",
        tags = "los",
        responses = {
            @ApiResponse(
                responseCode = "200",
                description = "Returnerer opplysninger los trenger for å fylle ut klageoppgaver",
                content = @Content(mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = LosOpplysningerSomManglerIKlageDto.class))
            )
        })
    @BeskyttetRessurs(action = READ, resource = FAGSAK)
    public Response hentLosdataForKlage(
        @NotNull
        @QueryParam(BehandlingUuidDto.NAME)
        @Parameter(description = BehandlingUuidDto.DESC)
        @Valid
        @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class)
        BehandlingUuidDto påklagdBehandlingUuid) {
        Optional<Behandling> behandling = behandlingRepository.hentBehandlingHvisFinnes(påklagdBehandlingUuid.getBehandlingUuid());
        if (behandling.isPresent()) {
            LosOpplysningerSomManglerIKlageDto dto = new LosOpplysningerSomManglerIKlageDto();
            dto.setUtenlandstilsnitt(behandling.get().getAksjonspunkter()
                .stream()
                .filter(ap -> !ap.erAvbrutt())
                .anyMatch(ap ->
                    ap.getAksjonspunktDefinisjon().getKode().equals(AksjonspunktKodeDefinisjon.AUTOMATISK_MARKERING_AV_UTENLANDSSAK_KODE)
                 || ap.getAksjonspunktDefinisjon().getKode().equals(AksjonspunktKodeDefinisjon.MANUELL_MARKERING_AV_UTLAND_SAKSTYPE_KODE)));

            Response.ResponseBuilder responseBuilder = Response.ok().entity(dto);
            return responseBuilder.build();
        } else {
            return Response.noContent().build();
        }
    }

    @GET
    @Path(BEHANDLING)
    @Operation(
        description = "Hent behandling gitt id",
        summary = ("Returnerer behandlingen som er tilknyttet id."),
        tags = "los",
        responses = {
            @ApiResponse(
                responseCode = "200",
                description = "Returnerer Behandling",
                content = @Content(mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = BehandlingMedFagsakDto.class)))
        })
    @BeskyttetRessurs(action = READ, resource = FAGSAK)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public Response hentBehandlingData(@NotNull @QueryParam(BehandlingUuidDto.NAME) @Parameter(description = BehandlingUuidDto.DESC) @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) BehandlingUuidDto behandlingUuid) {
        var behandling = behandlingRepository.hentBehandlingHvisFinnes(behandlingUuid.getBehandlingUuid());
        if (behandling.isPresent()) {
            BehandlingMedFagsakDto dto = new BehandlingMedFagsakDto();
            dto.setSakstype(behandling.get().getFagsakYtelseType());
            dto.setBehandlingResultatType(behandling.get().getBehandlingResultatType());
            dto.setEldsteDatoMedEndringFraSøker(behandlingProsessHendelseMapper.finnEldsteMottattdato(behandling.get()));

            Response.ResponseBuilder responseBuilder = Response.ok().entity(dto);
            return responseBuilder.build();
        } else {
            return Response.noContent().build();
        }
    }

    @GET
    @Path(MERKNAD)
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Henter merknad på behandling", tags = "los", responses = {
        @ApiResponse(responseCode = "200", description = "Returnerer merknader", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = MerknadDto.class))),
        @ApiResponse(responseCode = "204", description = "Var ingen merknader")
    })
    @BeskyttetRessurs(action = READ, resource = FAGSAK)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public Response getMerknad(@NotNull @QueryParam(BehandlingUuidDto.NAME) @Parameter(description = BehandlingUuidDto.DESC) @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) BehandlingUuidDto behandlingUuid) {
        var merknad = losMerknadTjeneste.hertMerknader(behandlingUuid.getBehandlingUuid());
        var response = merknad.isEmpty()? Response.noContent() : Response.ok(map(merknad.get()));
        return response.build();
    }

    @POST
    @Path(MERKNAD)
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Lagrer merknad på behandling", tags = "los")
    @BeskyttetRessurs(action = READ, resource = FAGSAK) // Står som read så veileder har tilgang
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public Response postMerknad(@Parameter(description = BehandlingUuidDto.DESC) @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) MerknadEndretDto merknadEndret) {
        losMerknadTjeneste.lagreMerknad(merknadEndret.overstyrSaksbehandlerIdent(getCurrentUserId()));
        return Response.ok().build();
    }

    private static String getCurrentUserId() {
        return SubjectHandler.getSubjectHandler().getUid();
    }

    private static MerknadDto map(BehandlingMerknad behandlingMerknad) {
        return new MerknadDto(new ArrayList<>(behandlingMerknad.merknadTyper()), behandlingMerknad.fritekst());
    }
}
