package no.nav.ung.sak.web.app.tjenester.behandling.aktivitetspenger;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.felles.sikkerhet.abac.BeskyttetRessurs;
import no.nav.k9.felles.sikkerhet.abac.BeskyttetRessursResourceType;
import no.nav.k9.felles.sikkerhet.abac.TilpassetAbacAttributt;
import no.nav.ung.kodeverk.vilkår.AndreLivsoppholdsytelserIkkeOppfyltÅrsak;
import no.nav.ung.kodeverk.vilkår.VilkårType;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.inngangsvilkår.AktivitetspengerInngangsvilkårResultatGrunnlag;
import no.nav.ung.sak.behandlingslager.inngangsvilkår.AndreLivsoppholdsytelserVurderingResultat;
import no.nav.ung.sak.behandlingslager.inngangsvilkår.InngangsvilkårVurderingRepository;
import no.nav.ung.sak.behandlingslager.inngangsvilkår.VilkårsvurderingResultat;
import no.nav.ung.sak.domene.typer.tid.TidslinjeUtil;
import no.nav.ung.sak.kontrakt.aktivitetspenger.vilkår.vurdering.AndreLivsoppholdsytelserVurderingDto;
import no.nav.ung.sak.kontrakt.aktivitetspenger.vilkår.vurdering.GenerellInngangsvilkårVurderingDto;
import no.nav.ung.sak.kontrakt.aktivitetspenger.vilkår.vurdering.InngangsvilkårVurderingDto;
import no.nav.ung.sak.kontrakt.behandling.BehandlingUuidDto;
import no.nav.ung.sak.perioder.VilkårsPerioderTilVurderingTjeneste;
import no.nav.ung.sak.typer.Periode;
import no.nav.ung.sak.web.server.abac.AbacAttributtSupplier;
import no.nav.ung.sak.web.server.caching.CacheControl;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
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
    private Instance<VilkårsPerioderTilVurderingTjeneste> vilkårsPerioderTilVurderingTjenester;

    public InngangsvilkårVurderingRestTjeneste() {
        // for CDI proxy
    }

    @Inject
    public InngangsvilkårVurderingRestTjeneste(BehandlingRepository behandlingRepository,
                                               InngangsvilkårVurderingRepository inngangsvilkårVurderingRepository,
                                               @Any Instance<VilkårsPerioderTilVurderingTjeneste> vilkårsPerioderTilVurderingTjenester) {
        this.vilkårsPerioderTilVurderingTjenester = vilkårsPerioderTilVurderingTjenester;
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
            .map(grunnlag -> tilVurderinger(behandling, grunnlag))
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
            .map(grunnlag -> tilLivsoppholdVurderinger(behandling, grunnlag))
            .orElse(List.of());
    }

    private List<InngangsvilkårVurderingDto> tilVurderinger(Behandling behandling, AktivitetspengerInngangsvilkårResultatGrunnlag grunnlag) {
        var bosted = grunnlag.hentBostedTidslinjeVilkårResultat().intersection(perioderTilVurdering(behandling, VilkårType.BOSTEDSVILKÅR)).stream()
            .map(s -> tilGenerellVurdering(s.getFom(), s.getTom(), s.getValue()));

        var bistand = grunnlag.hentBistandTidslinje().intersection(perioderTilVurdering(behandling, VilkårType.BISTANDSVILKÅR)).stream()
            .map(s -> tilGenerellVurdering(s.getFom(), s.getTom(), s.getValue()));

        var livsopphold = tilLivsoppholdVurderinger(behandling, grunnlag).stream().map(InngangsvilkårVurderingDto.class::cast);

        var aktivitet = grunnlag.hentAktivitetsvilkårTidslinje().intersection(perioderTilVurdering(behandling, VilkårType.AKTIVITETSVILKÅR)).stream()
            .map(s -> tilGenerellVurdering(s.getFom(), s.getTom(), s.getValue()));

        return Stream.of(bosted, bistand, livsopphold, aktivitet)
            .flatMap(vilkår -> vilkår.sorted(Comparator.comparing(v -> v.periode().getFom())))
            .toList();
    }

    private List<AndreLivsoppholdsytelserVurderingDto> tilLivsoppholdVurderinger(Behandling behandling, AktivitetspengerInngangsvilkårResultatGrunnlag grunnlag) {
        var perioderTilVurdering = perioderTilVurdering(behandling, VilkårType.ANDRE_LIVSOPPHOLDSYTELSER_VILKÅR);
        return grunnlag.hentLivsoppholdTidslinje().intersection(perioderTilVurdering).stream()
            .map(s -> {
                var v = (AndreLivsoppholdsytelserVurderingResultat) s.getValue();
                return new AndreLivsoppholdsytelserVurderingDto(
                    new Periode(s.getFom(), s.getTom()),
                    v.godkjent(),
                    (AndreLivsoppholdsytelserIkkeOppfyltÅrsak) v.ikkeOppfyltÅrsak(),
                    v.erManuellVurdering(),
                    v.begrunnelse(),
                    v.fritekstVurderingBrev(),
                    v.ytelseNavn(),
                    v.vurdertAv(),
                    v.vurdertTidspunkt());
            }).toList();
    }

    private LocalDateTimeline<Boolean> perioderTilVurdering(Behandling behandling, VilkårType vilkårType) {
        var tjeneste = VilkårsPerioderTilVurderingTjeneste.finnTjeneste(vilkårsPerioderTilVurderingTjenester, behandling.getFagsakYtelseType(), behandling.getType());
        return TidslinjeUtil.tilTidslinje(tjeneste.utled(behandling.getId(), vilkårType));
    }

    private static InngangsvilkårVurderingDto tilGenerellVurdering(LocalDate fom, LocalDate tom, VilkårsvurderingResultat v) {
        return new GenerellInngangsvilkårVurderingDto(
            v.vilkårType(),
            new Periode(fom, tom),
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
