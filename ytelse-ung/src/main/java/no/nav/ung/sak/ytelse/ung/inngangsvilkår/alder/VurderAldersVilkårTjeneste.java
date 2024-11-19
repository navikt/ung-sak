package no.nav.ung.sak.ytelse.ung.inngangsvilkår.alder;

import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.NavigableSet;

import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.kodeverk.vilkår.Avslagsårsak;
import no.nav.ung.kodeverk.vilkår.Utfall;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.VilkårBuilder;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriodeBuilder;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.domene.typer.tid.TidslinjeUtil;

public class VurderAldersVilkårTjeneste {

    public void vurderPerioder(VilkårBuilder vilkårBuilder, NavigableSet<DatoIntervallEntitet> perioderTilVurdering, LocalDate fødselsdato) {
        var førsteDagMedGodkjentAlder = fødselsdato.plusYears(18).with(TemporalAdjusters.lastDayOfMonth()).plusDays(1);
        var sisteDagMedGodkjentAlder = fødselsdato.plusYears(29).with(TemporalAdjusters.lastDayOfMonth());
        var regelInput = """
            { "fødselsdato": ":fødselsdato", "førsteDagMedGodkjentAlder": ":førsteDagMedGodkjentAlder", "sisteDagMedGodkjentAlder: ":sisteDagMedGodkjentAlder"}""".stripLeading()
            .replaceFirst(":fødselsdato", fødselsdato.toString())
            .replaceFirst(":førsteDagMedGodkjentAlder", førsteDagMedGodkjentAlder.toString())
            .replaceFirst(":sisteDagMedGodkjentAlder", sisteDagMedGodkjentAlder.toString());

        LocalDateTimeline<Boolean> tidslinjeTilVurdering = TidslinjeUtil.tilTidslinje(perioderTilVurdering);

        LocalDateTimeline<Boolean> tidslinjeSøkerForUng = tidslinjeTilVurdering.disjoint(new LocalDateInterval(førsteDagMedGodkjentAlder, LocalDate.MAX));
        LocalDateTimeline<Boolean> tidslinjeSøkerForGammel = tidslinjeTilVurdering.disjoint(new LocalDateInterval(LocalDate.MIN, sisteDagMedGodkjentAlder));
        LocalDateTimeline<Boolean> tidslinjeAldersvilkårOppfylt = tidslinjeTilVurdering.disjoint(tidslinjeSøkerForUng).disjoint(tidslinjeSøkerForGammel);

        for (LocalDateSegment<Boolean> segment : tidslinjeSøkerForUng.toSegments()) {
            VilkårPeriodeBuilder builder = vilkårBuilder.hentBuilderFor(DatoIntervallEntitet.fra(segment.getLocalDateInterval()));
            builder.medUtfall(Utfall.IKKE_OPPFYLT).medAvslagsårsak(Avslagsårsak.SØKER_UNDER_MINSTE_ALDER).medRegelInput(regelInput);
            vilkårBuilder.leggTil(builder);
        }
        for (LocalDateSegment<Boolean> oppfyltSegment : tidslinjeAldersvilkårOppfylt.toSegments()) {
            VilkårPeriodeBuilder builder = vilkårBuilder.hentBuilderFor(DatoIntervallEntitet.fra(oppfyltSegment.getLocalDateInterval()));
            builder.medUtfall(Utfall.OPPFYLT).medRegelInput(regelInput);
            vilkårBuilder.leggTil(builder);
        }
        for (LocalDateSegment<Boolean> segment : tidslinjeSøkerForGammel.toSegments()) {
            VilkårPeriodeBuilder builder = vilkårBuilder.hentBuilderFor(DatoIntervallEntitet.fra(segment.getLocalDateInterval()));
            builder.medUtfall(Utfall.IKKE_OPPFYLT).medAvslagsårsak(Avslagsårsak.SØKER_OVER_HØYESTE_ALDER).medRegelInput(regelInput);
            vilkårBuilder.leggTil(builder);
        }
    }

}
