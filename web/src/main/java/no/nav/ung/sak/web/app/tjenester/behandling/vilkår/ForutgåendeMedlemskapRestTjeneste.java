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
import no.nav.k9.søknad.ytelse.aktivitetspenger.v1.Bosteder;
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
import no.nav.ung.sak.kontrakt.vilkår.medlemskap.MedlemskapsPeriodeDto;
import no.nav.ung.sak.typer.Periode;
import no.nav.ung.sak.web.server.abac.AbacAttributtSupplier;
import no.nav.ung.sak.web.server.caching.CacheControl;
import no.nav.ung.ytelse.aktivitetspenger.medlemskap.ForutgåendeMedlemskapTjeneste;
import no.nav.ung.ytelse.aktivitetspenger.medlemskap.TrygdeavtaleLandOppslag;

import java.util.*;
import java.util.stream.Collectors;

import static no.nav.k9.felles.sikkerhet.abac.BeskyttetRessursActionType.READ;

@Path("")
@Produces(MediaType.APPLICATION_JSON)
@ApplicationScoped
@Transactional
public class ForutgåendeMedlemskapRestTjeneste {

    public static final String MEDLEMSKAP = "/behandling/medlemskap";
    private static final Map<String, String> LANDKODE_TIL_NORSK_NAVN = lagLandkodeTilNorskNavn();

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

        var medlemskap = forutgåendeMedlemskapTjeneste.utledForutgåendeBosteder(behandling.getFagsakId(), behandling.getId())
            .map(this::mapTilDto)
            .orElse(List.of());

        var vilkår = vilkårResultatRepository.hent(behandling.getId())
            .getVilkår(VilkårType.FORUTGÅENDE_MEDLEMSKAPSVILKÅRET);

        var utfall = finnUtfall(vilkår);

        MedlemskapAvslagsÅrsakType avslagsårsak = null;
        if (utfall == Utfall.IKKE_OPPFYLT) {
            avslagsårsak = finnAvslagsårsak(vilkår);
        }

        return new ForutgåendeMedlemskapResponse(medlemskap, utfall, avslagsårsak);
    }

    private static MedlemskapAvslagsÅrsakType finnAvslagsårsak(Optional<Vilkår> vilkår) {
        var avslagsårsaker = vilkår
            .map(Vilkår::getPerioder)
            .stream()
            .flatMap(List::stream)
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

    private static Utfall finnUtfall(Optional<Vilkår> vilkår) {
        Set<Utfall> alleUtfall = vilkår
            .map(Vilkår::getPerioder)
            .stream()
            .flatMap(List::stream)
            .map(VilkårPeriode::getGjeldendeUtfall)
            .collect(Collectors.toSet());

        if (alleUtfall.size() > 1) {
            throw new IllegalStateException("Kan ikke ha periodiserte utfall på medlemskap");
        }

        return alleUtfall.stream().findFirst().orElse(Utfall.IKKE_VURDERT);
    }

    private List<MedlemskapsPeriodeDto> mapTilDto(Bosteder bosteder) {
        return bosteder.getPerioder().entrySet().stream().map(it ->  {
            var di = it.getKey();
            var bosted = it.getValue();
            var landkode = bosted.getLand().getLandkode();

            return new MedlemskapsPeriodeDto(
                new Periode(di.getFraOgMed(), di.getTilOgMed()),
                mapLandTilNorskNavn(landkode),
                landkode,
                TrygdeavtaleLandOppslag.erGyldigTrygdeavtaleLand(bosted.getLand(), di.getFraOgMed())
            );
            }
        ).toList();
    }

    private static Map<String, String> lagLandkodeTilNorskNavn() {
        Map<String, String> result = new HashMap<>();
        for (String alpha2 : Locale.getISOCountries()) {
            try {
                Locale locale = new Locale.Builder().setRegion(alpha2).build();
                result.put(locale.getISO3Country(), locale.getDisplayCountry(Locale.forLanguageTag("nb-NO")));
            } catch (MissingResourceException | IllformedLocaleException ignored) {
            }
        }
        return Map.copyOf(result);
    }

    private static String mapLandTilNorskNavn(String landkodeAlpha3) {
        return LANDKODE_TIL_NORSK_NAVN.getOrDefault(landkodeAlpha3, landkodeAlpha3);
    }

}
