package no.nav.k9.sak.domene.typer.tid;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.Test;

import com.vladmihalcea.hibernate.type.range.Range;

public class DatoIntervallEntitetTest {

    @Test
    public void roundtrip_test() {
        LocalDate yearFom = LocalDate.now().withDayOfYear(1);
        LocalDate yearTom = LocalDate.now().withMonth(12).withDayOfMonth(31);

        var entitet = DatoIntervallEntitet.fraOgMedTilOgMed(yearFom, yearTom);
        var range = entitet.toRange();
        var roundTrip = DatoIntervallEntitet.fra(range);

        assertThat(entitet).isEqualTo(roundTrip);
    }

    @Test
    public void skal_ta_hensyn_til_inclusive_exclusive() {
        LocalDate yearFom = LocalDate.now().withDayOfYear(1);
        LocalDate yearTom = LocalDate.now().withMonth(12).withDayOfMonth(31);
        var entitet = DatoIntervallEntitet.fraOgMedTilOgMed(yearFom, yearTom);
        var entitet1 = DatoIntervallEntitet.fraOgMedTilOgMed(yearFom, yearTom.minusDays(1));
        var range = Range.closedOpen(yearFom, yearTom);
        var rangeEntiet = DatoIntervallEntitet.fra(range);

        assertThat(entitet).isNotEqualTo(rangeEntiet);
        assertThat(entitet1).isEqualTo(rangeEntiet);
    }
}
