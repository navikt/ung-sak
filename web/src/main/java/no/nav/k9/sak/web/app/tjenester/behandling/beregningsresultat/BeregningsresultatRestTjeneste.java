package no.nav.k9.sak.web.app.tjenester.behandling.beregningsresultat;

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
import no.nav.k9.sak.web.server.abac.AbacAttributtSupplier;
import no.nav.k9.sak.økonomi.tilkjentytelse.TilkjentYtelseTjeneste;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessurs;
import no.nav.vedtak.sikkerhet.abac.TilpassetAbacAttributt;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.Objects;

import static no.nav.k9.abac.BeskyttetRessursKoder.FAGSAK;
import static no.nav.vedtak.sikkerhet.abac.BeskyttetRessursActionAttributt.READ;

@Path("")
@Produces(MediaType.APPLICATION_JSON)
@ApplicationScoped
@Transactional
public class BeregningsresultatRestTjeneste {

    static public final String BEREGNINGSRESULTAT_PATH = "/behandling/beregningsresultat/";
    static public final String HAR_SAMME_RESULTAT_PATH = "/behandling/beregningsresultat/har-samme-resultat";

    private BehandlingRepository behandlingRepository;
    private BeregningsresultatTjeneste beregningsresultatTjeneste;

    public BeregningsresultatRestTjeneste() {
        // for resteasy
    }

    @Inject
    public BeregningsresultatRestTjeneste(BehandlingRepositoryProvider repositoryProvider,
                                          BeregningsresultatTjeneste beregningsresultatMedUttaksplanTjeneste,
                                          TilkjentYtelseTjeneste tilkjentYtelseTjeneste) {
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.beregningsresultatTjeneste = beregningsresultatMedUttaksplanTjeneste;
    }

    // FIXME K9 Erstatt denne tjenesten
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Path(BEREGNINGSRESULTAT_PATH)
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
    @Path(BEREGNINGSRESULTAT_PATH)
    @Operation(description = "Hent beregningsresultat med uttaksplan for foreldrepenger behandling", summary = ("Returnerer beregningsresultat med uttaksplan for behandling."), tags = "beregningsresultat")
    @BeskyttetRessurs(action = READ, resource = FAGSAK)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public BeregningsresultatDto hentBeregningsresultat(@NotNull @QueryParam(BehandlingUuidDto.NAME) @Parameter(description = BehandlingUuidDto.DESC) @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) BehandlingUuidDto behandlingUuid) {
        Behandling behandling = behandlingRepository.hentBehandling(behandlingUuid.getBehandlingUuid());
        return beregningsresultatTjeneste.lagBeregningsresultatMedUttaksplan(behandling).orElse(null);
    }

    @GET
    @Path(HAR_SAMME_RESULTAT_PATH)
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

        Behandling originalBehandling = behandling.getOriginalBehandling().orElseThrow(() -> new IllegalStateException("Revurdering må ha originalbehandling"));

        boolean harSammeResultatType = behandlingResultatType.equals(originalBehandling.getBehandlingResultatType());
        return harSammeResultatType;
    }
}
