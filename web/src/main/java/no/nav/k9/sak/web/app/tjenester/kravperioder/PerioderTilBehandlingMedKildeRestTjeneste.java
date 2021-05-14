package no.nav.k9.sak.web.app.tjenester.kravperioder;

import static no.nav.k9.abac.BeskyttetRessursKoder.FAGSAK;
import static no.nav.k9.felles.sikkerhet.abac.BeskyttetRessursActionAttributt.READ;

import java.util.Collection;
import java.util.List;
import java.util.Map;
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
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.kontrakt.behandling.BehandlingUuidDto;
import no.nav.k9.sak.kontrakt.krav.StatusForPerioderPåBehandling;
import no.nav.k9.sak.perioder.KravDokument;
import no.nav.k9.sak.perioder.SøktPeriode;
import no.nav.k9.sak.perioder.VilkårsPerioderTilVurderingTjeneste;
import no.nav.k9.sak.perioder.VurderSøknadsfristTjeneste;
import no.nav.k9.sak.perioder.VurdertSøktPeriode;
import no.nav.k9.sak.web.server.abac.AbacAttributtSupplier;

@ApplicationScoped
@Transactional
@Path("")
@Produces(MediaType.APPLICATION_JSON)
public class PerioderTilBehandlingMedKildeRestTjeneste {

    private BehandlingRepository behandlingRepository;
    private UtledStatusPåPerioderTjeneste statusPåPerioderTjeneste;
    private Instance<VilkårsPerioderTilVurderingTjeneste> perioderTilVurderingTjenester;
    private Instance<VurderSøknadsfristTjeneste<VurdertSøktPeriode.SøktPeriodeData>> søknadsfristTjenester;

    public PerioderTilBehandlingMedKildeRestTjeneste() {
    }

    @Inject
    public PerioderTilBehandlingMedKildeRestTjeneste(BehandlingRepository behandlingRepository,
                                                     @Any Instance<VilkårsPerioderTilVurderingTjeneste> perioderTilVurderingTjenester,
                                                     @Any Instance<VurderSøknadsfristTjeneste<VurdertSøktPeriode.SøktPeriodeData>> søknadsfristTjenester) {
        this.behandlingRepository = behandlingRepository;
        this.statusPåPerioderTjeneste = new UtledStatusPåPerioderTjeneste();
        this.perioderTilVurderingTjenester = perioderTilVurderingTjenester;
        this.søknadsfristTjenester = søknadsfristTjenester;
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
        var søknadsfristTjeneste = finnVurderSøknadsfristTjeneste(ref);
        var perioderTilVurderingTjeneste = VilkårsPerioderTilVurderingTjeneste.finnTjeneste(perioderTilVurderingTjenester, ref.getFagsakYtelseType(), ref.getBehandlingType());

        var kravdokumenter = søknadsfristTjeneste.relevanteKravdokumentForBehandling(ref);

        Map<KravDokument, List<SøktPeriode<VurdertSøktPeriode.SøktPeriodeData>>> kravdokumenterMedPeriode = søknadsfristTjeneste.hentPerioderTilVurdering(ref);
        var definerendeVilkår = perioderTilVurderingTjeneste.definerendeVilkår();

        var perioderTilVurdering = definerendeVilkår.stream()
            .map(it -> perioderTilVurderingTjeneste.utled(ref.getBehandlingId(), it))
            .flatMap(Collection::stream)
            .collect(Collectors.toCollection(TreeSet::new));

        var revurderingPerioderFraAndreParter = perioderTilVurderingTjeneste.utledRevurderingPerioder(ref);

        return statusPåPerioderTjeneste.utled(kravdokumenter, kravdokumenterMedPeriode, perioderTilVurdering, revurderingPerioderFraAndreParter);
    }

    private VurderSøknadsfristTjeneste<VurdertSøktPeriode.SøktPeriodeData> finnVurderSøknadsfristTjeneste(BehandlingReferanse ref) {
        FagsakYtelseType ytelseType = ref.getFagsakYtelseType();
        return FagsakYtelseTypeRef.Lookup.find(søknadsfristTjenester, ytelseType)
            .orElseThrow(() -> new UnsupportedOperationException("Har ikke " + VurderSøknadsfristTjeneste.class.getSimpleName() + " for ytelseType=" + ytelseType));
    }
}
