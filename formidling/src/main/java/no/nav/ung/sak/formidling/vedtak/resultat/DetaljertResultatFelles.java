package no.nav.ung.sak.formidling.vedtak.resultat;

import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.VilkårPeriodeResultatDto;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Delt, ytelse-agnostisk tidslinje-plumbing for utledning av perioder til vurdering og samlet
 * vilkårstidslinje. Brukes av de ytelse-spesifikke implementasjonene av
 * {@link DetaljertResultatTidslinjeUtleder}.
 */
public final class DetaljertResultatFelles {

    private DetaljertResultatFelles() {
    }


    //*** Delt tidslinje-plumbing ***

    // Utleder perioder til vurdering med relevante behandlingsårsaker for brev
    public static LocalDateTimeline<Set<BehandlingÅrsakType>> utledPerioderTilVurdering(
        LocalDateTimeline<Set<BehandlingÅrsakType>> prosesstriggerTidslinje,
        LocalDateTimeline<?> tilkjentYtelseTidslinje,
        LocalDateTimeline<?> kontrollertePerioderTidslinje) {
        return prosesstriggerTidslinje
            .mapValue(DetaljertResultatFelles::fjernIkkeRelevanteÅrsaker)
            .combine(tilkjentYtelseTidslinje, DetaljertResultatFelles::fjernIkkeRelevanteKontrollårsaker, LocalDateTimeline.JoinStyle.LEFT_JOIN)
            .combine(kontrollertePerioderTidslinje, DetaljertResultatFelles::fjernIkkeRelevanteKontrollårsaker, LocalDateTimeline.JoinStyle.LEFT_JOIN)
            .filterValue(it -> !it.isEmpty())
            .compress();
    }

    // Fjerner årsaker som ikke er relevant for brev
    public static Set<BehandlingÅrsakType> fjernIkkeRelevanteÅrsaker(Set<BehandlingÅrsakType> behandlingÅrsaker) {
        var årsaker = new HashSet<>(behandlingÅrsaker);
        //Rapportert inntekt er uinterressant uten kontrollert inntekt årsak
        årsaker.remove(BehandlingÅrsakType.RE_RAPPORTERING_INNTEKT);
        //uttalelse er uinterressant uten en annen årsak
        årsaker.remove(BehandlingÅrsakType.UTTALELSE_FRA_BRUKER);
        return årsaker;
    }

    // Fjern kontrollårsak hvis det ikke er noen kontroll eller tilkjent ytelse i perioden
    public static <T> LocalDateSegment<Set<BehandlingÅrsakType>> fjernIkkeRelevanteKontrollårsaker(LocalDateInterval di, LocalDateSegment<Set<BehandlingÅrsakType>> trigger, LocalDateSegment<T> kontrollEllerYtelseSegment) {
        var årsaker = new HashSet<>(trigger.getValue());
        if (kontrollEllerYtelseSegment == null) {
            årsaker.remove(BehandlingÅrsakType.RE_KONTROLL_REGISTER_INNTEKT);
        }
        return new LocalDateSegment<>(di, årsaker);
    }

    public static LocalDateTimeline<List<DetaljertVilkårResultat>> samleVilkårIEnTidslinje(List<VilkårPeriodeResultatDto> vilkårPeriodeResultatDtos) {
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

}
