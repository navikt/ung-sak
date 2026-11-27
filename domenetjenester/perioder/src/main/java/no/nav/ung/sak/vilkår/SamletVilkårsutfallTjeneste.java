package no.nav.ung.sak.vilkår;

import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;
import no.nav.ung.kodeverk.vilkår.VilkårType;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.Vilkårene;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriode;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.kontrakt.vilkår.VilkårUtfallSamlet;
import no.nav.ung.sak.kontrakt.vilkår.VilkårUtfallSamlet.VilkårUtfall;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public final class SamletVilkårsutfallTjeneste {

    private static final Logger log = LoggerFactory.getLogger(SamletVilkårsutfallTjeneste.class);

    private SamletVilkårsutfallTjeneste() {
    }

    public static LocalDateTimeline<VilkårUtfallSamlet> samleVilkårUtfall(Vilkårene vilkårene) {
        var allePerioder = vilkårene.getAlleIntervaller();
        if (allePerioder.isEmpty()) {
            return LocalDateTimeline.empty();
        }
        var maksPeriode = DatoIntervallEntitet.fra(allePerioder.getMinLocalDate(), allePerioder.getMaxLocalDate());
        return samleVilkårUtfall(vilkårene, maksPeriode);
    }

    public static LocalDateTimeline<VilkårUtfallSamlet> samleVilkårUtfall(Vilkårene vilkårene, DatoIntervallEntitet maksPeriode) {
        var timelinePerVilkår = vilkårene.getVilkårTidslinjer(maksPeriode);
        var alleForventedeVilkårTyper = timelinePerVilkår.keySet();
        var samletUtfall = samletVilkårUtfall(timelinePerVilkår, alleForventedeVilkårTyper);
        log.info("forventendeVilkårTyper={}, maksPeriode={}, timelinePerVilkår={}, samletUtfall={}", alleForventedeVilkårTyper, maksPeriode, timelinePerVilkår, samletUtfall);
        return samletUtfall;
    }

    static LocalDateTimeline<VilkårUtfallSamlet> samletVilkårUtfall(Map<VilkårType, LocalDateTimeline<VilkårPeriode>> timelinePerVilkår, Set<VilkårType> minimumVilkår) {
        var timeline = new LocalDateTimeline<List<VilkårUtfall>>(List.of());

        for (var e : timelinePerVilkår.entrySet()) {
            LocalDateTimeline<VilkårUtfall> utfallTimeline = e.getValue().mapValue(v -> new VilkårUtfall(e.getKey(), v.getAvslagsårsak(), v.getUtfall()));
            timeline = timeline.crossJoin(utfallTimeline.compress(), StandardCombinators::allValues);
        }

        return timeline.mapValue(VilkårUtfallSamlet::fra)
            .filterValue(v -> v.getUnderliggendeVilkårUtfall().stream().map(VilkårUtfall::getVilkårType).collect(Collectors.toSet()).containsAll(minimumVilkår));
    }
}
