package no.nav.foreldrepenger.ytelse.beregning.tilbaketrekk;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;
import java.time.Period;
import java.util.Collections;
import java.util.List;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.aktivitet.AktivitetStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.aktivitet.Inntektskategori;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatAndel;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatPeriode;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.ytelse.beregning.tilbaketrekk.BRAndelSammenligning;
import no.nav.foreldrepenger.ytelse.beregning.tilbaketrekk.MapBRAndelSammenligningTidslinje;
import no.nav.foreldrepenger.ytelse.beregning.tilbaketrekk.VurderBehovForÅHindreTilbaketrekk;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import java.time.LocalDate;

@Ignore
public class VurderBehovForÅHindreTilbaketrekkTest {

    private static final LocalDate SKJÆRINGSTIDSPUNKT = LocalDate.of(2019, Month.JANUARY, 20);
    private static final LocalDate ANDRE_PERIODE_FOM = SKJÆRINGSTIDSPUNKT.plusMonths(5);
    private static final LocalDate SISTE_UTTAKSDAG = SKJÆRINGSTIDSPUNKT.plusMonths(9);

    private static final Arbeidsgiver ARBEIDSGIVER1 = Arbeidsgiver.virksomhet("900050001");

    @BeforeClass
    public static void beforeClass() {
        settSimulertNåtidTil(LocalDate.of(2019, Month.FEBRUARY, 4));
    }

    @AfterClass
    public static void teardown() {
        settSimulertNåtidTil(LocalDate.now());
    }

    @Test
    public void ingenEndringSkalGiEmpty() {
        // Arrange
        int inntektBeløp = 1000;
        BeregningsresultatEntitet forrigeTY = lagBeregningsresultatFP(0, inntektBeløp);
        BeregningsresultatEntitet denneTY = lagBeregningsresultatFP(0, inntektBeløp);
        List<BeregningsresultatPeriode> forrigeTYPerioder = forrigeTY.getBeregningsresultatPerioder();
        List<BeregningsresultatPeriode> denneTYPerioder = denneTY.getBeregningsresultatPerioder();
        LocalDateTimeline<BRAndelSammenligning> brAndelTidslinje = MapBRAndelSammenligningTidslinje.opprettTidslinje(
            forrigeTYPerioder,
            denneTYPerioder
        );

        // Act
        boolean resultat = VurderBehovForÅHindreTilbaketrekk.skalVurdereTilbaketrekk(brAndelTidslinje);

        // Assert
        assertThat(resultat).isFalse();
    }

    @Test
    public void økningIRefusjonOgReduksjonFraBrukerSkalGiEndringsdato() {
        // Arrange
        BeregningsresultatEntitet forrigeTY = lagBeregningsresultatFP(200, 800);
        BeregningsresultatEntitet denneTY = lagBeregningsresultatFP(0, 1000);
        List<BeregningsresultatPeriode> forrigeTYPerioder = forrigeTY.getBeregningsresultatPerioder();
        List<BeregningsresultatPeriode> denneTYPerioder = denneTY.getBeregningsresultatPerioder();

        // Act
        boolean resultat = VurderBehovForÅHindreTilbaketrekk.skalVurdereTilbaketrekk(MapBRAndelSammenligningTidslinje.opprettTidslinje(
            forrigeTYPerioder,
            denneTYPerioder
        ));

        // Assert
        assertThat(resultat).isTrue();
    }

    @Test
    public void ingenRefusjonIForrigeOgFullRefusjonIRevurderingSkalGiEndringsdato() {
        // Arrange
        BeregningsresultatEntitet forrigeTY = lagBeregningsresultatFP(1000, 0);
        BeregningsresultatEntitet denneTY = lagBeregningsresultatFP(0, 1000);
        List<BeregningsresultatPeriode> forrigeTYPerioder = forrigeTY.getBeregningsresultatPerioder();
        List<BeregningsresultatPeriode> denneTYPerioder = denneTY.getBeregningsresultatPerioder();
        LocalDateTimeline<BRAndelSammenligning> brAndelTidslinje = MapBRAndelSammenligningTidslinje.opprettTidslinje(
            forrigeTYPerioder,
            denneTYPerioder
        );

        // Act
        boolean resultat = VurderBehovForÅHindreTilbaketrekk.skalVurdereTilbaketrekk(brAndelTidslinje);

        // Assert
        assertThat(resultat).isTrue();
    }

    @Test
    public void ingenTilkjentYtelseIRevurderingSkalGiEmpty() {
        // Arrange
        BeregningsresultatEntitet forrigeTY = lagBeregningsresultatFP(0, 1000);
        List<BeregningsresultatPeriode> forrigeTYPerioder = forrigeTY.getBeregningsresultatPerioder();
        List<BeregningsresultatPeriode> denneTYPerioder = Collections.emptyList();
        LocalDateTimeline<BRAndelSammenligning> brAndelTidslinje = MapBRAndelSammenligningTidslinje.opprettTidslinje(
            forrigeTYPerioder,
            denneTYPerioder
        );

        // Act
        boolean resultat = VurderBehovForÅHindreTilbaketrekk.skalVurdereTilbaketrekk(brAndelTidslinje);

        // Assert
        assertThat(resultat).isFalse();
    }

    @Test
    public void økningSkalGiEmpty() {
        // Arrange
        BeregningsresultatEntitet forrigeTY = lagBeregningsresultatFP(0, 1000);
        BeregningsresultatEntitet denneTY = lagBeregningsresultatFP(200, 800);
        List<BeregningsresultatPeriode> forrigeTYPerioder = forrigeTY.getBeregningsresultatPerioder();
        List<BeregningsresultatPeriode> denneTYPerioder = denneTY.getBeregningsresultatPerioder();
        LocalDateTimeline<BRAndelSammenligning> brAndelTidslinje = MapBRAndelSammenligningTidslinje.opprettTidslinje(
            forrigeTYPerioder,
            denneTYPerioder
        );

        // Act
        boolean resultat = VurderBehovForÅHindreTilbaketrekk.skalVurdereTilbaketrekk(brAndelTidslinje);

        // Assert
        assertThat(resultat).isFalse();
    }

    @Test
    public void reduksjonEtterUtbetaltTomSkalGiEmpty() {
        // Arrange
        BeregningsresultatEntitet forrigeTY = lagBeregningsresultatFP(200, 800);

        BeregningsresultatEntitet br = BeregningsresultatEntitet.builder()
            .medRegelSporing("regelsporing")
            .medRegelInput("regelinput")
            .build();

        BeregningsresultatPeriode periode1 = BeregningsresultatPeriode.builder()
            .medBeregningsresultatPeriodeFomOgTom(SKJÆRINGSTIDSPUNKT, ANDRE_PERIODE_FOM.minusDays(1))
            .build(br);
        lagAndel(periode1, ARBEIDSGIVER1, true, 200);
        lagAndel(periode1, ARBEIDSGIVER1, false, 800);
        BeregningsresultatPeriode periode2 = BeregningsresultatPeriode.builder()
            .medBeregningsresultatPeriodeFomOgTom(ANDRE_PERIODE_FOM, SISTE_UTTAKSDAG)
            .build(br);
        lagAndel(periode2, ARBEIDSGIVER1, true, 0);
        lagAndel(periode2, ARBEIDSGIVER1, false, 1000);
        BeregningsresultatEntitet denneTY = periode1.getBeregningsresultat();

        List<BeregningsresultatPeriode> forrigeTYPerioder = forrigeTY.getBeregningsresultatPerioder();
        List<BeregningsresultatPeriode> denneTYPerioder = denneTY.getBeregningsresultatPerioder();
        LocalDateTimeline<BRAndelSammenligning> brAndelTidslinje = MapBRAndelSammenligningTidslinje.opprettTidslinje(
            forrigeTYPerioder,
            denneTYPerioder
        );

        // Act
        boolean resultat = VurderBehovForÅHindreTilbaketrekk.skalVurdereTilbaketrekk(brAndelTidslinje);

        // Assert
        assertThat(resultat).isFalse();
    }

    private BeregningsresultatEntitet lagBeregningsresultatFP(int dagsatsBruker, int dagsatsArbeidsgiver) {
        List<BeregningsresultatPeriode> brpList = lagBeregningsresultatPeriode();
        brpList.forEach(brp -> {
            lagAndel(brp, ARBEIDSGIVER1, true, dagsatsBruker);
            if (dagsatsArbeidsgiver > 0) {
                lagAndel(brp, ARBEIDSGIVER1, false, dagsatsArbeidsgiver);
            }
        });
        return brpList.get(0).getBeregningsresultat();
    }

    private List<BeregningsresultatPeriode> lagBeregningsresultatPeriode() {
        BeregningsresultatEntitet br = BeregningsresultatEntitet.builder()
            .medRegelSporing("regelsporing")
            .medRegelInput("regelinput")
            .build();

        BeregningsresultatPeriode periode1 = BeregningsresultatPeriode.builder()
            .medBeregningsresultatPeriodeFomOgTom(SKJÆRINGSTIDSPUNKT, ANDRE_PERIODE_FOM.minusDays(1))
            .build(br);
        BeregningsresultatPeriode periode2 = BeregningsresultatPeriode.builder()
            .medBeregningsresultatPeriodeFomOgTom(ANDRE_PERIODE_FOM, SISTE_UTTAKSDAG)
            .build(br);
        return List.of(periode1, periode2);
    }

    private BeregningsresultatAndel lagAndel(BeregningsresultatPeriode brp, Arbeidsgiver arbeidsgiver, boolean brukerErMottaker, int dagsats) {
        return BeregningsresultatAndel.builder()
            .medBrukerErMottaker(brukerErMottaker)
            .medArbeidsgiver(arbeidsgiver)
            .medStillingsprosent(new BigDecimal(100))
            .medUtbetalingsgrad(new BigDecimal(100))
            .medAktivitetStatus(AktivitetStatus.ARBEIDSTAKER)
            .medInntektskategori(Inntektskategori.ARBEIDSTAKER)
            .medDagsats(dagsats)
            .medDagsatsFraBg(dagsats)
            .build(brp);
    }

    private static void settSimulertNåtidTil(LocalDate dato) {
        Period periode = Period.between(LocalDate.now(), dato);
    }
}
