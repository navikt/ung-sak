package no.nav.k9.sak.web.app.tjenester.kravperioder;

import static no.nav.k9.abac.BeskyttetRessursKoder.FAGSAK;
import static no.nav.k9.felles.sikkerhet.abac.BeskyttetRessursActionAttributt.READ;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.TreeSet;
import java.util.stream.Collectors;

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
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import no.nav.k9.felles.sikkerhet.abac.BeskyttetRessurs;
import no.nav.k9.felles.sikkerhet.abac.TilpassetAbacAttributt;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.k9.sak.kontrakt.behandling.BehandlingUuidDto;
import no.nav.k9.sak.kontrakt.krav.PeriodeMedUtfall;
import no.nav.k9.sak.kontrakt.krav.StatusForPerioderPåBehandling;
import no.nav.k9.sak.kontrakt.krav.StatusForPerioderPåBehandlingInkludertVilkår;
import no.nav.k9.sak.perioder.VilkårsPerioderTilVurderingTjeneste;
import no.nav.k9.sak.web.app.tjenester.behandling.søknadsfrist.SøknadsfristTjenesteProvider;
import no.nav.k9.sak.web.server.abac.AbacAttributtSupplier;

@ApplicationScoped
@Transactional
@Path("")
@Produces(MediaType.APPLICATION_JSON)
public class PerioderTilBehandlingMedKildeRestTjeneste {

    public static final String BEHANDLING_PERIODER = "/behandling/perioder";
    public static final String BEHANDLING_PERIODER_MED_VILKÅR = "/behandling/perioder-med-vilkar";
    private BehandlingRepository behandlingRepository;
    private VilkårResultatRepository vilkårResultatRepository;
    private SøknadsfristTjenesteProvider søknadsfristTjenesteProvider;
    private UtledStatusPåPerioderTjeneste statusPåPerioderTjeneste;
    private Instance<VilkårsPerioderTilVurderingTjeneste> perioderTilVurderingTjenester;

    public PerioderTilBehandlingMedKildeRestTjeneste() {
    }

    @Inject
    public PerioderTilBehandlingMedKildeRestTjeneste(BehandlingRepository behandlingRepository,
                                                     @Any Instance<VilkårsPerioderTilVurderingTjeneste> perioderTilVurderingTjenester,
                                                     VilkårResultatRepository vilkårResultatRepository,
                                                     SøknadsfristTjenesteProvider søknadsfristTjenesteProvider) {
        this.behandlingRepository = behandlingRepository;
        this.vilkårResultatRepository = vilkårResultatRepository;
        this.søknadsfristTjenesteProvider = søknadsfristTjenesteProvider;
        this.statusPåPerioderTjeneste = new UtledStatusPåPerioderTjeneste();
        this.perioderTilVurderingTjenester = perioderTilVurderingTjenester;
    }

    @GET
    @Path(BEHANDLING_PERIODER)
    @Operation(description = "Hent perioder til behandling og kilden til disse",
        summary = ("Hent perioder til behandling og kilden til disse"),
        tags = "perioder",
        responses = {
            @ApiResponse(responseCode = "200", description = "Liste med periode og årsaken til at perioden behandles", content = {
                @Content(mediaType = "application/json", schema = @Schema(implementation = StatusForPerioderPåBehandling.class))
            }),
        })
    @BeskyttetRessurs(action = READ, resource = FAGSAK)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public StatusForPerioderPåBehandling hentPerioderTilBehandling(@NotNull @QueryParam(BehandlingUuidDto.NAME) @Parameter(description = BehandlingUuidDto.DESC) @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) BehandlingUuidDto behandlingUuid) {
        var behandling = behandlingRepository.hentBehandling(behandlingUuid.getBehandlingUuid());
        var ref = BehandlingReferanse.fra(behandling);
        StatusForPerioderPåBehandling statusForPerioderPåBehandling = getStatusForPerioderPåBehandling(ref, behandling, VilkårsPerioderTilVurderingTjeneste.finnTjeneste(perioderTilVurderingTjenester, ref.getFagsakYtelseType(), ref.getBehandlingType()));

        return statusForPerioderPåBehandling;
    }

    @GET
    @Path(BEHANDLING_PERIODER_MED_VILKÅR)
    @Operation(description = "Hent perioder til behandling og kilden til disse",
        summary = ("Hent perioder til behandling og kilden til disse"),
        tags = "perioder",
        responses = {
            @ApiResponse(responseCode = "200", description = "Liste med periode og årsaken til at perioden behandles", content = {
                @Content(mediaType = "application/json", schema = @Schema(implementation = StatusForPerioderPåBehandling.class))
            }),
        })
    @BeskyttetRessurs(action = READ, resource = FAGSAK)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public StatusForPerioderPåBehandlingInkludertVilkår hentPerioderMedVilkårForBehandling(@NotNull @QueryParam(BehandlingUuidDto.NAME) @Parameter(description = BehandlingUuidDto.DESC) @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) BehandlingUuidDto behandlingUuid) {
        var behandling = behandlingRepository.hentBehandling(behandlingUuid.getBehandlingUuid());
        var ref = BehandlingReferanse.fra(behandling);
        var perioderTilVurderingTjeneste = VilkårsPerioderTilVurderingTjeneste.finnTjeneste(perioderTilVurderingTjenester, ref.getFagsakYtelseType(), ref.getBehandlingType());
        StatusForPerioderPåBehandling statusForPerioderPåBehandling = getStatusForPerioderPåBehandling(ref, behandling, perioderTilVurderingTjeneste);

        return new StatusForPerioderPåBehandlingInkludertVilkår(statusForPerioderPåBehandling,
            mapVilkårMedUtfall(behandling.getId(), perioderTilVurderingTjeneste),
            behandling.getOriginalBehandlingId().map(behandlingId -> mapVilkårMedUtfall(behandlingId, perioderTilVurderingTjeneste)).orElse(List.of()));
    }

    private List<PeriodeMedUtfall> mapVilkårMedUtfall(Long behandlingId, VilkårsPerioderTilVurderingTjeneste perioderTilVurderingTjeneste) {
        var opt = vilkårResultatRepository.hentHvisEksisterer(behandlingId);

        var definerendeVilkår = perioderTilVurderingTjeneste.definerendeVilkår();

        var result = new ArrayList<PeriodeMedUtfall>();

        for (VilkårType vilkårType : definerendeVilkår) {
            if (opt.isEmpty() || opt.get().getVilkår(vilkårType).isEmpty()) {
                continue;
            }

            var vilkår = opt.get().getVilkår(vilkårType).orElseThrow();
            result.addAll(vilkår.getPerioder()
                .stream()
                .map(it -> new PeriodeMedUtfall(it.getPeriode().tilPeriode(), it.getGjeldendeUtfall()))
                .toList());
        }

        return result;
    }

    private StatusForPerioderPåBehandling getStatusForPerioderPåBehandling(BehandlingReferanse ref, Behandling behandling, VilkårsPerioderTilVurderingTjeneste perioderTilVurderingTjeneste) {
        var søknadsfristTjeneste = søknadsfristTjenesteProvider.finnVurderSøknadsfristTjeneste(ref);

        var kravdokumenter = søknadsfristTjeneste.relevanteKravdokumentForBehandling(ref);
        var perioderSomSkalTilbakestilles = perioderTilVurderingTjeneste.perioderSomSkalTilbakestilles(ref.getBehandlingId());

        var kravdokumenterMedPeriode = søknadsfristTjeneste.hentPerioderTilVurdering(ref);
        var definerendeVilkår = perioderTilVurderingTjeneste.definerendeVilkår();

        var perioderTilVurdering = definerendeVilkår.stream()
            .map(it -> perioderTilVurderingTjeneste.utled(ref.getBehandlingId(), it))
            .flatMap(Collection::stream)
            .collect(Collectors.toCollection(TreeSet::new));

        perioderTilVurdering.addAll(perioderTilVurderingTjeneste.utledUtvidetRevurderingPerioder(ref));

        var revurderingPerioderFraAndreParter = perioderTilVurderingTjeneste.utledRevurderingPerioder(ref);
        var kantIKantVurderer = perioderTilVurderingTjeneste.getKantIKantVurderer();

        var statusForPerioderPåBehandling = statusPåPerioderTjeneste.utled(behandling, kantIKantVurderer, kravdokumenter, kravdokumenterMedPeriode, perioderTilVurdering, perioderSomSkalTilbakestilles, revurderingPerioderFraAndreParter);
        return statusForPerioderPåBehandling;
    }
}
