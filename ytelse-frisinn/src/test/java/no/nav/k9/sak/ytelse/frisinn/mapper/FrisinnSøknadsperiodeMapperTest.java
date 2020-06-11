package no.nav.k9.sak.ytelse.frisinn.mapper;

import no.nav.k9.kodeverk.uttak.UttakArbeidType;
import no.nav.k9.sak.domene.uttak.repo.UttakAktivitet;
import no.nav.k9.sak.domene.uttak.repo.UttakAktivitetPeriode;
import no.nav.k9.sak.typer.Periode;
import org.junit.Test;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

public class FrisinnSøknadsperiodeMapperTest {

    @Test
    public void skal_mappe_en_enkelt_periode() {
        // Arrange
        UttakAktivitetPeriode periode = lagPeriode(LocalDate.of(2020, 3, 30), LocalDate.of(2020, 4, 30));

        // Act
        List<Periode> resultat = map(periode);

        // Assert
        assertThat(resultat).hasSize(1);
        assertThat(resultat.get(0)).isEqualTo(new Periode(LocalDate.of(2020, 3, 30), LocalDate.of(2020, 4, 30)));
    }

    @Test
    public void skal_mappe_to_perioder_ingen_overlapp() {
        // Arrange
        UttakAktivitetPeriode periode1 = lagPeriode(LocalDate.of(2020, 3, 30), LocalDate.of(2020, 4, 30));
        UttakAktivitetPeriode periode2 = lagPeriode(LocalDate.of(2020, 5, 1), LocalDate.of(2020, 5, 31));

        // Act
        List<Periode> resultat = map(periode1, periode2);

        // Assert
        assertThat(resultat).hasSize(2);
        assertThat(resultat.get(0)).isEqualTo(new Periode(LocalDate.of(2020, 3, 30), LocalDate.of(2020, 4, 30)));
        assertThat(resultat.get(1)).isEqualTo(new Periode(LocalDate.of(2020, 5, 1), LocalDate.of(2020, 5, 31)));
    }

    @Test
    public void skal_mappe_to_perioder_med_overlapp() {
        // Arrange
        UttakAktivitetPeriode periode1 = lagPeriode(LocalDate.of(2020, 3, 30), LocalDate.of(2020, 4, 30));
        UttakAktivitetPeriode periode2 = lagPeriode(LocalDate.of(2020, 4, 1), LocalDate.of(2020, 4, 30));

        // Act
        List<Periode> resultat = map(periode1, periode2);

        // Assert
        assertThat(resultat).hasSize(1);
        assertThat(resultat.get(0)).isEqualTo(new Periode(LocalDate.of(2020, 3, 30), LocalDate.of(2020, 4, 30)));
    }

    @Test
    public void skal_mappe_tre_perioder_to_med_overlapp_en_uten() {
        // Arrange
        UttakAktivitetPeriode periode1 = lagPeriode(LocalDate.of(2020, 4, 12), LocalDate.of(2020, 4, 30));
        UttakAktivitetPeriode periode2 = lagPeriode(LocalDate.of(2020, 4, 18), LocalDate.of(2020, 4, 30));
        UttakAktivitetPeriode periode3 = lagPeriode(LocalDate.of(2020, 5, 1), LocalDate.of(2020, 5, 31));

        // Act
        List<Periode> resultat = map(periode1, periode2, periode3);

        // Assert
        assertThat(resultat).hasSize(2);
        assertThat(resultat.get(0)).isEqualTo(new Periode(LocalDate.of(2020, 4, 12), LocalDate.of(2020, 4, 30)));
        assertThat(resultat.get(1)).isEqualTo(new Periode(LocalDate.of(2020, 5, 1), LocalDate.of(2020, 5, 31)));
    }

    private List<Periode> map(UttakAktivitetPeriode...perioder) {
        List<UttakAktivitetPeriode> uttakPerioder = Arrays.asList(perioder);
        UttakAktivitet uttakAktivitet = new UttakAktivitet(uttakPerioder);
        return FrisinnSøknadsperiodeMapper.map(uttakAktivitet).stream()
            .sorted(Comparator.comparing(Periode::getFom))
            .collect(Collectors.toList());
    }

    private UttakAktivitetPeriode lagPeriode(LocalDate fom, LocalDate tom) {
        return new UttakAktivitetPeriode(UttakArbeidType.FRILANSER, fom ,tom);
    }


}
