package no.nav.k9.sak.ytelse.pleiepengerbarn.beregningsgrunnlag;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.time.Duration;

import org.junit.jupiter.api.Test;

import no.nav.pleiepengerbarn.uttak.kontrakter.ArbeidsforholdPeriodeInfo;

class FinnAktivitetsgradTest {

    public static final BigDecimal HUNDRE = BigDecimal.valueOf(100);
    public static final BigDecimal TJUEFEM = BigDecimal.valueOf(25);

    @Test
    void skal_gi_100_prosent_aktivitetsgrad_for_0_normal_og_0_faktisk() {
        var aktivitetsgrad = FinnAktivitetsgrad.finnAktivitetsgrad(new ArbeidsforholdPeriodeInfo(Duration.ZERO, Duration.ZERO));
        assertThat(aktivitetsgrad.compareTo(HUNDRE)).isEqualTo(0);
    }

    @Test
    void skal_gi_100_prosent_aktivitetsgrad_for_8_normal_og_8_faktisk() {
        var aktivitetsgrad = FinnAktivitetsgrad.finnAktivitetsgrad(new ArbeidsforholdPeriodeInfo(Duration.ofHours(8), Duration.ofHours(8)));
        assertThat(aktivitetsgrad.compareTo(HUNDRE)).isEqualTo(0);
    }

    @Test
    void skal_gi_0_prosent_aktivitetsgrad_for_8_normal_og_0_faktisk() {
        var aktivitetsgrad = FinnAktivitetsgrad.finnAktivitetsgrad(new ArbeidsforholdPeriodeInfo(Duration.ofHours(8), Duration.ZERO));
        assertThat(aktivitetsgrad.compareTo(BigDecimal.ZERO)).isEqualTo(0);
    }

    @Test
    void skal_gi_25_prosent_aktivitetsgrad_for_8_normal_og_2_faktisk() {
        var aktivitetsgrad = FinnAktivitetsgrad.finnAktivitetsgrad(new ArbeidsforholdPeriodeInfo(Duration.ofHours(8), Duration.ofHours(2)));
        assertThat(aktivitetsgrad.compareTo(TJUEFEM)).isEqualTo(0);
    }

    /**
     * Tester for dette siden det ikkje finnes validering på det i kontrakten, men usikker på om det nokon gang vil skje
     */
    @Test
    void skal_gi_0_prosent_aktivitetsgrad_for_0_normal_og_2_faktisk() {
        var aktivitetsgrad = FinnAktivitetsgrad.finnAktivitetsgrad(new ArbeidsforholdPeriodeInfo(Duration.ZERO, Duration.ofHours(2)));
        assertThat(aktivitetsgrad.compareTo(BigDecimal.ZERO)).isEqualTo(0);
    }

}
