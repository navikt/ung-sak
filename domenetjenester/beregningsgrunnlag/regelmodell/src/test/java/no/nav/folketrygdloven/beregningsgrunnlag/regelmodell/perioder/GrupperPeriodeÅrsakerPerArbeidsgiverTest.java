package no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.perioder;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.perioder.GrupperPeriodeÅrsakerPerArbeidsgiver;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.regelmodell.Aktivitet;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.regelmodell.ArbeidsforholdOgInntektsmelding;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.regelmodell.PeriodeÅrsak;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.regelmodell.grunnlag.inntekt.Arbeidsforhold;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.regelmodell.grunnlag.inntekt.Refusjonskrav;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.regelmodell.resultat.PeriodeSplittData;

public class GrupperPeriodeÅrsakerPerArbeidsgiverTest {
    @Test
    public void toArbeidsgivereTreDatoer() {
        // Arrange
        ArbeidsforholdOgInntektsmelding a1 = ArbeidsforholdOgInntektsmelding.builder()
            .medArbeidsforhold(Arbeidsforhold.builder()
                .medAktivitet(Aktivitet.ARBEIDSTAKERINNTEKT)
                .medOrgnr("a1")
                .build())
            .build();
        ArbeidsforholdOgInntektsmelding a2 = ArbeidsforholdOgInntektsmelding.builder()
            .medArbeidsforhold(Arbeidsforhold.builder()
                .medAktivitet(Aktivitet.ARBEIDSTAKERINNTEKT)
                .medOrgnr("a2")
                .build())
            .build();
        LocalDate january = LocalDate.of(2019, Month.JANUARY, 15);
        LocalDate february = LocalDate.of(2019, Month.FEBRUARY, 15);
        LocalDate march = LocalDate.of(2019, Month.MARCH, 15);

        Map<LocalDate, Set<PeriodeSplittData>> periodeMap = lagPeriodeMap(a1, a2, january, february, march);

        // Act
        Map<ArbeidsforholdOgInntektsmelding, List<Refusjonskrav>> resultatMap = GrupperPeriodeÅrsakerPerArbeidsgiver.grupper(periodeMap);

        // Assert
        assertThat(resultatMap).hasSize(2);
        List<Refusjonskrav> refusjon1 = resultatMap.get(a1);
        assertThat(refusjon1).hasSize(2);
        assertThat(refusjon1.get(0).getMånedsbeløp()).isEqualByComparingTo(BigDecimal.valueOf(40000));
        assertThat(refusjon1.get(0).getFom()).isEqualTo(january);
        assertThat(refusjon1.get(1).getMånedsbeløp()).isEqualByComparingTo(BigDecimal.valueOf(30000));
        assertThat(refusjon1.get(1).getFom()).isEqualTo(february);
        List<Refusjonskrav> refusjon2 = resultatMap.get(a2);
        assertThat(refusjon2).hasSize(2);
        assertThat(refusjon2.get(0).getMånedsbeløp()).isEqualByComparingTo(BigDecimal.valueOf(10000));
        assertThat(refusjon2.get(0).getFom()).isEqualTo(february);
        assertThat(refusjon2.get(1).getMånedsbeløp()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(refusjon2.get(1).getFom()).isEqualTo(march);
    }

    private Map<LocalDate, Set<PeriodeSplittData>> lagPeriodeMap(ArbeidsforholdOgInntektsmelding a1,
                                                                 ArbeidsforholdOgInntektsmelding a2,
                                                                 LocalDate january,
                                                                 LocalDate february,
                                                                 LocalDate march) {
        Map<LocalDate, Set<PeriodeSplittData>> periodeMap = new HashMap<>();
        PeriodeSplittData psd1 = PeriodeSplittData.builder()
            .medInntektsmelding(a1)
            .medPeriodeÅrsak(PeriodeÅrsak.ENDRING_I_REFUSJONSKRAV)
            .medRefusjonskravPrMåned(BigDecimal.valueOf(40000))
            .medFom(january)
            .build();
        PeriodeSplittData psd2 = PeriodeSplittData.builder()
            .medInntektsmelding(a1)
            .medPeriodeÅrsak(PeriodeÅrsak.ENDRING_I_REFUSJONSKRAV)
            .medRefusjonskravPrMåned(BigDecimal.valueOf(30000))
            .medFom(february)
            .build();
        PeriodeSplittData psd3 = PeriodeSplittData.builder()
            .medInntektsmelding(a2)
            .medPeriodeÅrsak(PeriodeÅrsak.ENDRING_I_REFUSJONSKRAV)
            .medRefusjonskravPrMåned(BigDecimal.valueOf(10000))
            .medFom(february)
            .build();
        PeriodeSplittData psd4 = PeriodeSplittData.builder()
            .medInntektsmelding(a2)
            .medPeriodeÅrsak(PeriodeÅrsak.REFUSJON_OPPHØRER)
            .medRefusjonskravPrMåned(BigDecimal.ZERO)
            .medFom(march)
            .build();
        periodeMap.put(january, Set.of(psd1));
        periodeMap.put(february, Set.of(psd2, psd3));
        periodeMap.put(march, Set.of(psd4));
        return periodeMap;
    }
}
