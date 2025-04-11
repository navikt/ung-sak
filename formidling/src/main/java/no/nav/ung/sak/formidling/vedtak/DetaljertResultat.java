package no.nav.ung.sak.formidling.vedtak;

import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;

import java.util.Set;
import java.util.stream.Collectors;

public record DetaljertResultat(
    Set<DetaljertResultatInfo> resultatInfo,
    Set<BehandlingÅrsakType> behandlingsårsaker,
    Set<DetaljertVilkårResultat> avslåtteVilkår,
    Set<DetaljertVilkårResultat> ikkeVurderteVilkår
) {

    public static DetaljertResultat of(
        DetaljertResultatInfo resultatInfo,
        Set<BehandlingÅrsakType> behandlingÅrsakTyper,
        Set<DetaljertVilkårResultat> avslåtteVilkår,
        Set<DetaljertVilkårResultat> ikkeVurderteVilkår) {
        return new DetaljertResultat(Set.of(resultatInfo), behandlingÅrsakTyper, avslåtteVilkår, ikkeVurderteVilkår);
    }


    public static String timelineToString(LocalDateTimeline<DetaljertResultat> detaljertResultatTidslinje) {
        return String.join(", ", detaljertResultatTidslinje.toSegments().stream()
            .map(it ->
                it.getLocalDateInterval().toString() + " -> " +
                    "resultatInfo: " + it.getValue().resultatInfo() +", "
                    + "behandlingÅrsaker: " + it.getValue().behandlingsårsaker() + " ")
            .collect(Collectors.toSet()));
    }

}
