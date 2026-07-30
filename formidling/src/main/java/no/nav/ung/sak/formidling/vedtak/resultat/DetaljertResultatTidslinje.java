package no.nav.ung.sak.formidling.vedtak.resultat;

import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.kodeverk.vilkår.VilkårType;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Grunnlaget formidling utleder resultatet sitt fra. Oppslag på behandlingsårsak gjelder kun perioder til vurdering,
 * siden øvrige perioder aldri har årsaker.
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

    public boolean harÅrsak(BehandlingÅrsakType årsak) {
        return tilVurdering().stream().anyMatch(it -> it.getValue().harÅrsak(årsak));
    }

    public LocalDateTimeline<DetaljertResultat> filtrerPåÅrsak(BehandlingÅrsakType... årsaker) {
        var ønskedeÅrsaker = Set.of(årsaker);
        return tilVurdering().filterValue(it -> it.behandlingsårsaker().stream().anyMatch(ønskedeÅrsaker::contains));
    }

    /**
     * Gjelder hele tidslinjen, ikke bare periodene til vurdering — vilkår er vurdert også utenfor disse.
     * Krever avslag i samtlige perioder, slik at delvise avslag overlates til de øvrige strategiene.
     */
    public boolean harKunAvslåttVilkår(VilkårType... vilkårTyper) {
        var ønskedeVilkår = Set.of(vilkårTyper);
        return !totalTidslinje.isEmpty() && totalTidslinje.stream()
            .allMatch(it -> it.getValue().avslåtteVilkår().stream()
                .anyMatch(vilkår -> ønskedeVilkår.contains(vilkår.vilkårType())));
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
                    + ", utbetalingsgrad: " + v.utbetalingsgrad();
            })
            .collect(Collectors.joining(", "));
    }
}
