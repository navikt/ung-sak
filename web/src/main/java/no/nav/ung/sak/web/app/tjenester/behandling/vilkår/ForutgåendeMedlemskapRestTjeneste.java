package no.nav.ung.sak.web.app.tjenester.behandling.vilkår;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.enterprise.context.ApplicationScoped;
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
import no.nav.k9.felles.sikkerhet.abac.BeskyttetRessursResourceType;
import no.nav.k9.felles.sikkerhet.abac.TilpassetAbacAttributt;
import no.nav.ung.kodeverk.vilkår.Avslagsårsak;
import no.nav.ung.kodeverk.vilkår.Utfall;
import no.nav.ung.kodeverk.vilkår.VilkårType;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.Vilkår;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriode;
import no.nav.ung.sak.kontrakt.aktivitetspenger.medlemskap.MedlemskapAvslagsÅrsakType;
import no.nav.ung.sak.kontrakt.behandling.BehandlingUuidDto;
import no.nav.ung.sak.kontrakt.vilkår.medlemskap.ForutgåendeMedlemskapResponse;
import no.nav.ung.sak.web.server.abac.AbacAttributtSupplier;
import no.nav.ung.sak.web.server.caching.CacheControl;
import no.nav.ung.ytelse.aktivitetspenger.medlemskap.ForutgåendeMedlemskapTjeneste;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static no.nav.k9.felles.sikkerhet.abac.BeskyttetRessursActionType.READ;

@Path("")
@Produces(MediaType.APPLICATION_JSON)
@ApplicationScoped
@Transactional
public class ForutgåendeMedlemskapRestTjeneste {

    public static final String MEDLEMSKAP = "/behandling/medlemskap";

    private BehandlingRepository behandlingRepository;
    private VilkårResultatRepository vilkårResultatRepository;
    private ForutgåendeMedlemskapTjeneste forutgåendeMedlemskapTjeneste;

    public ForutgåendeMedlemskapRestTjeneste() {
        // for CDI proxy
    }

    @Inject
    public ForutgåendeMedlemskapRestTjeneste(BehandlingRepository behandlingRepository,
                                             VilkårResultatRepository vilkårResultatRepository,
                                             ForutgåendeMedlemskapTjeneste forutgåendeMedlemskapTjeneste) {
        this.behandlingRepository = behandlingRepository;
        this.vilkårResultatRepository = vilkårResultatRepository;
        this.forutgåendeMedlemskapTjeneste = forutgåendeMedlemskapTjeneste;
    }

    @GET
    @Path(MEDLEMSKAP)
    @Operation(description = "Hent informasjon om forutgående medlemskap", tags = "forutgående medlemskap")
    @BeskyttetRessurs(action = READ, resource = BeskyttetRessursResourceType.FAGSAK)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    @CacheControl()
    public ForutgåendeMedlemskapResponse medlemskap(@NotNull @QueryParam(BehandlingUuidDto.NAME) @Parameter(description = BehandlingUuidDto.DESC) @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) BehandlingUuidDto behandlingUuid) {
        Behandling behandling = behandlingRepository.hentBehandling(behandlingUuid.getBehandlingUuid());

        var medlemskap = forutgåendeMedlemskapTjeneste.hentBostederSomDto(behandling.getId());

        var vilkår = vilkårResultatRepository.hent(behandling.getId())
            .getVilkår(VilkårType.FORUTGÅENDE_MEDLEMSKAPSVILKÅRET).orElseThrow( () -> new IllegalStateException("Mangler vilkårsvurdering av forutgående medlemskap"));

        var utfall = finnUtfall(vilkår);

        MedlemskapAvslagsÅrsakType avslagsårsak = null;
        if (utfall == Utfall.IKKE_OPPFYLT) {
            avslagsårsak = finnAvslagsårsak(vilkår);
        }

        return new ForutgåendeMedlemskapResponse(medlemskap, utfall, avslagsårsak);
    }

    private static MedlemskapAvslagsÅrsakType finnAvslagsårsak(Vilkår vilkår) {
        var avslagsårsaker = vilkår.getPerioder()
            .stream()
            .map(VilkårPeriode::getAvslagsårsak)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
        if (avslagsårsaker.size() > 1) {
            throw new IllegalStateException("Kan ikke ha flere enn en avslagsårsak for medlemskap");
        }
        Avslagsårsak avslagsårsak = avslagsårsaker.stream().findFirst().orElseThrow();

        return switch (avslagsårsak) {
            case SØKER_ER_IKKE_MEDLEM -> MedlemskapAvslagsÅrsakType.SØKER_IKKE_MEDLEM;
            default -> throw new IllegalStateException("Unexpected value: " + avslagsårsak);
        };

    }

    private static Utfall finnUtfall(Vilkår vilkår) {
        Set<Utfall> alleUtfall = vilkår.getPerioder()
            .stream()
            .map(VilkårPeriode::getGjeldendeUtfall)
            .collect(Collectors.toSet());

        if (alleUtfall.isEmpty()) {
            throw new IllegalStateException("Utviklerfeil: Mangler utfall");
        }

        if (alleUtfall.contains(Utfall.IKKE_VURDERT)) {
            return Utfall.IKKE_VURDERT;
        }

        if (alleUtfall.size() > 1) {
            throw new IllegalStateException("Utviklerfeil: Kan ikke ha periodiserte utfall på forutgående medlemskap. Har både oppfylte og ikke oppfylte.");
        }


        return alleUtfall.stream().findFirst().orElseThrow();
    }

}
