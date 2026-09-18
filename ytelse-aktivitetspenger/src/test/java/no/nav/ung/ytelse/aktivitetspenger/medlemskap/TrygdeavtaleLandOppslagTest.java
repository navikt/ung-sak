package no.nav.ung.ytelse.aktivitetspenger.medlemskap;

import no.nav.ung.kodeverk.geografisk.Landkoder;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TrygdeavtaleLandOppslagTest {

    @Test
    void norge_er_gyldig_etter_eøs_start() {
        assertThat(TrygdeavtaleLandOppslag.erGyldigTrygdeavtaleLand(
            Landkoder.fraKode("NOR"), LocalDate.of(2020, 1, 1))).isTrue();
    }

    @Test
    void tyskland_er_gyldig_fra_1994() {
        assertThat(TrygdeavtaleLandOppslag.erGyldigTrygdeavtaleLand(
            Landkoder.fraKode("DEU"), LocalDate.of(1994, 1, 1))).isTrue();
    }

    @Test
    void kroatia_er_ugyldig_før_2013() {
        assertThat(TrygdeavtaleLandOppslag.erGyldigTrygdeavtaleLand(
            Landkoder.fraKode("HRV"), LocalDate.of(2012, 1, 1))).isFalse();
    }

    @Test
    void kroatia_er_gyldig_fra_juli_2013() {
        assertThat(TrygdeavtaleLandOppslag.erGyldigTrygdeavtaleLand(
            Landkoder.fraKode("HRV"), LocalDate.of(2013, 7, 1))).isTrue();
    }

    @Test
    void sveits_er_ugyldig_før_2002() {
        assertThat(TrygdeavtaleLandOppslag.erGyldigTrygdeavtaleLand(
            Landkoder.fraKode("CHE"), LocalDate.of(2001, 1, 1))).isFalse();
    }

    @Test
    void sveits_er_gyldig_fra_juni_2002() {
        assertThat(TrygdeavtaleLandOppslag.erGyldigTrygdeavtaleLand(
            Landkoder.fraKode("CHE"), LocalDate.of(2002, 6, 1))).isTrue();
    }

    @Test
    void storbritannia_er_gyldig_etter_brexit() {
        assertThat(TrygdeavtaleLandOppslag.erGyldigTrygdeavtaleLand(
            Landkoder.fraKode("GBR"), LocalDate.of(2021, 1, 1))).isTrue();
    }

    @Test
    void storbritannia_er_gyldig_før_brexit() {
        assertThat(TrygdeavtaleLandOppslag.erGyldigTrygdeavtaleLand(
            Landkoder.fraKode("GBR"), LocalDate.of(2019, 1, 1))).isTrue();
    }

    @Test
    void polen_er_ugyldig_før_2004() {
        assertThat(TrygdeavtaleLandOppslag.erGyldigTrygdeavtaleLand(
            Landkoder.fraKode("POL"), LocalDate.of(2003, 1, 1))).isFalse();
    }

    @Test
    void polen_er_gyldig_fra_mai_2004() {
        assertThat(TrygdeavtaleLandOppslag.erGyldigTrygdeavtaleLand(
            Landkoder.fraKode("POL"), LocalDate.of(2004, 5, 1))).isTrue();
    }

    @Test
    void romania_er_gyldig_fra_2007() {
        assertThat(TrygdeavtaleLandOppslag.erGyldigTrygdeavtaleLand(
            Landkoder.fraKode("ROU"), LocalDate.of(2007, 1, 1))).isTrue();
    }

    @Test
    void romania_er_ugyldig_før_2007() {
        assertThat(TrygdeavtaleLandOppslag.erGyldigTrygdeavtaleLand(
            Landkoder.fraKode("ROU"), LocalDate.of(2006, 1, 1))).isFalse();
    }

    @Test
    void usa_er_ikke_gyldig_trygdeavtaleland() {
        assertThat(TrygdeavtaleLandOppslag.erGyldigTrygdeavtaleLand(
            Landkoder.fraKode("USA"), LocalDate.of(2020, 1, 1))).isFalse();
    }

    @Test
    void canada_er_ikke_gyldig_trygdeavtaleland() {
        assertThat(TrygdeavtaleLandOppslag.erGyldigTrygdeavtaleLand(
            Landkoder.fraKode("CAN"), LocalDate.of(2020, 1, 1))).isFalse();
    }

    @Test
    void null_landkode_kaster_exception() {
        assertThatThrownBy(() -> TrygdeavtaleLandOppslag.erGyldigTrygdeavtaleLand(
            null, LocalDate.of(2020, 1, 1)))
            .isInstanceOf(NullPointerException.class);
    }

    @Test
    void null_fom_kaster_exception() {
        assertThatThrownBy(() -> TrygdeavtaleLandOppslag.erGyldigTrygdeavtaleLand(
            Landkoder.fraKode("NOR"), null))
            .isInstanceOf(NullPointerException.class);
    }
}
