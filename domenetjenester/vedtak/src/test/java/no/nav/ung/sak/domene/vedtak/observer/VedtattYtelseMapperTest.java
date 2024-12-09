package no.nav.ung.sak.domene.vedtak.observer;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import no.nav.abakus.vedtak.ytelse.v1.anvisning.Anvisning;
import no.nav.abakus.vedtak.ytelse.v1.anvisning.Inntektklasse;
import no.nav.ung.kodeverk.arbeidsforhold.AktivitetStatus;
import no.nav.ung.kodeverk.arbeidsforhold.Inntektskategori;
import no.nav.ung.kodeverk.opptjening.OpptjeningAktivitetType;
import no.nav.ung.sak.behandlingslager.behandling.beregning.BeregningsresultatAndel;
import no.nav.ung.sak.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.ung.sak.behandlingslager.behandling.beregning.BeregningsresultatPeriode;
import no.nav.ung.sak.domene.iay.modell.ArbeidsforholdReferanse;
import no.nav.ung.sak.typer.Arbeidsgiver;
import no.nav.ung.sak.typer.EksternArbeidsforholdRef;
import no.nav.ung.sak.typer.InternArbeidsforholdRef;

class VedtattYtelseMapperTest {

    public static final LocalDate SKJÆRINGSTIDSPUNKT = LocalDate.now();

    @Test
    void skal_mappe_ett_arbeidsforhold_med_full_utbetaling_til_arbeidsgiver_aggregert() {

        BeregningsresultatEntitet resultat = BeregningsresultatEntitet.builder()
            .medRegelInput("input")
            .medRegelSporing("regelsporing")
            .build();

        Arbeidsgiver arbeidsgiver = Arbeidsgiver.virksomhet("123566324");
        BeregningsresultatPeriode periode = BeregningsresultatPeriode.builder()
            .medBeregningsresultatPeriodeFomOgTom(SKJÆRINGSTIDSPUNKT, SKJÆRINGSTIDSPUNKT.plusDays(10))
            .build(resultat);
        InternArbeidsforholdRef arbeidsforholdRef = InternArbeidsforholdRef.nullRef();
        int dagsats = 500;
        fullRefusjon(arbeidsgiver, periode, arbeidsforholdRef, dagsats);

        List<Anvisning> anvisninger = VedtattYtelseMapper.mapAnvisninger(resultat);

        assertThat(anvisninger.size()).isEqualTo(1);
        assertThat(anvisninger.get(0).getAndeler().size()).isEqualTo(1);
        assertThat(anvisninger.get(0).getAndeler().get(0).getArbeidsgiverIdent().erOrganisasjon()).isTrue();
        assertThat(anvisninger.get(0).getAndeler().get(0).getArbeidsgiverIdent().ident()).isEqualTo(arbeidsgiver.getIdentifikator());
        assertThat(anvisninger.get(0).getAndeler().get(0).getArbeidsforholdId()).isEqualTo(null);
        assertThat(anvisninger.get(0).getAndeler().get(0).getInntektklasse()).isEqualTo(Inntektklasse.ARBEIDSTAKER);
        assertThat(anvisninger.get(0).getAndeler().get(0).getUtbetalingsgrad().getVerdi()).isEqualByComparingTo(BigDecimal.valueOf(100));
        assertThat(anvisninger.get(0).getAndeler().get(0).getDagsats().getVerdi()).isEqualByComparingTo(BigDecimal.valueOf(dagsats));
        assertThat(anvisninger.get(0).getAndeler().get(0).getRefusjonsgrad().getVerdi()).isEqualByComparingTo(BigDecimal.valueOf(100));
    }


    @Test
    void skal_mappe_dagpenger() {

        BeregningsresultatEntitet resultat = BeregningsresultatEntitet.builder()
            .medRegelInput("input")
            .medRegelSporing("regelsporing")
            .build();

        BeregningsresultatPeriode periode = BeregningsresultatPeriode.builder()
            .medBeregningsresultatPeriodeFomOgTom(SKJÆRINGSTIDSPUNKT, SKJÆRINGSTIDSPUNKT.plusDays(10))
            .build(resultat);
        InternArbeidsforholdRef arbeidsforholdRef = InternArbeidsforholdRef.nyRef();
        int dagsats = 500;
        BeregningsresultatAndel.builder()
            .medStillingsprosent(BigDecimal.valueOf(100))
            .medArbeidsforholdType(OpptjeningAktivitetType.DAGPENGER)
            .medDagsats(dagsats)
            .medDagsatsFraBg(dagsats)
            .medBrukerErMottaker(true)
            .medUtbetalingsgrad(BigDecimal.valueOf(100))
            .medInntektskategori(Inntektskategori.DAGPENGER)
            .buildFor(periode);

        List<Anvisning> anvisninger = VedtattYtelseMapper.mapAnvisninger(resultat);

        assertThat(anvisninger.size()).isEqualTo(1);
        assertThat(anvisninger.get(0).getAndeler().size()).isEqualTo(1);
        assertThat(anvisninger.get(0).getAndeler().get(0).getInntektklasse()).isEqualTo(Inntektklasse.DAGPENGER);
        assertThat(anvisninger.get(0).getAndeler().get(0).getUtbetalingsgrad().getVerdi()).isEqualByComparingTo(BigDecimal.valueOf(100));
        assertThat(anvisninger.get(0).getAndeler().get(0).getDagsats().getVerdi()).isEqualByComparingTo(BigDecimal.valueOf(dagsats));
        assertThat(anvisninger.get(0).getAndeler().get(0).getRefusjonsgrad().getVerdi()).isEqualByComparingTo(BigDecimal.ZERO);
    }


    @Test
    void skal_mappe_brukers_andel_og_frilans_med_samme_inntektskategori() {

        BeregningsresultatEntitet resultat = BeregningsresultatEntitet.builder()
            .medRegelInput("input")
            .medRegelSporing("regelsporing")
            .build();

        BeregningsresultatPeriode periode = BeregningsresultatPeriode.builder()
            .medBeregningsresultatPeriodeFomOgTom(SKJÆRINGSTIDSPUNKT, SKJÆRINGSTIDSPUNKT.plusDays(10))
            .build(resultat);
        int dagsats = 500;
        BeregningsresultatAndel.builder()
            .medStillingsprosent(BigDecimal.valueOf(100))
            .medDagsats(0)
            .medDagsatsFraBg(0)
            .medBrukerErMottaker(true)
            .medUtbetalingsgrad(BigDecimal.valueOf(100))
            .medAktivitetStatus(AktivitetStatus.BRUKERS_ANDEL)
            .medInntektskategori(Inntektskategori.ARBEIDSTAKER_UTEN_FERIEPENGER)
            .buildFor(periode);

        BeregningsresultatAndel.builder()
            .medStillingsprosent(BigDecimal.valueOf(100))
            .medDagsats(dagsats)
            .medDagsatsFraBg(dagsats)
            .medBrukerErMottaker(true)
            .medUtbetalingsgrad(BigDecimal.valueOf(50))
            .medAktivitetStatus(AktivitetStatus.FRILANSER)
            .medInntektskategori(Inntektskategori.ARBEIDSTAKER_UTEN_FERIEPENGER)
            .buildFor(periode);

        List<Anvisning> anvisninger = VedtattYtelseMapper.mapAnvisninger(resultat);

        assertThat(anvisninger.size()).isEqualTo(1);
        assertThat(anvisninger.get(0).getAndeler().size()).isEqualTo(2);


        var frilansandel = anvisninger.get(0).getAndeler().stream().filter(a  -> a.getUtbetalingsgrad().getVerdi().compareTo(BigDecimal.valueOf(50)) == 0).findFirst().orElseThrow();
        assertThat(frilansandel.getInntektklasse()).isEqualTo(Inntektklasse.ARBEIDSTAKER_UTEN_FERIEPENGER);
        assertThat(frilansandel.getDagsats().getVerdi()).isEqualByComparingTo(BigDecimal.valueOf(dagsats));
        assertThat(frilansandel.getRefusjonsgrad().getVerdi()).isEqualByComparingTo(BigDecimal.ZERO);

        var brukers_andel = anvisninger.get(0).getAndeler().stream().filter(a  -> a.getUtbetalingsgrad().getVerdi().compareTo(BigDecimal.valueOf(100)) == 0).findFirst().orElseThrow();
        assertThat(brukers_andel.getInntektklasse()).isEqualTo(Inntektklasse.ARBEIDSTAKER_UTEN_FERIEPENGER);
        assertThat(brukers_andel.getDagsats().getVerdi()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(brukers_andel.getRefusjonsgrad().getVerdi()).isEqualByComparingTo(BigDecimal.ZERO);
    }




    private void fullRefusjon(Arbeidsgiver arbeidsgiver, BeregningsresultatPeriode periode, InternArbeidsforholdRef arbeidsforholdRef, int dagsats) {
        BeregningsresultatAndel.builder()
            .medArbeidsforholdRef(arbeidsforholdRef)
            .medArbeidsgiver(arbeidsgiver)
            .medStillingsprosent(BigDecimal.valueOf(100))
            .medArbeidsforholdType(OpptjeningAktivitetType.ARBEID)
            .medDagsats(dagsats)
            .medDagsatsFraBg(dagsats)
            .medBrukerErMottaker(false)
            .medUtbetalingsgrad(BigDecimal.valueOf(100))
            .medInntektskategori(Inntektskategori.ARBEIDSTAKER)
            .buildFor(periode);

        BeregningsresultatAndel.builder()
            .medArbeidsforholdRef(arbeidsforholdRef)
            .medArbeidsgiver(arbeidsgiver)
            .medStillingsprosent(BigDecimal.valueOf(100))
            .medArbeidsforholdType(OpptjeningAktivitetType.ARBEID)
            .medDagsats(0)
            .medDagsatsFraBg(0)
            .medBrukerErMottaker(true)
            .medUtbetalingsgrad(BigDecimal.valueOf(100))
            .medInntektskategori(Inntektskategori.ARBEIDSTAKER)
            .buildFor(periode);
    }

}
