package no.nav.k9.sak.behandlingslager.behandling.vilkår;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.DayOfWeek;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;

public class PåTversAvHelgErKantIKantVurdererTest {

    private KantIKantVurderer identifiserer = new PåTversAvHelgErKantIKantVurderer();

    @Test
    public void skal_koble_sammen_førsteDag_og_påfølgende_andreDag() {
        var førsteDag = LocalDate.of(2020, 5, 8);
        assertThat(førsteDag.getDayOfWeek()).isEqualTo(DayOfWeek.FRIDAY);
        var andreDag = LocalDate.of(2020, 5, 11);
        assertThat(andreDag.getDayOfWeek()).isEqualTo(DayOfWeek.MONDAY);

        var førsteDagPeriode = DatoIntervallEntitet.fraOgMedTilOgMed(førsteDag.minusDays(2), førsteDag);
        var andreDagPeriode = DatoIntervallEntitet.fraOgMedTilOgMed(andreDag, andreDag.plusDays(2));

        assertThat(identifiserer.erKantIKant(førsteDagPeriode, andreDagPeriode)).isTrue();
    }

    @Test
    public void skal_koble_sammen_lørdag_og_påfølgende_andreDag() {
        var førsteDag = LocalDate.of(2020, 5, 9);
        assertThat(førsteDag.getDayOfWeek()).isEqualTo(DayOfWeek.SATURDAY);
        var andreDag = LocalDate.of(2020, 5, 11);
        assertThat(andreDag.getDayOfWeek()).isEqualTo(DayOfWeek.MONDAY);

        var førsteDagPeriode = DatoIntervallEntitet.fraOgMedTilOgMed(førsteDag.minusDays(2), førsteDag);
        var andreDagPeriode = DatoIntervallEntitet.fraOgMedTilOgMed(andreDag, andreDag.plusDays(2));

        assertThat(identifiserer.erKantIKant(førsteDagPeriode, andreDagPeriode)).isTrue();
    }

    @Test
    public void skal_koble_sammen_søndag_og_påfølgende_andreDag() {
        var førsteDag = LocalDate.of(2020, 5, 10);
        assertThat(førsteDag.getDayOfWeek()).isEqualTo(DayOfWeek.SUNDAY);
        var andreDag = LocalDate.of(2020, 5, 11);
        assertThat(andreDag.getDayOfWeek()).isEqualTo(DayOfWeek.MONDAY);

        var førsteDagPeriode = DatoIntervallEntitet.fraOgMedTilOgMed(førsteDag.minusDays(2), førsteDag);
        var andreDagPeriode = DatoIntervallEntitet.fraOgMedTilOgMed(andreDag, andreDag.plusDays(2));

        assertThat(identifiserer.erKantIKant(førsteDagPeriode, andreDagPeriode)).isTrue();
    }

    @Test
    public void skal_koble_sammen_førsteDag_og_påfølgende_søndag() {
        var førsteDag = LocalDate.of(2020, 5, 8);
        assertThat(førsteDag.getDayOfWeek()).isEqualTo(DayOfWeek.FRIDAY);
        var andreDag = LocalDate.of(2020, 5, 10);
        assertThat(andreDag.getDayOfWeek()).isEqualTo(DayOfWeek.SUNDAY);

        var førsteDagPeriode = DatoIntervallEntitet.fraOgMedTilOgMed(førsteDag.minusDays(2), førsteDag);
        var andreDagPeriode = DatoIntervallEntitet.fraOgMedTilOgMed(andreDag, andreDag.plusDays(2));

        assertThat(identifiserer.erKantIKant(førsteDagPeriode, andreDagPeriode)).isTrue();
    }

    @Test
    public void skal_koble_sammen_førsteDag_og_påfølgende_lørdag() {
        var førsteDag = LocalDate.of(2020, 5, 8);
        assertThat(førsteDag.getDayOfWeek()).isEqualTo(DayOfWeek.FRIDAY);
        var andreDag = LocalDate.of(2020, 5, 9);
        assertThat(andreDag.getDayOfWeek()).isEqualTo(DayOfWeek.SATURDAY);

        var førsteDagPeriode = DatoIntervallEntitet.fraOgMedTilOgMed(førsteDag.minusDays(2), førsteDag);
        var andreDagPeriode = DatoIntervallEntitet.fraOgMedTilOgMed(andreDag, andreDag.plusDays(2));

        assertThat(identifiserer.erKantIKant(førsteDagPeriode, andreDagPeriode)).isTrue();
    }

    @Test
    public void skal_ikke_koble_sammen_tirsdag_og_påfølgende_torsdag() {
        var førsteDag = LocalDate.of(2020, 5, 5);
        assertThat(førsteDag.getDayOfWeek()).isEqualTo(DayOfWeek.TUESDAY);
        var andreDag = LocalDate.of(2020, 5, 7);
        assertThat(andreDag.getDayOfWeek()).isEqualTo(DayOfWeek.THURSDAY);

        var førsteDagPeriode = DatoIntervallEntitet.fraOgMedTilOgMed(førsteDag.minusDays(2), førsteDag);
        var andreDagPeriode = DatoIntervallEntitet.fraOgMedTilOgMed(andreDag, andreDag.plusDays(2));

        assertThat(identifiserer.erKantIKant(førsteDagPeriode, andreDagPeriode)).isFalse();
    }


    @Test
    public void skal_ikke_koble_sammen_tirsdag_og_påfølgende_onsdag() {
        var førsteDag = LocalDate.of(2020, 5, 5);
        assertThat(førsteDag.getDayOfWeek()).isEqualTo(DayOfWeek.TUESDAY);
        var andreDag = LocalDate.of(2020, 5, 6);
        assertThat(andreDag.getDayOfWeek()).isEqualTo(DayOfWeek.WEDNESDAY);

        var førsteDagPeriode = DatoIntervallEntitet.fraOgMedTilOgMed(førsteDag.minusDays(2), førsteDag);
        var andreDagPeriode = DatoIntervallEntitet.fraOgMedTilOgMed(andreDag, andreDag.plusDays(2));

        assertThat(identifiserer.erKantIKant(førsteDagPeriode, andreDagPeriode)).isTrue();
    }

    @Test
    public void skal_ikke_koble_sammen_torsdag_og_påfølgende_lørdag() {
        var førsteDag = LocalDate.of(2020, 5, 7);
        assertThat(førsteDag.getDayOfWeek()).isEqualTo(DayOfWeek.THURSDAY);
        var andreDag = LocalDate.of(2020, 5, 9);
        assertThat(andreDag.getDayOfWeek()).isEqualTo(DayOfWeek.SATURDAY);

        var førsteDagPeriode = DatoIntervallEntitet.fraOgMedTilOgMed(førsteDag.minusDays(2), førsteDag);
        var andreDagPeriode = DatoIntervallEntitet.fraOgMedTilOgMed(andreDag, andreDag.plusDays(2));

        assertThat(identifiserer.erKantIKant(førsteDagPeriode, andreDagPeriode)).isFalse();
    }

    @Test
    public void skal_ikke_koble_sammen_torsdag_og_påfølgende_søndag() {
        var førsteDag = LocalDate.of(2020, 5, 7);
        assertThat(førsteDag.getDayOfWeek()).isEqualTo(DayOfWeek.THURSDAY);
        var andreDag = LocalDate.of(2020, 5, 10);
        assertThat(andreDag.getDayOfWeek()).isEqualTo(DayOfWeek.SUNDAY);

        var førsteDagPeriode = DatoIntervallEntitet.fraOgMedTilOgMed(førsteDag.minusDays(2), førsteDag);
        var andreDagPeriode = DatoIntervallEntitet.fraOgMedTilOgMed(andreDag, andreDag.plusDays(2));

        assertThat(identifiserer.erKantIKant(førsteDagPeriode, andreDagPeriode)).isFalse();
    }
}
