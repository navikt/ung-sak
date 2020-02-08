package no.nav.foreldrepenger.web.app.tjenester.behandling.beregningsresultat;

import static no.nav.vedtak.sikkerhet.abac.BeskyttetRessursActionAttributt.READ;
import static no.nav.vedtak.sikkerhet.abac.BeskyttetRessursResourceAttributt.FAGSAK;

import java.util.List;

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
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.kontrakter.tilkjentytelse.TilkjentYtelse;
import no.nav.foreldrepenger.web.app.tjenester.behandling.beregningsresultat.dto.BeregningsresultatDto;
import no.nav.foreldrepenger.web.server.abac.AbacAttributtSupplier;
import no.nav.foreldrepenger.økonomi.tilkjentytelse.TilkjentYtelseTjeneste;
import no.nav.k9.kodeverk.behandling.BehandlingResultatType;
import no.nav.k9.kodeverk.behandling.BehandlingType;
import no.nav.k9.kodeverk.behandling.KonsekvensForYtelsen;
import no.nav.k9.sak.kontrakt.behandling.BehandlingIdDto;
import no.nav.k9.sak.kontrakt.behandling.BehandlingUuidDto;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessurs;
import no.nav.vedtak.sikkerhet.abac.TilpassetAbacAttributt;

@Path("")
@Produces(MediaType.APPLICATION_JSON)
@ApplicationScoped
@Transactional
public class BeregningsresultatRestTjeneste {

    static public final String BEREGNINGSRESULTAT_PATH = "/behandling/beregningsresultat/";
    static public final String TILKJENTYTELSE_PATH = "/behandling/beregningsresultat/tilkjentytelse";
    static public final String HAR_SAMME_RESULTAT_PATH = "/behandling/beregningsresultat/har-samme-resultat";

    private BehandlingRepository behandlingRepository;
    private BeregningsresultatTjeneste beregningsresultatTjeneste;
    private TilkjentYtelseTjeneste tilkjentYtelseTjeneste;

    public BeregningsresultatRestTjeneste() {
        // for resteasy
    }

    @Inject
    public BeregningsresultatRestTjeneste(BehandlingRepositoryProvider behandlingRepositoryProvider,
                                          BeregningsresultatTjeneste beregningsresultatMedUttaksplanTjeneste, TilkjentYtelseTjeneste tilkjentYtelseTjeneste) {
        this.behandlingRepository = behandlingRepositoryProvider.getBehandlingRepository();
        this.beregningsresultatTjeneste = beregningsresultatMedUttaksplanTjeneste;
        this.tilkjentYtelseTjeneste = tilkjentYtelseTjeneste;
    }

    // FIXME K9 Erstatt denne tjenesten
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Path(BEREGNINGSRESULTAT_PATH)
    @Operation(description = "Hent beregningsresultat med uttaksplan for foreldrepenger behandling", summary = ("Returnerer beregningsresultat med uttaksplan for behandling."), tags = "beregningsresultat")
    @BeskyttetRessurs(action = READ, ressurs = FAGSAK)
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
    @Path(BEREGNINGSRESULTAT_PATH)
    @Operation(description = "Hent beregningsresultat med uttaksplan for foreldrepenger behandling", summary = ("Returnerer beregningsresultat med uttaksplan for behandling."), tags = "beregningsresultat")
    @BeskyttetRessurs(action = READ, ressurs = FAGSAK)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public BeregningsresultatDto hentBeregningsresultat(@NotNull @QueryParam(BehandlingUuidDto.NAME) @Parameter(description = BehandlingUuidDto.DESC) @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) BehandlingUuidDto uuidDto) {
        Behandling behandling = behandlingRepository.hentBehandling(uuidDto.getBehandlingUuid());
        return beregningsresultatTjeneste.lagBeregningsresultatMedUttaksplan(behandling).orElse(null);
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Path(TILKJENTYTELSE_PATH)
    @Operation(description = "Hent beregningsresultat", summary = ("Brukes av fpoppdrag."), tags = "beregningsresultat")
    @BeskyttetRessurs(action = READ, ressurs = FAGSAK)
    @Deprecated
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public TilkjentYtelse hentTilkjentYtelse(@NotNull @Parameter(description = "BehandlingId for aktuell behandling") @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) BehandlingIdDto behandlingIdDto) {
        Long behandlingId = behandlingIdDto.getBehandlingId();
        Behandling behandling = behandlingId != null
            ? behandlingRepository.hentBehandling(behandlingId)
            : behandlingRepository.hentBehandling(behandlingIdDto.getBehandlingUuid());
        return tilkjentYtelseTjeneste.hentilkjentYtelse(behandling.getId());
    }

    @GET
    @Path(TILKJENTYTELSE_PATH)
    @Operation(description = "Hent beregningsresultat", summary = ("Brukes av fpoppdrag."), tags = "beregningsresultat")
    @BeskyttetRessurs(action = READ, ressurs = FAGSAK)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public TilkjentYtelse hentTilkjentYtelse(@NotNull @QueryParam(BehandlingUuidDto.NAME) @Parameter(description = BehandlingUuidDto.DESC) @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) BehandlingUuidDto uuidDto) {
        Behandling behandling = behandlingRepository.hentBehandling(uuidDto.getBehandlingUuid());
        return tilkjentYtelseTjeneste.hentilkjentYtelse(behandling.getId());
    }

    @GET
    @Path(HAR_SAMME_RESULTAT_PATH)
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Har revurdering samme resultat som original behandling", tags = "beregningsresultat")
    @BeskyttetRessurs(action = READ, ressurs = FAGSAK)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public Boolean harRevurderingSammeResultatSomOriginalBehandling(@NotNull @QueryParam(BehandlingUuidDto.NAME) @Parameter(description = BehandlingUuidDto.DESC) @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) BehandlingUuidDto uuidDto) {
        Behandling behandling = behandlingRepository.hentBehandling(uuidDto.getBehandlingUuid());
        if (!BehandlingType.REVURDERING.getKode().equals(behandling.getType().getKode())) {
            throw new IllegalStateException("Behandling må være en revurdering");
        }

        var behandlingsresultat = behandling.getBehandlingsresultat();
        if (behandlingsresultat == null) {
            return false;
        }

        List<KonsekvensForYtelsen> konsekvenserForYtelsen = behandlingsresultat.getKonsekvenserForYtelsen();
        if (konsekvenserForYtelsen != null) {
            return konsekvenserForYtelsen.stream().anyMatch(kfy -> KonsekvensForYtelsen.INGEN_ENDRING.getKode().equals(kfy.getKode()));
        }

        Behandling originalBehandling = behandling.getOriginalBehandling().orElseThrow(() -> new IllegalStateException("Revurdering må ha originalbehandling"));
        Behandlingsresultat originaltBehandlingsresultat = originalBehandling.getBehandlingsresultat();
        BehandlingResultatType behandlingResultatType = behandlingsresultat.getBehandlingResultatType();

        boolean harSammeResultatType = behandlingResultatType.getKode().equals(originaltBehandlingsresultat.getBehandlingResultatType().getKode());
        return harSammeResultatType;
    }
}
