package no.nav.ung.sak.vilkår;

import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.kodeverk.vilkår.Utfall;
import no.nav.ung.kodeverk.vilkår.VilkårType;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.VilkårBuilder;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.Vilkårene;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriode;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriodeBuilder;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.kontrakt.vilkår.VilkårUtfallSamlet;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.atIndex;

public class SamletVilkårsutfallTjenesteTest {

    @Test
    void tomme_vilkår_gir_tom_tidslinje() {
        assertThat(SamletVilkårsutfallTjeneste.samleVilkårUtfall(Vilkårene.builder().build())).isEmpty();
    }

    @Test
    void utleder_maksperiode_fra_vilkårene() {
        var vilkårene = Vilkårene.builder()
            .leggTil(new VilkårBuilder(VilkårType.ALDERSVILKÅR)
                .leggTil(new VilkårPeriodeBuilder().medPeriode("2026-01-01", "2026-01-31").medUtfall(Utfall.OPPFYLT)))
            .leggTil(new VilkårBuilder(VilkårType.UNGDOMSPROGRAMVILKÅRET)
                .leggTil(new VilkårPeriodeBuilder().medPeriode("2026-01-10", "2026-02-28").medUtfall(Utfall.IKKE_OPPFYLT)))
            .build();

        var resultat = SamletVilkårsutfallTjeneste.samleVilkårUtfall(vilkårene);

        var overlapp = LocalDateInterval.parseFrom("2026-01-10", "2026-01-31");
        assertThat(resultat.getLocalDateIntervals()).containsExactly(overlapp);
        assertThat(resultat.getSegment(overlapp).getValue().getSamletUtfall()).isEqualTo(Utfall.IKKE_OPPFYLT);
        assertThat(resultat.getSegment(overlapp).getValue().getUnderliggendeVilkårUtfall()).hasSize(2);
    }

    @Test
    void avgrenser_utfall_til_maksperioden() {
        var vilkårene = Vilkårene.builder()
            .leggTil(new VilkårBuilder(VilkårType.ALDERSVILKÅR)
                .leggTil(new VilkårPeriodeBuilder().medPeriode("2026-01-01", "2026-01-31").medUtfall(Utfall.OPPFYLT)))
            .build();
        var maksPeriode = DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.of(2026, 1, 10), LocalDate.of(2026, 1, 20));

        var resultat = SamletVilkårsutfallTjeneste.samleVilkårUtfall(vilkårene, maksPeriode);

        assertThat(resultat.getLocalDateIntervals()).containsExactly(maksPeriode.toLocalDateInterval());
        assertThat(resultat.getSegment(maksPeriode.toLocalDateInterval()).getValue().getSamletUtfall()).isEqualTo(Utfall.OPPFYLT);
    }

    @Test
    void ingen_samlet_utfall() throws Exception {

        Map<VilkårType, LocalDateTimeline<VilkårPeriode>> input = Map.of(
            VilkårType.ALDERSVILKÅR, toTimeline(List.of()),
            VilkårType.UNGDOMSPROGRAMVILKÅRET, toTimeline(List.of()));
        var output = SamletVilkårsutfallTjeneste.samletVilkårUtfall(input, input.keySet());
        assertThat(output).isEmpty();
    }

    @Test
    void har_samlet_utfall_ikke_oppfylt_når_ett_ikke_er_det() throws Exception {

        LocalDate f1 = LocalDate.of(2026, 1, 1), t1 = f1.plusDays(10);
        LocalDate f2 = f1.plusDays(1), t2 = f2.plusDays(10);

        var vilkårene = Vilkårene.builder()
            .leggTil(new VilkårBuilder(VilkårType.ALDERSVILKÅR).leggTil(new VilkårPeriodeBuilder().medPeriode(f1, t1).medUtfall(Utfall.OPPFYLT)))
            .leggTil(new VilkårBuilder(VilkårType.UNGDOMSPROGRAMVILKÅRET).leggTil(new VilkårPeriodeBuilder().medPeriode(f2, t2).medUtfall(Utfall.IKKE_OPPFYLT)))
            .build();

        var output = SamletVilkårsutfallTjeneste.samleVilkårUtfall(vilkårene, DatoIntervallEntitet.fraOgMedTilOgMed(f1, t2));

        assertThat(output).isNotEmpty();

        // sjekk intervaller ok
        var overlappendePeriode = new LocalDateInterval(f2, t1);
        assertThat(output.getLocalDateIntervals()).isEqualTo(new TreeSet<>(List.of(overlappendePeriode))); // har bare med overlappende periode

        // sjekk hvert intervall -samlet utfall og underliggende
        assertThat(output.getSegment(overlappendePeriode).getValue().getSamletUtfall()).isEqualTo(Utfall.IKKE_OPPFYLT);
        assertThat(output.getSegment(overlappendePeriode).getValue().getUnderliggendeVilkårUtfall()).hasSize(2);
    }

    @Test
    void har_samlet_utfall_oppfylt_for_begge_vilkårene() throws Exception {
        var t = List.of(
            new Tuple("2021-02-01", "9999-12-31"),
            new Tuple("2020-08-01", "9999-12-31"));
        var allePerioder = Tuple.allePerioder(t);

        LocalDate mindato = allePerioder.getMinLocalDate();
        LocalDate maksdato = allePerioder.getMaxLocalDate();

        var vilkårene = Vilkårene.builder()
            .leggTil(new VilkårBuilder(VilkårType.ALDERSVILKÅR).leggTil(new VilkårPeriodeBuilder().medPeriode(t.get(0).d0, t.get(1).d1).medUtfall(Utfall.OPPFYLT)))
            .leggTil(new VilkårBuilder(VilkårType.UNGDOMSPROGRAMVILKÅRET).leggTil(new VilkårPeriodeBuilder().medPeriode(t.get(1).d0, t.get(1).d1).medUtfall(Utfall.OPPFYLT)))
            .build();

        var output = SamletVilkårsutfallTjeneste.samleVilkårUtfall(vilkårene, DatoIntervallEntitet.fraOgMedTilOgMed(mindato, maksdato));
        assertThat(output).isNotEmpty();

        // sjekk intervaller ok
        List<LocalDateInterval> uniIntervall = List.copyOf(allePerioder.getLocalDateIntervals());
        LocalDateInterval p1 = uniIntervall.get(0); // denne finnes ikke
        LocalDateInterval p2 = uniIntervall.get(1);

        assertThat(output.getLocalDateIntervals()).containsOnly(p2).doesNotContain(p1); // har bare overlappende periode

        // sjekk hvert intervall -samlet utfall og underliggende
        assertThat(output.getSegment(p2).getValue().getSamletUtfall()).isEqualTo(Utfall.OPPFYLT);
        assertThat(output.getSegment(p2).getValue().getUnderliggendeVilkårUtfall()).hasSize(2);
    }

    @Test
    void har_samlet_utfall_oppfylt_for_begge_vilkårene_med_flere_overlappende_perioder_IKKE_OPPFYLT() throws Exception {

        var vilkårene = Vilkårene.builder()
            .leggTil(new VilkårBuilder(VilkårType.ALDERSVILKÅR)
                .leggTil(new VilkårPeriodeBuilder().medPeriode("2020-08-01", "9999-12-31").medUtfall(Utfall.IKKE_OPPFYLT)))
            .leggTil(new VilkårBuilder(VilkårType.UNGDOMSPROGRAMVILKÅRET)
                .leggTil(new VilkårPeriodeBuilder().medPeriode("2020-11-01", "2021-12-31").medUtfall(Utfall.IKKE_VURDERT))
                .leggTil(new VilkårPeriodeBuilder().medPeriode("2022-01-01", "9999-12-31").medUtfall(Utfall.OPPFYLT)))
            .build();

        var allePerioder = vilkårene.getAlleIntervaller();
        LocalDate mindato = allePerioder.getMinLocalDate();
        LocalDate maksdato = allePerioder.getMaxLocalDate();
        var output = SamletVilkårsutfallTjeneste.samleVilkårUtfall(vilkårene, DatoIntervallEntitet.fraOgMedTilOgMed(mindato, maksdato));
        assertThat(output).isNotEmpty();

        var outputIntervaller = List.copyOf(output.getLocalDateIntervals());
        assertThat(outputIntervaller).hasSize(2);
        var first = outputIntervaller.get(0);
        var second = outputIntervaller.get(1);

        assertThat(first).isEqualTo(LocalDateInterval.parseFrom("2020-11-01", "2021-12-31"));
        assertThat(second).isEqualTo(LocalDateInterval.parseFrom("2022-01-01", "9999-12-31"));

        VilkårUtfallSamlet firstValue = output.getSegment(first).getValue();
        assertThat(firstValue.getSamletUtfall()).as(first.toString()).isEqualTo(Utfall.IKKE_OPPFYLT);
        assertThat(firstValue.getUnderliggendeVilkårUtfall()).as(first.toString()).hasSize(2).satisfies(v -> {
            assertThat(v.getVilkårType()).isEqualTo(VilkårType.ALDERSVILKÅR);
            assertThat(v.getVilkårUtfall()).isEqualTo(Utfall.IKKE_OPPFYLT);
        }, atIndex(0));
        assertThat(firstValue.getUnderliggendeVilkårUtfall()).as(first.toString()).hasSize(2).satisfies(v -> {
            assertThat(v.getVilkårType()).isEqualTo(VilkårType.UNGDOMSPROGRAMVILKÅRET);
            assertThat(v.getVilkårUtfall()).isEqualTo(Utfall.IKKE_VURDERT);
        }, atIndex(1));

        VilkårUtfallSamlet secondValue = output.getSegment(second).getValue();

        assertThat(secondValue.getSamletUtfall()).as(second.toString()).isEqualTo(Utfall.IKKE_OPPFYLT);
        assertThat(secondValue.getUnderliggendeVilkårUtfall()).as(second.toString()).hasSize(2).satisfies(v -> {
            assertThat(v.getVilkårType()).isEqualTo(VilkårType.ALDERSVILKÅR);
            assertThat(v.getVilkårUtfall()).isEqualTo(Utfall.IKKE_OPPFYLT);
        }, atIndex(0));
        assertThat(secondValue.getUnderliggendeVilkårUtfall()).as(second.toString()).hasSize(2).satisfies(v -> {
            assertThat(v.getVilkårType()).isEqualTo(VilkårType.UNGDOMSPROGRAMVILKÅRET);
            assertThat(v.getVilkårUtfall()).isEqualTo(Utfall.OPPFYLT);
        }, atIndex(1));

    }

    @Test
    void har_samlet_utfall_oppfylt_for_begge_vilkårene_med_flere_overlappende_perioder_OPPFYLT() throws Exception {

        var vilkårene = Vilkårene.builder()
            .leggTil(new VilkårBuilder(VilkårType.ALDERSVILKÅR)
                .leggTil(new VilkårPeriodeBuilder().medPeriode("2020-08-01", "9999-12-31").medUtfall(Utfall.OPPFYLT)))
            .leggTil(new VilkårBuilder(VilkårType.UNGDOMSPROGRAMVILKÅRET)
                .leggTil(new VilkårPeriodeBuilder().medPeriode("2020-11-01", "2021-12-31").medUtfall(Utfall.IKKE_VURDERT))
                .leggTil(new VilkårPeriodeBuilder().medPeriode("2022-01-01", "9999-12-31").medUtfall(Utfall.OPPFYLT)))
            .build();

        var allePerioder = vilkårene.getAlleIntervaller();
        LocalDate mindato = allePerioder.getMinLocalDate();
        LocalDate maksdato = allePerioder.getMaxLocalDate();
        var output = SamletVilkårsutfallTjeneste.samleVilkårUtfall(vilkårene, DatoIntervallEntitet.fraOgMedTilOgMed(mindato, maksdato));
        assertThat(output).isNotEmpty();

        var outputIntervaller = List.copyOf(output.getLocalDateIntervals());
        assertThat(outputIntervaller).hasSize(2);
        var first = outputIntervaller.get(0);
        var second = outputIntervaller.get(1);

        assertThat(first).isEqualTo(LocalDateInterval.parseFrom("2020-11-01", "2021-12-31"));
        assertThat(second).isEqualTo(LocalDateInterval.parseFrom("2022-01-01", "9999-12-31"));

        VilkårUtfallSamlet firstValue = output.getSegment(first).getValue();
        assertThat(firstValue.getSamletUtfall()).as(first.toString()).isEqualTo(Utfall.IKKE_VURDERT);
        assertThat(firstValue.getUnderliggendeVilkårUtfall()).as(first.toString()).hasSize(2).satisfies(v -> {
            assertThat(v.getVilkårType()).isEqualTo(VilkårType.ALDERSVILKÅR);
            assertThat(v.getVilkårUtfall()).isEqualTo(Utfall.OPPFYLT);
        }, atIndex(0));
        assertThat(firstValue.getUnderliggendeVilkårUtfall()).as(first.toString()).hasSize(2).satisfies(v -> {
            assertThat(v.getVilkårType()).isEqualTo(VilkårType.UNGDOMSPROGRAMVILKÅRET);
            assertThat(v.getVilkårUtfall()).isEqualTo(Utfall.IKKE_VURDERT);
        }, atIndex(1));

        VilkårUtfallSamlet secondValue = output.getSegment(second).getValue();

        assertThat(secondValue.getSamletUtfall()).as(second.toString()).isEqualTo(Utfall.OPPFYLT);
        assertThat(secondValue.getUnderliggendeVilkårUtfall()).as(second.toString()).hasSize(2).satisfies(v -> {
            assertThat(v.getVilkårType()).isEqualTo(VilkårType.ALDERSVILKÅR);
            assertThat(v.getVilkårUtfall()).isEqualTo(Utfall.OPPFYLT);
        }, atIndex(0));
        assertThat(secondValue.getUnderliggendeVilkårUtfall()).as(second.toString()).hasSize(2).satisfies(v -> {
            assertThat(v.getVilkårType()).isEqualTo(VilkårType.UNGDOMSPROGRAMVILKÅRET);
            assertThat(v.getVilkårUtfall()).isEqualTo(Utfall.OPPFYLT);
        }, atIndex(1));

    }
    @Test
    void har_samlet_utfall_oppfylt_for_minst_omsorgfor_vilkår() throws Exception {

        LocalDate f1 = LocalDate.of(2026, 1, 1), t1 = f1.plusDays(10);
        LocalDate f2 = f1.plusDays(1), t2 = f2.plusDays(10);

        var vilkårOmsorgFor = List.of(new VilkårPeriodeBuilder().medPeriode(f1, t1).medUtfall(Utfall.OPPFYLT).build());
        var vilkårUNGDOMSPROGRAMVILKÅRET = List.of(new VilkårPeriodeBuilder().medPeriode(f2, t2).medUtfall(Utfall.IKKE_OPPFYLT).build());

        Map<VilkårType, LocalDateTimeline<VilkårPeriode>> input = Map.of(
            VilkårType.ALDERSVILKÅR, toTimeline(vilkårOmsorgFor),
            VilkårType.UNGDOMSPROGRAMVILKÅRET, toTimeline(vilkårUNGDOMSPROGRAMVILKÅRET));
        var output = SamletVilkårsutfallTjeneste.samletVilkårUtfall(input, Set.of(VilkårType.ALDERSVILKÅR));

        assertThat(output).isNotEmpty();

        // sjekk intervaller ok
        LocalDateInterval p1 = new LocalDateInterval(f1, f1);
        LocalDateInterval p2 = new LocalDateInterval(f2, t1);
        assertThat(output.getLocalDateIntervals()).isEqualTo(new TreeSet<>(List.of(p1, p2)));

        // sjekk hvert intervall -samlet utfall og underliggende
        assertThat(output.getSegment(p1).getValue().getSamletUtfall()).isEqualTo(Utfall.OPPFYLT);
        assertThat(output.getSegment(p1).getValue().getUnderliggendeVilkårUtfall()).hasSize(1);

        assertThat(output.getSegment(p2).getValue().getSamletUtfall()).isEqualTo(Utfall.IKKE_OPPFYLT);
        assertThat(output.getSegment(p2).getValue().getUnderliggendeVilkårUtfall()).hasSize(2);

    }

    @Test
    void har_samlet_utfall_oppfylt_for_minst_UNGDOMSPROGRAMVILKÅRET_vilkår() throws Exception {

        LocalDate f1 = LocalDate.of(2026, 1, 1), t1 = f1.plusDays(10);
        LocalDate f2 = f1.plusDays(1), t2 = f2.plusDays(10);

        var vilkårOmsorgFor = List.of(new VilkårPeriodeBuilder().medPeriode(f1, t1).medUtfall(Utfall.OPPFYLT).build());
        var vilkårUNGDOMSPROGRAMVILKÅRET = List.of(
            new VilkårPeriodeBuilder().medPeriode(f2, t1).medUtfall(Utfall.OPPFYLT).build(),
            new VilkårPeriodeBuilder().medPeriode(t1.plusDays(1), t2).medUtfall(Utfall.IKKE_OPPFYLT).build());

        Map<VilkårType, LocalDateTimeline<VilkårPeriode>> input = Map.of(
            VilkårType.ALDERSVILKÅR, toTimeline(vilkårOmsorgFor),
            VilkårType.UNGDOMSPROGRAMVILKÅRET, toTimeline(vilkårUNGDOMSPROGRAMVILKÅRET));
        var output = SamletVilkårsutfallTjeneste.samletVilkårUtfall(input, Set.of(VilkårType.UNGDOMSPROGRAMVILKÅRET));

        assertThat(output).isNotEmpty();

        // sjekk intervaller ok
        LocalDateInterval p2 = new LocalDateInterval(f2, t1);
        LocalDateInterval p3 = new LocalDateInterval(t2, t2);
        assertThat(output.getLocalDateIntervals()).isEqualTo(new TreeSet<>(List.of(p2, p3)));

        // sjekk hvert intervall -samlet utfall og underliggende
        assertThat(output.getSegment(p2).getValue().getSamletUtfall()).isEqualTo(Utfall.OPPFYLT);
        assertThat(output.getSegment(p2).getValue().getUnderliggendeVilkårUtfall()).hasSize(2);

        assertThat(output.getSegment(p3).getValue().getSamletUtfall()).isEqualTo(Utfall.IKKE_OPPFYLT);
        assertThat(output.getSegment(p3).getValue().getUnderliggendeVilkårUtfall()).hasSize(1);
    }

    private LocalDateTimeline<VilkårPeriode> toTimeline(List<VilkårPeriode> vilkårOmsorgFor) {
        return new LocalDateTimeline<VilkårPeriode>(vilkårOmsorgFor.stream().map(v -> new LocalDateSegment<>(v.getFom(), v.getTom(), v)).collect(Collectors.toList()));
    }

    record Tuple(LocalDate d0, LocalDate d1) {

        Tuple(String d0, String d1) {
            this(LocalDate.parse(d0), LocalDate.parse(d1));
        }

        static LocalDateTimeline<Boolean> allePerioder(List<Tuple> liste) {
            var segmenter = liste.stream().map(v -> new LocalDateSegment<>(v.d0, v.d1, true)).toList();
            return new LocalDateTimeline<Boolean>(segmenter, (i, v1, v2) -> new LocalDateSegment<>(i, v1.getValue()));
        }
    }
}
