package no.nav.ung.sak.domene.behandling.steg.beregning.barnetillegg;

import static no.nav.ung.sak.domene.typer.tid.AbstractLocalDateInterval.TIDENES_ENDE;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.sak.typer.AktørId;

class LagBarnetilleggTidslinjeTest {

    public static final int BARNETILLEGG_SATS = 36;
    public static final int BARNETILLEGG_SATS_HØY = 37;
    public static final LocalDate ENDRET_SATS_DATO = LocalDate.of(2025, 1, 1);


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


        var resultat = LagBarnetilleggTidslinje.beregnBarnetillegg(fødselOgDødInfos);

        var forventetResultat = new LocalDateTimeline<>(List.of(
            new LocalDateSegment<>(fødselsdato1, dødsdato1, new Barnetillegg(BARNETILLEGG_SATS, 1)),
            new LocalDateSegment<>(fødselsdato2, dødsdato2, new Barnetillegg(BARNETILLEGG_SATS, 1)),
            new LocalDateSegment<>(fødselsdato3, ENDRET_SATS_DATO.minusDays(1), new Barnetillegg(BARNETILLEGG_SATS, 1)),
            new LocalDateSegment<>(ENDRET_SATS_DATO, TIDENES_ENDE, new Barnetillegg(BARNETILLEGG_SATS_HØY, 1))));

        assertThat(resultat.barnetilleggTidslinje()).isEqualTo(forventetResultat);
    }

    @Test
    void skal_få_barnetillegg_for_flere_barn_samtidig() {
        var fødselsdato1 = LocalDate.parse("2020-01-01");
        var fødselsdato2 = LocalDate.parse("2020-01-15");
        var fødselsdato3 = LocalDate.parse("2020-01-17");

        var fødselOgDødInfos = List.of(
            new FødselOgDødInfo(AktørId.dummy(), fødselsdato1, null),
            new FødselOgDødInfo(AktørId.dummy(), fødselsdato2, null),
            new FødselOgDødInfo(AktørId.dummy(), fødselsdato3, null)
        );

        var resultat = LagBarnetilleggTidslinje.beregnBarnetillegg(fødselOgDødInfos);

        var forventetResultat = new LocalDateTimeline<>(List.of(
            new LocalDateSegment<>(fødselsdato1, fødselsdato2.minusDays(1), new Barnetillegg(BARNETILLEGG_SATS, 1)),
            new LocalDateSegment<>(fødselsdato2, fødselsdato3.minusDays(1), new Barnetillegg(BARNETILLEGG_SATS * 2, 2)),
            new LocalDateSegment<>(fødselsdato3, ENDRET_SATS_DATO.minusDays(1), new Barnetillegg(BARNETILLEGG_SATS * 3, 3)),
            new LocalDateSegment<>(ENDRET_SATS_DATO, TIDENES_ENDE, new Barnetillegg(BARNETILLEGG_SATS_HØY* 3, 3))
));

        assertThat(resultat.barnetilleggTidslinje()).isEqualTo(forventetResultat);
    }


    @Test
    void skal_få_barnetillegg_for_barn_som_er_født_og_dør_samme_dag() {
        var fødselsdato1 = LocalDate.of(2020, 1, 1);
        var dødsdato1 = LocalDate.of(2020, 1, 1);

        var fødselOgDødInfos = List.of(
            new FødselOgDødInfo(AktørId.dummy(), fødselsdato1, dødsdato1)
        );

        var resultat = LagBarnetilleggTidslinje.beregnBarnetillegg(fødselOgDødInfos);

        var forventetResultat = new LocalDateTimeline<>(List.of(
            new LocalDateSegment<>(fødselsdato1, dødsdato1, new Barnetillegg(BARNETILLEGG_SATS, 1))
        ));

        assertThat(resultat.barnetilleggTidslinje()).isEqualTo(forventetResultat);
    }

    @Test
    void skal_få_barnetillegg_for_barn_som_er_født_og_dør_siste_dag_i_måneden() {
        var fødselsdato1 = LocalDate.of(2020, 2, 29);
        var dødsdato1 = LocalDate.of(2020, 2, 29);

        var fødselOgDødInfos = List.of(
            new FødselOgDødInfo(AktørId.dummy(), fødselsdato1, dødsdato1)
        );

        var relevantTidslinje = new LocalDateTimeline<>(
            LocalDate.of(2019, 12, 15),
            LocalDate.of(2020, 3, 15),
            true
        );

        var resultat = LagBarnetilleggTidslinje.beregnBarnetillegg(fødselOgDødInfos);

        var forventetResultat = new LocalDateTimeline<>(List.of(
            new LocalDateSegment<>(fødselsdato1, dødsdato1, new Barnetillegg(BARNETILLEGG_SATS, 1))));

        assertThat(resultat.barnetilleggTidslinje()).isEqualTo(forventetResultat);
    }


    @Test
    void skal_få_endret_barnetillegg_ved_ny_sats() {
        var fødselsdato1 = LocalDate.of(2020, 1, 1);

        var fødselOgDødInfos = List.of(
            new FødselOgDødInfo(AktørId.dummy(), fødselsdato1, null)
        );

        var resultat = LagBarnetilleggTidslinje.beregnBarnetillegg(fødselOgDødInfos);

        var forventetResultat = new LocalDateTimeline<>(List.of(
            new LocalDateSegment<>(fødselsdato1, ENDRET_SATS_DATO.minusDays(1), new Barnetillegg(BARNETILLEGG_SATS, 1)),
            new LocalDateSegment<>(ENDRET_SATS_DATO, TIDENES_ENDE, new Barnetillegg(BARNETILLEGG_SATS_HØY, 1)))
        );

        assertThat(resultat.barnetilleggTidslinje()).isEqualTo(forventetResultat);
    }

}
