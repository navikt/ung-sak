package no.nav.folketrygdloven.beregningsgrunnlag.rest.fakta;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Test;

import no.nav.folketrygdloven.beregningsgrunnlag.gradering.AktivitetGradering;
import no.nav.folketrygdloven.beregningsgrunnlag.gradering.AndelGradering;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BGAndelArbeidsforhold;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPeriode;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPrStatusOgAndel;
import no.nav.foreldrepenger.domene.iay.modell.Inntektsmelding;
import no.nav.foreldrepenger.domene.iay.modell.InntektsmeldingBuilder;
import no.nav.k9.kodeverk.arbeidsforhold.AktivitetStatus;
import no.nav.k9.sak.kontrakt.beregningsgrunnlag.BeregningsgrunnlagArbeidsforholdDto;
import no.nav.k9.sak.kontrakt.beregningsgrunnlag.FaktaOmBeregningAndelDto;
import no.nav.k9.sak.kontrakt.beregningsgrunnlag.FordelBeregningsgrunnlagAndelDto;
import no.nav.k9.sak.typer.Arbeidsgiver;
import no.nav.k9.sak.typer.InternArbeidsforholdRef;
import no.nav.vedtak.konfig.Tid;
import no.nav.vedtak.util.Tuple;

public class RefusjonDtoTjenesteImplTest {

    private static final LocalDate SKJÆRINGSTIDSPUNKT_OPPTJENING = LocalDate.of(2018, Month.MAY, 10);

    private final static InternArbeidsforholdRef ARB_ID = InternArbeidsforholdRef.nyRef();
    private final static String ORGNR = "123456780";
    private final static Arbeidsgiver ARBEIDSGIVER = Arbeidsgiver.virksomhet(ORGNR);
    private final static String ORGNR_2 = "3242521";
    private final static Arbeidsgiver ARBEIDSGIVER2 = Arbeidsgiver.virksomhet(ORGNR_2);
    private static final BigDecimal GRUNNBELØP = BigDecimal.valueOf(100_000);
    private static final BigDecimal SEKS_G = GRUNNBELØP.multiply(BigDecimal.valueOf(6));


    @Test
    public void skal_ikkje_kunne_endre_refusjon_for_andel_uten_gradering_og_uten_refusjon() {
        //Arrange
        BeregningsgrunnlagEntitet bg = BeregningsgrunnlagEntitet.builder()
            .medSkjæringstidspunkt(SKJÆRINGSTIDSPUNKT_OPPTJENING)
            .medGrunnbeløp(GRUNNBELØP)
            .build();
        BeregningsgrunnlagPeriode periode = BeregningsgrunnlagPeriode.builder()
            .medBeregningsgrunnlagPeriode(SKJÆRINGSTIDSPUNKT_OPPTJENING, null)
            .build(bg);
        BeregningsgrunnlagPrStatusOgAndel andel = BeregningsgrunnlagPrStatusOgAndel.builder()
            .medAktivitetStatus(AktivitetStatus.ARBEIDSTAKER)
            .medBGAndelArbeidsforhold(BGAndelArbeidsforhold.builder().medArbeidsgiver(ARBEIDSGIVER))
            .build(periode);
        BeregningsgrunnlagPrStatusOgAndel.builder()
            .medAktivitetStatus(AktivitetStatus.ARBEIDSTAKER)
            .medBGAndelArbeidsforhold(BGAndelArbeidsforhold.builder().medArbeidsgiver(ARBEIDSGIVER2)
            .medRefusjonskravPrÅr(SEKS_G.add(BigDecimal.valueOf(100))))
        .build(periode);

        List<Inntektsmelding> inntektsmeldinger = List.of(InntektsmeldingBuilder.builder()
            .medRefusjon(SEKS_G.add(BigDecimal.valueOf(100)))
            .medArbeidsgiver(ARBEIDSGIVER2)
            .build());

        // Act
        boolean skalKunneEndreRefusjon = RefusjonDtoTjeneste.skalKunneEndreRefusjon(andel, AktivitetGradering.INGEN_GRADERING, inntektsmeldinger);

        // Assert
        assertThat(skalKunneEndreRefusjon).isFalse();
    }

    @Test
    public void skal_ikkje_kunne_endre_refusjon_for_andel_med_gradering_og_med_refusjon() {
        //Arrange
        BeregningsgrunnlagEntitet bg = BeregningsgrunnlagEntitet.builder()
            .medSkjæringstidspunkt(SKJÆRINGSTIDSPUNKT_OPPTJENING)
            .medGrunnbeløp(GRUNNBELØP)
            .build();
        BeregningsgrunnlagPeriode periode = BeregningsgrunnlagPeriode.builder()
            .medBeregningsgrunnlagPeriode(SKJÆRINGSTIDSPUNKT_OPPTJENING, null)
            .build(bg);
        BigDecimal refPrMnd = BigDecimal.valueOf(100);
        BeregningsgrunnlagPrStatusOgAndel andel = BeregningsgrunnlagPrStatusOgAndel.builder()
            .medAktivitetStatus(AktivitetStatus.ARBEIDSTAKER)
            .medBGAndelArbeidsforhold(BGAndelArbeidsforhold.builder().medArbeidsgiver(ARBEIDSGIVER)
                .medRefusjonskravPrÅr(refPrMnd.multiply(BigDecimal.valueOf(12))))
            .build(periode);
        BeregningsgrunnlagPrStatusOgAndel.builder()
            .medAktivitetStatus(AktivitetStatus.ARBEIDSTAKER)
            .medBGAndelArbeidsforhold(BGAndelArbeidsforhold.builder().medArbeidsgiver(ARBEIDSGIVER2)
                .medRefusjonskravPrÅr(SEKS_G.add(BigDecimal.valueOf(100))))
            .build(periode);

        Inntektsmelding im = InntektsmeldingBuilder.builder()
            .medRefusjon(refPrMnd.multiply(BigDecimal.valueOf(12)))
            .medArbeidsgiver(ARBEIDSGIVER)
            .build();
        Inntektsmelding im2 = InntektsmeldingBuilder.builder()
            .medRefusjon(SEKS_G.add(BigDecimal.valueOf(100)))
            .medArbeidsgiver(ARBEIDSGIVER2)
            .build();
        List<Inntektsmelding> inntektsmeldinger = List.of(im, im2);

        // Act
        boolean skalKunneEndreRefusjon = RefusjonDtoTjeneste.skalKunneEndreRefusjon(andel, AktivitetGradering.INGEN_GRADERING, inntektsmeldinger);

        // Assert
        assertThat(skalKunneEndreRefusjon).isFalse();
    }

    @Test
    public void skal_ikkje_kunne_endre_refusjon_for_andel_med_gradering_og_uten_refusjon_totalrefusjon_under_6G() {
        //Arrange
        BeregningsgrunnlagEntitet bg = BeregningsgrunnlagEntitet.builder()
            .medSkjæringstidspunkt(SKJÆRINGSTIDSPUNKT_OPPTJENING)
            .medGrunnbeløp(GRUNNBELØP)
            .build();
        BeregningsgrunnlagPeriode periode = BeregningsgrunnlagPeriode.builder()
            .medBeregningsgrunnlagPeriode(SKJÆRINGSTIDSPUNKT_OPPTJENING, null)
            .build(bg);
        BeregningsgrunnlagPrStatusOgAndel andel = BeregningsgrunnlagPrStatusOgAndel.builder()
            .medAktivitetStatus(AktivitetStatus.ARBEIDSTAKER)
            .medBGAndelArbeidsforhold(BGAndelArbeidsforhold.builder().medArbeidsgiver(ARBEIDSGIVER)
                .medRefusjonskravPrÅr(BigDecimal.ZERO))
            .build(periode);
        BeregningsgrunnlagPrStatusOgAndel.builder()
            .medAktivitetStatus(AktivitetStatus.ARBEIDSTAKER)
            .medBGAndelArbeidsforhold(BGAndelArbeidsforhold.builder().medArbeidsgiver(ARBEIDSGIVER2)
                .medRefusjonskravPrÅr(SEKS_G.subtract(BigDecimal.valueOf(100))))
            .build(periode);

        Inntektsmelding im = InntektsmeldingBuilder.builder()
            .medRefusjon(BigDecimal.ZERO)
            .medArbeidsgiver(ARBEIDSGIVER)
            .build();
        Inntektsmelding im2 = InntektsmeldingBuilder.builder()
            .medRefusjon(SEKS_G.subtract(BigDecimal.valueOf(100)))
            .medArbeidsgiver(ARBEIDSGIVER2)
            .build();
        List<Inntektsmelding> inntektsmeldinger = List.of(im, im2);

        // Act
        boolean skalKunneEndreRefusjon = RefusjonDtoTjeneste.skalKunneEndreRefusjon(andel, AktivitetGradering.INGEN_GRADERING, inntektsmeldinger);

        // Assert
        assertThat(skalKunneEndreRefusjon).isFalse();
    }

    @Test
    public void skal_kunne_endre_refusjon_for_andel_med_gradering_og_uten_refusjon_totalrefusjon_lik_6G() {
        //Arrange
        BeregningsgrunnlagEntitet bg = BeregningsgrunnlagEntitet.builder()
            .medSkjæringstidspunkt(SKJÆRINGSTIDSPUNKT_OPPTJENING)
            .medGrunnbeløp(GRUNNBELØP)
            .build();
        BeregningsgrunnlagPeriode periode = BeregningsgrunnlagPeriode.builder()
            .medBeregningsgrunnlagPeriode(SKJÆRINGSTIDSPUNKT_OPPTJENING, null)
            .build(bg);
        BeregningsgrunnlagPrStatusOgAndel andel = BeregningsgrunnlagPrStatusOgAndel.builder()
            .medAktivitetStatus(AktivitetStatus.ARBEIDSTAKER)
            .medBGAndelArbeidsforhold(BGAndelArbeidsforhold.builder().medArbeidsgiver(ARBEIDSGIVER)
                .medRefusjonskravPrÅr(BigDecimal.ZERO))
            .build(periode);
        BeregningsgrunnlagPrStatusOgAndel.builder()
            .medAktivitetStatus(AktivitetStatus.ARBEIDSTAKER)
            .medBGAndelArbeidsforhold(BGAndelArbeidsforhold.builder().medArbeidsgiver(ARBEIDSGIVER2)
                .medRefusjonskravPrÅr(SEKS_G))
            .build(periode);

        AndelGradering andelGradering = AndelGradering.builder()
            .medArbeidsgiver(ARBEIDSGIVER)
            .medArbeidsforholdRef(ARB_ID)
            .medStatus(AktivitetStatus.ARBEIDSTAKER)
            .leggTilGradering(new AndelGradering.Gradering(SKJÆRINGSTIDSPUNKT_OPPTJENING, Tid.TIDENES_ENDE, BigDecimal.TEN))
            .build();

        Inntektsmelding im = InntektsmeldingBuilder.builder()
            .medRefusjon(BigDecimal.ZERO)
            .medArbeidsgiver(ARBEIDSGIVER)
            .build();
        Inntektsmelding im2 = InntektsmeldingBuilder.builder()
            .medRefusjon(SEKS_G)
            .medArbeidsgiver(ARBEIDSGIVER2)
            .build();
        List<Inntektsmelding> inntektsmeldinger = List.of(im, im2);


        // Act
        boolean skalKunneEndreRefusjon = RefusjonDtoTjeneste.skalKunneEndreRefusjon(andel,
            new AktivitetGradering(andelGradering), inntektsmeldinger);

        // Assert
        assertThat(skalKunneEndreRefusjon).isTrue();
    }

    @Test
    public void skal_kunne_endre_refusjon_for_andel_med_gradering_og_uten_refusjon_totalrefusjon_over_6G() {
        //Arrange
        BeregningsgrunnlagEntitet bg = BeregningsgrunnlagEntitet.builder()
            .medSkjæringstidspunkt(SKJÆRINGSTIDSPUNKT_OPPTJENING)
            .medGrunnbeløp(GRUNNBELØP)
            .build();
        BeregningsgrunnlagPeriode periode = BeregningsgrunnlagPeriode.builder()
            .medBeregningsgrunnlagPeriode(SKJÆRINGSTIDSPUNKT_OPPTJENING, null)
            .build(bg);
        BeregningsgrunnlagPrStatusOgAndel andel = BeregningsgrunnlagPrStatusOgAndel.builder()
            .medAktivitetStatus(AktivitetStatus.ARBEIDSTAKER)
            .medBGAndelArbeidsforhold(BGAndelArbeidsforhold.builder().medArbeidsgiver(ARBEIDSGIVER)
                .medRefusjonskravPrÅr(BigDecimal.ZERO))
            .build(periode);
        BeregningsgrunnlagPrStatusOgAndel.builder()
            .medAktivitetStatus(AktivitetStatus.ARBEIDSTAKER)
            .medBGAndelArbeidsforhold(BGAndelArbeidsforhold.builder().medArbeidsgiver(ARBEIDSGIVER2)
                .medRefusjonskravPrÅr(SEKS_G.add(BigDecimal.valueOf(100))))
            .build(periode);

        AndelGradering andelGradering = AndelGradering.builder()
            .medArbeidsgiver(ARBEIDSGIVER)
            .medArbeidsforholdRef(ARB_ID)
            .medStatus(AktivitetStatus.ARBEIDSTAKER)
            .leggTilGradering(new AndelGradering.Gradering(SKJÆRINGSTIDSPUNKT_OPPTJENING, Tid.TIDENES_ENDE, BigDecimal.TEN))
            .build();

        Inntektsmelding im = InntektsmeldingBuilder.builder()
            .medRefusjon(BigDecimal.ZERO)
            .medArbeidsgiver(ARBEIDSGIVER)
            .build();
        Inntektsmelding im2 = InntektsmeldingBuilder.builder()
            .medRefusjon(SEKS_G.add(BigDecimal.valueOf(100)))
            .medArbeidsgiver(ARBEIDSGIVER2)
            .build();
        List<Inntektsmelding> inntektsmeldinger = List.of(im, im2);

        // Act
        boolean skalKunneEndreRefusjon = RefusjonDtoTjeneste.skalKunneEndreRefusjon(andel,
            new AktivitetGradering(andelGradering), inntektsmeldinger);

        // Assert
        assertThat(skalKunneEndreRefusjon).isTrue();
    }




    @Test
    public void skal_slå_sammen_refusjon_for_andeler_i_samme_arbeidsforhold() {
        Integer refusjonskrav1 = 10000;
        Integer refusjonskrav2 = 15000;
        Integer refusjonskrav3 = 20000;
        List<Tuple<Boolean, Integer>> refusjon = List.of(new Tuple<>(true, refusjonskrav1), new Tuple<>(false, refusjonskrav2), new Tuple<>(false, refusjonskrav3));
        List<FordelBeregningsgrunnlagAndelDto> andeler = lagAndeler(refusjon, 50000);
        RefusjonDtoTjeneste.slåSammenRefusjonForAndelerISammeArbeidsforhold(andeler);

        FordelBeregningsgrunnlagAndelDto andelSomIkkjeErLagtTilManuelt = andeler.stream().filter(a -> !a.getLagtTilAvSaksbehandler()).findFirst().get();
        assertThat(andelSomIkkjeErLagtTilManuelt.getRefusjonskravPrAar()).isEqualByComparingTo(BigDecimal.valueOf(refusjonskrav1+refusjonskrav2+refusjonskrav3));
        FordelBeregningsgrunnlagAndelDto andelSomErLagtTilManuelt = andeler.stream().filter(FaktaOmBeregningAndelDto::getLagtTilAvSaksbehandler).findFirst().get();
        assertThat(andelSomErLagtTilManuelt.getRefusjonskravPrAar()).isNull();
    }

    private List<FordelBeregningsgrunnlagAndelDto> lagAndeler(List<Tuple<Boolean, Integer>> refusjonskrav, Integer refusjonFraInntektsmelding) {
        BeregningsgrunnlagArbeidsforholdDto arbeidsforholdDto = new BeregningsgrunnlagArbeidsforholdDto();
        arbeidsforholdDto.setArbeidsgiverId("432423423");
        return refusjonskrav.stream().map(tuple -> {
            FordelBeregningsgrunnlagAndelDto andel = new FordelBeregningsgrunnlagAndelDto(new FaktaOmBeregningAndelDto());
            andel.setArbeidsforhold(arbeidsforholdDto);
            andel.setLagtTilAvSaksbehandler(tuple.getElement1());
            andel.setRefusjonskravPrAar(BigDecimal.valueOf(tuple.getElement2()));
            if (tuple.getElement1()) {
                andel.setRefusjonskravFraInntektsmeldingPrÅr(BigDecimal.valueOf(refusjonFraInntektsmelding));
            }
            return andel;
        }).collect(Collectors.toList());
    }
}
