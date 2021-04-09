package no.nav.k9.sak.ytelse.pleiepengerbarn.vilkår;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.sak.kontrakt.sykdom.Resultat;
import no.nav.k9.sak.kontrakt.sykdom.SykdomVurderingType;
import no.nav.k9.sak.typer.Periode;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomGrunnlag;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomInnleggelsePeriode;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomInnleggelser;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomVurdering;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomVurderingVersjon;

class SykdomSamletVurderingFinnGrunnlagforskjellerTest {

    @Test
    public void nyttGrunnlagErLikt() {
        List<SykdomVurderingVersjon> gmlVurderinger = Arrays.asList(vurderingVersjonMock(
            SykdomVurderingType.KONTINUERLIG_TILSYN_OG_PLEIE,
            Resultat.IKKE_OPPFYLT,
            new Periode(LocalDate.of(2021, 1, 1), LocalDate.of(2021, 1, 15))));
        LocalDateTimeline<SykdomSamletVurdering> gmlTidslinje = SykdomSamletVurdering.grunnlagTilTidslinje(grunnlagMock(gmlVurderinger, null));
        List<SykdomVurderingVersjon> nyVurderinger = Arrays.asList(vurderingVersjonMock(
            SykdomVurderingType.KONTINUERLIG_TILSYN_OG_PLEIE,
            Resultat.IKKE_OPPFYLT,
            new Periode(LocalDate.of(2021, 1, 1), LocalDate.of(2021, 1, 15))));
        LocalDateTimeline<SykdomSamletVurdering> nyTidslinje = SykdomSamletVurdering.grunnlagTilTidslinje(grunnlagMock(nyVurderinger, null));


        LocalDateTimeline<Boolean> timeline = SykdomSamletVurdering.finnGrunnlagsforskjellerFullKombinertTidslinje(gmlTidslinje, nyTidslinje);

        Assertions.assertTrue(timeline.isEmpty());
    }

    @Test
    public void nyttGrunnlagHarSammeUtstrekningMenOverstyrer() {
        List<SykdomVurderingVersjon> gmlVurderinger = Arrays.asList(vurderingVersjonMock(
            SykdomVurderingType.KONTINUERLIG_TILSYN_OG_PLEIE,
            Resultat.IKKE_OPPFYLT,
            new Periode(LocalDate.of(2021, 1, 1), LocalDate.of(2021, 1, 15))));
        LocalDateTimeline<SykdomSamletVurdering> gmlTidslinje = SykdomSamletVurdering.grunnlagTilTidslinje(grunnlagMock(gmlVurderinger, null));
        List<SykdomVurderingVersjon> nyVurderinger = Arrays.asList(vurderingVersjonMock(
            SykdomVurderingType.KONTINUERLIG_TILSYN_OG_PLEIE,
            Resultat.OPPFYLT,
            new Periode(LocalDate.of(2021, 1, 1), LocalDate.of(2021, 1, 15))));
        LocalDateTimeline<SykdomSamletVurdering> nyTidslinje = SykdomSamletVurdering.grunnlagTilTidslinje(grunnlagMock(nyVurderinger, null));


        LocalDateTimeline<Boolean> timeline = SykdomSamletVurdering.finnGrunnlagsforskjellerFullKombinertTidslinje(gmlTidslinje, nyTidslinje);

        Assertions.assertTrue(timeline.isContinuous());
        LocalDateInterval interval = timeline.getLocalDateIntervals().first();
        assertThat(interval.getFomDato()).isEqualTo(LocalDate.of(2021, 1, 1));
        assertThat(interval.getTomDato()).isEqualTo(LocalDate.of(2021, 1, 15));
    }

    @Test
    public void gammeltGrunnlagHarFlereVurderingerForSammePeriode() {
        List<SykdomVurderingVersjon> gmlVurderinger = Arrays.asList(
            vurderingVersjonMock(
                SykdomVurderingType.KONTINUERLIG_TILSYN_OG_PLEIE,
                Resultat.IKKE_OPPFYLT,
                0L,
                new Periode(LocalDate.of(2021, 1, 1), LocalDate.of(2021, 1, 15))
            ),
            vurderingVersjonMock(
                SykdomVurderingType.KONTINUERLIG_TILSYN_OG_PLEIE,
                Resultat.OPPFYLT,
                1L,
                new Periode(LocalDate.of(2021, 1, 10), LocalDate.of(2021, 1, 15))
            )
        );

        LocalDateTimeline<SykdomSamletVurdering> gmlTidslinje = SykdomSamletVurdering.grunnlagTilTidslinje(grunnlagMock(gmlVurderinger, null));
        List<SykdomVurderingVersjon> nyVurderinger = Arrays.asList(vurderingVersjonMock(
            SykdomVurderingType.KONTINUERLIG_TILSYN_OG_PLEIE,
            Resultat.OPPFYLT,
            new Periode(LocalDate.of(2021, 1, 1), LocalDate.of(2021, 1, 15))));
        LocalDateTimeline<SykdomSamletVurdering> nyTidslinje = SykdomSamletVurdering.grunnlagTilTidslinje(grunnlagMock(nyVurderinger, null));


        LocalDateTimeline<Boolean> timeline = SykdomSamletVurdering.finnGrunnlagsforskjellerFullKombinertTidslinje(gmlTidslinje, nyTidslinje);

        Assertions.assertTrue(timeline.isContinuous());
        LocalDateInterval interval = timeline.getLocalDateIntervals().first();
        assertThat(interval.getFomDato()).isEqualTo(LocalDate.of(2021, 1, 1));
        assertThat(interval.getTomDato()).isEqualTo(LocalDate.of(2021, 1, 9));
    }

    @Test
    public void nyttGrunnlagOverstyrerDelerAvGammel() {
        List<SykdomVurderingVersjon> gmlVurderinger = Arrays.asList(vurderingVersjonMock(
            SykdomVurderingType.KONTINUERLIG_TILSYN_OG_PLEIE,
            Resultat.IKKE_OPPFYLT,
            new Periode(LocalDate.of(2021, 1, 1), LocalDate.of(2021, 1, 15))));
        LocalDateTimeline<SykdomSamletVurdering> gmlTidslinje = SykdomSamletVurdering.grunnlagTilTidslinje(grunnlagMock(gmlVurderinger, null));
        List<SykdomVurderingVersjon> nyVurderinger = Arrays.asList(vurderingVersjonMock(
            SykdomVurderingType.KONTINUERLIG_TILSYN_OG_PLEIE,
            Resultat.OPPFYLT,
            new Periode(LocalDate.of(2021, 1, 1), LocalDate.of(2021, 1, 10))));
        LocalDateTimeline<SykdomSamletVurdering> nyTidslinje = SykdomSamletVurdering.grunnlagTilTidslinje(grunnlagMock(nyVurderinger, null));


        LocalDateTimeline<Boolean> timeline = SykdomSamletVurdering.finnGrunnlagsforskjellerFullKombinertTidslinje(gmlTidslinje, nyTidslinje);

        Assertions.assertTrue(timeline.isContinuous());
        LocalDateInterval interval = timeline.getLocalDateIntervals().first();
        assertThat(interval.getFomDato()).isEqualTo(LocalDate.of(2021, 1, 1));
        assertThat(interval.getTomDato()).isEqualTo(LocalDate.of(2021, 1, 15));
    }

    @Test
    public void nyttGrunnlagHarTilstøtendePeriode() {
        List<SykdomVurderingVersjon> gmlVurderinger = Arrays.asList(vurderingVersjonMock(
            SykdomVurderingType.KONTINUERLIG_TILSYN_OG_PLEIE,
            Resultat.IKKE_OPPFYLT,
            new Periode(LocalDate.of(2021, 1, 1), LocalDate.of(2021, 1, 15))));
        LocalDateTimeline<SykdomSamletVurdering> gmlTidslinje = SykdomSamletVurdering.grunnlagTilTidslinje(grunnlagMock(gmlVurderinger, null));
        List<SykdomVurderingVersjon> nyVurderinger = Arrays.asList(vurderingVersjonMock(
            SykdomVurderingType.KONTINUERLIG_TILSYN_OG_PLEIE,
            Resultat.OPPFYLT,
            new Periode(LocalDate.of(2021, 1, 16), LocalDate.of(2021, 1, 20))));
        LocalDateTimeline<SykdomSamletVurdering> nyTidslinje = SykdomSamletVurdering.grunnlagTilTidslinje(grunnlagMock(nyVurderinger, null));


        LocalDateTimeline<Boolean> timeline = SykdomSamletVurdering.finnGrunnlagsforskjellerFullKombinertTidslinje(gmlTidslinje, nyTidslinje);

        Assertions.assertTrue(timeline.isContinuous());
        LocalDateInterval interval = timeline.getLocalDateIntervals().first();
        assertThat(interval.getFomDato()).isEqualTo(LocalDate.of(2021, 1, 1));
        assertThat(interval.getTomDato()).isEqualTo(LocalDate.of(2021, 1, 20));
    }

    @Test
    public void nyttGrunnlagErIkkeTilstøtende() {
        List<SykdomVurderingVersjon> gmlVurderinger = Arrays.asList(vurderingVersjonMock(
            SykdomVurderingType.KONTINUERLIG_TILSYN_OG_PLEIE,
            Resultat.IKKE_OPPFYLT,
            new Periode(LocalDate.of(2021, 1, 1), LocalDate.of(2021, 1, 10))));
        LocalDateTimeline<SykdomSamletVurdering> gmlTidslinje = SykdomSamletVurdering.grunnlagTilTidslinje(grunnlagMock(gmlVurderinger, null));
        List<SykdomVurderingVersjon> nyVurderinger = Arrays.asList(vurderingVersjonMock(
            SykdomVurderingType.KONTINUERLIG_TILSYN_OG_PLEIE,
            Resultat.OPPFYLT,
            new Periode(LocalDate.of(2021, 1, 15), LocalDate.of(2021, 1, 20))));
        LocalDateTimeline<SykdomSamletVurdering> nyTidslinje = SykdomSamletVurdering.grunnlagTilTidslinje(grunnlagMock(nyVurderinger, null));


        LocalDateTimeline<Boolean> timeline = SykdomSamletVurdering.finnGrunnlagsforskjellerFullKombinertTidslinje(gmlTidslinje, nyTidslinje);

        Assertions.assertFalse(timeline.isContinuous());
        LocalDateInterval first = timeline.getLocalDateIntervals().first();
        assertThat(first.getFomDato()).isEqualTo(LocalDate.of(2021, 1, 1));
        assertThat(first.getTomDato()).isEqualTo(LocalDate.of(2021, 1, 10));
        LocalDateInterval last = timeline.getLocalDateIntervals().last();
        assertThat(last.getFomDato()).isEqualTo(LocalDate.of(2021, 1, 15));
        assertThat(last.getTomDato()).isEqualTo(LocalDate.of(2021, 1, 20));
    }

    @Test
    public void nyttGrunnlagHarInnleggelse() {
        List<SykdomVurderingVersjon> gmlVurderinger = Arrays.asList(vurderingVersjonMock(
            SykdomVurderingType.KONTINUERLIG_TILSYN_OG_PLEIE,
            Resultat.OPPFYLT,
            new Periode(LocalDate.of(2021, 1, 1), LocalDate.of(2021, 1, 15))));
        LocalDateTimeline<SykdomSamletVurdering> gmlTidslinje = SykdomSamletVurdering.grunnlagTilTidslinje(grunnlagMock(gmlVurderinger, null));
        List<SykdomVurderingVersjon> nyVurderinger = Arrays.asList(vurderingVersjonMock(
            SykdomVurderingType.KONTINUERLIG_TILSYN_OG_PLEIE,
            Resultat.OPPFYLT,
            new Periode(LocalDate.of(2021, 1, 1), LocalDate.of(2021, 1, 15))));
        SykdomInnleggelser innleggelser = innleggelserMock(new Periode(LocalDate.of(2021, 1, 15), LocalDate.of(2021, 1, 20)));
        LocalDateTimeline<SykdomSamletVurdering> nyTidslinje = SykdomSamletVurdering.grunnlagTilTidslinje(grunnlagMock(nyVurderinger, innleggelser));


        LocalDateTimeline<Boolean> timeline = SykdomSamletVurdering.finnGrunnlagsforskjellerFullKombinertTidslinje(gmlTidslinje, nyTidslinje);

        Assertions.assertTrue(timeline.isContinuous());
        LocalDateInterval interval = timeline.getLocalDateIntervals().first();
        assertThat(interval.getFomDato()).isEqualTo(LocalDate.of(2021, 1, 15));
        assertThat(interval.getTomDato()).isEqualTo(LocalDate.of(2021, 1, 20));
    }

    @Test
    public void nyttGrunnlagHarInnleggelseIkkeVurdering() { //Ugyldig case? Med for full testdekning.
        List<SykdomVurderingVersjon> gmlVurderinger = Arrays.asList(vurderingVersjonMock(
            SykdomVurderingType.KONTINUERLIG_TILSYN_OG_PLEIE,
            Resultat.OPPFYLT,
            new Periode(LocalDate.of(2021, 1, 1), LocalDate.of(2021, 1, 15))));
        SykdomInnleggelser innleggelser = innleggelserMock(new Periode(LocalDate.of(2021, 1, 15), LocalDate.of(2021, 1, 20)));
        LocalDateTimeline<SykdomSamletVurdering> gmlTidslinje = SykdomSamletVurdering.grunnlagTilTidslinje(grunnlagMock(gmlVurderinger, innleggelser));
        LocalDateTimeline<SykdomSamletVurdering> nyTidslinje = SykdomSamletVurdering.grunnlagTilTidslinje(grunnlagMock(Collections.emptyList(), innleggelser));


        LocalDateTimeline<Boolean> timeline = SykdomSamletVurdering.finnGrunnlagsforskjellerFullKombinertTidslinje(gmlTidslinje, nyTidslinje);

        Assertions.assertTrue(timeline.isContinuous());
        LocalDateInterval interval = timeline.getLocalDateIntervals().first();
        assertThat(interval.getFomDato()).isEqualTo(LocalDate.of(2021, 1, 1));
        assertThat(interval.getTomDato()).isEqualTo(LocalDate.of(2021, 1, 15));
    }

    @Test
    public void nyttGrunnlagHarToOmsorgspersoner() {
        List<SykdomVurderingVersjon> gmlVurderinger = Arrays.asList(vurderingVersjonMock(
            SykdomVurderingType.KONTINUERLIG_TILSYN_OG_PLEIE,
            Resultat.OPPFYLT,
            new Periode(LocalDate.of(2021, 1, 1), LocalDate.of(2021, 1, 15))));
        LocalDateTimeline<SykdomSamletVurdering> gmlTidslinje = SykdomSamletVurdering.grunnlagTilTidslinje(grunnlagMock(gmlVurderinger, null));
        List<SykdomVurderingVersjon> nyVurderinger = Arrays.asList(
            vurderingVersjonMock(
                SykdomVurderingType.KONTINUERLIG_TILSYN_OG_PLEIE,
                Resultat.OPPFYLT,
                new Periode(LocalDate.of(2021, 1, 1), LocalDate.of(2021, 1, 15))),
            vurderingVersjonMock(
                SykdomVurderingType.TO_OMSORGSPERSONER,
                Resultat.OPPFYLT,
                new Periode(LocalDate.of(2021, 1, 10), LocalDate.of(2021, 1, 15))));
        LocalDateTimeline<SykdomSamletVurdering> nyTidslinje = SykdomSamletVurdering.grunnlagTilTidslinje(grunnlagMock(nyVurderinger, null));


        LocalDateTimeline<Boolean> timeline = SykdomSamletVurdering.finnGrunnlagsforskjellerFullKombinertTidslinje(gmlTidslinje, nyTidslinje);

        Assertions.assertTrue(timeline.isContinuous());
        LocalDateInterval interval = timeline.getLocalDateIntervals().first();
        assertThat(interval.getFomDato()).isEqualTo(LocalDate.of(2021, 1, 10));
        assertThat(interval.getTomDato()).isEqualTo(LocalDate.of(2021, 1, 15));
    }

    @Test
    public void nyttGrunnlagOverstyrerToOmsorgspersoner() {
        List<SykdomVurderingVersjon> gmlVurderinger = Arrays.asList(vurderingVersjonMock(
            SykdomVurderingType.KONTINUERLIG_TILSYN_OG_PLEIE,
            Resultat.OPPFYLT,
            new Periode(LocalDate.of(2021, 1, 1), LocalDate.of(2021, 1, 15))),
            vurderingVersjonMock(
                SykdomVurderingType.TO_OMSORGSPERSONER,
                Resultat.IKKE_OPPFYLT,
                new Periode(LocalDate.of(2021, 1, 9), LocalDate.of(2021, 1, 15))));
        LocalDateTimeline<SykdomSamletVurdering> gmlTidslinje = SykdomSamletVurdering.grunnlagTilTidslinje(grunnlagMock(gmlVurderinger, null));
        List<SykdomVurderingVersjon> nyVurderinger = Arrays.asList(
            vurderingVersjonMock(
                SykdomVurderingType.KONTINUERLIG_TILSYN_OG_PLEIE,
                Resultat.OPPFYLT,
                new Periode(LocalDate.of(2021, 1, 1), LocalDate.of(2021, 1, 15))),
            vurderingVersjonMock(
                SykdomVurderingType.TO_OMSORGSPERSONER,
                Resultat.OPPFYLT,
                new Periode(LocalDate.of(2021, 1, 10), LocalDate.of(2021, 1, 15))));
        LocalDateTimeline<SykdomSamletVurdering> nyTidslinje = SykdomSamletVurdering.grunnlagTilTidslinje(grunnlagMock(nyVurderinger, null));


        LocalDateTimeline<Boolean> timeline = SykdomSamletVurdering.finnGrunnlagsforskjellerFullKombinertTidslinje(gmlTidslinje, nyTidslinje);

        Assertions.assertTrue(timeline.isContinuous());
        LocalDateInterval interval = timeline.getLocalDateIntervals().first();
        assertThat(interval.getFomDato()).isEqualTo(LocalDate.of(2021, 1, 9));
        assertThat(interval.getTomDato()).isEqualTo(LocalDate.of(2021, 1, 15));
    }


    private SykdomGrunnlag grunnlagMock(List<SykdomVurderingVersjon> vurderinger, SykdomInnleggelser innleggelser) {
        return new SykdomGrunnlag(null, new ArrayList<>(), new ArrayList<>(), vurderinger, innleggelser, null, "test", LocalDateTime.now());
    }

    private SykdomInnleggelser innleggelserMock(Periode... perioder) {
        return new SykdomInnleggelser(
            0L,
            Arrays.asList(perioder).stream().map(p -> { return new SykdomInnleggelsePeriode(p.getFom(), p.getTom(), "", LocalDateTime.now()); }).collect(Collectors.toList()),
            "",
            LocalDateTime.now()
        );
    }

    private SykdomVurderingVersjon vurderingVersjonMock(SykdomVurderingType type, Resultat resultat, Periode... perioder) {
        return vurderingVersjonMock(type, resultat, 0L, perioder);
    }

    private SykdomVurderingVersjon vurderingVersjonMock(SykdomVurderingType type, Resultat resultat, long rangering, Periode... perioder) {
        var vurdering = new SykdomVurdering(type, Collections.emptyList(), "", LocalDateTime.now());
        vurdering.setRangering(rangering);

        SykdomVurderingVersjon vurderingVersjon = new SykdomVurderingVersjon(
            vurdering,
            "",
            resultat,
            1L,
            "",
            LocalDateTime.now(),
            null,
            null,
            null,
            null,
            Collections.emptyList(),
            Arrays.asList(perioder)
        );
        vurdering.addVersjon(vurderingVersjon);
        return vurderingVersjon;
    }

}
