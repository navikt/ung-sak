package no.nav.k9.sak.ytelse.beregning.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Period;
import java.util.List;

import org.junit.jupiter.api.Test;

import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.k9.sak.typer.Arbeidsgiver;
import no.nav.k9.sak.ytelse.beregning.regelmodell.BeregningsresultatAndel;
import no.nav.k9.sak.ytelse.beregning.regelmodell.BeregningsresultatPeriode;
import no.nav.k9.sak.ytelse.beregning.regelmodell.beregningsgrunnlag.AktivitetStatus;
import no.nav.k9.sak.ytelse.beregning.regelmodell.beregningsgrunnlag.Arbeidsforhold;
import no.nav.k9.sak.ytelse.beregning.regelmodell.beregningsgrunnlag.Inntektskategori;
import no.nav.k9.sak.ytelse.beregning.regelmodell.feriepenger.BeregningsresultatFeriepengerPrÅr;
import no.nav.k9.sak.ytelse.beregning.regelmodell.feriepenger.BeregningsresultatFeriepengerRegelModell;

class MapBeregningsresultatFeriepengerFraRegelTilVLTest {


    private static final LocalDate STP = LocalDate.now();
    private static final LocalDateInterval PERIODE = LocalDateInterval.withPeriodAfterDate(STP, Period.ofMonths(10));
    public static final String ORGNR = "910909088";
    private static final Arbeidsforhold ARBEIDSFORHOLD = Arbeidsforhold.nyttArbeidsforholdHosVirksomhet(ORGNR, null);
    private static final long DAGSATS = 500L;
    private static final long DAGSATS_FRA_BG = 500L;
    private static final BigDecimal UTBETALINGSGRAD = BigDecimal.valueOf(100);


    @Test
    public void skal_lage_feriepengeresultat_om_årsbeløp_ikkje_avrundes_til_0() {
        // Arrange
        BeregningsresultatPeriode periode = lagPeriodeMedAndel(BigDecimal.valueOf(1.5));
        no.nav.k9.sak.behandlingslager.behandling.beregning.BeregningsresultatEntitet beregningsresultat = lagVlBeregningsresultat();
        BeregningsresultatFeriepengerRegelModell regelmodell = BeregningsresultatFeriepengerRegelModell.builder()
            .medBeregningsresultatPerioder(List.of(periode))
            .build();

        // Act
        MapBeregningsresultatFeriepengerFraRegelTilVL.mapTilResultatFraRegelModell(beregningsresultat, regelmodell);

        // Assert
        // Assert
        var andelTimeline = beregningsresultat.getBeregningsresultatAndelTimeline();
        assertThat(andelTimeline.toSegments()).allSatisfy(seg -> {
            assertThat(seg.getValue()).hasSize(1);
            assertThat(seg.getValue()).anySatisfy(b -> {
                assertThat(b.getFeriepengerÅrsbeløp()).isNotNull();
            });
        });
    }

    @Test
    public void skal_ikkje_lage_feriepengeresultat_om_årsbeløp_avrundes_til_0() {
        // Arrange
        BeregningsresultatPeriode periode = lagPeriodeMedAndel(BigDecimal.valueOf(0.1));
        no.nav.k9.sak.behandlingslager.behandling.beregning.BeregningsresultatEntitet beregningsresultat = lagVlBeregningsresultat();
        BeregningsresultatFeriepengerRegelModell regelmodell = BeregningsresultatFeriepengerRegelModell.builder()
            .medBeregningsresultatPerioder(List.of(periode))
            .build();

        // Act
        MapBeregningsresultatFeriepengerFraRegelTilVL.mapTilResultatFraRegelModell(beregningsresultat, regelmodell);

        // Assert
        var andelTimeline = beregningsresultat.getBeregningsresultatAndelTimeline();
        assertThat(andelTimeline.toSegments()).allSatisfy(seg -> {
            assertThat(seg.getValue()).hasSize(1);
            assertThat(seg.getValue()).allSatisfy(b -> {
                assertThat(b.getFeriepengerÅrsbeløp()).isNull();
            });
        });
    }

    private no.nav.k9.sak.behandlingslager.behandling.beregning.BeregningsresultatEntitet lagVlBeregningsresultat() {
        no.nav.k9.sak.behandlingslager.behandling.beregning.BeregningsresultatEntitet beregningsresultat = no.nav.k9.sak.behandlingslager.behandling.beregning.BeregningsresultatEntitet.builder()
            .medRegelInput("Regelinput")
            .medRegelSporing("Regelsporing")
            .build();
        no.nav.k9.sak.behandlingslager.behandling.beregning.BeregningsresultatPeriode vlBeregningsresultatPeriode = no.nav.k9.sak.behandlingslager.behandling.beregning.BeregningsresultatPeriode
            .builder()
            .medBeregningsresultatPeriodeFomOgTom(PERIODE.getFomDato(), PERIODE.getTomDato())
            .build(beregningsresultat);

        no.nav.k9.sak.behandlingslager.behandling.beregning.BeregningsresultatAndel
            .builder()
            .medBrukerErMottaker(true)
            .medDagsats((int) DAGSATS)
            .medDagsatsFraBg((int) DAGSATS_FRA_BG)
            .medInntektskategori(no.nav.k9.kodeverk.arbeidsforhold.Inntektskategori.ARBEIDSTAKER)
            .medUtbetalingsgrad(UTBETALINGSGRAD)
            .medAktivitetStatus(no.nav.k9.kodeverk.arbeidsforhold.AktivitetStatus.ARBEIDSTAKER)
            .medArbeidsgiver(Arbeidsgiver.virksomhet(ORGNR))
            .medStillingsprosent(BigDecimal.valueOf(100)).buildFor(vlBeregningsresultatPeriode);
        return beregningsresultat;
    }

    private BeregningsresultatPeriode lagPeriodeMedAndel(BigDecimal årsbeløp) {
        BeregningsresultatPeriode periode = new BeregningsresultatPeriode(PERIODE, null, null, null, null, null, null);
        BeregningsresultatAndel andel = BeregningsresultatAndel.builder().medAktivitetStatus(AktivitetStatus.ATFL)
            .medBrukerErMottaker(true)
            .medInntektskategori(Inntektskategori.ARBEIDSTAKER)
            .medArbeidsforhold(ARBEIDSFORHOLD)
            .medDagsats(DAGSATS)
            .medDagsatsFraBg(DAGSATS_FRA_BG)
            .medUtbetalingssgrad(UTBETALINGSGRAD)
            .build(periode);
        BeregningsresultatFeriepengerPrÅr.builder().medÅrsbeløp(årsbeløp)
            .medOpptjeningÅr(LocalDate.now()).build(andel);
        return periode;
    }

}
