package no.nav.ung.ytelse.ungdomsprogramytelsen.del1.steg.aldersvilkår;

import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.kodeverk.vilkår.Avslagsårsak;
import no.nav.ung.kodeverk.vilkår.Utfall;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.VilkårBuilder;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriodeBuilder;
import no.nav.ung.sak.tid.DatoIntervallEntitet;
import no.nav.ung.sak.tid.TidslinjeUtil;

import java.time.LocalDate;
import java.util.NavigableSet;

public class VurderAldersVilkårTjeneste {

    public void vurderPerioder(VilkårBuilder vilkårBuilder, NavigableSet<DatoIntervallEntitet> perioderTilVurdering, LocalDate fødselsdato) {
        var førsteDagMedGodkjentAlder = fødselsdato.plusYears(18);
        var sisteDagMedGodkjentAlder = fødselsdato.plusYears(30).minusDays(1);

        var regelInput = """
            { "fødselsdato": ":fødselsdato", "førsteDagMedGodkjentAlder": ":førsteDagMedGodkjentAlder", "sisteDagMedGodkjentAlder: ":sisteDagMedGodkjentAlder"}""".stripLeading()
            .replaceFirst(":fødselsdato", fødselsdato.toString())
            .replaceFirst(":førsteDagMedGodkjentAlder", førsteDagMedGodkjentAlder.toString())
            .replaceFirst(":sisteDagMedGodkjentAlder", sisteDagMedGodkjentAlder.toString());

        LocalDateTimeline<Boolean> tilVurderingTidslinje = TidslinjeUtil.tilTidslinje(perioderTilVurdering);

        LocalDateTimeline<Boolean> forUngTidslinje = tilVurderingTidslinje.intersection(new LocalDateTimeline<>(LocalDate.MIN, førsteDagMedGodkjentAlder.minusDays(1), true));
        LocalDateTimeline<Boolean> forGammelTidslinje = tilVurderingTidslinje.intersection(new LocalDateTimeline<>(sisteDagMedGodkjentAlder.plusDays(1), LocalDate.MAX, true));
        LocalDateTimeline<Boolean> godkjentAlderTidsinje = tilVurderingTidslinje.disjoint(forUngTidslinje).disjoint(forGammelTidslinje);

        for (LocalDateSegment<Boolean> segment : forUngTidslinje.toSegments()) {
            VilkårPeriodeBuilder builder = vilkårBuilder.hentBuilderFor(segment.getFom(), segment.getTom());
            builder.medUtfall(Utfall.IKKE_OPPFYLT).medAvslagsårsak(Avslagsårsak.SØKER_UNDER_MINSTE_ALDER).medRegelInput(regelInput);
            vilkårBuilder.leggTil(builder);
        }
        for (LocalDateSegment<Boolean> segment : forGammelTidslinje.toSegments()) {
            VilkårPeriodeBuilder builder = vilkårBuilder.hentBuilderFor(segment.getFom(), segment.getTom());
            builder.medUtfall(Utfall.IKKE_OPPFYLT).medAvslagsårsak(Avslagsårsak.SØKER_OVER_HØYESTE_ALDER).medRegelInput(regelInput);
            vilkårBuilder.leggTil(builder);
        }
        for (LocalDateSegment<Boolean> segment : godkjentAlderTidsinje.toSegments()) {
            VilkårPeriodeBuilder builder = vilkårBuilder.hentBuilderFor(segment.getFom(), segment.getTom());
            builder.medUtfall(Utfall.OPPFYLT).medRegelInput(regelInput);
            vilkårBuilder.leggTil(builder);
        }
    }

}
