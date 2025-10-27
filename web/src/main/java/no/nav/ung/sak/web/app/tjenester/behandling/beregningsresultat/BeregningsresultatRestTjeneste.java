package no.nav.ung.sak.web.app.tjenester.behandling.beregningsresultat;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import no.nav.k9.felles.sikkerhet.abac.BeskyttetRessurs;
import no.nav.k9.felles.sikkerhet.abac.BeskyttetRessursResourceType;
import no.nav.k9.felles.sikkerhet.abac.TilpassetAbacAttributt;
import no.nav.ung.kodeverk.behandling.BehandlingResultatType;
import no.nav.ung.kodeverk.behandling.BehandlingType;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.ung.sak.kontrakt.behandling.BehandlingUuidDto;
import no.nav.ung.sak.kontrakt.beregningsresultat.BeregningsresultatDto;
import no.nav.ung.sak.web.server.abac.AbacAttributtSupplier;

import java.util.Objects;

import static no.nav.k9.felles.sikkerhet.abac.BeskyttetRessursActionType.READ;

@Path(BeregningsresultatRestTjeneste.BASE_PATH)
@Produces(MediaType.APPLICATION_JSON)
@ApplicationScoped
@Transactional
public class BeregningsresultatRestTjeneste {

    public static final String BASE_PATH = "/behandling/beregningsresultat";
    static public final String BEREGNINGSRESULTAT_PATH = BASE_PATH;
    public static final String HAR_SAMME_RESULTAT = "/har-samme-resultat";
    static public final String HAR_SAMME_RESULTAT_PATH = BASE_PATH + HAR_SAMME_RESULTAT;

    private BehandlingRepository behandlingRepository;
    private BeregningsresultatTjeneste beregningsresultatTjeneste;

    public BeregningsresultatRestTjeneste() {
        // for resteasy
    }

    @Inject
    public BeregningsresultatRestTjeneste(BehandlingRepositoryProvider repositoryProvider,
                                          BeregningsresultatTjeneste beregningsresultatMedUttaksplanTjeneste) {
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.beregningsresultatTjeneste = beregningsresultatMedUttaksplanTjeneste;
    }

    // Brukes av verdikjede for verifisering av resultat
    @GET
    @Operation(description = "Hent beregningsresultat med uttaksplan fra behandling", summary = ("Returnerer beregningsresultat med uttaksplan for behandling."), tags = "beregningsresultat")
    @BeskyttetRessurs(action = READ, resource = BeskyttetRessursResourceType.FAGSAK)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public BeregningsresultatDto hentBeregningsresultat(@NotNull @QueryParam(BehandlingUuidDto.NAME) @Parameter(description = BehandlingUuidDto.DESC) @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) BehandlingUuidDto behandlingUuid) {
        Behandling behandling = behandlingRepository.hentBehandling(behandlingUuid.getBehandlingUuid());
        return beregningsresultatTjeneste.lagBeregningsresultat(behandling).orElse(null);
    }

    @GET
    @Path(HAR_SAMME_RESULTAT)
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Har revurdering samme resultat som original behandling", tags = "beregningsresultat")
    @BeskyttetRessurs(action = READ, resource = BeskyttetRessursResourceType.FAGSAK)
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

}
