package no.nav.ung.sak.vilkår;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;
import no.nav.ung.kodeverk.vilkår.VilkårType;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.Vilkårene;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriode;
import no.nav.ung.sak.kontrakt.vilkår.VilkårUtfallSamlet;

public class SamleVilkårResultat {

    public static LocalDateTimeline<VilkårUtfallSamlet> samleVilkårUtfall(Vilkårene vilkårene) {
        var timelinePerVilkår = vilkårene.getVilkårTidslinjer();
        Set<VilkårType> alleForventedeVilkårTyper = timelinePerVilkår.keySet();
        return samletVilkårUtfall(timelinePerVilkår, alleForventedeVilkårTyper);
    }


    static LocalDateTimeline<VilkårUtfallSamlet> samletVilkårUtfall(Map<VilkårType, LocalDateTimeline<VilkårPeriode>> timelinePerVilkår, Set<VilkårType> minimumVilkår) {
        var timeline = new LocalDateTimeline<List<VilkårUtfallSamlet.VilkårUtfall>>(List.of());

        for (var e : timelinePerVilkår.entrySet()) {
            LocalDateTimeline<VilkårUtfallSamlet.VilkårUtfall> utfallTimeline = e.getValue().mapValue(v -> new VilkårUtfallSamlet.VilkårUtfall(e.getKey(), v.getAvslagsårsak(), v.getUtfall()));
            timeline = timeline.crossJoin(utfallTimeline.compress(), StandardCombinators::allValues);
        }

        var resultat = timeline.mapValue(VilkårUtfallSamlet::fra)
            .filterValue(v -> v.getUnderliggendeVilkårUtfall().stream().map(VilkårUtfallSamlet.VilkårUtfall::getVilkårType).collect(Collectors.toSet()).containsAll(minimumVilkår));
        return resultat;
    }
}
