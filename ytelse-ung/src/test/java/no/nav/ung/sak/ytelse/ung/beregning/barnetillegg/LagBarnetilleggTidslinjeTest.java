package no.nav.ung.sak.ytelse.ung.beregning.barnetillegg;

import static no.nav.ung.sak.domene.typer.tid.AbstractLocalDateInterval.TIDENES_ENDE;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Iterator;
import java.util.List;
import java.util.NavigableSet;

import org.junit.jupiter.api.Test;

import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;

class LagBarnetilleggTidslinjeTest {

    @Test
    void skal_lage_tidslinjer_for_tre_barn_med_fødselsdatoer_midt_i_måneden() {
        var fødselsdato1 = LocalDate.parse("2014-07-18", DateTimeFormatter.ISO_LOCAL_DATE);
        var fødselsdato2 = LocalDate.parse("2020-03-18", DateTimeFormatter.ISO_LOCAL_DATE);
        var fødselsdato3 = LocalDate.parse("2024-10-09", DateTimeFormatter.ISO_LOCAL_DATE);

        var resultat = LagBarnetilleggTidslinje.beregnBarnetillegg(new LocalDateTimeline<>(List.of(
            new LocalDateSegment<>(fødselsdato1, fødselsdato2.minusDays(1), 1),
            new LocalDateSegment<>(fødselsdato2, fødselsdato3.minusDays(1), 2),
            new LocalDateSegment<>(fødselsdato3, TIDENES_ENDE, 3)
        )));

        var segmenter = resultat.toSegments();
        assertThat(segmenter.size()).isEqualTo(3);
        var segmentIterator = segmenter.iterator();
        var første = segmentIterator.next();
        assertThat(første.getFom()).isEqualTo(LocalDate.parse("2014-08-01", DateTimeFormatter.ISO_LOCAL_DATE));
        assertThat(første.getValue().antallBarn()).isEqualTo(1);

        var andre = segmentIterator.next();
        assertThat(andre.getFom()).isEqualTo(LocalDate.parse("2020-04-01", DateTimeFormatter.ISO_LOCAL_DATE));
        assertThat(andre.getValue().antallBarn()).isEqualTo(2);

        var tredje = segmentIterator.next();
        assertThat(tredje.getFom()).isEqualTo(LocalDate.parse("2024-11-01", DateTimeFormatter.ISO_LOCAL_DATE));
        assertThat(tredje.getValue().antallBarn()).isEqualTo(3);

    }
}
