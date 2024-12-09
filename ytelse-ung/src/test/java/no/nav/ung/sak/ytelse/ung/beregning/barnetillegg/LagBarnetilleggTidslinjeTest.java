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

        var forventetResultat = new LocalDateTimeline<>(List.of(
            new LocalDateSegment<>(LocalDate.parse("2014-08-01", DateTimeFormatter.ISO_LOCAL_DATE), LocalDate.parse("2020-03-31", DateTimeFormatter.ISO_LOCAL_DATE), new Barnetillegg(36, 1)),
            new LocalDateSegment<>(LocalDate.parse("2020-04-01", DateTimeFormatter.ISO_LOCAL_DATE), LocalDate.parse("2024-10-31", DateTimeFormatter.ISO_LOCAL_DATE), new Barnetillegg(72, 2)),
            new LocalDateSegment<>(LocalDate.parse("2024-11-01", DateTimeFormatter.ISO_LOCAL_DATE), TIDENES_ENDE, new Barnetillegg(108, 3))
        ));

        assertThat(resultat).isEqualTo(forventetResultat);
    }
}
