package no.nav.ung.sak.formidling.vedtak.resultat;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.StandardCombinators;
import no.nav.fpsak.tidsserie.LocalDateSegmentCombinator;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.LocalDateTimeline.JoinStyle;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.VilkårPeriodeResultatDto;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.ung.sak.behandlingslager.tilkjentytelse.TilkjentYtelseRepository;
import no.nav.ung.sak.behandlingslager.tilkjentytelse.TilkjentYtelseVerdi;
import no.nav.ung.sak.perioder.ProsessTriggerPeriodeUtleder;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Felles, ytelse-agnostisk utleder av detaljert resultat-tidslinje. Produserer en tynn
 * grunnlagstidslinje (behandlingsårsaker + avslåtte/ikke-vurderte vilkår + tilkjent ytelse) uten
 * klassifisering; strategiene utleder selv sitt resultat fra behandlingsårsak/vilkår/tilkjent ytelse.
 *
 * Registrert for alle ytelser som deler denne flyten. Ytelse-spesifikk {@link ProsessTriggerPeriodeUtleder}
 * slås opp per behandling.
 */
@FagsakYtelseTypeRef(FagsakYtelseType.UNGDOMSYTELSE)
@FagsakYtelseTypeRef(FagsakYtelseType.AKTIVITETSPENGER)
@ApplicationScoped
public class DefaultDetaljertResultatTidslinjeUtleder implements DetaljertResultatTidslinjeUtleder {

    private Instance<ProsessTriggerPeriodeUtleder> prosessTriggerPeriodeUtledere;
    private TilkjentYtelseRepository tilkjentYtelseRepository;
    private VilkårResultatRepository vilkårResultatRepository;

    DefaultDetaljertResultatTidslinjeUtleder() {
    }

    @Inject
    public DefaultDetaljertResultatTidslinjeUtleder(
        @Any Instance<ProsessTriggerPeriodeUtleder> prosessTriggerPeriodeUtledere,
        TilkjentYtelseRepository tilkjentYtelseRepository,
        VilkårResultatRepository vilkårResultatRepository) {
        this.prosessTriggerPeriodeUtledere = prosessTriggerPeriodeUtledere;
        this.tilkjentYtelseRepository = tilkjentYtelseRepository;
        this.vilkårResultatRepository = vilkårResultatRepository;
    }

    @Override
    public DetaljertResultatTidslinje utledDetaljertResultat(Behandling behandling) {
        var prosessTriggerPeriodeUtleder = FagsakYtelseTypeRef.Lookup
            .find(prosessTriggerPeriodeUtledere, behandling.getFagsakYtelseType())
            .orElseThrow(() -> new IllegalStateException("Fant ingen ProsessTriggerPeriodeUtleder for ytelse " + behandling.getFagsakYtelseType()));

        var tilkjentYtelseTidslinje = tilkjentYtelseRepository.hentTidslinje(behandling.getId()).compress();

        var perioderTilVurdering = utledPerioderTilVurdering(
            prosessTriggerPeriodeUtleder.utledTidslinje(behandling.getId()));

        var samletVilkårTidslinje = samleVilkårIEnTidslinje(vilkårResultatRepository.hentVilkårResultater(behandling.getId()));

        // Kombinerer (ikke intersection) med hele vilkårstidslinjen slik at også oppfylte perioder utenfor perioder-til-
        // vurdering blir med. Da ser strategiene hele vilkårsbildet. Behandlingsårsaker settes kun på perioder til vurdering.
        var vilkårOgBehandlingsårsakerTidslinje = perioderTilVurdering
            .combine(samletVilkårTidslinje,
                (p, behandlingÅrsaker, vilkårResultater) -> {
                    boolean tilVurdering = behandlingÅrsaker != null;
                    Set<BehandlingÅrsakType> årsaker = tilVurdering ? behandlingÅrsaker.getValue() : Set.of();
                    List<DetaljertVilkårResultat> vilkår = vilkårResultater != null ? vilkårResultater.getValue() : List.of();
                    return new LocalDateSegment<>(p, new DetaljertResultatPeriodeGrunnlag(vilkår, årsaker, tilVurdering));
                }, JoinStyle.CROSS_JOIN);

        return DetaljertResultatTidslinje.av(vilkårOgBehandlingsårsakerTidslinje
            .combine(tilkjentYtelseTidslinje, byggDetaljertResultatCombinator(), JoinStyle.LEFT_JOIN)
            .compress());
    }

    // Utleder perioder til vurdering med relevante behandlingsårsaker for brev
    private static LocalDateTimeline<Set<BehandlingÅrsakType>> utledPerioderTilVurdering(
        LocalDateTimeline<Set<BehandlingÅrsakType>> prosesstriggerTidslinje) {
        return prosesstriggerTidslinje
            .mapValue(DefaultDetaljertResultatTidslinjeUtleder::fjernIkkeRelevanteÅrsaker)
            .filterValue(it -> !it.isEmpty())
            .compress();
    }

    private static Set<BehandlingÅrsakType> fjernIkkeRelevanteÅrsaker(Set<BehandlingÅrsakType> behandlingÅrsaker) {
        var årsaker = new HashSet<>(behandlingÅrsaker);
        //Rapportert inntekt er uinterressant uten kontrollert inntekt årsak
        årsaker.remove(BehandlingÅrsakType.RE_RAPPORTERING_INNTEKT);
        //uttalelse er uinterressant uten en annen årsak
        årsaker.remove(BehandlingÅrsakType.UTTALELSE_FRA_BRUKER);
        return årsaker;
    }

    private static LocalDateTimeline<List<DetaljertVilkårResultat>> samleVilkårIEnTidslinje(List<VilkårPeriodeResultatDto> vilkårPeriodeResultatDtos) {
        var vilkårPeriodeResultatMap = vilkårPeriodeResultatDtos.stream()
            .collect(Collectors.groupingBy(
                VilkårPeriodeResultatDto::getVilkårType,
                Collectors.collectingAndThen(
                    Collectors.mapping(it -> new LocalDateSegment<>(
                            it.getPeriode().getFom(),
                            it.getPeriode().getTom(),
                            new DetaljertVilkårResultat(it.getAvslagsårsak(), it.getVilkårType(), it.getUtfall())
                        ), Collectors.toList()
                    ),
                    list -> new LocalDateTimeline<>(list) // Ikke bruk Method reference da det gir kompilerings feil runtime!
                )
            ));

        var samletVilkårTidslinje = LocalDateTimeline.<List<DetaljertVilkårResultat>>empty();
        for (var entry : vilkårPeriodeResultatMap.entrySet()) {
            LocalDateTimeline<DetaljertVilkårResultat> v = entry.getValue();
            samletVilkårTidslinje = samletVilkårTidslinje.crossJoin(v, StandardCombinators::allValues);
        }
        return samletVilkårTidslinje;
    }

    private LocalDateSegmentCombinator<DetaljertResultatPeriodeGrunnlag, TilkjentYtelseVerdi, DetaljertResultat> byggDetaljertResultatCombinator() {
        return (p, lhs, rhs) -> {
            var grunnlag = lhs.getValue();
            if (grunnlag == null) {
                throw new IllegalStateException("Ingen vilkårsresultat for periode %s".formatted(p));
            }
            var resultat = new DetaljertResultat(
                grunnlag.behandlingÅrsaker(),
                grunnlag.avslåtteVilkår(),
                grunnlag.ikkeVurderteVilkår(),
                Utbetalingsgrad.av(rhs != null ? rhs.getValue() : null),
                grunnlag.tilVurdering());
            return new LocalDateSegment<>(p, resultat);
        };
    }
}

