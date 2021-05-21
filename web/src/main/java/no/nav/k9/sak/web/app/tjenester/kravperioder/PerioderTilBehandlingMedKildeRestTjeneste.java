package no.nav.k9.sak.web.app.tjenester.kravperioder;

import static no.nav.k9.abac.BeskyttetRessursKoder.FAGSAK;
import static no.nav.k9.felles.sikkerhet.abac.BeskyttetRessursActionAttributt.READ;

import java.util.Collection;
import java.util.TreeSet;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import no.nav.k9.felles.sikkerhet.abac.BeskyttetRessurs;
import no.nav.k9.felles.sikkerhet.abac.TilpassetAbacAttributt;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.kontrakt.behandling.BehandlingUuidDto;
import no.nav.k9.sak.kontrakt.krav.StatusForPerioderPåBehandling;
import no.nav.k9.sak.perioder.VilkårsPerioderTilVurderingTjeneste;
import no.nav.k9.sak.web.app.tjenester.behandling.søknadsfrist.SøknadsfristTjenesteProvider;
import no.nav.k9.sak.web.server.abac.AbacAttributtSupplier;

@ApplicationScoped
@Transactional
@Path("")
@Produces(MediaType.APPLICATION_JSON)
public class PerioderTilBehandlingMedKildeRestTjeneste {

    private BehandlingRepository behandlingRepository;
    private SøknadsfristTjenesteProvider søknadsfristTjenesteProvider;
    private UtledStatusPåPerioderTjeneste statusPåPerioderTjeneste;
    private Instance<VilkårsPerioderTilVurderingTjeneste> perioderTilVurderingTjenester;

    public PerioderTilBehandlingMedKildeRestTjeneste() {
    }

    @Inject
    public PerioderTilBehandlingMedKildeRestTjeneste(BehandlingRepository behandlingRepository,
                                                     @Any Instance<VilkårsPerioderTilVurderingTjeneste> perioderTilVurderingTjenester,
                                                     SøknadsfristTjenesteProvider søknadsfristTjenesteProvider) {
        this.behandlingRepository = behandlingRepository;
        this.søknadsfristTjenesteProvider = søknadsfristTjenesteProvider;
        this.statusPåPerioderTjeneste = new UtledStatusPåPerioderTjeneste();
        this.perioderTilVurderingTjenester = perioderTilVurderingTjenester;
    }

    @GET
    @Path("/behandling/perioder")
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
        var søknadsfristTjeneste = søknadsfristTjenesteProvider.finnVurderSøknadsfristTjeneste(ref);
        var perioderTilVurderingTjeneste = VilkårsPerioderTilVurderingTjeneste.finnTjeneste(perioderTilVurderingTjenester, ref.getFagsakYtelseType(), ref.getBehandlingType());

        var kravdokumenter = søknadsfristTjeneste.relevanteKravdokumentForBehandling(ref);

        var kravdokumenterMedPeriode = søknadsfristTjeneste.hentPerioderTilVurdering(ref);
        var definerendeVilkår = perioderTilVurderingTjeneste.definerendeVilkår();

        var perioderTilVurdering = definerendeVilkår.stream()
            .map(it -> perioderTilVurderingTjeneste.utled(ref.getBehandlingId(), it))
            .flatMap(Collection::stream)
            .collect(Collectors.toCollection(TreeSet::new));

        var revurderingPerioderFraAndreParter = perioderTilVurderingTjeneste.utledRevurderingPerioder(ref);

        var statusForPerioderPåBehandling = statusPåPerioderTjeneste.utled(kravdokumenter, kravdokumenterMedPeriode, perioderTilVurdering, revurderingPerioderFraAndreParter);

        return statusForPerioderPåBehandling;
    }
}
