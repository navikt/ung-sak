package no.nav.k9.sak.web.app.tjenester.behandling.beregningsresultat;

import static no.nav.k9.abac.BeskyttetRessursKoder.FAGSAK;
import static no.nav.vedtak.sikkerhet.abac.BeskyttetRessursActionAttributt.READ;

import java.util.Objects;

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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import no.nav.k9.kodeverk.behandling.BehandlingResultatType;
import no.nav.k9.kodeverk.behandling.BehandlingType;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.kontrakt.behandling.BehandlingIdDto;
import no.nav.k9.sak.kontrakt.behandling.BehandlingUuidDto;
import no.nav.k9.sak.kontrakt.beregningsresultat.BeregningsresultatDto;
import no.nav.k9.sak.kontrakt.beregningsresultat.BeregningsresultatMedUtbetaltePeriodeDto;
import no.nav.k9.sak.kontrakt.beregningsresultat.TilkjentYtelseDto;
import no.nav.k9.sak.web.server.abac.AbacAttributtSupplier;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessurs;
import no.nav.vedtak.sikkerhet.abac.TilpassetAbacAttributt;

@Path(BeregningsresultatRestTjeneste.BASE_PATH)
@Produces(MediaType.APPLICATION_JSON)
@ApplicationScoped
@Transactional
public class BeregningsresultatRestTjeneste {

    public static final String BASE_PATH = "/behandling/beregningsresultat";
    public static final String BEREGNINGSRESULTAT = "/";
    static public final String BEREGNINGSRESULTAT_PATH = BASE_PATH + BEREGNINGSRESULTAT;
    public static final String HAR_SAMME_RESULTAT = "/har-samme-resultat";
    static public final String HAR_SAMME_RESULTAT_PATH = BASE_PATH + HAR_SAMME_RESULTAT;
    public static final String UTBETALT = "/utbetalt";
    public static final String TILKJENT_YTELSE = "/tilkjent_ytelse";
    static public final String BEREGNINGSRESULTAT_UTBETALT_PATH = BASE_PATH + UTBETALT;

    private BehandlingRepository behandlingRepository;
    private BeregningsresultatTjeneste beregningsresultatTjeneste;
    private TilkjentYtelseTjeneste tilkjentYtelseTjeneste;

    public BeregningsresultatRestTjeneste() {
        // for resteasy
    }

    @Inject
    public BeregningsresultatRestTjeneste(BehandlingRepositoryProvider repositoryProvider,
                                          BeregningsresultatTjeneste beregningsresultatMedUttaksplanTjeneste,
                                          TilkjentYtelseTjeneste tilkjentYtelseTjeneste) {
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.beregningsresultatTjeneste = beregningsresultatMedUttaksplanTjeneste;
        this.tilkjentYtelseTjeneste = tilkjentYtelseTjeneste;
    }

    // FIXME K9 Erstatt denne tjenesten
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Path(BEREGNINGSRESULTAT)
    @Operation(description = "Hent beregningsresultat med uttaksplan for foreldrepenger behandling", summary = ("Returnerer beregningsresultat med uttaksplan for behandling."), tags = "beregningsresultat")
    @BeskyttetRessurs(action = READ, resource = FAGSAK)
    @Deprecated
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public BeregningsresultatDto hentBeregningsresultat(@NotNull @Parameter(description = "BehandlingId for aktuell behandling") @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) BehandlingIdDto behandlingIdDto) {
        Long behandlingId = behandlingIdDto.getBehandlingId();
        Behandling behandling = behandlingId != null
            ? behandlingRepository.hentBehandling(behandlingId)
            : behandlingRepository.hentBehandling(behandlingIdDto.getBehandlingUuid());
        return beregningsresultatTjeneste.lagBeregningsresultatMedUttaksplan(behandling)
            .orElse(null);
    }

    // FIXME K9 Erstatt denne tjenesten
    @GET
    @Path(BEREGNINGSRESULTAT)
    @Operation(description = "Hent beregningsresultat med uttaksplan for foreldrepenger behandling", summary = ("Returnerer beregningsresultat med uttaksplan for behandling."), tags = "beregningsresultat")
    @BeskyttetRessurs(action = READ, resource = FAGSAK)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public BeregningsresultatDto hentBeregningsresultat(@NotNull @QueryParam(BehandlingUuidDto.NAME) @Parameter(description = BehandlingUuidDto.DESC) @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) BehandlingUuidDto behandlingUuid) {
        Behandling behandling = behandlingRepository.hentBehandling(behandlingUuid.getBehandlingUuid());
        return beregningsresultatTjeneste.lagBeregningsresultatMedUttaksplan(behandling).orElse(null);
    }

    @GET
    @Path(HAR_SAMME_RESULTAT)
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Har revurdering samme resultat som original behandling", tags = "beregningsresultat")
    @BeskyttetRessurs(action = READ, resource = FAGSAK)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public Boolean harRevurderingSammeResultatSomOriginalBehandling(@NotNull @QueryParam(BehandlingUuidDto.NAME) @Parameter(description = BehandlingUuidDto.DESC) @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) BehandlingUuidDto behandlingUuid) {
        Behandling behandling = behandlingRepository.hentBehandling(behandlingUuid.getBehandlingUuid());
        if (!BehandlingType.REVURDERING.getKode().equals(behandling.getType().getKode())) {
            throw new IllegalStateException("Behandling må være en revurdering");
        }

        var behandlingResultatType = behandling.getBehandlingResultatType();
        if (behandlingResultatType.isBehandlingsresultatIkkeEndret() || Objects.equals(BehandlingResultatType.IKKE_FASTSATT, behandlingResultatType)) {
            return false;
        }

        Long originalBehandlingId = behandling.getOriginalBehandlingId().orElseThrow(() -> new IllegalStateException("Revurdering må ha originalbehandling"));
        Behandling originalBehandling = behandlingRepository.hentBehandling(originalBehandlingId);

        boolean harSammeResultatType = behandlingResultatType.equals(originalBehandling.getBehandlingResultatType());
        return harSammeResultatType;
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Path(UTBETALT)
    @Operation(description = "Hent beregningsresultat med uttaksplan for foreldrepenger behandling", summary = ("Returnerer beregningsresultat med uttaksplan for behandling."), tags = "beregningsresultat")
    @BeskyttetRessurs(action = READ, resource = FAGSAK)
    @Deprecated
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public BeregningsresultatMedUtbetaltePeriodeDto hentBeregningsresultatMedUtbetaling(@NotNull @Parameter(description = "BehandlingId for aktuell behandling") @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) BehandlingIdDto behandlingIdDto) {
        Long behandlingId = behandlingIdDto.getBehandlingId();
        Behandling behandling = behandlingId != null
            ? behandlingRepository.hentBehandling(behandlingId)
            : behandlingRepository.hentBehandling(behandlingIdDto.getBehandlingUuid());
        return beregningsresultatTjeneste.lagBeregningsresultatMedUtbetaltePerioder(behandling) .orElse(null);
    }

    @GET
    @Path(UTBETALT)
    @Operation(description = "Hent beregningsresultat med uttaksplan for foreldrepenger behandling", summary = ("Returnerer beregningsresultat med uttaksplan for behandling."), tags = "beregningsresultat")
    @BeskyttetRessurs(action = READ, resource = FAGSAK)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public BeregningsresultatMedUtbetaltePeriodeDto hentBeregningsresultatMedUtbetaling(@NotNull @QueryParam(BehandlingUuidDto.NAME) @Parameter(description = BehandlingUuidDto.DESC) @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) BehandlingUuidDto behandlingUuid) {
        Behandling behandling = behandlingRepository.hentBehandling(behandlingUuid.getBehandlingUuid());
        return beregningsresultatTjeneste.lagBeregningsresultatMedUtbetaltePerioder(behandling).orElse(null);
    }

    @GET
    @Path(TILKJENT_YTELSE)
    @Operation(description = "Hent tilkjent ytelse for foreldrepenger behandling", summary = ("Returnerer beregningsresultat med uttaksplan for behandling."), tags = "beregningsresultat")
    @BeskyttetRessurs(action = READ, resource = FAGSAK)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public TilkjentYtelseDto hentTilkjentYtelse(@NotNull @QueryParam(BehandlingUuidDto.NAME) @Parameter(description = BehandlingUuidDto.DESC) @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) BehandlingUuidDto behandlingUuid) {
        Behandling behandling = behandlingRepository.hentBehandling(behandlingUuid.getBehandlingUuid());
        return tilkjentYtelseTjeneste.hentilkjentYtelse(behandling);
    }
}
