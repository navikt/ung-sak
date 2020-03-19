package no.nav.foreldrepenger.ytelse.beregning.tilbaketrekk;


import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import no.nav.k9.kodeverk.arbeidsforhold.Inntektskategori;
import no.nav.k9.sak.behandlingslager.behandling.beregning.BeregningsresultatAndel;
import no.nav.k9.sak.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.k9.sak.behandlingslager.behandling.beregning.BeregningsresultatPeriode;
import no.nav.k9.sak.typer.Arbeidsgiver;
import no.nav.k9.sak.typer.InternArbeidsforholdRef;

/**
 * Dokumentasjon: https://confluence.adeo.no/display/MODNAV/5g+Fordele+beregningsgrunnlag+riktig+bakover+i+tid
 */
public class OmfordelUtbetaltYtelseV2Test {

    private static final LocalDate SKJÆRINGSTIDSPUNKT = LocalDate.now();
    private static final LocalDate BEREGNINGSRESULTAT_PERIODE_TOM = SKJÆRINGSTIDSPUNKT.plusDays(33);
    private static final Arbeidsgiver ARBEIDSGIVER = Arbeidsgiver.virksomhet("900050001");
    private static final InternArbeidsforholdRef REF1 = InternArbeidsforholdRef.nyRef();
    private static final InternArbeidsforholdRef REF2 = InternArbeidsforholdRef.nyRef();
    private BeregningsresultatPeriode bgBrPeriode;

    @Before
    public void setup() {
        bgBrPeriode = lagBeregningsresultatPeriode();
    }

    /**
     * Case 1a: Ingen endring
     */
    @Test
    public void ingen_endring_bruker_arbeidsgiver() {
        // Arrange

        List<BeregningsresultatAndel> forrigeAndeler = List.of(
            lagAndel(true, 600),
            lagAndel(false, 1500)
        );
        List<BeregningsresultatAndel> bgAndeler = List.of(
            lagAndel(true, 600),
            lagAndel(false, 1500)
        );

        // Act
        var resultat = OmfordelUtbetaltYtelseV2.omfordel(forrigeAndeler, bgAndeler);

        // Assert
        assertThat(resultat).hasSize(2);
        assertThat(resultat.get(0).build(bgBrPeriode)).satisfies(andel -> {
            assertThat(andel.getDagsats()).isEqualTo(600);
            assertThat(andel.erBrukerMottaker()).isTrue();
        });
        assertThat(resultat.get(1).build(bgBrPeriode)).satisfies(andel -> {
            assertThat(andel.getDagsats()).isEqualTo(1500);
            assertThat(andel.erBrukerMottaker()).isFalse();
        });
    }

    /**
     * Case 1b: Ingen endring
     */
    @Test
    public void ingen_endring_bruker() {
        // Arrange
        List<BeregningsresultatAndel> forrigeAndeler = List.of(lagAndel(true, 2100));
        List<BeregningsresultatAndel> bgAndeler = List.of(lagAndel(true, 2100));

        // Act
        List<BeregningsresultatAndel.Builder> resultat = OmfordelUtbetaltYtelseV2.omfordel(forrigeAndeler, bgAndeler);

        // Assert
        assertThat(resultat).hasSize(1);
        assertThat(resultat.get(0).build(bgBrPeriode)).satisfies(andel -> {
            assertThat(andel).isNotSameAs(bgAndeler.get(0));
            assertThat(andel.getDagsats()).isEqualTo(2100);
            assertThat(andel.erBrukerMottaker()).isTrue();
        });
    }

    /**
     * Case 1c: Ingen endring
     */
    @Test
    public void ingen_endring_arbeidsgiver() {
        // Arrange
        List<BeregningsresultatAndel> forrigeAndeler = List.of(
            lagAndel(true, 0),
            lagAndel(false, 2100)
        );
        List<BeregningsresultatAndel> bgAndeler = List.of(
            lagAndel(true, 0),
            lagAndel(false, 2100)
        );

        // Act
        var resultat = OmfordelUtbetaltYtelseV2.omfordel(forrigeAndeler, bgAndeler);

        // Assert
        assertThat(resultat).hasSize(2);
        assertThat(resultat.get(0).build(bgBrPeriode)).satisfies(andel -> {
            assertThat(andel.getDagsats()).isEqualTo(0);
            assertThat(andel.erBrukerMottaker()).isTrue();
        });
        assertThat(resultat.get(1).build(bgBrPeriode)).satisfies(andel -> {
            assertThat(andel.getDagsats()).isEqualTo(2100);
            assertThat(andel.erBrukerMottaker()).isFalse();
        });
    }

    /**
     * Case 2: Utbetaling tidligere til bruker skulle vært til arbeidsgiver
     */
    @Test
    public void utbetaling_tidligere_til_bruker_skulle_vært_til_ag() {
        // Arrange
        List<BeregningsresultatAndel> forrigeAndeler = List.of(
            lagAndel(true, 1800),
            lagAndel(false, 300)
        );
        List<BeregningsresultatAndel> bgAndeler = List.of(
            lagAndel(true, 1000),
            lagAndel(false, 1100)
        );

        // Act
        var resultat = OmfordelUtbetaltYtelseV2.omfordel(forrigeAndeler, bgAndeler);

        // Assert
        assertThat(resultat).hasSize(2);
        assertThat(resultat.get(0).build(bgBrPeriode)).satisfies(andel -> {
            assertThat(andel.getDagsats()).isEqualTo(1800);
            assertThat(andel.erBrukerMottaker()).isTrue();
        });
        assertThat(resultat.get(1).build(bgBrPeriode)).satisfies(andel -> {
            assertThat(andel.getDagsats()).isEqualTo(300);
            assertThat(andel.erBrukerMottaker()).isFalse();
        });
    }

    /**
     * Case 3: Utbetaling var tidligere til arbeidsgiver, skulle vært til bruker
     */
    @Test
    public void utbetaling_var_tidligere_til_ag_skulle_vært_til_bruker() {
        // Arrange
        List<BeregningsresultatAndel> forrigeAndeler = List.of(
            lagAndel(true, 600),
            lagAndel(false, 1500)
        );
        List<BeregningsresultatAndel> bgAndeler = List.of(
            lagAndel(true, 1200),
            lagAndel(false, 900)
        );

        // Act
        var resultat = OmfordelUtbetaltYtelseV2.omfordel(forrigeAndeler, bgAndeler);

        // Assert
        assertThat(resultat).hasSize(2);
        assertThat(resultat.get(0).build(bgBrPeriode)).satisfies(andel -> {
            assertThat(andel.getDagsats()).isEqualTo(1200);
            assertThat(andel.erBrukerMottaker()).isTrue();
        });
        assertThat(resultat.get(1).build(bgBrPeriode)).satisfies(andel -> {
            assertThat(andel.getDagsats()).isEqualTo(900);
            assertThat(andel.erBrukerMottaker()).isFalse();
        });
    }

    /**
     * Case 4: økt inntekt, økning utbetales arbeidsgiver
     */
    @Test
    public void økt_inntekt_økning_utbetales_arbeidsgiver() {
        // Arrange
        List<BeregningsresultatAndel> forrigeAndeler = List.of(
            lagAndel(true, 100),
            lagAndel(false, 800)
        );
        List<BeregningsresultatAndel> bgAndeler = List.of(
            lagAndel(true, 1300),
            lagAndel(false, 800)
        );

        // Act
        var resultat = OmfordelUtbetaltYtelseV2.omfordel(forrigeAndeler, bgAndeler);

        // Assert
        assertThat(resultat).hasSize(2);
        assertThat(resultat.get(0).build(bgBrPeriode)).satisfies(andel -> {
            assertThat(andel.getDagsats()).isEqualTo(1300);
            assertThat(andel.erBrukerMottaker()).isTrue();
        });
        assertThat(resultat.get(1).build(bgBrPeriode)).satisfies(andel -> {
            assertThat(andel.getDagsats()).isEqualTo(800);
            assertThat(andel.erBrukerMottaker()).isFalse();
        });
    }

    /**
     * Case 4.2: økt inntekt, alt utbetales til bruker før og etter
     */
    @Test
    public void økt_inntekt_økning_utbetales_bruker() {
        // Arrange
        List<BeregningsresultatAndel> forrigeAndeler = List.of(
            lagAndel(true, 900)
        );
        List<BeregningsresultatAndel> bgAndeler = List.of(
            lagAndel(true, 2100)
        );

        // Act
        var resultat = OmfordelUtbetaltYtelseV2.omfordel(forrigeAndeler, bgAndeler);

        // Assert
        assertThat(resultat).hasSize(1);
        assertThat(resultat.get(0).build(bgBrPeriode)).satisfies(andel -> {
            assertThat(andel.getDagsats()).isEqualTo(2100);
            assertThat(andel.erBrukerMottaker()).isTrue();
        });
    }

    /**
     * Case 5.1: økt inntekt, fordeles til arbeidsgiver
     */
    @Test
    public void økt_inntekt_utbetales_arbeidsgiver() {
        // Arrange
        List<BeregningsresultatAndel> forrigeAndeler = List.of(
            lagAndel(true, 100),
            lagAndel(false, 800)
        );
        List<BeregningsresultatAndel> bgAndeler = List.of(
            lagAndel(true, 100),
            lagAndel(false, 2000)
        );

        // Act
        var resultat = OmfordelUtbetaltYtelseV2.omfordel(forrigeAndeler, bgAndeler);

        // Assert
        assertThat(resultat).hasSize(2);
        assertThat(resultat.get(0).build(bgBrPeriode)).satisfies(andel -> {
            assertThat(andel.getDagsats()).isEqualTo(100);
            assertThat(andel.erBrukerMottaker()).isTrue();
        });
        assertThat(resultat.get(1).build(bgBrPeriode)).satisfies(andel -> {
            assertThat(andel.getDagsats()).isEqualTo(2000);
            assertThat(andel.erBrukerMottaker()).isFalse();
        });

    }

    /**
     * Case 5.2: økt inntekt, fordeles til arbeidsgiver, 0 til bruker
     */
    @Test
    public void økt_inntekt_utbetales_arbeidsgiver_0_til_bruker() {
        // Arrange
        List<BeregningsresultatAndel> forrigeAndeler = List.of(
            lagAndel(true, 0),
            lagAndel(false, 900)
        );
        List<BeregningsresultatAndel> bgAndeler = List.of(
            lagAndel(true, 0),
            lagAndel(false, 2100)
        );

        // Act
        var resultat = OmfordelUtbetaltYtelseV2.omfordel(forrigeAndeler, bgAndeler);

        // Assert
        assertThat(resultat).hasSize(2);
        assertThat(resultat.get(0).build(bgBrPeriode)).satisfies(andel -> {
            assertThat(andel.getDagsats()).isEqualTo(0);
            assertThat(andel.erBrukerMottaker()).isTrue();
        });
        assertThat(resultat.get(1).build(bgBrPeriode)).satisfies(andel -> {
            assertThat(andel.getDagsats()).isEqualTo(2100);
            assertThat(andel.erBrukerMottaker()).isFalse();
        });

    }

    /**
     * Case 6a: økt inntekt, mer til bruker, mer til arbeidsgiver
     */
    @Test
    public void case6a() {
        // Arrange
        List<BeregningsresultatAndel> forrigeAndeler = List.of(
            lagAndel(true, 100),
            lagAndel(false, 800)
        );
        List<BeregningsresultatAndel> bgAndeler = List.of(
            lagAndel(true, 900),
            lagAndel(false, 1200)
        );

        // Act
        var resultat = OmfordelUtbetaltYtelseV2.omfordel(forrigeAndeler, bgAndeler);

        // Assert
        assertThat(resultat).hasSize(2);
        assertThat(resultat.get(0).build(bgBrPeriode)).satisfies(andel -> {
            assertThat(andel.getDagsats()).isEqualTo(900);
            assertThat(andel.erBrukerMottaker()).isTrue();
        });
        assertThat(resultat.get(1).build(bgBrPeriode)).satisfies(andel -> {
            assertThat(andel.getDagsats()).isEqualTo(1200);
            assertThat(andel.erBrukerMottaker()).isFalse();
        });
    }

    /**
     * Case 6b: økt inntekt, mer til bruker, mer til arbeidsgiver
     */
    @Test
    public void case6b() {
        // Arrange
        List<BeregningsresultatAndel> forrigeAndeler = List.of(
            lagAndel(true, 900),
            lagAndel(false, 0)
        );
        List<BeregningsresultatAndel> bgAndeler = List.of(
            lagAndel(true, 1000),
            lagAndel(false, 1100)
        );

        // Act
        var resultat = OmfordelUtbetaltYtelseV2.omfordel(forrigeAndeler, bgAndeler);

        // Assert
        assertThat(resultat).hasSize(2);
        assertThat(resultat.get(0).build(bgBrPeriode)).satisfies(andel -> {
            assertThat(andel.getDagsats()).isEqualTo(1000);
            assertThat(andel.erBrukerMottaker()).isTrue();
        });
        assertThat(resultat.get(1).build(bgBrPeriode)).satisfies(andel -> {
            assertThat(andel.getDagsats()).isEqualTo(1100);
            assertThat(andel.erBrukerMottaker()).isFalse();
        });
    }

    /**
     * Case 6c: økt inntekt, mer til bruker, mer til arbeidsgiver
     */
    @Test
    public void case6c() {
        // Arrange
        List<BeregningsresultatAndel> forrigeAndeler = List.of(
            lagAndel(true, 0),
            lagAndel(false, 900)
        );
        List<BeregningsresultatAndel> bgAndeler = List.of(
            lagAndel(true, 1000),
            lagAndel(false, 1100)
        );

        // Act
        var resultat = OmfordelUtbetaltYtelseV2.omfordel(forrigeAndeler, bgAndeler);

        // Assert
        assertThat(resultat).hasSize(2);
        assertThat(resultat.get(0).build(bgBrPeriode)).satisfies(andel -> {
            assertThat(andel.getDagsats()).isEqualTo(1000);
            assertThat(andel.erBrukerMottaker()).isTrue();
        });
        assertThat(resultat.get(1).build(bgBrPeriode)).satisfies(andel -> {
            assertThat(andel.getDagsats()).isEqualTo(1100);
            assertThat(andel.erBrukerMottaker()).isFalse();
        });
    }

    /**
     * Case 7a: økt inntekt, mindre til bruker, mer til arbeidsgiver
     */
    @Test
    public void case7a() {
        // Arrange
        List<BeregningsresultatAndel> forrigeAndeler = List.of(
            lagAndel(true, 800),
            lagAndel(false, 100)
        );
        List<BeregningsresultatAndel> bgAndeler = List.of(
            lagAndel(true, 600),
            lagAndel(false, 1500)
        );

        // Act
        var resultat = OmfordelUtbetaltYtelseV2.omfordel(forrigeAndeler, bgAndeler);

        // Assert
        assertThat(resultat).hasSize(2);
        assertThat(resultat.get(0).build(bgBrPeriode)).satisfies(andel -> {
            assertThat(andel.getDagsats()).isEqualTo(800);
            assertThat(andel.erBrukerMottaker()).isTrue();
        });
        assertThat(resultat.get(1).build(bgBrPeriode)).satisfies(andel -> {
            assertThat(andel.getDagsats()).isEqualTo(1300);
            assertThat(andel.erBrukerMottaker()).isFalse();
        });
    }

    /**
     * Case 7b: Økt inntekt, mindre til bruker, mer til arbeidsgiver
     */
    @Test
    public void case7b() {
        // Arrange
        List<BeregningsresultatAndel> forrigeAndeler = List.of(
            lagAndel(true, 900),
            lagAndel(false, 0)
        );
        List<BeregningsresultatAndel> bgAndeler = List.of(
            lagAndel(true, 600),
            lagAndel(false, 1500)
        );

        // Act
        var resultat = OmfordelUtbetaltYtelseV2.omfordel(forrigeAndeler, bgAndeler);

        // Assert
        assertThat(resultat).hasSize(2);
        assertThat(resultat.get(0).build(bgBrPeriode)).satisfies(andel -> {
            assertThat(andel.getDagsats()).isEqualTo(900);
            assertThat(andel.erBrukerMottaker()).isTrue();
        });
        assertThat(resultat.get(1).build(bgBrPeriode)).satisfies(andel -> {
            assertThat(andel.getDagsats()).isEqualTo(1200);
            assertThat(andel.erBrukerMottaker()).isFalse();
        });
    }

    /**
     * Case 7c: Økt inntekt, mindre til bruker, mer til arbeidsgiver
     */
    @Test
    public void case7c() {
        // Arrange
        List<BeregningsresultatAndel> forrigeAndeler = List.of(
            lagAndel(true, 800),
            lagAndel(false, 100)
        );
        List<BeregningsresultatAndel> bgAndeler = List.of(
            lagAndel(true, 0),
            lagAndel(false, 2100)
        );

        // Act
        var resultat = OmfordelUtbetaltYtelseV2.omfordel(forrigeAndeler, bgAndeler);

        // Assert
        assertThat(resultat).hasSize(2);
        assertThat(resultat.get(0).build(bgBrPeriode)).satisfies(andel -> {
            assertThat(andel.getDagsats()).isEqualTo(800);
            assertThat(andel.erBrukerMottaker()).isTrue();
        });
        assertThat(resultat.get(1).build(bgBrPeriode)).satisfies(andel -> {
            assertThat(andel.getDagsats()).isEqualTo(1300);
            assertThat(andel.erBrukerMottaker()).isFalse();
        });
    }

    /**
     * Case 8a: økt inntekt, mindre til AG, mer til bruker
     */
    @Test
    public void case8a() {
        // Arrange
        List<BeregningsresultatAndel> forrigeAndeler = List.of(
            lagAndel(true, 100),
            lagAndel(false, 800)
        );
        List<BeregningsresultatAndel> bgAndeler = List.of(
            lagAndel(true, 1400),
            lagAndel(false, 700)
        );

        // Act
        var resultat = OmfordelUtbetaltYtelseV2.omfordel(forrigeAndeler, bgAndeler);

        // Assert
        assertThat(resultat).hasSize(2);
        assertThat(resultat.get(0).build(bgBrPeriode)).satisfies(andel -> {
            assertThat(andel.getDagsats()).isEqualTo(1400);
            assertThat(andel.erBrukerMottaker()).isTrue();
        });
        assertThat(resultat.get(1).build(bgBrPeriode)).satisfies(andel -> {
            assertThat(andel.getDagsats()).isEqualTo(700);
            assertThat(andel.erBrukerMottaker()).isFalse();
        });
    }

    /**
     * Case 8b: økt inntekt, mindre til AG, mer til bruker
     */
    @Test
    public void case8b() {
        // Arrange
        List<BeregningsresultatAndel> forrigeAndeler = List.of(
            lagAndel(true, 800),
            lagAndel(false, 100)
        );
        List<BeregningsresultatAndel> bgAndeler = List.of(
            lagAndel(true, 2100),
            lagAndel(false, 0)
        );

        // Act
        var resultat = OmfordelUtbetaltYtelseV2.omfordel(forrigeAndeler, bgAndeler);

        // Assert
        assertThat(resultat).hasSize(1);
        assertThat(resultat.get(0).build(bgBrPeriode)).satisfies(andel -> {
            assertThat(andel.getDagsats()).isEqualTo(2100);
            assertThat(andel.erBrukerMottaker()).isTrue();
        });
    }

    /**
     * Case 8c: økt inntekt, mindre til AG, mer til bruker
     */
    @Test
    public void case8c() {
        // Arrange
        List<BeregningsresultatAndel> forrigeAndeler = List.of(
            lagAndel(true, 0),
            lagAndel(false, 900)
        );
        List<BeregningsresultatAndel> bgAndeler = List.of(
            lagAndel(true, 1400),
            lagAndel(false, 700)
        );

        // Act
        var resultat = OmfordelUtbetaltYtelseV2.omfordel(forrigeAndeler, bgAndeler);

        // Assert
        assertThat(resultat).hasSize(2);
        assertThat(resultat.get(0).build(bgBrPeriode)).satisfies(andel -> {
            assertThat(andel.getDagsats()).isEqualTo(1400);
            assertThat(andel.erBrukerMottaker()).isTrue();
        });
        assertThat(resultat.get(1).build(bgBrPeriode)).satisfies(andel -> {
            assertThat(andel.getDagsats()).isEqualTo(700);
            assertThat(andel.erBrukerMottaker()).isFalse();
        });
    }

    /**
     * Case 9a: redusert inntekt, mindre til bruker, ingen refusjon
     */
    @Test
    public void case9a() {
        // Arrange
        List<BeregningsresultatAndel> forrigeAndeler = List.of(
            lagAndel(true, 2100),
            lagAndel(false, 0)
        );
        List<BeregningsresultatAndel> bgAndeler = List.of(
            lagAndel(true, 900),
            lagAndel(false, 0)
        );

        // Act
        var resultat = OmfordelUtbetaltYtelseV2.omfordel(forrigeAndeler, bgAndeler);

        // Assert
        assertThat(resultat).hasSize(1);
        assertThat(resultat.get(0).build(bgBrPeriode)).satisfies(andel -> {
            assertThat(andel.getDagsats()).isEqualTo(900);
            assertThat(andel.erBrukerMottaker()).isTrue();
        });
    }

    /**
     * Case 9b: redusert inntekt, mindre til bruker, ingen refusjon
     */
    @Test
    public void case9b() {
        // Arrange
        List<BeregningsresultatAndel> forrigeAndeler = List.of(
            lagAndel(true, 2100));
        List<BeregningsresultatAndel> bgAndeler = List.of(
            lagAndel(true, 0));

        // Act
        var resultat = OmfordelUtbetaltYtelseV2.omfordel(forrigeAndeler, bgAndeler);

        // Assert
        assertThat(resultat).hasSize(1);
        assertThat(resultat.get(0).build(bgBrPeriode)).satisfies(andel -> {
            assertThat(andel.getDagsats()).isEqualTo(0);
            assertThat(andel.erBrukerMottaker()).isTrue();
        });
    }

    /**
     * Case 10a: redusert inntekt, kan hindre tilbaketrekk av redusert beløp til bruker, begrenset til utbetalt refusjon.
     */
    @Test
    public void case10a() {
        // Arrange
        List<BeregningsresultatAndel> forrigeAndeler = List.of(
            lagAndel(true, 900),
            lagAndel(false, 1200)
        );
        List<BeregningsresultatAndel> bgAndeler = List.of(
            lagAndel(true, 200),
            lagAndel(false, 1200)
        );

        // Act
        var resultat = OmfordelUtbetaltYtelseV2.omfordel(forrigeAndeler, bgAndeler);

        // Assert
        assertThat(resultat).hasSize(2);
        assertThat(resultat.get(0).build(bgBrPeriode)).satisfies(andel -> {
            assertThat(andel.getDagsats()).isEqualTo(900);
            assertThat(andel.erBrukerMottaker()).isTrue();
        });
        assertThat(resultat.get(1).build(bgBrPeriode)).satisfies(andel -> {
            assertThat(andel.getDagsats()).isEqualTo(500);
            assertThat(andel.erBrukerMottaker()).isFalse();
        });
    }

    /**
     * Case 10b: redusert inntekt, kan hindre tilbaketrekk av redusert beløp til bruker, begrenset til utbetalt refusjon.
     */
    @Test
    public void case10b() {
        // Arrange
        List<BeregningsresultatAndel> forrigeAndeler = List.of(
            lagAndel(true, 1200),
            lagAndel(false, 900)
        );
        List<BeregningsresultatAndel> bgAndeler = List.of(
            lagAndel(true, 0),
            lagAndel(false, 900)
        );

        // Act
        var resultat = OmfordelUtbetaltYtelseV2.omfordel(forrigeAndeler, bgAndeler);

        // Assert
        assertThat(resultat).hasSize(2);
        assertThat(resultat.get(0).build(bgBrPeriode)).satisfies(andel -> {
            assertThat(andel.getDagsats()).isEqualTo(0);
            assertThat(andel.erBrukerMottaker()).isTrue();
        });
        assertThat(resultat.get(1).build(bgBrPeriode)).satisfies(andel -> {
            assertThat(andel.getDagsats()).isEqualTo(900);
            assertThat(andel.erBrukerMottaker()).isFalse();
        });
    }

    /**
     * Case 10c: redusert inntekt, kan hindre tilbaketrekk av redusert beløp til bruker, begrenset til utbetalt refusjon.
     */
    @Test
    public void case10c() {
        // Arrange
        List<BeregningsresultatAndel> forrigeAndeler = List.of(
            lagAndel(true, 1500),
            lagAndel(false, 600)
        );
        List<BeregningsresultatAndel> bgAndeler = List.of(
            lagAndel(true, 800),
            lagAndel(false, 600)
        );

        // Act
        var resultat = OmfordelUtbetaltYtelseV2.omfordel(forrigeAndeler, bgAndeler);

        // Assert
        assertThat(resultat).hasSize(2);
        assertThat(resultat.get(0).build(bgBrPeriode)).satisfies(andel -> {
            assertThat(andel.getDagsats()).isEqualTo(800);
            assertThat(andel.erBrukerMottaker()).isTrue();
        });
        assertThat(resultat.get(1).build(bgBrPeriode)).satisfies(andel -> {
            assertThat(andel.getDagsats()).isEqualTo(600);
            assertThat(andel.erBrukerMottaker()).isFalse();
        });
    }

    /**
     * Case 11a: redusert inntekt, mindre refusjon
     */
    @Test
    public void case11a() {
        // Arrange
        List<BeregningsresultatAndel> forrigeAndeler = List.of(
            lagAndel(true, 1000),
            lagAndel(false, 1100)
        );
        List<BeregningsresultatAndel> bgAndeler = List.of(
            lagAndel(true, 1000),
            lagAndel(false, 400)
        );

        // Act
        var resultat = OmfordelUtbetaltYtelseV2.omfordel(forrigeAndeler, bgAndeler);

        // Assert
        assertThat(resultat).hasSize(2);
        assertThat(resultat.get(0).build(bgBrPeriode)).satisfies(andel -> {
            assertThat(andel.getDagsats()).isEqualTo(1000);
            assertThat(andel.erBrukerMottaker()).isTrue();
        });
        assertThat(resultat.get(1).build(bgBrPeriode)).satisfies(andel -> {
            assertThat(andel.getDagsats()).isEqualTo(400);
            assertThat(andel.erBrukerMottaker()).isFalse();
        });
    }

    /**
     * Case 11b: redusert inntekt, mindre refusjon
     */
    @Test
    public void case11b() {
        // Arrange
        List<BeregningsresultatAndel> forrigeAndeler = List.of(
            lagAndel(true, 1400),
            lagAndel(false, 700)
        );
        List<BeregningsresultatAndel> bgAndeler = List.of(
            lagAndel(true, 1400),
            lagAndel(false, 0)
        );

        // Act
        var resultat = OmfordelUtbetaltYtelseV2.omfordel(forrigeAndeler, bgAndeler);

        // Assert
        assertThat(resultat).hasSize(1);
        assertThat(resultat.get(0).build(bgBrPeriode)).satisfies(andel -> {
            assertThat(andel.getDagsats()).isEqualTo(1400);
            assertThat(andel.erBrukerMottaker()).isTrue();
        });
    }

    /**
     * Case 12a: redusert inntekt, mindre refusjon og mindre tilbruker
     */
    @Test
    public void case12a() {
        // Arrange
        List<BeregningsresultatAndel> forrigeAndeler = List.of(
            lagAndel(true, 1000),
            lagAndel(false, 1100)
        );
        List<BeregningsresultatAndel> bgAndeler = List.of(
            lagAndel(true, 900),
            lagAndel(false, 300)
        );

        // Act
        var resultat = OmfordelUtbetaltYtelseV2.omfordel(forrigeAndeler, bgAndeler);

        // Assert
        assertThat(resultat).hasSize(2);
        assertThat(resultat.get(0).build(bgBrPeriode)).satisfies(andel -> {
            assertThat(andel.getDagsats()).isEqualTo(1000);
            assertThat(andel.erBrukerMottaker()).isTrue();
        });
        assertThat(resultat.get(1).build(bgBrPeriode)).satisfies(andel -> {
            assertThat(andel.getDagsats()).isEqualTo(200);
            assertThat(andel.erBrukerMottaker()).isFalse();
        });
    }

    /**
     * Case 12b:
     */
    @Test
    public void case12b() {
        // Arrange
        List<BeregningsresultatAndel> forrigeAndeler = List.of(
            lagAndel(true, 600),
            lagAndel(false, 1500)
        );
        List<BeregningsresultatAndel> bgAndeler = List.of(
            lagAndel(true, 0),
            lagAndel(false, 1400)
        );

        // Act
        var resultat = OmfordelUtbetaltYtelseV2.omfordel(forrigeAndeler, bgAndeler);

        // Assert
        assertThat(resultat).hasSize(2);
        assertThat(resultat.get(0).build(bgBrPeriode)).satisfies(andel -> {
            assertThat(andel.getDagsats()).isEqualTo(600);
            assertThat(andel.erBrukerMottaker()).isTrue();
        });
        assertThat(resultat.get(1).build(bgBrPeriode)).satisfies(andel -> {
            assertThat(andel.getDagsats()).isEqualTo(800);
            assertThat(andel.erBrukerMottaker()).isFalse();
        });
    }

    /**
     * Case 12c:
     */
    @Test
    public void case12c() {
        // Arrange
        List<BeregningsresultatAndel> forrigeAndeler = List.of(
            lagAndel(true, 1400),
            lagAndel(false, 700)
        );
        List<BeregningsresultatAndel> bgAndeler = List.of(
            lagAndel(true, 800),
            lagAndel(false, 600)
        );

        // Act
        var resultat = OmfordelUtbetaltYtelseV2.omfordel(forrigeAndeler, bgAndeler);

        // Assert
        assertThat(resultat).hasSize(1);
        assertThat(resultat.get(0).build(bgBrPeriode)).satisfies(andel -> {
            assertThat(andel.getDagsats()).isEqualTo(1400);
            assertThat(andel.erBrukerMottaker()).isTrue();
        });
    }

    /**
     * Case 12d:
     */
    @Test
    public void case12d() {
        // Arrange
        List<BeregningsresultatAndel> forrigeAndeler = List.of(
            lagAndel(true, 1900),
            lagAndel(false, 200)
        );
        List<BeregningsresultatAndel> bgAndeler = List.of(
            lagAndel(true, 1300),
            lagAndel(false, 100)
        );

        // Act
        var resultat = OmfordelUtbetaltYtelseV2.omfordel(forrigeAndeler, bgAndeler);

        // Assert
        assertThat(resultat).hasSize(2);
        assertThat(resultat.get(0).build(bgBrPeriode)).satisfies(andel -> {
            assertThat(andel.getDagsats()).isEqualTo(1300);
            assertThat(andel.erBrukerMottaker()).isTrue();
        });
        assertThat(resultat.get(1).build(bgBrPeriode)).satisfies(andel -> {
            assertThat(andel.getDagsats()).isEqualTo(100);
            assertThat(andel.erBrukerMottaker()).isFalse();
        });
    }

    /**
     * Case 13a: redusert inntekt, mindre refusjon, mer til bruker
     */
    @Test
    public void case13a() {
        // Arrange
        List<BeregningsresultatAndel> forrigeAndeler = List.of(
            lagAndel(true, 500),
            lagAndel(false, 1600)
        );
        List<BeregningsresultatAndel> bgAndeler = List.of(
            lagAndel(true, 600),
            lagAndel(false, 800)
        );

        // Act
        var resultat = OmfordelUtbetaltYtelseV2.omfordel(forrigeAndeler, bgAndeler);

        // Assert
        assertThat(resultat).hasSize(2);
        assertThat(resultat.get(0).build(bgBrPeriode)).satisfies(andel -> {
            assertThat(andel.getDagsats()).isEqualTo(600);
            assertThat(andel.erBrukerMottaker()).isTrue();
        });
        assertThat(resultat.get(1).build(bgBrPeriode)).satisfies(andel -> {
            assertThat(andel.getDagsats()).isEqualTo(800);
            assertThat(andel.erBrukerMottaker()).isFalse();
        });
    }

    /**
     * Case 13b: redusert inntekt, mindre refusjon, mer til bruker
     */
    @Test
    public void case13b() {
        // Arrange
        List<BeregningsresultatAndel> forrigeAndeler = List.of(
            lagAndel(true, 500),
            lagAndel(false, 1600)
        );
        List<BeregningsresultatAndel> bgAndeler = List.of(
            lagAndel(true, 1400),
            lagAndel(false, 0)
        );

        // Act
        var resultat = OmfordelUtbetaltYtelseV2.omfordel(forrigeAndeler, bgAndeler);

        // Assert
        assertThat(resultat).hasSize(1);
        assertThat(resultat.get(0).build(bgBrPeriode)).satisfies(andel -> {
            assertThat(andel.getDagsats()).isEqualTo(1400);
            assertThat(andel.erBrukerMottaker()).isTrue();
        });
    }

    /**
     * Case 13c: redusert inntekt, mindre refusjon, mer til bruker
     */
    @Test
    public void case13c() {
        // Arrange
        List<BeregningsresultatAndel> forrigeAndeler = List.of(
            lagAndel(true, 0),
            lagAndel(false, 2100)
        );
        List<BeregningsresultatAndel> bgAndeler = List.of(
            lagAndel(true, 200),
            lagAndel(false, 1200)
        );

        // Act
        var resultat = OmfordelUtbetaltYtelseV2.omfordel(forrigeAndeler, bgAndeler);

        // Assert
        assertThat(resultat).hasSize(2);
        assertThat(resultat.get(0).build(bgBrPeriode)).satisfies(andel -> {
            assertThat(andel.getDagsats()).isEqualTo(200);
            assertThat(andel.erBrukerMottaker()).isTrue();
        });
        assertThat(resultat.get(1).build(bgBrPeriode)).satisfies(andel -> {
            assertThat(andel.getDagsats()).isEqualTo(1200);
            assertThat(andel.erBrukerMottaker()).isFalse();
        });
    }

    /**
     * Case 14a:
     */
    @Test
    public void case14a() {
        // Arrange
        List<BeregningsresultatAndel> forrigeAndeler = List.of(
            lagAndel(true, 1400),
            lagAndel(false, 700)
        );
        List<BeregningsresultatAndel> bgAndeler = List.of(
            lagAndel(true, 600),
            lagAndel(false, 800)
        );

        // Act
        var resultat = OmfordelUtbetaltYtelseV2.omfordel(forrigeAndeler, bgAndeler);

        // Assert
        assertThat(resultat).hasSize(1);
        assertThat(resultat.get(0).build(bgBrPeriode)).satisfies(andel -> {
            assertThat(andel.getDagsats()).isEqualTo(1400);
            assertThat(andel.erBrukerMottaker()).isTrue();
        });
    }

    /**
     * Case 14b:
     */
    @Test
    public void case14b() {
        // Arrange
        List<BeregningsresultatAndel> forrigeAndeler = List.of(
            lagAndel(true, 800),
            lagAndel(false, 1300)
        );
        List<BeregningsresultatAndel> bgAndeler = List.of(
            lagAndel(true, 0),
            lagAndel(false, 1400)
        );

        // Act
        var resultat = OmfordelUtbetaltYtelseV2.omfordel(forrigeAndeler, bgAndeler);

        // Assert
        assertThat(resultat).hasSize(2);
        assertThat(resultat.get(0).build(bgBrPeriode)).satisfies(andel -> {
            assertThat(andel.getDagsats()).isEqualTo(800);
            assertThat(andel.erBrukerMottaker()).isTrue();
        });
        assertThat(resultat.get(1).build(bgBrPeriode)).satisfies(andel -> {
            assertThat(andel.getDagsats()).isEqualTo(600);
            assertThat(andel.erBrukerMottaker()).isFalse();
        });
    }

    /**
     * Case 14c:
     */
    @Test
    public void case14c() {
        // Arrange
        List<BeregningsresultatAndel> forrigeAndeler = List.of(
            lagAndel(true, 2000),
            lagAndel(false, 100)
        );
        List<BeregningsresultatAndel> bgAndeler = List.of(
            lagAndel(true, 1200),
            lagAndel(false, 200)
        );

        // Act
        var resultat = OmfordelUtbetaltYtelseV2.omfordel(forrigeAndeler, bgAndeler);

        // Assert
        assertThat(resultat).hasSize(2);
        assertThat(resultat.get(0).build(bgBrPeriode)).satisfies(andel -> {
            assertThat(andel.getDagsats()).isEqualTo(1200);
            assertThat(andel.erBrukerMottaker()).isTrue();
        });
        assertThat(resultat.get(1).build(bgBrPeriode)).satisfies(andel -> {
            assertThat(andel.getDagsats()).isEqualTo(200);
            assertThat(andel.erBrukerMottaker()).isFalse();
        });
    }

    /**
     * Case 14d:
     */
    @Test
    public void case14d() {
        // Arrange
        List<BeregningsresultatAndel> forrigeAndeler = List.of(
            lagAndel(true, 2000),
            lagAndel(false, 100)
        );
        List<BeregningsresultatAndel> bgAndeler = List.of(
            lagAndel(true, 0),
            lagAndel(false, 200)
        );

        // Act
        var resultat = OmfordelUtbetaltYtelseV2.omfordel(forrigeAndeler, bgAndeler);

        // Assert
        assertThat(resultat).hasSize(2);
        assertThat(resultat.get(0).build(bgBrPeriode)).satisfies(andel -> {
            assertThat(andel.getDagsats()).isEqualTo(0);
            assertThat(andel.erBrukerMottaker()).isTrue();
        });
        assertThat(resultat.get(1).build(bgBrPeriode)).satisfies(andel -> {
            assertThat(andel.getDagsats()).isEqualTo(200);
            assertThat(andel.erBrukerMottaker()).isFalse();
        });
    }

    /**
     * Case 15a: Når inntekt går ned og alt ble utbetalt til bruker i første behandling er det ikke mulig å hindre tilbaketrekk. For mye utbetalt til bruker vil aldri kunne bli mindre enn refusjon.
     */
    @Test
    public void case15a() {
        // Arrange
        List<BeregningsresultatAndel> forrigeAndeler = List.of(
            lagAndel(true, 2100),
            lagAndel(false, 0)
        );
        List<BeregningsresultatAndel> bgAndeler = List.of(
            lagAndel(true, 600),
            lagAndel(false, 800)
        );

        // Act
        var resultat = OmfordelUtbetaltYtelseV2.omfordel(forrigeAndeler, bgAndeler);

        // Assert
        assertThat(resultat).hasSize(2);
        assertThat(resultat.get(0).build(bgBrPeriode)).satisfies(andel -> {
            assertThat(andel.getDagsats()).isEqualTo(600);
            assertThat(andel.erBrukerMottaker()).isTrue();
        });
        assertThat(resultat.get(1).build(bgBrPeriode)).satisfies(andel -> {
            assertThat(andel.getDagsats()).isEqualTo(800);
            assertThat(andel.erBrukerMottaker()).isFalse();
        });
    }

    /**
     * Case 15b: Når inntekt går ned og alt ble utbetalt til bruker i første behandling er det ikke mulig å hindre tilbaketrekk. For mye utbetalt til bruker vil aldri kunne bli mindre enn refusjon.
     */
    @Test
    public void case15b() {
        // Arrange
        List<BeregningsresultatAndel> forrigeAndeler = List.of(
            lagAndel(true, 2100),
            lagAndel(false, 0)
        );
        List<BeregningsresultatAndel> bgAndeler = List.of(
            lagAndel(true, 1800),
            lagAndel(false, 200)
        );

        // Act
        var resultat = OmfordelUtbetaltYtelseV2.omfordel(forrigeAndeler, bgAndeler);

        // Assert
        assertThat(resultat).hasSize(2);
        assertThat(resultat.get(0).build(bgBrPeriode)).satisfies(andel -> {
            assertThat(andel.getDagsats()).isEqualTo(1800);
            assertThat(andel.erBrukerMottaker()).isTrue();
        });
        assertThat(resultat.get(1).build(bgBrPeriode)).satisfies(andel -> {
            assertThat(andel.getDagsats()).isEqualTo(200);
            assertThat(andel.erBrukerMottaker()).isFalse();
        });
    }

    /**
     * Case: Når det er tilkommet en inntektsmelding og et refusjonskrav har andelen gått fra å ikke ha referanse ti lå ha referanse.
     * I disse tilfellene klarer vi ikke å omfordele, og lar andelene være som de var.
     */
    @Test
    public void tilkommetRefKravOgArbeidsforholdRefSkalOmfordeles() {
        // Arrange
        List<BeregningsresultatAndel> forrigeAndeler = List.of(
            lagAndel(true, 2100),
            lagAndel(false, 0)
        );
        List<BeregningsresultatAndel> bgAndeler = List.of(
            lagAndel(true, 0, REF1),
            lagAndel(false, 2100, REF1)
        );

        // Act
        var resultat = OmfordelUtbetaltYtelseV2.omfordel(forrigeAndeler, bgAndeler);

        // Assert
        assertThat(resultat).hasSize(2);
        assertThat(resultat.get(0).build(bgBrPeriode)).satisfies(andel -> {
            assertThat(andel.getDagsats()).isEqualTo(0);
            assertThat(andel.erBrukerMottaker()).isTrue();
        });
        assertThat(resultat.get(1).build(bgBrPeriode)).satisfies(andel -> {
            assertThat(andel.getDagsats()).isEqualTo(2100);
            assertThat(andel.erBrukerMottaker()).isFalse();
        });
    }

    /**
     * Case: Ny IM slår sammen andeler med referanse til en andel uten referanse.
     */
    @Test
    public void slåttSammenAndelerMistetReferanse() {
        // Arrange
        List<BeregningsresultatAndel> forrigeAndeler = List.of(
            lagAndel(true, 1100, REF1),
            lagAndel(true, 1000, REF2),
            lagAndel(false, 0, REF1),
            lagAndel(false, 0, REF2)
            );
        List<BeregningsresultatAndel> bgAndeler = List.of(
            lagAndel(true, 0),
            lagAndel(false, 2100)
        );

        // Act
        var resultat = OmfordelUtbetaltYtelseV2.omfordel(forrigeAndeler, bgAndeler);

        // Assert
        assertThat(resultat).hasSize(1);
        assertThat(resultat.get(0).build(bgBrPeriode)).satisfies(andel -> {
            assertThat(andel.getDagsats()).isEqualTo(2100);
            assertThat(andel.erBrukerMottaker()).isTrue();
        });
    }

    /**
     * Case: Andeler uten matchende ref og andeler uten ref merges inn på andel uten ref.
     */
    @Test
    public void slåSammenAndelerMedOgUtenRefTilEnSomManglerRef() {
        // Arrange
        List<BeregningsresultatAndel> forrigeAndeler = List.of(
            lagAndel(true, 500, REF1),
            lagAndel(true, 1000, REF2),
            lagAndel(true, 500),
            lagAndel(false, 0, REF1),
            lagAndel(false, 0, REF2),
            lagAndel(false, 0)
        );
        List<BeregningsresultatAndel> bgAndeler = List.of(
            lagAndel(true, 100),
            lagAndel(false, 2000)
        );

        // Act
        var resultat = OmfordelUtbetaltYtelseV2.omfordel(forrigeAndeler, bgAndeler);

        // Assert
        assertThat(resultat).hasSize(2);
        assertThat(resultat.get(0).build(bgBrPeriode)).satisfies(andel -> {
            assertThat(andel.getDagsats()).isEqualTo(2000);
            assertThat(andel.erBrukerMottaker()).isTrue();
        });
        assertThat(resultat.get(1).build(bgBrPeriode)).satisfies(andel -> {
            assertThat(andel.getDagsats()).isEqualTo(100);
            assertThat(andel.erBrukerMottaker()).isFalse();
        });

    }

    private BeregningsresultatPeriode lagBeregningsresultatPeriode() {
        BeregningsresultatEntitet br = BeregningsresultatEntitet.builder()
            .medRegelInput("input")
            .medRegelSporing("sporing")
            .build();
        return BeregningsresultatPeriode.builder()
            .medBeregningsresultatPeriodeFomOgTom(SKJÆRINGSTIDSPUNKT, BEREGNINGSRESULTAT_PERIODE_TOM)
            .build(br);
    }

    private BeregningsresultatAndel lagAndel(boolean erBrukerMottaker, int dagsats) {
        return lagAndel(erBrukerMottaker, dagsats, null);
    }

    private BeregningsresultatAndel lagAndel(boolean erBrukerMottaker, int dagsats, InternArbeidsforholdRef ref) {
        return BeregningsresultatAndel.builder()
            .medBrukerErMottaker(erBrukerMottaker)
            .medStillingsprosent(BigDecimal.valueOf(100))
            .medUtbetalingsgrad(BigDecimal.valueOf(100))
            .medInntektskategori(Inntektskategori.ARBEIDSTAKER)
            .medDagsatsFraBg(dagsats)
            .medDagsats(dagsats)
            .medArbeidsgiver(ARBEIDSGIVER)
            .medArbeidsforholdRef(ref)
            .build(bgBrPeriode);
    }

}
