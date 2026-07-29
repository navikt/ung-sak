package no.nav.ung.sak.formidling.vedtak.resultat;

import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;

import java.util.stream.Collectors;

/**
 * Grunnlaget brev-strategiene utleder resultatet sitt fra. Inneholder <em>hele</em> vilkårsbildet for behandlingen,
 * ikke bare periodene som er til vurdering. Perioder utenfor vurdering har tomt sett med behandlingsårsaker.
 *
 * <p>Strategier og innholdbyggere skal normalt bruke {@link #tilVurdering()}. {@link #heleBildet()} er kun for
 * avslagsvurdering, som må kunne skille fullt avslag fra delvis avslag.</p>
 */
public record DetaljertResultatTidslinje(LocalDateTimeline<DetaljertResultat> heleBildet) {

    public static DetaljertResultatTidslinje av(LocalDateTimeline<DetaljertResultat> heleBildet) {
        return new DetaljertResultatTidslinje(heleBildet);
    }

    public static DetaljertResultatTidslinje tom() {
        return new DetaljertResultatTidslinje(LocalDateTimeline.empty());
    }

    public LocalDateTimeline<DetaljertResultat> tilVurdering() {
        return heleBildet.filterValue(DetaljertResultat::tilVurdering);
    }

    /**
     * Om noen av periodene til vurdering har den gitte behandlingsårsaken.
     */
    public boolean harÅrsak(BehandlingÅrsakType årsak) {
        return tilVurdering().stream().anyMatch(it -> it.getValue().harÅrsak(årsak));
    }

    @Override
    public String toString() {
        return heleBildet.toSegments().stream()
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
