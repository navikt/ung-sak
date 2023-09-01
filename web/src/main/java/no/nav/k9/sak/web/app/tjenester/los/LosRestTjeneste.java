package no.nav.k9.sak.web.app.tjenester.los;

import static no.nav.k9.abac.BeskyttetRessursKoder.FAGSAK;
import static no.nav.k9.felles.sikkerhet.abac.BeskyttetRessursActionAttributt.READ;
import static no.nav.k9.sak.web.app.tjenester.los.LosRestTjeneste.BASE_PATH;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import no.nav.k9.felles.jpa.TomtResultatException;
import no.nav.k9.felles.sikkerhet.abac.BeskyttetRessurs;
import no.nav.k9.felles.sikkerhet.abac.TilpassetAbacAttributt;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.SkjermlenkeType;
import no.nav.k9.kodeverk.historikk.HistorikkinnslagType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.historikk.HistorikkTjenesteAdapter;
import no.nav.k9.sak.kontrakt.behandling.BehandlingUuidDto;
import no.nav.k9.sak.kontrakt.produksjonsstyring.los.BehandlingMedFagsakDto;
import no.nav.k9.sak.perioder.KravDokument;
import no.nav.k9.sak.perioder.VurderSøknadsfristTjeneste;
import no.nav.k9.sak.perioder.VurdertSøktPeriode;
import no.nav.k9.sak.web.app.tjenester.behandling.BehandlingDtoTjeneste;
import no.nav.k9.sak.web.server.abac.AbacAttributtSupplier;
import no.nav.k9.sikkerhet.context.SubjectHandler;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.Set;

@ApplicationScoped
@Path(BASE_PATH)
@Transactional
@Produces(MediaType.APPLICATION_JSON)
public class LosRestTjeneste {

    public static final String BASE_PATH = "/los";
    public static final String MERKNAD = "/merknad";
    public static final String BEHANDLING = "/behandling";
    public static final String MERKNAD_PATH = BASE_PATH + MERKNAD;
    public static final String BEHANDLING_PATH = BASE_PATH + BEHANDLING;

    private LosSystemUserKlient losKlient;
    private HistorikkTjenesteAdapter historikkTjenesteAdapter;
    private BehandlingRepository behandlingRepository;
    private Instance<VurderSøknadsfristTjeneste<?>> søknadsfristTjenester;


    public LosRestTjeneste() {
        // For Rest-CDI
    }

    @Inject
    public LosRestTjeneste(
        LosSystemUserKlient losKlient,
        HistorikkTjenesteAdapter historikkTjenesteAdapter,
        BehandlingRepository behandlingRepository,
        BehandlingDtoTjeneste behandlingDtoTjeneste,
        @Any Instance<VurderSøknadsfristTjeneste<?>> søknadsfristTjenester) {
        this.losKlient = losKlient;
        this.historikkTjenesteAdapter = historikkTjenesteAdapter;
        this.behandlingRepository = behandlingRepository;
        this.søknadsfristTjenester = søknadsfristTjenester;
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
            dto.setEldsteDatoMedEndringFraSøker(finnEldsteMottattdato(behandling.get()));

            Response.ResponseBuilder responseBuilder = Response.ok().entity(dto);
            return responseBuilder.build();
        } else {
            return Response.noContent().build();
        }
    }

    private LocalDateTime finnEldsteMottattdato(Behandling behandling) {
        final var behandlingRef = BehandlingReferanse.fra(behandling);
        final var søknadsfristTjeneste = finnVurderSøknadsfristTjeneste(behandlingRef);
        if (søknadsfristTjeneste == null) {
            return null;
        }

        final Set<KravDokument> kravdokumenter = søknadsfristTjeneste.relevanteKravdokumentForBehandling(behandlingRef);
        if (kravdokumenter.isEmpty()) {
            return null;
        }

        return kravdokumenter.stream()
            .min(Comparator.comparing(KravDokument::getInnsendingsTidspunkt))
            .get()
            .getInnsendingsTidspunkt();
    }

    private VurderSøknadsfristTjeneste<VurdertSøktPeriode.SøktPeriodeData> finnVurderSøknadsfristTjeneste(BehandlingReferanse ref) {
        final FagsakYtelseType ytelseType = ref.getFagsakYtelseType();

        @SuppressWarnings("unchecked")
        final var tjeneste = (VurderSøknadsfristTjeneste<VurdertSøktPeriode.SøktPeriodeData>) FagsakYtelseTypeRef.Lookup.find(søknadsfristTjenester, ytelseType).orElse(null);
        return tjeneste;
    }

    @GET
    @Path(MERKNAD)
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Henter merknad på oppgave i los", tags = "los")
    @BeskyttetRessurs(action = READ, resource = FAGSAK)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public Response getMerknad(@NotNull @QueryParam(BehandlingUuidDto.NAME) @Parameter(description = BehandlingUuidDto.DESC) @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) BehandlingUuidDto behandlingUuid) {
        var merknad = losKlient.hentMerknad(behandlingUuid.getBehandlingUuid());
        var response = (merknad == null) ? Response.noContent() : Response.ok(merknad);
        return response.build();
    }

    @POST
    @Path(MERKNAD)
    @Operation(description = "Lagrer merknad på oppgave i los", tags = "los")
    @BeskyttetRessurs(action = READ, resource = FAGSAK) // Står som read så veileder har tilgang
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public Response postMerknad(@Parameter(description = BehandlingUuidDto.DESC) @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) MerknadEndretDto merknadEndret) {
        var merknad = losKlient.lagreMerknad(merknadEndret.overstyrSaksbehandlerIdent(getCurrentUserId()));
        if (merknad == null) {
            lagHistorikkinnslag(merknadEndret, HistorikkinnslagType.MERKNAD_FJERNET);
            return Response.noContent().build();
        } else {
            lagHistorikkinnslag(merknadEndret, HistorikkinnslagType.MERKNAD_NY);
            return Response.ok(merknad).build();
        }
    }

    private void lagHistorikkinnslag(MerknadEndretDto merknad, HistorikkinnslagType historikkinnslagType) {
        historikkTjenesteAdapter.tekstBuilder()
            .medSkjermlenke(SkjermlenkeType.UDEFINERT)
            .medHendelse(historikkinnslagType, String.join(",", merknad.merknadKoder()))
            .medBegrunnelse(merknad.fritekst());

        Long behandlingId = behandlingRepository.hentBehandling(merknad.behandlingUuid()).getId();
        historikkTjenesteAdapter.opprettHistorikkInnslag(behandlingId, HistorikkinnslagType.FAKTA_ENDRET);
    }

    private static String getCurrentUserId() {
        return SubjectHandler.getSubjectHandler().getUid();
    }
}
