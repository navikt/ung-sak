package no.nav.ung.sak.web.app.tjenester.behandling.aktivitetspenger;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
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
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.k9.felles.sikkerhet.abac.BeskyttetRessurs;
import no.nav.k9.felles.sikkerhet.abac.BeskyttetRessursResourceType;
import no.nav.k9.felles.sikkerhet.abac.TilpassetAbacAttributt;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.inngangsvilkår.AktivitetspengerInngangsvilkårResultatGrunnlag;
import no.nav.ung.sak.behandlingslager.inngangsvilkår.AndreLivsoppholdsytelserResultatHolder;
import no.nav.ung.sak.behandlingslager.inngangsvilkår.InngangsvilkårVurderingRepository;
import no.nav.ung.sak.behandlingslager.inngangsvilkår.VilkårsvurderingResultat;
import no.nav.ung.sak.kontrakt.aktivitetspenger.vilkår.vurdering.AndreLivsoppholdsytelserVurderingDto;
import no.nav.ung.sak.kontrakt.aktivitetspenger.vilkår.vurdering.GenerellInngangsvilkårVurderingDto;
import no.nav.ung.sak.kontrakt.aktivitetspenger.vilkår.vurdering.InngangsvilkårVurderingDto;
import no.nav.ung.sak.kontrakt.behandling.BehandlingUuidDto;
import no.nav.ung.sak.typer.Periode;
import no.nav.ung.sak.web.server.abac.AbacAttributtSupplier;
import no.nav.ung.sak.web.server.caching.CacheControl;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static no.nav.k9.felles.sikkerhet.abac.BeskyttetRessursActionType.READ;

@Path("")
@Produces(MediaType.APPLICATION_JSON)
@ApplicationScoped
@Transactional
public class InngangsvilkårVurderingRestTjeneste {

    public static final String BASE_PATH = "/behandling/vilkar/inngangsvilkar-vurderinger";
    public static final String ANDRE_LIVSOPPHOLDSYTELSER_PATH = BASE_PATH + "/andre-livsoppholdsytelser";

    private BehandlingRepository behandlingRepository;
    private InngangsvilkårVurderingRepository inngangsvilkårVurderingRepository;

    public InngangsvilkårVurderingRestTjeneste() {
        // for CDI proxy
    }

    @Inject
    public InngangsvilkårVurderingRestTjeneste(BehandlingRepository behandlingRepository,
                                               InngangsvilkårVurderingRepository inngangsvilkårVurderingRepository) {
        this.behandlingRepository = behandlingRepository;
        this.inngangsvilkårVurderingRepository = inngangsvilkårVurderingRepository;
    }

    @GET
    @Path(BASE_PATH)
    @Operation(description = "Hent vurderinger av alle inngangsvilkår for en behandling", tags = "vilkår", responses = {
        @ApiResponse(description = "Returnerer vurderinger per vilkår og periode, tom liste hvis ikke eksisterer")
    })
    @BeskyttetRessurs(action = READ, resource = BeskyttetRessursResourceType.FAGSAK)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    @CacheControl()
    public List<InngangsvilkårVurderingDto> hentInngangsvilkårVurderinger(@NotNull @QueryParam(BehandlingUuidDto.NAME) @Parameter(description = BehandlingUuidDto.DESC) @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) BehandlingUuidDto behandlingUuid) {
        var behandling = behandlingRepository.hentBehandling(behandlingUuid.getBehandlingUuid());
        return inngangsvilkårVurderingRepository.hentEksisterendeGrunnlag(behandling.getId())
            .map(InngangsvilkårVurderingRestTjeneste::tilVurderinger)
            .orElse(List.of());
    }

    @GET
    @Path(ANDRE_LIVSOPPHOLDSYTELSER_PATH)
    @Operation(description = "Hent vurderinger av andre livsoppholdsytelser-vilkåret for en behandling", tags = "vilkår", responses = {
        @ApiResponse(description = "Returnerer vurderinger per periode, tom liste hvis ikke eksisterer")
    })
    @BeskyttetRessurs(action = READ, resource = BeskyttetRessursResourceType.FAGSAK)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    @CacheControl()
    public List<AndreLivsoppholdsytelserVurderingDto> hentAndreLivsoppholdsytelserVurderinger(@NotNull @QueryParam(BehandlingUuidDto.NAME) @Parameter(description = BehandlingUuidDto.DESC) @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) BehandlingUuidDto behandlingUuid) {
        var behandling = behandlingRepository.hentBehandling(behandlingUuid.getBehandlingUuid());
        return inngangsvilkårVurderingRepository.hentEksisterendeGrunnlag(behandling.getId())
            .map(InngangsvilkårVurderingRestTjeneste::tilLivsoppholdVurderinger)
            .orElse(List.of());
    }

    private static List<InngangsvilkårVurderingDto> tilVurderinger(AktivitetspengerInngangsvilkårResultatGrunnlag grunnlag) {
        var bosted = grunnlag.hentBostedTidslinje()
            .stream()
            .map(LocalDateSegment::getValue)
            .map(p -> tilGenerellVurdering(p.getPeriode().tilPeriode(), p.tilVilkårsvurderingResultat()));

        var bistand = grunnlag.hentBistandTidslinje().stream()
            .map(s -> tilGenerellVurdering(new Periode(s.getFom(), s.getTom()), s.getValue()));

        var livsopphold = tilLivsoppholdVurderinger(grunnlag).stream().map(InngangsvilkårVurderingDto.class::cast);

        var aktivitet = grunnlag.hentAktivitetsvilkårTidslinje().stream()
            .map(s -> tilGenerellVurdering(new Periode(s.getFom(), s.getTom()), s.getValue()));

        return Stream.of(bosted, bistand, livsopphold, aktivitet)
            .flatMap(vilkår -> vilkår.sorted(Comparator.comparing(v -> v.periode().getFom())))
            .toList();
    }

    private static List<AndreLivsoppholdsytelserVurderingDto> tilLivsoppholdVurderinger(AktivitetspengerInngangsvilkårResultatGrunnlag grunnlag) {
        return grunnlag.getAndreLivsoppholdsytelserResultatHolder()
            .map(AndreLivsoppholdsytelserResultatHolder::getVurderinger)
            .orElse(Set.of()).stream()
            .sorted(Comparator.comparing(p -> p.getPeriode().getFomDato()))
            .map(p -> new AndreLivsoppholdsytelserVurderingDto(
                p.getPeriode().tilPeriode(),
                p.isGodkjent(),
                p.getIkkeOppfyltÅrsak(),
                p.isManuellVurdering(),
                p.getBegrunnelse(),
                p.getFritekstVurderingBrev(),
                p.getYtelseNavn(),
                p.getVurdertAv(),
                p.getVurdertTidspunkt())
            ).toList();
    }

    private static InngangsvilkårVurderingDto tilGenerellVurdering(Periode periode, VilkårsvurderingResultat v) {
        return new GenerellInngangsvilkårVurderingDto(
            v.vilkårType(),
            periode,
            v.godkjent(),
            v.ikkeOppfyltÅrsak() != null ? v.ikkeOppfyltÅrsak().getKode() : null,
            v.erManuellVurdering(),
            v.begrunnelse(),
            v.fritekstVurderingBrev(),
            v.vurdertAv(),
            v.vurdertTidspunkt()
        );
    }
}
