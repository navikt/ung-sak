package no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom;

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
import no.nav.k9.sak.typer.Periode;

class SykdomUtilsTest {
    
    @Test
    public void tilTidslinjeHåndtererEnVerdi() {       
        final List<SykdomVurderingVersjon> versjoner = Arrays.asList(
            createSykdomVurderingOgVersjonMock(
                1L,
                new Periode(LocalDate.of(2020, 1, 1), LocalDate.of(2020, 1, 5)),
                new Periode(LocalDate.of(2020, 1, 10), LocalDate.of(2020, 1, 15))
            )
        );

        final NavigableSet<LocalDateSegment<SykdomVurderingVersjon>> segments = SykdomUtils.tilTidslinje(versjoner).toSegments();
        assertThat(segments.size()).isEqualTo(2);
    }
    
    @Test
    public void tilTidslinjeVelgerPrioritertVersjonOgSlårSammenPerioderMedSammeVerdi() {       
        final List<SykdomVurderingVersjon> versjoner = Arrays.asList(
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
        final NavigableSet<LocalDateSegment<SykdomVurderingVersjon>> segments = new TreeSet<>(SykdomUtils.tilTidslinje(versjoner).toSegments());
        
        assertThat(segments.size()).isEqualTo(3);
        verify(segments.pollFirst(), LocalDate.of(2020, 1, 1), LocalDate.of(2020, 1, 5), 1);
        verify(segments.pollFirst(), LocalDate.of(2020, 1, 10), LocalDate.of(2020, 1, 13), 1);
        verify(segments.pollFirst(), LocalDate.of(2020, 1, 14), LocalDate.of(2020, 1, 29), 2);
    }
    
    private void verify(LocalDateSegment<SykdomVurderingVersjon> ds, LocalDate fom, LocalDate tom, long rangering) {
        assertThat(ds.getValue().getSykdomVurdering().getRangering()).isEqualTo(rangering);
        assertThat(ds.getFom()).isEqualTo(fom);
        assertThat(ds.getTom()).isEqualTo(tom);
    }
        
    private SykdomVurderingVersjon createSykdomVurderingOgVersjonMock(long rangering, Periode... perioder) {
        return new SykdomVurderingVersjon(
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
    
    private SykdomVurdering createSykdomVurderingMock(long rangering) {
        var s = new SykdomVurdering(SykdomVurderingType.KONTINUERLIG_TILSYN_OG_PLEIE, Collections.emptyList(), "", LocalDateTime.now());
        s.setRangering(Long.valueOf(rangering));
        return s;
    }
}
