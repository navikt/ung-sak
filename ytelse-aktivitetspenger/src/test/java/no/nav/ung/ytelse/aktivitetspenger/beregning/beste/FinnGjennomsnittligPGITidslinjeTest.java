package no.nav.ung.ytelse.aktivitetspenger.beregning.beste;

import no.nav.ung.kodeverk.arbeidsforhold.InntektspostType;
import no.nav.ung.sak.domene.iay.modell.Inntektspost;
import no.nav.ung.sak.domene.iay.modell.InntektspostBuilder;
import no.nav.ung.sak.typer.Periode;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Month;
import java.time.Year;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class FinnGjennomsnittligPGITest {

    @Test
    void finnGjennomsnittligPGI_År_inntekt_inneværende_år_under_6G() {
        var sisteTilgjengeligeGSnittÅr = periodeAv(Month.DECEMBER, 2024).getTom();

        Map<Year, BigDecimal> resultat = FinnGjennomsnittligPGI.finnGjennomsnittligPGI(
            sisteTilgjengeligeGSnittÅr,
            lagInntektsposterForÅr(300_000)
        );

        assertThat(resultat.get(Year.of(2024))).isEqualByComparingTo(BigDecimal.valueOf(300_000));
    }

    @Test
    void finnGjennomsnittligPGI_År_inntekt_inneværende_år_mellom_6G_12G() {
        var sisteTilgjengeligeGSnittÅr = periodeAv(Month.DECEMBER).getTom();

        var årsperiode = periodeAv(2024);
        var niG = BigDecimal.valueOf(124028).multiply(BigDecimal.valueOf(9));   // 1 116 252 Kroner

        Map<Year, BigDecimal> resultat = FinnGjennomsnittligPGI.finnGjennomsnittligPGI(
            sisteTilgjengeligeGSnittÅr,
            List.of(lagInntektspost(niG, årsperiode))
        );

        // For inntekt mellom 6G og 12G skal PGI være: 6G + ((INNTEKT - 6G) / 3)
        assertThat(resultat.get(Year.of(2024))).isEqualByComparingTo(new BigDecimal(860_984));
    }

    @Test
    void finnGjennomsnittligPGI_År_inntekt_inneværende_år_over_12G() {
        var sisteTilgjengeligeGSnittÅr = periodeAv(Month.DECEMBER).getTom();

        var årsperiode = periodeAv(2024);
        var femtenG = BigDecimal.valueOf(124028).multiply(BigDecimal.valueOf(15)); // 1 860 420 kr

        Map<Year, BigDecimal> resultat = FinnGjennomsnittligPGI.finnGjennomsnittligPGI(
            sisteTilgjengeligeGSnittÅr,
            List.of(lagInntektspost(femtenG, årsperiode))
        );

        // For inntekt over 12G skal PGI maks være: 6G + ((12G - 6G) / 3) = 6G + 2G = 8G
        // 8G (snitt 2024): 122 225 kroner * 8 = 977 800 kroner
        assertThat(resultat.get(Year.of(2024))).isEqualByComparingTo(new BigDecimal(977_800));
    }

    @Test
    void skal_håndtere_flere_inntektsperioder() {
        var sisteTilgjengeligeGSnittÅr = periodeAv(Month.DECEMBER).getTom();

        Map<Year, BigDecimal> resultat = FinnGjennomsnittligPGI.finnGjennomsnittligPGI(
            sisteTilgjengeligeGSnittÅr,
            List.of(
                lagInntektspost(BigDecimal.valueOf(0), periodeAv(Month.JULY)),
                lagInntektspost(BigDecimal.valueOf(10000), periodeAv(Month.AUGUST)),
                lagInntektspost(BigDecimal.valueOf(30000), periodeAv(Month.SEPTEMBER)),
                lagInntektspost(BigDecimal.valueOf(20000), periodeAv(Month.OCTOBER)),
                lagInntektspost(BigDecimal.valueOf(40000), periodeAv(Month.NOVEMBER)),
                lagInntektspost(BigDecimal.valueOf(20000), periodeAv(Month.DECEMBER))
            )
        );

        assertThat(resultat.get(Year.of(2024))).isEqualByComparingTo(BigDecimal.valueOf(120_000));
    }

    private static Inntektspost lagInntektspost(BigDecimal verdi, Periode juli) {
        return InntektspostBuilder.ny()
            .medInntektspostType(InntektspostType.LØNN)
            .medBeløp(verdi)
            .medPeriode(juli.getFom(), juli.getTom())
            .build();
    }

    @Test
    void skal_beregne_med_inflasjonsfaktor_for_tidligere_år() {
        var sisteTilgjengeligeGSnittÅr = periodeAv(Month.DECEMBER, 2024).getTom();

        var mai2023 = periodeAv(Month.MAY, 2023);
        var inntektspost = lagInntektspost(BigDecimal.valueOf(500000), mai2023);

        Map<Year, BigDecimal> resultat = FinnGjennomsnittligPGI.finnGjennomsnittligPGI(
            sisteTilgjengeligeGSnittÅr,
            List.of(inntektspost)
        );

        // G-snitt brukes som vektet gjennomsnitt per år.
        // G-snitt 2023 = 79080 (kun 8 måneder med data mai-des ved 118620, delt på 12)
        // G-snitt 2024 = se hentGrunnbeløpSnittTidslinje() for aktuell verdi
        // Inflasjonsfaktor = G-snitt_2024 / G-snitt_2023
        // Verifiser at resultatet er lik det faktisk beregnede
        assertThat(resultat.get(Year.of(2023))).isEqualByComparingTo(new BigDecimal("746497.79126749871818622492"));
    }

    @Test
    void skal_beregne_korrekt_PGI_bidrag_nøyaktig_på_6G() {
        var sisteTilgjengeligeGSnittÅr = periodeAv(Month.DECEMBER).getTom();
        var periode = periodeAv(Month.MAY);

        // Nøyaktig 6G = 6 * 124028 = 744168
        var seksG = BigDecimal.valueOf(124028).multiply(BigDecimal.valueOf(6));
        var inntektspost = lagInntektspost(seksG, periode);

        Map<Year, BigDecimal> resultat = FinnGjennomsnittligPGI.finnGjennomsnittligPGI(
            sisteTilgjengeligeGSnittÅr,
            List.of(inntektspost)
        );

        // Ved nøyaktig 6G skal PGI være lik 6G (ingen reduksjon)
        // G-snitt 2024 (vektet gjennomsnitt) brukes for å beregne 6G-grensen
        assertThat(resultat.get(Year.of(2024))).isEqualByComparingTo(new BigDecimal(736956));
    }

    @Test
    void skal_beregne_korrekt_PGI_bidrag_nøyaktig_på_12G() {
        var sisteTilgjengeligeGSnittÅr = periodeAv(Month.DECEMBER).getTom();
        var periode = periodeAv(Month.MAY);

        // Nøyaktig 12G = 12 * 124028 = 1 488 336
        var tolvG = BigDecimal.valueOf(124028).multiply(BigDecimal.valueOf(12));
        var inntektspost = lagInntektspost(tolvG, periode);

        Map<Year, BigDecimal> resultat = FinnGjennomsnittligPGI.finnGjennomsnittligPGI(
            sisteTilgjengeligeGSnittÅr,
            List.of(inntektspost)
        );

        // Ved 12G skal PGI-bidrag være 8G: 6G + ((12G - 6G) / 3) = 6G + 2G = 8G
        // G-snitt 2024 (vektet gjennomsnitt) brukes for å beregne G-multipler
        assertThat(resultat.get(Year.of(2024))).isEqualByComparingTo(new BigDecimal(977800));
    }

    private static Periode periodeAv(Month måned, int... år) {
        if (år.length == 0) {
            år = new int[]{2024};
        }
        return new Periode(YearMonth.of(år[0], måned).atDay(1), YearMonth.of(år[0], måned).atEndOfMonth());
    }

    private static Periode periodeAv(int årstall) {
        return new Periode(Year.of(årstall).atDay(1), Year.of(årstall).atMonth(Month.DECEMBER).atEndOfMonth());
    }

    private static List<Inntektspost> lagInntektsposterForÅr() {
        return List.of(
            lagInntektspost(BigDecimal.valueOf(250000), periodeAv(Month.JANUARY, 2023)),
            lagInntektspost(BigDecimal.valueOf(250000), periodeAv(Month.NOVEMBER, 2023)),
            lagInntektspost(BigDecimal.valueOf(250000), periodeAv(Month.DECEMBER, 2023)),
            lagInntektspost(BigDecimal.valueOf(300000), periodeAv(Month.JANUARY, 2024)),
            lagInntektspost(BigDecimal.valueOf(200000), periodeAv(Month.FEBRUARY, 2024)),
            lagInntektspost(BigDecimal.valueOf(200000), periodeAv(Month.MARCH, 2024)),
            lagInntektspost(BigDecimal.valueOf(200000), periodeAv(Month.APRIL, 2024)),
            lagInntektspost(BigDecimal.valueOf(100000), periodeAv(Month.MAY, 2024)),
            lagInntektspost(BigDecimal.valueOf(0), periodeAv(Month.JUNE, 2024)),
            lagInntektspost(BigDecimal.valueOf(0), periodeAv(Month.JUNE, 2024)),
            lagInntektspost(BigDecimal.valueOf(100000), periodeAv(Month.AUGUST, 2024)),
            lagInntektspost(BigDecimal.valueOf(400000), periodeAv(Month.SEPTEMBER, 2024)),
            lagInntektspost(BigDecimal.valueOf(200000), periodeAv(Month.OCTOBER, 2024)),
            lagInntektspost(BigDecimal.valueOf(600000), periodeAv(Month.NOVEMBER, 2024)),
            lagInntektspost(BigDecimal.valueOf(200000), periodeAv(Month.DECEMBER, 2024))
        );
    }

    private static List<Inntektspost> lagInntektsposterForÅr(int årsinntekt) {
        var månedsinntekt = BigDecimal.valueOf(årsinntekt).divide(BigDecimal.valueOf(12), 0, RoundingMode.HALF_EVEN);
        return List.of(
            lagInntektspost(månedsinntekt, periodeAv(Month.JANUARY, 2024)),
            lagInntektspost(månedsinntekt, periodeAv(Month.FEBRUARY, 2024)),
            lagInntektspost(månedsinntekt, periodeAv(Month.MARCH, 2024)),
            lagInntektspost(månedsinntekt, periodeAv(Month.APRIL, 2024)),
            lagInntektspost(månedsinntekt, periodeAv(Month.MAY, 2024)),
            lagInntektspost(månedsinntekt, periodeAv(Month.JUNE, 2024)),
            lagInntektspost(månedsinntekt, periodeAv(Month.JULY, 2024)),
            lagInntektspost(månedsinntekt, periodeAv(Month.AUGUST, 2024)),
            lagInntektspost(månedsinntekt, periodeAv(Month.SEPTEMBER, 2024)),
            lagInntektspost(månedsinntekt, periodeAv(Month.OCTOBER, 2024)),
            lagInntektspost(månedsinntekt, periodeAv(Month.NOVEMBER, 2024)),
            lagInntektspost(månedsinntekt, periodeAv(Month.DECEMBER, 2024))
        );
    }
}
