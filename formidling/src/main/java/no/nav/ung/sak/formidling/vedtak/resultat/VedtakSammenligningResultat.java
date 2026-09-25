package no.nav.ung.sak.formidling.vedtak.resultat;

import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.kodeverk.vilkår.VilkårType;

import java.util.Map;

public record VedtakSammenligningResultat(
    LocalDateTimeline<Map<VilkårType, VilkårEndringType>> vilkårDifferanse,
    LocalDateTimeline<DagsatsEndringType> dagsatsDifferanse
) {

    public boolean erUendret() {
        return dagsatsDifferanse.stream().allMatch(segment -> segment.getValue() == DagsatsEndringType.UENDRET)
            && vilkårDifferanse.stream().allMatch(segment -> segment.getValue().values().stream().allMatch(VilkårEndringType.UENDRET::equals));
    }

    @Override
    public String toString() {
        return "vilkårDifferanse: " + vilkårDifferanse.segmenter() + ", dagsatsDifferanse: " + dagsatsDifferanse.segmenter();
    }
}
