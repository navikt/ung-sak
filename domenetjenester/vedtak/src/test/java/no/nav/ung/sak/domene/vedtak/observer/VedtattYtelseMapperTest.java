package no.nav.ung.sak.domene.vedtak.observer;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import no.nav.abakus.vedtak.ytelse.v1.anvisning.Anvisning;
import no.nav.abakus.vedtak.ytelse.v1.anvisning.Inntektklasse;
import no.nav.k9.kodeverk.arbeidsforhold.AktivitetStatus;
import no.nav.k9.kodeverk.arbeidsforhold.Inntektskategori;
import no.nav.k9.kodeverk.opptjening.OpptjeningAktivitetType;
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
    void skal_mappe_ett_arbeidsforhold_med_full_utbetaling_til_bruker() {

        BeregningsresultatEntitet resultat = BeregningsresultatEntitet.builder()
            .medRegelInput("input")
            .medRegelSporing("regelsporing")
            .build();

        Arbeidsgiver arbeidsgiver = Arbeidsgiver.virksomhet("123566324");
        BeregningsresultatPeriode periode = BeregningsresultatPeriode.builder()
            .medBeregningsresultatPeriodeFomOgTom(SKJÆRINGSTIDSPUNKT, SKJÆRINGSTIDSPUNKT.plusDays(10))
            .build(resultat);
        InternArbeidsforholdRef arbeidsforholdRef = InternArbeidsforholdRef.nyRef();
        String eksternReferanse = "jifesjsioejf";
        int dagsats = 500;
        ArbeidsforholdReferanse arbeidsforholdReferanse = lagReferanser(arbeidsgiver, arbeidsforholdRef, eksternReferanse);
        BeregningsresultatAndel arbeidsforhold = BeregningsresultatAndel.builder()
            .medArbeidsforholdRef(arbeidsforholdRef)
            .medArbeidsgiver(arbeidsgiver)
            .medStillingsprosent(BigDecimal.valueOf(100))
            .medArbeidsforholdType(OpptjeningAktivitetType.ARBEID)
            .medDagsats(dagsats)
            .medDagsatsFraBg(dagsats)
            .medBrukerErMottaker(true)
            .medUtbetalingsgrad(BigDecimal.valueOf(100))
            .medInntektskategori(Inntektskategori.ARBEIDSTAKER)
            .buildFor(periode);

        List<Anvisning> anvisninger = VedtattYtelseMapper.mapAnvisninger(resultat, List.of(arbeidsforholdReferanse));

        assertThat(anvisninger.size()).isEqualTo(1);
        assertThat(anvisninger.get(0).getAndeler().size()).isEqualTo(1);
        assertThat(anvisninger.get(0).getAndeler().get(0).getArbeidsgiverIdent().erOrganisasjon()).isTrue();
        assertThat(anvisninger.get(0).getAndeler().get(0).getArbeidsgiverIdent().ident()).isEqualTo(arbeidsgiver.getIdentifikator());
        assertThat(anvisninger.get(0).getAndeler().get(0).getArbeidsforholdId()).isEqualTo(eksternReferanse);
        assertThat(anvisninger.get(0).getAndeler().get(0).getInntektklasse()).isEqualTo(Inntektklasse.ARBEIDSTAKER);
        assertThat(anvisninger.get(0).getAndeler().get(0).getUtbetalingsgrad().getVerdi()).isEqualByComparingTo(BigDecimal.valueOf(100));
        assertThat(anvisninger.get(0).getAndeler().get(0).getDagsats().getVerdi()).isEqualByComparingTo(BigDecimal.valueOf(dagsats));
        assertThat(anvisninger.get(0).getAndeler().get(0).getRefusjonsgrad().getVerdi()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void skal_mappe_to_arbeidsforhold_med_full_utbetaling_til_arbeidsgiver() {

        BeregningsresultatEntitet resultat = BeregningsresultatEntitet.builder()
            .medRegelInput("input")
            .medRegelSporing("regelsporing")
            .build();

        BeregningsresultatPeriode periode = BeregningsresultatPeriode.builder()
            .medBeregningsresultatPeriodeFomOgTom(SKJÆRINGSTIDSPUNKT, SKJÆRINGSTIDSPUNKT.plusDays(10))
            .build(resultat);
        InternArbeidsforholdRef arbeidsforholdRef1 = InternArbeidsforholdRef.nyRef();
        String eksternReferanse1 = "jifesjsioejf";
        int dagsats1 = 500;
        Arbeidsgiver arbeidsgiver1 = Arbeidsgiver.virksomhet("123566324");
        ArbeidsforholdReferanse arbeidsforholdReferanse1 = lagReferanser(arbeidsgiver1, arbeidsforholdRef1, eksternReferanse1);
        fullRefusjon(arbeidsgiver1, periode, arbeidsforholdRef1, dagsats1);

        InternArbeidsforholdRef arbeidsforholdRef2 = InternArbeidsforholdRef.nyRef();
        String eksternReferanse2 = "husefiuse";
        int dagsats2 = 200;
        Arbeidsgiver arbeidsgiver2 = Arbeidsgiver.virksomhet("463465235");
        ArbeidsforholdReferanse arbeidsforholdReferanse2 = lagReferanser(arbeidsgiver1, arbeidsforholdRef2, eksternReferanse2);
        fullRefusjon(arbeidsgiver2, periode, arbeidsforholdRef2, dagsats2);

        List<Anvisning> anvisninger = VedtattYtelseMapper.mapAnvisninger(resultat, List.of(arbeidsforholdReferanse1, arbeidsforholdReferanse2));

        assertThat(anvisninger.size()).isEqualTo(1);
        assertThat(anvisninger.get(0).getAndeler().size()).isEqualTo(2);
        assertThat(anvisninger.get(0).getAndeler().get(0).getArbeidsgiverIdent().erOrganisasjon()).isTrue();
        assertThat(anvisninger.get(0).getAndeler().get(0).getArbeidsgiverIdent().ident()).isEqualTo(arbeidsgiver1.getIdentifikator());
        assertThat(anvisninger.get(0).getAndeler().get(0).getArbeidsforholdId()).isEqualTo(eksternReferanse1);
        assertThat(anvisninger.get(0).getAndeler().get(0).getInntektklasse()).isEqualTo(Inntektklasse.ARBEIDSTAKER);
        assertThat(anvisninger.get(0).getAndeler().get(0).getUtbetalingsgrad().getVerdi()).isEqualByComparingTo(BigDecimal.valueOf(100));
        assertThat(anvisninger.get(0).getAndeler().get(0).getDagsats().getVerdi()).isEqualByComparingTo(BigDecimal.valueOf(dagsats1));
        assertThat(anvisninger.get(0).getAndeler().get(0).getRefusjonsgrad().getVerdi()).isEqualByComparingTo(BigDecimal.valueOf(100));

        assertThat(anvisninger.get(0).getAndeler().get(1).getArbeidsgiverIdent().erOrganisasjon()).isTrue();
        assertThat(anvisninger.get(0).getAndeler().get(1).getArbeidsgiverIdent().ident()).isEqualTo(arbeidsgiver2.getIdentifikator());
        assertThat(anvisninger.get(0).getAndeler().get(1).getArbeidsforholdId()).isEqualTo(eksternReferanse2);
        assertThat(anvisninger.get(0).getAndeler().get(0).getInntektklasse()).isEqualTo(Inntektklasse.ARBEIDSTAKER);
        assertThat(anvisninger.get(0).getAndeler().get(1).getUtbetalingsgrad().getVerdi()).isEqualByComparingTo(BigDecimal.valueOf(100));
        assertThat(anvisninger.get(0).getAndeler().get(1).getDagsats().getVerdi()).isEqualByComparingTo(BigDecimal.valueOf(dagsats2));
        assertThat(anvisninger.get(0).getAndeler().get(1).getRefusjonsgrad().getVerdi()).isEqualByComparingTo(BigDecimal.valueOf(100));
    }

    private ArbeidsforholdReferanse lagReferanser(Arbeidsgiver arbeidsgiver1, InternArbeidsforholdRef arbeidsforholdRef2, String eksternReferanse2) {
        return new ArbeidsforholdReferanse(arbeidsgiver1, arbeidsforholdRef2, EksternArbeidsforholdRef.ref(eksternReferanse2));
    }

    @Test
    void skal_mappe_ett_arbeidsforhold_med_full_utbetaling_til_arbeidsgiver() {

        BeregningsresultatEntitet resultat = BeregningsresultatEntitet.builder()
            .medRegelInput("input")
            .medRegelSporing("regelsporing")
            .build();

        Arbeidsgiver arbeidsgiver = Arbeidsgiver.virksomhet("123566324");
        BeregningsresultatPeriode periode = BeregningsresultatPeriode.builder()
            .medBeregningsresultatPeriodeFomOgTom(SKJÆRINGSTIDSPUNKT, SKJÆRINGSTIDSPUNKT.plusDays(10))
            .build(resultat);
        InternArbeidsforholdRef arbeidsforholdRef = InternArbeidsforholdRef.nyRef();
        String eksternReferanse = "jifesjsioejf";
        int dagsats = 500;
        ArbeidsforholdReferanse arbeidsforholdReferanse = lagReferanser(arbeidsgiver, arbeidsforholdRef, eksternReferanse);
        fullRefusjon(arbeidsgiver, periode, arbeidsforholdRef, dagsats);

        List<Anvisning> anvisninger = VedtattYtelseMapper.mapAnvisninger(resultat, List.of(arbeidsforholdReferanse));

        assertThat(anvisninger.size()).isEqualTo(1);
        assertThat(anvisninger.get(0).getAndeler().size()).isEqualTo(1);
        assertThat(anvisninger.get(0).getAndeler().get(0).getArbeidsgiverIdent().erOrganisasjon()).isTrue();
        assertThat(anvisninger.get(0).getAndeler().get(0).getArbeidsgiverIdent().ident()).isEqualTo(arbeidsgiver.getIdentifikator());
        assertThat(anvisninger.get(0).getAndeler().get(0).getArbeidsforholdId()).isEqualTo(eksternReferanse);
        assertThat(anvisninger.get(0).getAndeler().get(0).getInntektklasse()).isEqualTo(Inntektklasse.ARBEIDSTAKER);
        assertThat(anvisninger.get(0).getAndeler().get(0).getUtbetalingsgrad().getVerdi()).isEqualByComparingTo(BigDecimal.valueOf(100));
        assertThat(anvisninger.get(0).getAndeler().get(0).getDagsats().getVerdi()).isEqualByComparingTo(BigDecimal.valueOf(dagsats));
        assertThat(anvisninger.get(0).getAndeler().get(0).getRefusjonsgrad().getVerdi()).isEqualByComparingTo(BigDecimal.valueOf(100));
    }


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

        List<Anvisning> anvisninger = VedtattYtelseMapper.mapAnvisninger(resultat, List.of());

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

        List<Anvisning> anvisninger = VedtattYtelseMapper.mapAnvisninger(resultat, List.of());

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

        List<Anvisning> anvisninger = VedtattYtelseMapper.mapAnvisninger(resultat, List.of());

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
