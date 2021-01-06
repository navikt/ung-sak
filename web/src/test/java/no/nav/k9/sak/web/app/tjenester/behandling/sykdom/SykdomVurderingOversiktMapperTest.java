package no.nav.k9.sak.web.app.tjenester.behandling.sykdom;

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
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.Resultat;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomVurdering;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomVurderingPeriode;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomVurderingType;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomVurderingVersjon;

public class SykdomVurderingOversiktMapperTest {

    
    @Test
    public void tilTidslinjeHåndtererEnVerdi() {
        final SykdomVurderingOversiktMapper mapper = new SykdomVurderingOversiktMapper();
        
        final List<SykdomVurderingVersjon> versjoner = Arrays.asList(createSykdomVurderingOgVersjonMock(1L, LocalDate.of(2020, 1, 1), LocalDate.of(2020, 1, 5), LocalDate.of(2020, 1, 10), LocalDate.of(2020, 1, 15)));

        final NavigableSet<LocalDateSegment<SykdomVurderingVersjon>> segments = mapper.tilTidslinje(versjoner).toSegments();
        assertThat(segments.size()).isEqualTo(2);
    }
    
    @Test
    public void tilTidslinjeVelgerPrioritertVersjonOgSlårSammenPerioderMedSammeVerdi() {
        final SykdomVurderingOversiktMapper mapper = new SykdomVurderingOversiktMapper();
        
        final List<SykdomVurderingVersjon> versjoner = Arrays.asList(createSykdomVurderingOgVersjonMock(1L, LocalDate.of(2020, 1, 1), LocalDate.of(2020, 1, 5), LocalDate.of(2020, 1, 10), LocalDate.of(2020, 1, 15)),
                createSykdomVurderingOgVersjonMock(2L, LocalDate.of(2020, 1, 14), LocalDate.of(2020, 1, 17), LocalDate.of(2020, 1, 18), LocalDate.of(2020, 1, 29)));
        final NavigableSet<LocalDateSegment<SykdomVurderingVersjon>> segments = new TreeSet<>(mapper.tilTidslinje(versjoner).toSegments());
        
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
        
    private SykdomVurderingVersjon createSykdomVurderingOgVersjonMock(long rangering, LocalDate fom1, LocalDate tom1, LocalDate fom2, LocalDate tom2) {
        return new SykdomVurderingVersjon(createSykdomVurderingMock(rangering), "", Resultat.OPPFYLT, Long.valueOf(0L), "", LocalDateTime.now(), null, null, null, null, Collections.emptyList(), Arrays.asList(
                new SykdomVurderingPeriode(1L, null, fom1, tom1, "", LocalDateTime.now()),
                new SykdomVurderingPeriode(1L, null, fom2, tom2, "", LocalDateTime.now())
                ));
    }
    
    private SykdomVurdering createSykdomVurderingMock(long rangering) {
        return new SykdomVurdering(SykdomVurderingType.KONTINUERLIG_TILSYN_OG_PLEIE, Long.valueOf(rangering), null, Collections.emptyList(), Long.valueOf(1L), "", LocalDateTime.now());
    }
}