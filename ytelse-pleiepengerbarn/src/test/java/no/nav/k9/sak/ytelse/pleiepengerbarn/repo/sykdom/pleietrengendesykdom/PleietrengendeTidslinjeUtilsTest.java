package no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.pleietrengendesykdom;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.NavigableSet;
import java.util.TreeSet;

import org.junit.jupiter.api.Test;

import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.k9.sak.kontrakt.sykdom.Resultat;
import no.nav.k9.sak.kontrakt.sykdom.SykdomVurderingType;
import no.nav.k9.sak.typer.Periode;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.pleietrengendesykdom.PleietrengendeSykdomVurdering;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.pleietrengendesykdom.PleietrengendeSykdomVurderingVersjon;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.pleietrengendesykdom.PleietrengendeTidslinjeUtils;

class PleietrengendeTidslinjeUtilsTest {

    @Test
    void tilTidslinjeHåndtererEnVerdi() {
        final List<PleietrengendeSykdomVurderingVersjon> versjoner = Arrays.asList(
            createSykdomVurderingOgVersjonMock(
                1L,
                new Periode(LocalDate.of(2020, 1, 1), LocalDate.of(2020, 1, 5)),
                new Periode(LocalDate.of(2020, 1, 10), LocalDate.of(2020, 1, 15))
            )
        );

        final NavigableSet<LocalDateSegment<PleietrengendeSykdomVurderingVersjon>> segments = PleietrengendeTidslinjeUtils.tilTidslinje(versjoner).toSegments();
        assertThat(segments).hasSize(2);
    }

    @Test
    void tilTidslinjeVelgerPrioritertVersjonOgSlårSammenPerioderMedSammeVerdi() {
        final List<PleietrengendeSykdomVurderingVersjon> versjoner = Arrays.asList(
            createSykdomVurderingOgVersjonMock(
                1L,
                new Periode(LocalDate.of(2020, 1, 1), LocalDate.of(2020, 1, 5)),
                new Periode(LocalDate.of(2020, 1, 10), LocalDate.of(2020, 1, 15))
            ),
            createSykdomVurderingOgVersjonMock(
                2L,
                new Periode(LocalDate.of(2020, 1, 14), LocalDate.of(2020, 1, 17)),
                new Periode(LocalDate.of(2020, 1, 18), LocalDate.of(2020, 1, 29)))
        );
        final NavigableSet<LocalDateSegment<PleietrengendeSykdomVurderingVersjon>> segments = new TreeSet<>(PleietrengendeTidslinjeUtils.tilTidslinje(versjoner).toSegments());

        assertThat(segments).hasSize(3);
        verify(segments.pollFirst(), LocalDate.of(2020, 1, 1), LocalDate.of(2020, 1, 5), 1);
        verify(segments.pollFirst(), LocalDate.of(2020, 1, 10), LocalDate.of(2020, 1, 13), 1);
        verify(segments.pollFirst(), LocalDate.of(2020, 1, 14), LocalDate.of(2020, 1, 29), 2);
    }

    private void verify(LocalDateSegment<PleietrengendeSykdomVurderingVersjon> ds, LocalDate fom, LocalDate tom, long rangering) {
        assertThat(ds.getValue().getSykdomVurdering().getRangering()).isEqualTo(rangering);
        assertThat(ds.getFom()).isEqualTo(fom);
        assertThat(ds.getTom()).isEqualTo(tom);
    }

    private PleietrengendeSykdomVurderingVersjon createSykdomVurderingOgVersjonMock(long rangering, Periode... perioder) {
        return new PleietrengendeSykdomVurderingVersjon(
            createSykdomVurderingMock(rangering),
            "",
            Resultat.OPPFYLT,
            Long.valueOf(0L),
            "",
            LocalDateTime.now(),
            null,
            null,
            null,
            null,
            Collections.emptyList(),
            Arrays.asList(perioder)
        );
    }

    private PleietrengendeSykdomVurdering createSykdomVurderingMock(long rangering) {
        var s = new PleietrengendeSykdomVurdering(SykdomVurderingType.KONTINUERLIG_TILSYN_OG_PLEIE, Collections.emptyList(), "", LocalDateTime.now());
        s.setRangering(Long.valueOf(rangering));
        return s;
    }
}
