package no.nav.k9.sak.ytelse.pleiepengerbarn.revurdering;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;

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

class UtledTilkjentYtelseEndringTest {

    @BeforeEach
    void setUp() {
    }


    @Test
    void skal_gi_ingen_endring_dersom_kun_en_periode_uten_endring() {
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

        var endringerForMottakers = UtledTilkjentYtelseEndring.utledEndringer(resultat, originaltResultat);

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

        var endringerForMottakers = UtledTilkjentYtelseEndring.utledEndringer(resultat, originaltResultat);

        assertThat(endringerForMottakers.size()).isEqualTo(1);
        assertThat(endringerForMottakers.get(0).nøkkel()).isEqualTo(new UtledTilkjentYtelseEndring.MottakerNøkkel(brukerErMottaker, arbeidsgiver, null, aktivitetStatus, inntektskategori));
        var endretIntervaller = endringerForMottakers.get(0).tidslinjeMedEndringIYtelse().getLocalDateIntervals();
        assertThat(endretIntervaller.size()).isEqualTo(1);
        assertThat(endretIntervaller.contains(periode2.getPeriode().toLocalDateInterval())).isTrue();
    }

    @Test
    void skal_gi_endring_dersom_endring_i_periode() {
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


        var endringerForMottakers = UtledTilkjentYtelseEndring.utledEndringer(resultat, originaltResultat);

        assertThat(endringerForMottakers.size()).isEqualTo(1);
        assertThat(endringerForMottakers.get(0).nøkkel()).isEqualTo(new UtledTilkjentYtelseEndring.MottakerNøkkel(brukerErMottaker, arbeidsgiver, null, aktivitetStatus, inntektskategori));
        var endretIntervaller = endringerForMottakers.get(0).tidslinjeMedEndringIYtelse().getLocalDateIntervals();
        assertThat(endretIntervaller.size()).isEqualTo(1);
        assertThat(endretIntervaller.contains(new LocalDateInterval(originalTom1.plusDays(1), tom1))).isTrue();
    }


    @Test
    void skal_gi_endring_dersom_endret_dagsats() {
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

        var endringerForMottakers = UtledTilkjentYtelseEndring.utledEndringer(resultat, originaltResultat);

        assertThat(endringerForMottakers.size()).isEqualTo(1);
        assertThat(endringerForMottakers.get(0).nøkkel()).isEqualTo(new UtledTilkjentYtelseEndring.MottakerNøkkel(brukerErMottaker, arbeidsgiver, null, aktivitetStatus, inntektskategori));
        var endretIntervaller = endringerForMottakers.get(0).tidslinjeMedEndringIYtelse().getLocalDateIntervals();
        assertThat(endretIntervaller.size()).isEqualTo(1);
        assertThat(endretIntervaller.contains(periode1.getPeriode().toLocalDateInterval())).isTrue();
    }

    @Test
    void skal_gi_endring_dersom_endret_utbetalingsgrad() {
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

        var endringerForMottakers = UtledTilkjentYtelseEndring.utledEndringer(resultat, originaltResultat);

        assertThat(endringerForMottakers.size()).isEqualTo(1);
        assertThat(endringerForMottakers.get(0).nøkkel()).isEqualTo(new UtledTilkjentYtelseEndring.MottakerNøkkel(brukerErMottaker, arbeidsgiver, null, aktivitetStatus, inntektskategori));
        var endretIntervaller = endringerForMottakers.get(0).tidslinjeMedEndringIYtelse().getLocalDateIntervals();
        assertThat(endretIntervaller.size()).isEqualTo(1);
        assertThat(endretIntervaller.contains(periode1.getPeriode().toLocalDateInterval())).isTrue();
    }

    @Test
    void skal_gi_endring_dersom_endret_feriepengerÅrsbeløp() {
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

        var endringerForMottakers = UtledTilkjentYtelseEndring.utledEndringer(resultat, originaltResultat);

        assertThat(endringerForMottakers.size()).isEqualTo(1);
        assertThat(endringerForMottakers.get(0).nøkkel()).isEqualTo(new UtledTilkjentYtelseEndring.MottakerNøkkel(brukerErMottaker, arbeidsgiver, null, aktivitetStatus, inntektskategori));
        var endretIntervaller = endringerForMottakers.get(0).tidslinjeMedEndringIYtelse().getLocalDateIntervals();
        assertThat(endretIntervaller.size()).isEqualTo(1);
        assertThat(endretIntervaller.contains(periode1.getPeriode().toLocalDateInterval())).isTrue();
    }


    @Test
    void skal_gi_to_endringer_dersom_endret_inntektskategori_en_for_tilkommet_og_en_for_bortfalt() {
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

        var endringerForMottakers = UtledTilkjentYtelseEndring.utledEndringer(resultat, originaltResultat);

        assertThat(endringerForMottakers.size()).isEqualTo(2);
        assertThat(endringerForMottakers.get(0).nøkkel()).isEqualTo(new UtledTilkjentYtelseEndring.MottakerNøkkel(brukerErMottaker, arbeidsgiver, null, aktivitetStatus, inntektskategori));
        var endretIntervaller = endringerForMottakers.get(0).tidslinjeMedEndringIYtelse().getLocalDateIntervals();
        assertThat(endretIntervaller.size()).isEqualTo(1);
        assertThat(endretIntervaller.contains(periode1.getPeriode().toLocalDateInterval())).isTrue();

        assertThat(endringerForMottakers.get(1).nøkkel()).isEqualTo(new UtledTilkjentYtelseEndring.MottakerNøkkel(brukerErMottaker, arbeidsgiver, null, aktivitetStatus, Inntektskategori.DAGPENGER));
        var endretIntervaller2 = endringerForMottakers.get(1).tidslinjeMedEndringIYtelse().getLocalDateIntervals();
        assertThat(endretIntervaller2.size()).isEqualTo(1);
        assertThat(endretIntervaller2.contains(periode1.getPeriode().toLocalDateInterval())).isTrue();
    }

    @Test
    void skal_gi_to_endringer_dersom_endret_aktivitetstatus_en_for_tilkommet_og_en_for_bortfalt() {
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

        var endringerForMottakers = UtledTilkjentYtelseEndring.utledEndringer(resultat, originaltResultat);

        assertThat(endringerForMottakers.size()).isEqualTo(2);
        assertThat(endringerForMottakers.get(0).nøkkel()).isEqualTo(new UtledTilkjentYtelseEndring.MottakerNøkkel(brukerErMottaker, arbeidsgiver, null, AktivitetStatus.IKKE_YRKESAKTIV, inntektskategori));
        var endretIntervaller = endringerForMottakers.get(0).tidslinjeMedEndringIYtelse().getLocalDateIntervals();
        assertThat(endretIntervaller.size()).isEqualTo(1);
        assertThat(endretIntervaller.contains(periode1.getPeriode().toLocalDateInterval())).isTrue();

        assertThat(endringerForMottakers.get(1).nøkkel()).isEqualTo(new UtledTilkjentYtelseEndring.MottakerNøkkel(brukerErMottaker, arbeidsgiver, null, aktivitetStatus, inntektskategori));
        var endretIntervaller2 = endringerForMottakers.get(1).tidslinjeMedEndringIYtelse().getLocalDateIntervals();
        assertThat(endretIntervaller2.size()).isEqualTo(1);
        assertThat(endretIntervaller2.contains(periode1.getPeriode().toLocalDateInterval())).isTrue();
    }


    @Test
    void skal_gi_endring_dersom_ny_aktivitet() {
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

        var endringerForMottakers = UtledTilkjentYtelseEndring.utledEndringer(resultat, originaltResultat);

        assertThat(endringerForMottakers.size()).isEqualTo(1);
        assertThat(endringerForMottakers.get(0).nøkkel()).isEqualTo(new UtledTilkjentYtelseEndring.MottakerNøkkel(brukerErMottaker, arbeidsgiver2, null, aktivitetStatus, inntektskategori));
        var endretIntervaller = endringerForMottakers.get(0).tidslinjeMedEndringIYtelse().getLocalDateIntervals();
        assertThat(endretIntervaller.size()).isEqualTo(1);
        assertThat(endretIntervaller.contains(periode1.getPeriode().toLocalDateInterval())).isTrue();
    }

    private static BeregningsresultatEntitet lagResultat() {
        var builder = BeregningsresultatEntitet.builder();
        var resultat = builder
            .medRegelInput("test")
            .medRegelSporing("test")
            .build();
        return resultat;
    }

    private static BeregningsresultatPeriode lagPeriode(LocalDate fom1, LocalDate tom1, BeregningsresultatEntitet resultat) {
        var periode1 = BeregningsresultatPeriode.builder()
            .medBeregningsresultatPeriodeFomOgTom(fom1, tom1).build(resultat);
        return periode1;
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
