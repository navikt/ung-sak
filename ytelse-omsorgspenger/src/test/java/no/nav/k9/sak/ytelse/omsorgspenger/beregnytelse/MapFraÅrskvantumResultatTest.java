package no.nav.k9.sak.ytelse.omsorgspenger.beregnytelse;

import no.nav.k9.kodeverk.uttak.UttakArbeidType;
import no.nav.k9.sak.kontrakt.uttak.*;
import no.nav.k9.sak.ytelse.beregning.regelmodell.UttakResultatPeriode;
import no.nav.k9.sak.ytelse.omsorgspenger.årskvantum.api.ÅrskvantumResultat;
import org.junit.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class MapFraÅrskvantumResultatTest {

    private static final BigDecimal _100 = BigDecimal.valueOf(100L);

    @Test
    public void map_fra_årskvantum_resultat() throws Exception {

        var år = new ÅrskvantumResultat();
        år.setUttaksperioder(lagUttaksperioder());

        List<UttakResultatPeriode> perioder = new MapFraÅrskvantumResultat().mapFra(år);
        assertThat(perioder).hasSize(4);

        assertThat(perioder.stream().filter(p -> p.getErOppholdsPeriode())).hasSize(2);
        assertThat(perioder.stream().filter(p -> !p.getErOppholdsPeriode())).hasSize(2);
    }

    private List<UttaksperiodeOmsorgspenger> lagUttaksperioder() {
        var fom = LocalDate.now();
        var tom = fom.plusDays(3);
        var arbeidsforhold1 = new UttakArbeidsforhold("921484240", null, UttakArbeidType.ARBEIDSTAKER, null);
        var arbeidsforhold2 = new UttakArbeidsforhold("90589477", null, UttakArbeidType.ARBEIDSTAKER, null);
        var arbeidsforhold3 = new UttakArbeidsforhold("980484939", null, UttakArbeidType.ARBEIDSTAKER, null);
        return List.of(
            innvilget(fom, tom, _100, arbeidsforhold1),
            avslått(fom, tom, arbeidsforhold2),
            avslått(tom.plusDays(1), tom.plusDays(10), arbeidsforhold1),
            innvilget(tom.plusDays(1), tom.plusDays(10), _100, arbeidsforhold3));
    }

    private UttaksperiodeOmsorgspenger innvilget(LocalDate fom, LocalDate tom, BigDecimal utbetalingsgrad, UttakArbeidsforhold arbeidsforhold) {
        return new UttaksperiodeOmsorgspenger(new Periode(fom, tom), new UttakUtbetalingsgradOmsorgspenger(utbetalingsgrad),
                                              OmsorgspengerUtfall.INNVILGET, Duration.ofHours(1), arbeidsforhold);
    }

    private UttaksperiodeOmsorgspenger avslått(LocalDate fom, LocalDate tom, UttakArbeidsforhold arbeidsforhold) {
        return new UttaksperiodeOmsorgspenger(new Periode(fom, tom), new UttakUtbetalingsgradOmsorgspenger(BigDecimal.ZERO),
                                              OmsorgspengerUtfall.AVSLÅTT, Duration.ofHours(1), arbeidsforhold);
    }
}
