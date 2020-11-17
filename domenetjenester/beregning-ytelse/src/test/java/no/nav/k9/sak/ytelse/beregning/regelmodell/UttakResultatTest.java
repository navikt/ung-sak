package no.nav.k9.sak.ytelse.beregning.regelmodell;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.uttak.UttakArbeidType;
import no.nav.k9.sak.ytelse.beregning.regelmodell.beregningsgrunnlag.Arbeidsforhold;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class UttakResultatTest {

    @Test
    public void skal_ikke_håndtere_overlapp_med_forskjellig_arbeidsforhold() {

        Assertions.assertThrows(IllegalArgumentException.class, () ->{
            var periode1 = new UttakResultatPeriode(LocalDate.now(), LocalDate.now().plusDays(2), List.of(new UttakAktivitet(BigDecimal.TEN, BigDecimal.TEN, Arbeidsforhold.nyttArbeidsforholdHosVirksomhet("000000000"), UttakArbeidType.ARBEIDSTAKER, false)), true);
            var periode2 = new UttakResultatPeriode(LocalDate.now(), LocalDate.now().plusDays(2), List.of(new UttakAktivitet(BigDecimal.TEN, BigDecimal.TEN, Arbeidsforhold.nyttArbeidsforholdHosVirksomhet("000000001"), UttakArbeidType.ARBEIDSTAKER, false)), true);

            var uttakResultat = new UttakResultat(FagsakYtelseType.OMSORGSPENGER, List.of(periode1, periode2));

            uttakResultat.getUttakPeriodeTimeline();

        });
    }

    @Test
    public void skal_håndtere_overlapp_med_forskjellig_arbeidsforhold() {
        var periode1 = new UttakResultatPeriode(LocalDate.now(), LocalDate.now().plusDays(2), List.of(new UttakAktivitet(BigDecimal.TEN, BigDecimal.TEN, Arbeidsforhold.nyttArbeidsforholdHosVirksomhet("000000000"), UttakArbeidType.ARBEIDSTAKER, false)), true);
        var periode2 = new UttakResultatPeriode(LocalDate.now(), LocalDate.now().plusDays(2), List.of(new UttakAktivitet(BigDecimal.TEN, BigDecimal.TEN, Arbeidsforhold.nyttArbeidsforholdHosVirksomhet("000000001"), UttakArbeidType.ARBEIDSTAKER, false)), true);

        var uttakResultat = new UttakResultat(FagsakYtelseType.OMSORGSPENGER, List.of(periode1, periode2));

        var uttakPeriodeTimelineMedOverlapp = uttakResultat.getUttakPeriodeTimelineMedOverlapp();

        assertThat(uttakPeriodeTimelineMedOverlapp).isNotNull();
    }
}
