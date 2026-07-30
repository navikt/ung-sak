package no.nav.ung.sak.formidling.vedtak.resultat;

import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;

import java.util.stream.Collectors;

/**
 * Grunnlaget formidling utleder resultatet sitt fra.
 */
public record DetaljertResultatTidslinje(LocalDateTimeline<DetaljertResultat> totalTidslinje) {

    public static DetaljertResultatTidslinje av(LocalDateTimeline<DetaljertResultat> heleBildet) {
        return new DetaljertResultatTidslinje(heleBildet);
    }

    public static DetaljertResultatTidslinje tom() {
        return new DetaljertResultatTidslinje(LocalDateTimeline.empty());
    }

    public LocalDateTimeline<DetaljertResultat> tilVurdering() {
        return totalTidslinje.filterValue(DetaljertResultat::tilVurdering);
    }

    /**
     * Om noen av periodene til vurdering har den gitte behandlingsårsaken.
     */
    public boolean harÅrsak(BehandlingÅrsakType årsak) {
        return tilVurdering().stream().anyMatch(it -> it.getValue().harÅrsak(årsak));
    }

    @Override
    public String toString() {
        return totalTidslinje.toSegments().stream()
            .map(it -> {
                var v = it.getValue();
                return it.getLocalDateInterval() + " -> "
                    + (v.tilVurdering() ? "tilVurdering" : "ikkeTilVurdering")
                    + ", behandlingÅrsaker: " + v.behandlingsårsaker()
                    + ", avslåtteVilkår: " + v.avslåtteVilkår()
                    + ", ikkeVurderteVilkår: " + v.ikkeVurderteVilkår()
                    + ", tilkjentYtelse: " + v.tilkjentYtelse();
            })
            .collect(Collectors.joining(", "));
    }
}
