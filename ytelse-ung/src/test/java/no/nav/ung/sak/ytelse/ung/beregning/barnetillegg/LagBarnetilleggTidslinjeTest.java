package no.nav.ung.sak.ytelse.ung.beregning.barnetillegg;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.sak.typer.AktørId;

class LagBarnetilleggTidslinjeTest {

    @Test
    void skal_få_barnetillegg_fra_alle_barn_som_har_levd_i_måneden() {
        var fødselsdato1 = LocalDate.parse("2020-01-01");
        var dødsdato1 = LocalDate.parse("2020-01-14");
        var fødselsdato2 = LocalDate.parse("2020-01-15");
        var dødsdato2 = LocalDate.parse("2020-01-15");
        var fødselsdato3 = LocalDate.parse("2020-01-17");

        var fødselOgDødInfos = List.of(
            new FødselOgDødInfo(AktørId.dummy(), fødselsdato1, dødsdato1),
            new FødselOgDødInfo(AktørId.dummy(), fødselsdato2, dødsdato2),
            new FødselOgDødInfo(AktørId.dummy(), fødselsdato3, null)
        );

        var relevantTidslinje = new LocalDateTimeline<Boolean>(
            LocalDate.of(2019, 12, 15),
            LocalDate.of(2020, 3, 15),
            true
        );

        var resultat = LagBarnetilleggTidslinje.beregnBarnetillegg(relevantTidslinje, fødselOgDødInfos);

        var forventetResultat = new LocalDateTimeline<>(List.of(
            new LocalDateSegment<>(LocalDate.of(2020, 2, 1), LocalDate.of(2020, 2, 29), new Barnetillegg(108, 3)),
            new LocalDateSegment<>(LocalDate.of(2020, 3, 1), LocalDate.of(2020, 3, 15), new Barnetillegg(36, 1))));

        assertThat(resultat.barnetilleggTidslinje()).isEqualTo(forventetResultat);
    }

    @Test
    void skal_få_barnetillegg_for_barn_som_er_født_og_dør_samme_dag() {
        var fødselsdato1 = LocalDate.of(2020,1,1);
        var dødsdato1 = LocalDate.of(2020,1,1);

        var fødselOgDødInfos = List.of(
            new FødselOgDødInfo(AktørId.dummy(), fødselsdato1, dødsdato1)
        );

        var relevantTidslinje = new LocalDateTimeline<>(
            LocalDate.of(2019, 12, 15),
            LocalDate.of(2020, 3, 15),
            true
        );

        var resultat = LagBarnetilleggTidslinje.beregnBarnetillegg(relevantTidslinje, fødselOgDødInfos);

        var forventetResultat = new LocalDateTimeline<>(List.of(
            new LocalDateSegment<>(LocalDate.of(2020, 2, 1), LocalDate.of(2020, 2, 29), new Barnetillegg(36, 1))));

        assertThat(resultat.barnetilleggTidslinje()).isEqualTo(forventetResultat);
    }

    @Test
    void skal_få_barnetillegg_for_barn_som_er_født_og_dør_siste_dag_i_måneden() {
        var fødselsdato1 = LocalDate.of(2020,2,29);
        var dødsdato1 = LocalDate.of(2020,2,29);

        var fødselOgDødInfos = List.of(
            new FødselOgDødInfo(AktørId.dummy(), fødselsdato1, dødsdato1)
        );

        var relevantTidslinje = new LocalDateTimeline<>(
            LocalDate.of(2019, 12, 15),
            LocalDate.of(2020, 3, 15),
            true
        );

        var resultat = LagBarnetilleggTidslinje.beregnBarnetillegg(relevantTidslinje, fødselOgDødInfos);

        var forventetResultat = new LocalDateTimeline<>(List.of(
            new LocalDateSegment<>(LocalDate.of(2020, 3, 1), LocalDate.of(2020, 3, 15), new Barnetillegg(36, 1))));

        assertThat(resultat.barnetilleggTidslinje()).isEqualTo(forventetResultat);
    }

}
