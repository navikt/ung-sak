package no.nav.ung.ytelse.aktivitetspenger.beregning.beste;

import no.nav.ung.kodeverk.arbeidsforhold.InntektspostType;
import no.nav.ung.sak.domene.iay.modell.Inntektspost;
import no.nav.ung.sak.domene.iay.modell.InntektspostBuilder;
import no.nav.ung.sak.typer.Periode;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Month;
import java.time.Year;
import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class FinnGjennomsnittligPGITest {

    @Test
    void finnGjennomsnittligPGI_År_inntekt_inneværende_år_under_6G() {
        var detteÅret = Year.now();

        var resultat = FinnGjennomsnittligPGI.finnGjennomsnittligPGI(
            detteÅret,
            detteÅret,
            List.of(
                lagInntektspost(new BigDecimal(300_000), årsperiodeAv(detteÅret))
            )
        );

        assertThat(resultat.pgiPerÅr().get(detteÅret)).isEqualByComparingTo(BigDecimal.valueOf(300_000));
    }

    @Test
    void finnGjennomsnittligPGI_År_inntekt_inneværende_år_mellom_6G_12G() {
        var år2024 = Year.of(2024);
        var årsperiode = årsperiodeAv(år2024);
        var niG = BigDecimal.valueOf(124028).multiply(BigDecimal.valueOf(9));

        var resultat = FinnGjennomsnittligPGI.finnGjennomsnittligPGI(
            år2024,
            år2024,
            List.of(lagInntektspost(niG, årsperiode))
        );

        // For inntekt mellom 6G og 12G skal PGI være: 6G + ((INNTEKT - 6G) / 3)
        assertThat(resultat.pgiPerÅr().get(år2024)).isEqualByComparingTo(new BigDecimal(860_984));
    }

    @Test
    void finnGjennomsnittligPGI_År_inntekt_inneværende_år_over_12G() {
        var år2024 = Year.of(2024);
        var årsperiode = årsperiodeAv(år2024);
        var femtenG = BigDecimal.valueOf(124028).multiply(BigDecimal.valueOf(15)); // 1 860 420 kr

        var resultat = FinnGjennomsnittligPGI.finnGjennomsnittligPGI(
            år2024,
            år2024,
            List.of(lagInntektspost(femtenG, årsperiode))
        );

        // For inntekt over 12G skal PGI maks være: 6G + ((12G - 6G) / 3) = 6G + 2G = 8G
        // 8G (snitt 2024): 122 225 kroner * 8 = 977 800 kroner
        assertThat(resultat.pgiPerÅr().get(Year.of(2024))).isEqualByComparingTo(new BigDecimal(977_800));
    }

    @Test
    void skal_beregne_med_inflasjonsfaktor_for_tidligere_år() {
        var sisteLigningsår = Year.of(2024);

        var inntektspost = lagInntektspost(
            BigDecimal.valueOf(500_000), årsperiodeAv(sisteLigningsår)
        );

        var resultat = FinnGjennomsnittligPGI.finnGjennomsnittligPGI(
            sisteLigningsår,
            sisteLigningsår.plusYears(1),
            List.of(inntektspost)
        );

        // 128 116 kroner (G-snitt 2025) / 122 225 kroner (G-snitt 2024) * 500 000 = 1,048198 * 500 000 = 524 098
        assertThat(resultat.pgiPerÅr().get(sisteLigningsår)).isEqualByComparingTo(new BigDecimal("524098.9977500000"));
    }

    @Test
    void skal_beregne_korrekt_PGI_bidrag_nøyaktig_på_6G() {
        var år2024 = Year.of(2024);
        var periode = årsperiodeAv(år2024);

        // Nøyaktig 6G = 6 * 124028 = 744168
        var seksG = BigDecimal.valueOf(124028).multiply(BigDecimal.valueOf(6));
        var inntektspost = lagInntektspost(seksG, periode);

        var resultat = FinnGjennomsnittligPGI.finnGjennomsnittligPGI(
            år2024,
            år2024,
            List.of(inntektspost)
        );

        // Ved nøyaktig 6G skal PGI være lik 6G (ingen reduksjon)
        // G-snitt 2024 (vektet gjennomsnitt) brukes for å beregne 6G-grensen
        assertThat(resultat.pgiPerÅr().get(Year.of(2024))).isEqualByComparingTo(new BigDecimal(736956));
    }

    @Test
    void skal_beregne_korrekt_PGI_bidrag_nøyaktig_på_12G() {
        var år2024 = Year.of(2024);
        var periode = årsperiodeAv(år2024);

        // Nøyaktig 12G = 12 * 124028 = 1 488 336
        var tolvG = BigDecimal.valueOf(124028).multiply(BigDecimal.valueOf(12));
        var inntektspost = lagInntektspost(tolvG, periode);

        var resultat = FinnGjennomsnittligPGI.finnGjennomsnittligPGI(
            år2024,
            år2024,
            List.of(inntektspost)
        );

        // Ved 12G skal PGI-bidrag være 8G: 6G + ((12G - 6G) / 3) = 6G + 2G = 8G
        // G-snitt 2024 (vektet gjennomsnitt) brukes for å beregne G-multipler
        assertThat(resultat.pgiPerÅr().get(Year.of(2024))).isEqualByComparingTo(new BigDecimal(977800));
    }

    private static Periode årsperiodeAv(Year år) {
        return new Periode(år.atDay(1), år.atMonth(Month.DECEMBER).atEndOfMonth());
    }

    private static Inntektspost lagInntektspost(BigDecimal verdi, Periode periode) {
        return InntektspostBuilder.ny()
            .medInntektspostType(InntektspostType.LØNN)
            .medBeløp(verdi)
            .medPeriode(periode.getFom(), periode.getTom())
            .build();
    }
}
