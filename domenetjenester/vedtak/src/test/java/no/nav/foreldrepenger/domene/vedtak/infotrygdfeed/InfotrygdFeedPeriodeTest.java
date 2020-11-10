package no.nav.foreldrepenger.domene.vedtak.infotrygdfeed;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

public class InfotrygdFeedPeriodeTest {

    @Test
    public void fomTom() {
        LocalDate fom = LocalDate.of(2020, 1, 1);
        LocalDate tom = fom.plusMonths(1);
        InfotrygdFeedPeriode periode = new InfotrygdFeedPeriode(fom, tom);
        assertThat(periode.getFom()).isEqualTo(fom);
        assertThat(periode.getTom()).isEqualTo(tom);
    }

    @Test
    public void valider_lik_dato() {
        LocalDate dato = LocalDate.of(2020, 1, 1);
        new InfotrygdFeedPeriode(dato, dato);
    }

    @Test
    public void valider_annullert() {
        InfotrygdFeedPeriode.annullert();
        new InfotrygdFeedPeriode(null, null);
    }

    @Test
    public void valider_ubegrenset_tom() {
        new InfotrygdFeedPeriode(LocalDate.of(2020, 1, 1), null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void valider_fom_etter_tom() {
        LocalDate før = LocalDate.of(2020, 1, 1);
        LocalDate etter = før.plusDays(1);
        new InfotrygdFeedPeriode(etter, før);
    }

    @Test(expected = IllegalArgumentException.class)
    public void valider_ubegrenset_fom_med_tom() {
        new InfotrygdFeedPeriode(null, LocalDate.of(2020, 1, 1));
    }
}
