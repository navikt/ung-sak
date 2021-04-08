package no.nav.k9.sak.vilkår;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.kodeverk.vilkår.Utfall;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriode;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriodeBuilder;

public class VilkårTjenesteSamletUtfallTest {

    @Test
    void ingen_samlet_utfall() throws Exception {

        var vilkårTjeneste = new VilkårTjeneste();

        Map<VilkårType, LocalDateTimeline<VilkårPeriode>> input = Map.of(
            VilkårType.OMSORGEN_FOR, toTimeline(List.of()),
            VilkårType.UTVIDETRETT, toTimeline(List.of()));
        var output = vilkårTjeneste.samletVilkårUtfall(input);
        assertThat(output).isEmpty();
    }

    @Test
    void har_samlet_utfall_oppfylt() throws Exception {

        var vilkårTjeneste = new VilkårTjeneste();

        LocalDate f1 = LocalDate.now(), t1 = f1.plusDays(10);
        LocalDate f2 = LocalDate.now().plusDays(1), t2 = f2.plusDays(10);

        var vilkårOmsorgFor = List.of(new VilkårPeriodeBuilder().medPeriode(f1, t1).medUtfall(Utfall.OPPFYLT).build());
        var vilkårUtvidetRett = List.of(new VilkårPeriodeBuilder().medPeriode(f2, t2).medUtfall(Utfall.IKKE_OPPFYLT).build());

        Map<VilkårType, LocalDateTimeline<VilkårPeriode>> input = Map.of(
            VilkårType.OMSORGEN_FOR, toTimeline(vilkårOmsorgFor),
            VilkårType.UTVIDETRETT, toTimeline(vilkårUtvidetRett));
        var output = vilkårTjeneste.samletVilkårUtfall(input);
        assertThat(output).isNotEmpty();

        // sjekk intervaller ok
        LocalDateInterval p1 = new LocalDateInterval(f1, f1);
        LocalDateInterval p2 = new LocalDateInterval(f2, t1);
        LocalDateInterval p3 = new LocalDateInterval(t2, t2);
        assertThat(output.getLocalDateIntervals()).isEqualTo(new TreeSet<>(List.of(p1, p2, p3)));

        // sjekk hvert intervall -samlet utfall og underliggende
        assertThat(output.getSegment(p1).getValue().getSamletUtfall()).isEqualTo(Utfall.OPPFYLT);
        assertThat(output.getSegment(p1).getValue().getUnderliggendeVilkårUtfall()).hasSize(1);

        assertThat(output.getSegment(p2).getValue().getSamletUtfall()).isEqualTo(Utfall.IKKE_OPPFYLT);
        assertThat(output.getSegment(p2).getValue().getUnderliggendeVilkårUtfall()).hasSize(2);

        assertThat(output.getSegment(p3).getValue().getSamletUtfall()).isEqualTo(Utfall.IKKE_OPPFYLT);
        assertThat(output.getSegment(p3).getValue().getUnderliggendeVilkårUtfall()).hasSize(1);

    }

    private LocalDateTimeline<VilkårPeriode> toTimeline(List<VilkårPeriode> vilkårOmsorgFor) {
        return new LocalDateTimeline<VilkårPeriode>(vilkårOmsorgFor.stream().map(v -> new LocalDateSegment<>(v.getFom(), v.getTom(), v)).collect(Collectors.toList()));
    }
}
