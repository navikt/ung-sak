package no.nav.ung.sak.formidling.vedtak.resultat;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.*;
import no.nav.fpsak.tidsserie.LocalDateTimeline.JoinStyle;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.kodeverk.vilkår.VilkårType;
import no.nav.ung.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.VilkårPeriodeResultatDto;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.ung.sak.behandlingslager.tilkjentytelse.TilkjentYtelseRepository;
import no.nav.ung.sak.behandlingslager.tilkjentytelse.TilkjentYtelseVerdi;
import no.nav.ung.sak.perioder.ProsessTriggerPeriodeUtleder;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Dependent
public class DetaljertResultatTidslinjeUtlederImpl implements DetaljertResultatTidslinjeUtleder {

    private final Instance<ProsessTriggerPeriodeUtleder> prosessTriggerPeriodeUtledere;
    private final TilkjentYtelseRepository tilkjentYtelseRepository;
    private final VilkårResultatRepository vilkårResultatRepository;
    private final Instance<DetaljertResultatForPeriodeUtleder> detaljertResultatForPeriodeUtledere;

    @Inject
    public DetaljertResultatTidslinjeUtlederImpl(
        @Any Instance<ProsessTriggerPeriodeUtleder> prosessTriggerPeriodeUtledere,
        TilkjentYtelseRepository tilkjentYtelseRepository,
        VilkårResultatRepository vilkårResultatRepository,
        @Any Instance<DetaljertResultatForPeriodeUtleder> detaljertResultatForPeriodeUtledere) {

        this.prosessTriggerPeriodeUtledere = prosessTriggerPeriodeUtledere;
        this.tilkjentYtelseRepository = tilkjentYtelseRepository;
        this.vilkårResultatRepository = vilkårResultatRepository;
        this.detaljertResultatForPeriodeUtledere = detaljertResultatForPeriodeUtledere;
    }


    @Override
    public LocalDateTimeline<DetaljertResultat> utledDetaljertResultat(Behandling behandling) {
        var tilkjentYtelseTidslinje = tilkjentYtelseRepository.hentTidslinje(behandling.getId()).compress();
        var kontrollertePerioderTidslinje = tilkjentYtelseRepository.hentKontrollerInntektTidslinje(behandling.getId()).compress();
        var prosessTriggerPeriodeUtleder = FagsakYtelseTypeRef.Lookup.find(prosessTriggerPeriodeUtledere, behandling.getFagsakYtelseType()).orElseThrow();

        var perioderTilVurdering = prosessTriggerPeriodeUtleder.utledTidslinje(behandling.getId())
            .mapValue(DetaljertResultatTidslinjeUtlederImpl::fjernIkkeRelevanteÅrsaker)
            .combine(tilkjentYtelseTidslinje, DetaljertResultatTidslinjeUtlederImpl::fjernIkkeRelevanteKontrollårsaker, JoinStyle.LEFT_JOIN)
            .combine(kontrollertePerioderTidslinje, DetaljertResultatTidslinjeUtlederImpl::fjernIkkeRelevanteKontrollårsaker, JoinStyle.LEFT_JOIN)
            .filterValue(it -> !it.isEmpty())
            .compress();

        var vilkårPeriodeResultatMap = hentVilkårTidslinjer(behandling.getId());
        var samletVilkårTidslinje = samleVilkårIEnTidslinje(vilkårPeriodeResultatMap);

        var vilkårOgBehandlingsårsakerTidslinje = perioderTilVurdering
            .intersection(samletVilkårTidslinje,
                (p, behandlingÅrsaker, vilkårResultater)
                    -> new LocalDateSegment<>(p, new SamletVilkårResultatOgBehandlingÅrsaker(vilkårResultater.getValue(), behandlingÅrsaker.getValue(), behandling.erManueltOpprettet())));

        DetaljertResultatForPeriodeUtleder detaljertResultatUtleder = FagsakYtelseTypeRef.Lookup.find(detaljertResultatForPeriodeUtledere, behandling.getFagsakYtelseType()).orElseThrow();

        var detaljertResultatTidslinje = vilkårOgBehandlingsårsakerTidslinje
            .combine(tilkjentYtelseTidslinje, bestemResultatForPeriodeCombinator(detaljertResultatUtleder), JoinStyle.LEFT_JOIN);

        return detaljertResultatTidslinje.compress();

    }

    // Fjerner årsaker som ikke er relevant for brev

    private static Set<BehandlingÅrsakType> fjernIkkeRelevanteÅrsaker(Set<BehandlingÅrsakType> behandlingÅrsaker) {
            var årsaker = new HashSet<>(behandlingÅrsaker);
            //Rapportert inntekt er uinterressant uten kontrollert inntekt årsak
            årsaker.remove(BehandlingÅrsakType.RE_RAPPORTERING_INNTEKT);
            //uttalelse er uinterressant uten en annen årsak
            årsaker.remove(BehandlingÅrsakType.UTTALELSE_FRA_BRUKER);
            return årsaker;
    }

    // Fjern kontrollårsak hvis det ikke er noen kontroll eller tilkjent ytelse i perioden
    private static <T> LocalDateSegment<Set<BehandlingÅrsakType>> fjernIkkeRelevanteKontrollårsaker(LocalDateInterval di, LocalDateSegment<Set<BehandlingÅrsakType>> trigger, LocalDateSegment<T> kontrollEllerYtelseSegment) {
            var årsaker = new HashSet<>(trigger.getValue());
            if (kontrollEllerYtelseSegment == null) {
                årsaker.remove(BehandlingÅrsakType.RE_KONTROLL_REGISTER_INNTEKT);
            }
            return new LocalDateSegment<>(di, årsaker);
    }

    private LocalDateSegmentCombinator<SamletVilkårResultatOgBehandlingÅrsaker, TilkjentYtelseVerdi, DetaljertResultat> bestemResultatForPeriodeCombinator(
        DetaljertResultatForPeriodeUtleder detaljertResultatUtleder) {
        return (p, lhs, rhs) -> {
            SamletVilkårResultatOgBehandlingÅrsaker vilkårResultat = lhs.getValue();
            var tilkjentYtelse = rhs != null ? rhs.getValue() : null;

            if (vilkårResultat == null) {
                throw new IllegalStateException("Ingen vilkårsresultat for periode %s og tilkjentytelse %s".formatted(p, tilkjentYtelse));
            }

            var vilkårSomIkkeErVurdert = vilkårResultat.ikkeVurderteVilkår();
            var avslåtteVilkår = vilkårResultat.avslåtteVilkår();
            var behandlingsårsaker = vilkårResultat.behandlingÅrsaker();

            var resultatType = detaljertResultatUtleder.bestemDetaljertResultat(p, vilkårResultat, tilkjentYtelse);

            var resultat = new DetaljertResultat(resultatType, behandlingsårsaker, avslåtteVilkår, vilkårSomIkkeErVurdert);

            return new LocalDateSegment<>(p, resultat);
        };
    }

    @SuppressWarnings("Convert2MethodRef")
    private Map<VilkårType, LocalDateTimeline<DetaljertVilkårResultat>> hentVilkårTidslinjer(Long behandlingId) {
        return vilkårResultatRepository.hentVilkårResultater(behandlingId).stream()
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
    }

    private static LocalDateTimeline<List<DetaljertVilkårResultat>> samleVilkårIEnTidslinje(Map<VilkårType, LocalDateTimeline<DetaljertVilkårResultat>> vilkårPeriodeResultatMap) {
        var samletVilkårTidslinje = LocalDateTimeline.<List<DetaljertVilkårResultat>>empty();
        for (var entry : vilkårPeriodeResultatMap.entrySet()) {
            LocalDateTimeline<DetaljertVilkårResultat> v = entry.getValue();
            samletVilkårTidslinje = samletVilkårTidslinje.crossJoin(v, StandardCombinators::allValues);
        }
        return samletVilkårTidslinje;
    }

}
