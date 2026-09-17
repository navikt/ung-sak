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
import no.nav.ung.sak.behandlingslager.behandling.startdato.StartdatoGrunnlag;
import no.nav.ung.sak.behandlingslager.behandling.startdato.StartdatoRepository;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriode;
import no.nav.ung.sak.kontrakt.aktivitetspenger.medlemskap.MedlemskapAvslagsÅrsakType;
import no.nav.ung.sak.kontrakt.behandling.BehandlingUuidDto;
import no.nav.ung.sak.kontrakt.vilkår.medlemskap.ForutgåendeMedlemskapResponse;
import no.nav.ung.sak.kontrakt.vilkår.medlemskap.MedlemskapDto;
import no.nav.ung.sak.kontrakt.vilkår.medlemskap.MedlemskapPeriodeResultatDto;
import no.nav.ung.sak.typer.Periode;
import no.nav.ung.sak.web.server.abac.AbacAttributtSupplier;
import no.nav.ung.sak.web.server.caching.CacheControl;
import no.nav.ung.ytelse.aktivitetspenger.medlemskap.ForutgåendeMedlemskapTjeneste;

import java.util.List;

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
    private StartdatoRepository startdatoRepository;

    public ForutgåendeMedlemskapRestTjeneste() {
        // for CDI proxy
    }

    @Inject
    public ForutgåendeMedlemskapRestTjeneste(BehandlingRepository behandlingRepository,
                                             VilkårResultatRepository vilkårResultatRepository,
                                             ForutgåendeMedlemskapTjeneste forutgåendeMedlemskapTjeneste, StartdatoRepository startdatoRepository) {
        this.behandlingRepository = behandlingRepository;
        this.vilkårResultatRepository = vilkårResultatRepository;
        this.forutgåendeMedlemskapTjeneste = forutgåendeMedlemskapTjeneste;
        this.startdatoRepository = startdatoRepository;
    }

    @GET
    @Path(MEDLEMSKAP)
    @Operation(description = "Hent informasjon om forutgående medlemskap", tags = "forutgående medlemskap")
    @BeskyttetRessurs(action = READ, resource = BeskyttetRessursResourceType.FAGSAK)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    @CacheControl()
    public ForutgåendeMedlemskapResponse medlemskap(@NotNull @QueryParam(BehandlingUuidDto.NAME) @Parameter(description = BehandlingUuidDto.DESC) @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) BehandlingUuidDto behandlingUuid) {
        Behandling behandling = behandlingRepository.hentBehandling(behandlingUuid.getBehandlingUuid());

        var medlemskap = forutgåendeMedlemskapTjeneste.hentMedlemskapForBehandlingSomDto(behandling.getId());

        var vilkår = vilkårResultatRepository.hent(behandling.getId())
            .getVilkår(VilkårType.FORUTGÅENDE_MEDLEMSKAPSVILKÅRET)
            .orElseThrow(() -> new IllegalStateException("Mangler vilkårsvurdering av forutgående medlemskap"));

        var startdatoGrunnlag = startdatoRepository.hentGrunnlag(behandling.getId()).orElseThrow();
        var vilkårsperioder = vilkår.getPerioder().stream()
            .filter(it -> it.getUtfall() != Utfall.IKKE_RELEVANT)
            .map(vp -> new MedlemskapPeriodeResultatDto(
                new Periode(vp.getPeriode().getFomDato(), vp.getPeriode().getTomDato()),
                vp.getGjeldendeUtfall(),
                mapAvslagsårsak(vp.getAvslagsårsak()),
                vp.getBegrunnelse(),
                finnOppgittMedlemskapRelevantForPerioden(vp, medlemskap, startdatoGrunnlag)
            ))
            .toList();

        return new ForutgåendeMedlemskapResponse(medlemskap.stream().findFirst().orElse(null), vilkårsperioder);
    }

    private static MedlemskapDto finnOppgittMedlemskapRelevantForPerioden(VilkårPeriode vp, List<MedlemskapDto> medlemskap, StartdatoGrunnlag startdatoGrunnlag) {
        var relevantStartdatoGrunnlag = startdatoGrunnlag.getOppgitteStartdatoer().getStartdatoer().stream()
            .filter(startdato -> vp.getPeriode().overlapper(startdato.getStartdato(), startdato.getStartdato()))
            .findFirst();
        return relevantStartdatoGrunnlag
            .flatMap(v -> medlemskap.stream()
                .filter(m -> m.journalpostId().equals(v.getJournalpostId().getVerdi()))
                .findFirst())
            .orElse(null);

    }

    private static MedlemskapAvslagsÅrsakType mapAvslagsårsak(Avslagsårsak avslagsårsak) {
        if (avslagsårsak == null) return null;
        return switch (avslagsårsak) {
            case SØKER_ER_IKKE_MEDLEM -> MedlemskapAvslagsÅrsakType.SØKER_IKKE_MEDLEM;
            default -> throw new IllegalStateException("Unexpected value: " + avslagsårsak);
        };
    }

}
