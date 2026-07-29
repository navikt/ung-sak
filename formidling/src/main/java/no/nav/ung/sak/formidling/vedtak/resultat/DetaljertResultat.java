package no.nav.ung.sak.formidling.vedtak.resultat;

import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.sak.behandlingslager.tilkjentytelse.TilkjentYtelseVerdi;

import java.math.BigDecimal;
import java.util.Set;
import java.util.stream.Collectors;

public record DetaljertResultat(
    Set<BehandlingÅrsakType> behandlingsårsaker,
    Set<DetaljertVilkårResultat> avslåtteVilkår,
    Set<DetaljertVilkårResultat> ikkeVurderteVilkår,
    TilkjentYtelseVerdi tilkjentYtelse,
    boolean tilVurdering
) {

    public boolean harPositivUtbetaling() {
        return tilkjentYtelse != null && tilkjentYtelse.utbetalingsgrad().compareTo(BigDecimal.ZERO) > 0;
    }

    public boolean harÅrsak(BehandlingÅrsakType årsak) {
        return behandlingsårsaker.contains(årsak);
    }

    public static LocalDateTimeline<DetaljertResultat> filtrerPåÅrsak(LocalDateTimeline<DetaljertResultat> resultatTidslinje, BehandlingÅrsakType... årsaker) {
        var ønskedeÅrsaker = Set.of(årsaker);
        return resultatTidslinje.filterValue(it -> it.behandlingsårsaker().stream().anyMatch(ønskedeÅrsaker::contains));
    }

    public static LocalDateTimeline<DetaljertResultat> kunTilVurdering(LocalDateTimeline<DetaljertResultat> resultatTidslinje) {
        return resultatTidslinje.filterValue(DetaljertResultat::tilVurdering);
    }


    public static String timelineToString(LocalDateTimeline<DetaljertResultat> detaljertResultatTidslinje) {
        return detaljertResultatTidslinje == null ? "null" :
            String.join(", ", detaljertResultatTidslinje.toSegments().stream()
            .map(it ->
                it.getLocalDateInterval().toString() + " -> " +
                    "behandlingÅrsaker: " + it.getValue().behandlingsårsaker()
                    + ", avslåtteVilkår: " + it.getValue().avslåtteVilkår()
                    + ", ikkeVurderteVilkår: " + it.getValue().ikkeVurderteVilkår()
            )
            .collect(Collectors.toSet()));
    }
}
