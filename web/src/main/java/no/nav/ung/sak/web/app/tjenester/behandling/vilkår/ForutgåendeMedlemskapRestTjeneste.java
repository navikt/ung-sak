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
import no.nav.k9.søknad.felles.type.Periode;
import no.nav.k9.søknad.ytelse.aktivitetspenger.v1.Bosteder;
import no.nav.ung.kodeverk.vilkår.Avslagsårsak;
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
import no.nav.ung.sak.web.server.abac.AbacAttributtSupplier;
import no.nav.ung.sak.web.server.caching.CacheControl;
import no.nav.ung.ytelse.aktivitetspenger.medlemskap.ForutgåendeMedlemskapTjeneste;
import no.nav.ung.ytelse.aktivitetspenger.medlemskap.TrygdeavtaleLandOppslag;

import java.util.List;
import java.util.Map;

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
        var vilkår = vilkårResultatRepository.hent(behandling.getId())
            .getVilkår(VilkårType.FORUTGÅENDE_MEDLEMSKAPSVILKÅRET);

        var medlemskap = forutgåendeMedlemskapTjeneste.utledForutgåendeBosteder(behandling.getFagsakId(), behandling.getId())
            .map(bosteder -> mapTilResponse(bosteder, vilkår.orElse(null)))
            .orElse(List.of());

        return new ForutgåendeMedlemskapResponse(medlemskap);
    }

    private List<MedlemskapsPeriodeDto> mapTilResponse(Bosteder bosteder, no.nav.ung.sak.behandlingslager.behandling.vilkår.Vilkår vilkår) {
        return bosteder.getPerioder().entrySet().stream()
            .map(entry -> mapBostedsEntry(vilkår, entry))
            .toList();
    }

    private MedlemskapsPeriodeDto mapBostedsEntry(Vilkår vilkår, Map.Entry<Periode, Bosteder.BostedPeriodeInfo> entry) {
        Periode bostedPeriode = entry.getKey();
        Bosteder.BostedPeriodeInfo bosted = entry.getValue();

        var periode = new no.nav.ung.sak.typer.Periode(bostedPeriode.getFraOgMed(), bostedPeriode.getTilOgMed());
        var vilkårPeriode = vilkår != null ? vilkår.finnPeriodeSomInneholderDato(bostedPeriode.getFraOgMed()).orElse(null) : null;
        var utfall = vilkårPeriode != null ? vilkårPeriode.getGjeldendeUtfall() : null;
        var avslagsårsak = mapAvslagsårsak(vilkårPeriode);

        return new MedlemskapsPeriodeDto(
            periode,
            bosted.getLand().getLandkode(),
            TrygdeavtaleLandOppslag.erGyldigTrygdeavtaleLand(bosted.getLand(), bostedPeriode.getFraOgMed()),
            utfall,
            avslagsårsak
        );
    }

    private MedlemskapAvslagsÅrsakType mapAvslagsårsak(VilkårPeriode vilkårPeriode) {
        if (vilkårPeriode == null || vilkårPeriode.getAvslagsårsak() == null || vilkårPeriode.getAvslagsårsak() == Avslagsårsak.UDEFINERT) {
            return null;
        }
        return switch (vilkårPeriode.getAvslagsårsak()) {
            case SØKER_ER_IKKE_MEDLEM -> MedlemskapAvslagsÅrsakType.SØKER_IKKE_MEDLEM;
            default -> null;
        };
    }

}
