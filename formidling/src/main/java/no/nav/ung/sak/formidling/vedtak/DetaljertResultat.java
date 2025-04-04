package no.nav.ung.sak.formidling.vedtak;

import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;

import java.util.Set;
import java.util.stream.Collectors;

public record DetaljertResultat(
    Set<DetaljertResultatType> resultatTyper,
    Set<BehandlingÅrsakType> behandlingsårsaker,
    Set<DetaljertVilkårResultat> avslåtteVilkår,
    Set<DetaljertVilkårResultat> ikkeVurderteVilkår
) {

    public static DetaljertResultat of(
        DetaljertResultatType resultatType,
        Set<BehandlingÅrsakType> behandlingÅrsakTyper,
        Set<DetaljertVilkårResultat> avslåtteVilkår,
        Set<DetaljertVilkårResultat> ikkeVurderteVilkår) {
        return new DetaljertResultat(Set.of(resultatType), behandlingÅrsakTyper, avslåtteVilkår, ikkeVurderteVilkår );
    }


    public static String timelineTostring(LocalDateTimeline<DetaljertResultat> detaljertResultatTidslinje) {
        return String.join(", ", detaljertResultatTidslinje.toSegments().stream()
            .map(it -> it.getLocalDateInterval().toString() + " -> " + it.getValue().resultatTyper()).collect(Collectors.toSet()));
    }

}
