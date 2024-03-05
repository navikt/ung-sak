package no.nav.k9.sak.ytelse.pleiepengerbarn.revurdering;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.k9.kodeverk.arbeidsforhold.AktivitetStatus;
import no.nav.k9.kodeverk.arbeidsforhold.Inntektskategori;
import no.nav.k9.sak.behandlingslager.behandling.beregning.BeregningsresultatAndel;
import no.nav.k9.sak.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.k9.sak.behandlingslager.behandling.beregning.BeregningsresultatPeriode;
import no.nav.k9.sak.typer.Arbeidsgiver;
import no.nav.k9.sak.typer.Beløp;
import no.nav.k9.sak.typer.InternArbeidsforholdRef;

class UtledTilkjentYtelseEndringTest {


    @Test
    void skal_gi_ingen_endring_dersom_kun_en_periode_uten_endring() {
        // Arrange
        var resultat = lagResultat();
        var originaltResultat = lagResultat();

        var fom1 = LocalDate.now();
        var tom1 = fom1.plusDays(3);
        var periode1 = lagPeriode(fom1, tom1, resultat);
        var originalPeriode1 = lagPeriode(fom1, tom1, originaltResultat);
        var brukerErMottaker = true;
        var arbeidsgiver = Arbeidsgiver.virksomhet("123456789");
        var aktivitetStatus = AktivitetStatus.ARBEIDSTAKER;
        var inntektskategori = Inntektskategori.ARBEIDSTAKER;

        lagAndel(periode1, brukerErMottaker, arbeidsgiver, aktivitetStatus, inntektskategori, 1000, 100, 0);
        lagAndel(originalPeriode1, brukerErMottaker, arbeidsgiver, aktivitetStatus, inntektskategori, 1000, 100, 0);

        // Act
        var endringerForMottakers = act(resultat, originaltResultat);

        // Assert
        assertThat(endringerForMottakers.size()).isEqualTo(0);
    }

    @Test
    void skal_gi_endring_dersom_ny_periode() {
        var resultat = lagResultat();
        var originaltResultat = lagResultat();

        var fom1 = LocalDate.now();
        var tom1 = fom1.plusDays(3);
        var periode1 = lagPeriode(fom1, tom1, resultat);
        var originalPeriode1 = lagPeriode(fom1, tom1, originaltResultat);
        var brukerErMottaker = true;
        var arbeidsgiver = Arbeidsgiver.virksomhet("123456789");
        var aktivitetStatus = AktivitetStatus.ARBEIDSTAKER;
        var inntektskategori = Inntektskategori.ARBEIDSTAKER;

        lagAndel(periode1, brukerErMottaker, arbeidsgiver, aktivitetStatus, inntektskategori, 1000, 100, 0);
        lagAndel(originalPeriode1, brukerErMottaker, arbeidsgiver, aktivitetStatus, inntektskategori, 1000, 100, 0);


        var fom2 = tom1.plusDays(1);
        var tom2 = fom2.plusDays(3);
        var periode2 = lagPeriode(fom2, tom2, resultat);
        lagAndel(periode2, brukerErMottaker, arbeidsgiver, aktivitetStatus, inntektskategori, 1000, 100, 0);

        // Act
        var endringerForMottakers = act(resultat, originaltResultat);

        // Assert
        assertThat(endringerForMottakers.size()).isEqualTo(1);
        assertThat(endringerForMottakers.get(0).nøkkel()).isEqualTo(new MottakerNøkkel(brukerErMottaker, arbeidsgiver, null, aktivitetStatus, inntektskategori));
        validerEndretTidslinje(endringerForMottakers, 0, periode2.getPeriode().toLocalDateInterval());
    }

    @Test
    void skal_gi_endring_dersom_endring_i_periode() {
        // Arrange
        var resultat = lagResultat();
        var originaltResultat = lagResultat();

        var fom1 = LocalDate.now();
        var tom1 = fom1.plusDays(3);
        var originalTom1 = fom1.plusDays(2);

        var periode1 = lagPeriode(fom1, tom1, resultat);
        var originalPeriode1 = lagPeriode(fom1, originalTom1, originaltResultat);
        var brukerErMottaker = true;
        var arbeidsgiver = Arbeidsgiver.virksomhet("123456789");
        var aktivitetStatus = AktivitetStatus.ARBEIDSTAKER;
        var inntektskategori = Inntektskategori.ARBEIDSTAKER;

        lagAndel(periode1, brukerErMottaker, arbeidsgiver, aktivitetStatus, inntektskategori, 1000, 100, 0);
        lagAndel(originalPeriode1, brukerErMottaker, arbeidsgiver, aktivitetStatus, inntektskategori, 1000, 100, 0);

        // Act
        var endringerForMottakers = act(resultat, originaltResultat);

        // Assert
        assertThat(endringerForMottakers.size()).isEqualTo(1);
        assertThat(endringerForMottakers.get(0).nøkkel()).isEqualTo(new MottakerNøkkel(brukerErMottaker, arbeidsgiver, null, aktivitetStatus, inntektskategori));
        validerEndretTidslinje(endringerForMottakers, 0, new LocalDateInterval(originalTom1.plusDays(1), tom1));
    }


    @Test
    void skal_gi_endring_dersom_endret_dagsats() {
        // Arrange
        var resultat = lagResultat();
        var originaltResultat = lagResultat();

        var fom1 = LocalDate.now();
        var tom1 = fom1.plusDays(3);
        var periode1 = lagPeriode(fom1, tom1, resultat);
        var originalPeriode1 = lagPeriode(fom1, tom1, originaltResultat);
        var brukerErMottaker = true;
        var arbeidsgiver = Arbeidsgiver.virksomhet("123456789");
        var aktivitetStatus = AktivitetStatus.ARBEIDSTAKER;
        var inntektskategori = Inntektskategori.ARBEIDSTAKER;

        lagAndel(periode1, brukerErMottaker, arbeidsgiver, aktivitetStatus, inntektskategori, 1000, 100, 0);
        lagAndel(originalPeriode1, brukerErMottaker, arbeidsgiver, aktivitetStatus, inntektskategori, 1001, 100, 0);

        // Act
        var endringerForMottakers = act(resultat, originaltResultat);

        assertThat(endringerForMottakers.size()).isEqualTo(1);
        assertThat(endringerForMottakers.get(0).nøkkel()).isEqualTo(new MottakerNøkkel(brukerErMottaker, arbeidsgiver, null, aktivitetStatus, inntektskategori));
        validerEndretTidslinje(endringerForMottakers, 0, periode1.getPeriode().toLocalDateInterval());
    }

    @Test
    void skal_gi_endring_dersom_endret_utbetalingsgrad() {
        // Arrange
        var resultat = lagResultat();
        var originaltResultat = lagResultat();

        var fom1 = LocalDate.now();
        var tom1 = fom1.plusDays(3);
        var periode1 = lagPeriode(fom1, tom1, resultat);
        var originalPeriode1 = lagPeriode(fom1, tom1, originaltResultat);
        var brukerErMottaker = true;
        var arbeidsgiver = Arbeidsgiver.virksomhet("123456789");
        var aktivitetStatus = AktivitetStatus.ARBEIDSTAKER;
        var inntektskategori = Inntektskategori.ARBEIDSTAKER;

        lagAndel(periode1, brukerErMottaker, arbeidsgiver, aktivitetStatus, inntektskategori, 1000, 100, 0);
        lagAndel(originalPeriode1, brukerErMottaker, arbeidsgiver, aktivitetStatus, inntektskategori, 1000, 99, 0);

        // Act
        var endringerForMottakers = act(resultat, originaltResultat);

        // Assert
        assertThat(endringerForMottakers.size()).isEqualTo(1);
        assertThat(endringerForMottakers.get(0).nøkkel()).isEqualTo(new MottakerNøkkel(brukerErMottaker, arbeidsgiver, null, aktivitetStatus, inntektskategori));
        validerEndretTidslinje(endringerForMottakers, 0, periode1.getPeriode().toLocalDateInterval());
    }

    @Test
    void skal_gi_endring_dersom_endret_feriepengerÅrsbeløp() {
        // Arrange
        var resultat = lagResultat();
        var originaltResultat = lagResultat();

        var fom1 = LocalDate.now();
        var tom1 = fom1.plusDays(3);
        var periode1 = lagPeriode(fom1, tom1, resultat);
        var originalPeriode1 = lagPeriode(fom1, tom1, originaltResultat);
        var brukerErMottaker = true;
        var arbeidsgiver = Arbeidsgiver.virksomhet("123456789");
        var aktivitetStatus = AktivitetStatus.ARBEIDSTAKER;
        var inntektskategori = Inntektskategori.ARBEIDSTAKER;

        lagAndel(periode1, brukerErMottaker, arbeidsgiver, aktivitetStatus, inntektskategori, 1000, 100, 0);
        lagAndel(originalPeriode1, brukerErMottaker, arbeidsgiver, aktivitetStatus, inntektskategori, 1000, 100, 1);

        // Act
        var endringerForMottakers = act(resultat, originaltResultat);

        assertThat(endringerForMottakers.size()).isEqualTo(1);
        assertThat(endringerForMottakers.get(0).nøkkel()).isEqualTo(new MottakerNøkkel(brukerErMottaker, arbeidsgiver, null, aktivitetStatus, inntektskategori));
        validerEndretTidslinje(endringerForMottakers, 0, periode1.getPeriode().toLocalDateInterval());
    }


    @Test
    void skal_gi_to_endringer_dersom_endret_inntektskategori_en_for_tilkommet_og_en_for_bortfalt() {
        // Arrange
        var resultat = lagResultat();
        var originaltResultat = lagResultat();

        var fom1 = LocalDate.now();
        var tom1 = fom1.plusDays(3);
        var periode1 = lagPeriode(fom1, tom1, resultat);
        var originalPeriode1 = lagPeriode(fom1, tom1, originaltResultat);
        var brukerErMottaker = true;
        var arbeidsgiver = Arbeidsgiver.virksomhet("123456789");
        var aktivitetStatus = AktivitetStatus.ARBEIDSTAKER;
        var inntektskategori = Inntektskategori.ARBEIDSTAKER;

        lagAndel(periode1, brukerErMottaker, arbeidsgiver, aktivitetStatus, Inntektskategori.DAGPENGER, 1000, 100, 0);
        lagAndel(originalPeriode1, brukerErMottaker, arbeidsgiver, aktivitetStatus, inntektskategori, 1000, 100, 0);

        // Act
        var endringerForMottakers = act(resultat, originaltResultat);

        assertThat(endringerForMottakers.size()).isEqualTo(2);
        var nøkler = endringerForMottakers.stream().map(UtbetalingsendringerForMottaker::nøkkel).toList();
        assertThat(nøkler.contains(new MottakerNøkkel(brukerErMottaker, arbeidsgiver, InternArbeidsforholdRef.nullRef(), aktivitetStatus, inntektskategori))).isTrue();
        validerEndretTidslinje(endringerForMottakers, 0, periode1.getPeriode().toLocalDateInterval());

        assertThat(nøkler.contains(new MottakerNøkkel(brukerErMottaker, arbeidsgiver, InternArbeidsforholdRef.nullRef(), aktivitetStatus, Inntektskategori.DAGPENGER))).isTrue();
        validerEndretTidslinje(endringerForMottakers, 1, periode1.getPeriode().toLocalDateInterval());
    }

    @Test
    void skal_gi_to_endringer_dersom_endret_aktivitetstatus_en_for_tilkommet_og_en_for_bortfalt() {
        // Arrange
        var resultat = lagResultat();
        var originaltResultat = lagResultat();

        var fom1 = LocalDate.now();
        var tom1 = fom1.plusDays(3);
        var periode1 = lagPeriode(fom1, tom1, resultat);
        var originalPeriode1 = lagPeriode(fom1, tom1, originaltResultat);
        var brukerErMottaker = true;
        var arbeidsgiver = Arbeidsgiver.virksomhet("123456789");
        var aktivitetStatus = AktivitetStatus.ARBEIDSTAKER;
        var inntektskategori = Inntektskategori.ARBEIDSTAKER;

        lagAndel(periode1, brukerErMottaker, arbeidsgiver, AktivitetStatus.IKKE_YRKESAKTIV, inntektskategori, 1000, 100, 0);
        lagAndel(originalPeriode1, brukerErMottaker, arbeidsgiver, aktivitetStatus, inntektskategori, 1000, 100, 0);

        // Act
        var endringerForMottakers = act(resultat, originaltResultat);

        assertThat(endringerForMottakers.size()).isEqualTo(2);
        var nøkler = endringerForMottakers.stream().map(UtbetalingsendringerForMottaker::nøkkel).toList();
        assertThat(nøkler.contains(new MottakerNøkkel(brukerErMottaker, arbeidsgiver, InternArbeidsforholdRef.nullRef(), AktivitetStatus.IKKE_YRKESAKTIV, inntektskategori))).isTrue();
        validerEndretTidslinje(endringerForMottakers, 0, periode1.getPeriode().toLocalDateInterval());

        assertThat(nøkler.contains(new MottakerNøkkel(brukerErMottaker, arbeidsgiver, InternArbeidsforholdRef.nullRef(), aktivitetStatus, inntektskategori))).isTrue();
        validerEndretTidslinje(endringerForMottakers, 1, periode1.getPeriode().toLocalDateInterval());
    }


    @Test
    void skal_gi_endring_dersom_ny_aktivitet() {
        // Arrange
        var resultat = lagResultat();
        var originaltResultat = lagResultat();

        var fom1 = LocalDate.now();
        var tom1 = fom1.plusDays(3);
        var periode1 = lagPeriode(fom1, tom1, resultat);
        var originalPeriode1 = lagPeriode(fom1, tom1, originaltResultat);
        var brukerErMottaker = true;
        var arbeidsgiver = Arbeidsgiver.virksomhet("123456789");
        var arbeidsgiver2 = Arbeidsgiver.virksomhet("123456788");

        var aktivitetStatus = AktivitetStatus.ARBEIDSTAKER;
        var inntektskategori = Inntektskategori.ARBEIDSTAKER;

        lagAndel(periode1, brukerErMottaker, arbeidsgiver, aktivitetStatus, inntektskategori, 1000, 100, 0);
        lagAndel(periode1, brukerErMottaker, arbeidsgiver2, aktivitetStatus, inntektskategori, 1000, 100, 0);

        lagAndel(originalPeriode1, brukerErMottaker, arbeidsgiver, aktivitetStatus, inntektskategori, 1000, 100, 0);

        // Act
        var endringerForMottakers = act(resultat, originaltResultat);

        assertThat(endringerForMottakers.size()).isEqualTo(1);
        assertThat(endringerForMottakers.get(0).nøkkel()).isEqualTo(new MottakerNøkkel(brukerErMottaker, arbeidsgiver2, null, aktivitetStatus, inntektskategori));
        validerEndretTidslinje(endringerForMottakers, 0, periode1.getPeriode().toLocalDateInterval());
    }

    private static void validerEndretTidslinje(List<UtbetalingsendringerForMottaker> endringerForMottakers, int index, LocalDateInterval periode1) {
        var endretIntervaller2 = endringerForMottakers.get(index).tidslinjeMedEndringIYtelse().getLocalDateIntervals();
        assertThat(endretIntervaller2.size()).isEqualTo(1);
        assertThat(endretIntervaller2.contains(periode1)).isTrue();
    }

    private static List<UtbetalingsendringerForMottaker> act(BeregningsresultatEntitet resultat, BeregningsresultatEntitet originaltResultat) {
        return UtledTilkjentYtelseEndring.utledEndringer(resultat, originaltResultat);
    }

    private static BeregningsresultatEntitet lagResultat() {
        var builder = BeregningsresultatEntitet.builder();
        return builder
            .medRegelInput("test")
            .medRegelSporing("test")
            .build();
    }

    private static BeregningsresultatPeriode lagPeriode(LocalDate fom1, LocalDate tom1, BeregningsresultatEntitet resultat) {
        return BeregningsresultatPeriode.builder()
            .medBeregningsresultatPeriodeFomOgTom(fom1, tom1).build(resultat);
    }

    private static void lagAndel(BeregningsresultatPeriode periode1, boolean brukerErMottaker, Arbeidsgiver arbeidsgiver, AktivitetStatus aktivitetStatus, Inntektskategori inntektskategori, int dagsats, int utbetalingsgrad, int feriepengerÅrsbeløp) {
        BeregningsresultatAndel.builder()
            .medAktivitetStatus(aktivitetStatus)
            .medBrukerErMottaker(brukerErMottaker)
            .medArbeidsgiver(arbeidsgiver)
            .medInntektskategori(inntektskategori)
            .medDagsats(dagsats)
            .medDagsatsFraBg(dagsats)
            .medFeriepengerÅrsbeløp(new Beløp(feriepengerÅrsbeløp))
            .medUtbetalingsgrad(BigDecimal.valueOf(utbetalingsgrad))
            .medStillingsprosent(BigDecimal.valueOf(100))
            .buildFor(periode1);
    }
}
