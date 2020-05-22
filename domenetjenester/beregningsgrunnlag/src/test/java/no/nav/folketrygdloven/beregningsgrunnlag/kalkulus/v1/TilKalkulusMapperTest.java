package no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.v1;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.Test;

import no.nav.k9.sak.domene.arbeidsforhold.impl.SakInntektsmeldinger;
import no.nav.k9.sak.domene.iay.modell.InntektsmeldingBuilder;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.typer.Arbeidsgiver;
import no.nav.k9.sak.typer.EksternArbeidsforholdRef;
import no.nav.k9.sak.typer.InternArbeidsforholdRef;
import no.nav.k9.sak.typer.PeriodeAndel;
import no.nav.k9.sak.typer.Saksnummer;

public class TilKalkulusMapperTest {

    private DatoIntervallEntitet periode1 = DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now().minusDays(30), LocalDate.now().minusDays(25));
    private DatoIntervallEntitet periode2 = DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now(), LocalDate.now().plusDays(10));

    @Test
    public void skal_filtrere_ut_inntektsmeldinger_som_ikke_gjelder_for_vilkårs_periode() {
        var sakInntektsmeldinger = new SakInntektsmeldinger(new Saksnummer("123123123"));
        var inntektsmelding1 = InntektsmeldingBuilder.builder()
            .medArbeidsgiver(Arbeidsgiver.virksomhet("000000000"))
            .medArbeidsforholdId(InternArbeidsforholdRef.nullRef())
            .medArbeidsforholdId(EksternArbeidsforholdRef.nullRef())
            .medInnsendingstidspunkt(LocalDateTime.now().minusDays(10))
            .medBeløp(BigDecimal.TEN)
            .medKanalreferanse("AR123")
            .medOppgittFravær(List.of(new PeriodeAndel(LocalDate.now().minusDays(30), LocalDate.now().minusDays(25))))
            .medRefusjon(BigDecimal.TEN)
            .build();
        var inntektsmelding2 = InntektsmeldingBuilder.builder()
            .medArbeidsgiver(Arbeidsgiver.virksomhet("000000001"))
            .medArbeidsforholdId(InternArbeidsforholdRef.nullRef())
            .medArbeidsforholdId(EksternArbeidsforholdRef.nullRef())
            .medInnsendingstidspunkt(LocalDateTime.now().minusDays(9))
            .medBeløp(BigDecimal.ONE)
            .medKanalreferanse("AR124")
            .medOppgittFravær(List.of(new PeriodeAndel(LocalDate.now().minusDays(26), LocalDate.now().minusDays(25))))
            .medRefusjon(BigDecimal.ONE)
            .build();
        var inntektsmelding3 = InntektsmeldingBuilder.builder()
            .medArbeidsgiver(Arbeidsgiver.virksomhet("000000000"))
            .medArbeidsforholdId(InternArbeidsforholdRef.nullRef())
            .medArbeidsforholdId(EksternArbeidsforholdRef.nullRef())
            .medInnsendingstidspunkt(LocalDateTime.now().minusDays(9))
            .medBeløp(BigDecimal.ONE)
            .medKanalreferanse("AR125")
            .medOppgittFravær(List.of(new PeriodeAndel(LocalDate.now().minusDays(30), LocalDate.now().minusDays(25))))
            .medRefusjon(BigDecimal.ONE)
            .build();
        var inntektsmelding4 = InntektsmeldingBuilder.builder()
            .medArbeidsgiver(Arbeidsgiver.virksomhet("000000000"))
            .medArbeidsforholdId(InternArbeidsforholdRef.nullRef())
            .medArbeidsforholdId(EksternArbeidsforholdRef.nullRef())
            .medInnsendingstidspunkt(LocalDateTime.now().minusDays(10))
            .medBeløp(BigDecimal.ONE)
            .medKanalreferanse("AR126")
            .medOppgittFravær(List.of(new PeriodeAndel(LocalDate.now(), LocalDate.now().plusDays(5))))
            .medRefusjon(BigDecimal.ONE)
            .build();

        sakInntektsmeldinger.leggTil(1L, UUID.randomUUID(), LocalDateTime.now(), inntektsmelding1);
        sakInntektsmeldinger.leggTil(2L, UUID.randomUUID(), LocalDateTime.now(), inntektsmelding2);
        sakInntektsmeldinger.leggTil(3L, UUID.randomUUID(), LocalDateTime.now(), inntektsmelding3);
        sakInntektsmeldinger.leggTil(4L, UUID.randomUUID(), LocalDateTime.now(), inntektsmelding4);

        var relevanteInntektsmeldinger = TilKalkulusMapper.utledInntektsmeldingerSomGjelderForPeriode(sakInntektsmeldinger, periode1);

        assertThat(relevanteInntektsmeldinger).containsExactlyInAnyOrder(inntektsmelding2, inntektsmelding3);

        relevanteInntektsmeldinger = TilKalkulusMapper.utledInntektsmeldingerSomGjelderForPeriode(sakInntektsmeldinger, periode2);
        assertThat(relevanteInntektsmeldinger).containsExactlyInAnyOrder(inntektsmelding4);
    }

    @Test
    public void skal_filtrere_ut_inntektsmeldinger_som_ikke_gjelder_for_vilkårs_periode_2() {
        var sakInntektsmeldinger = new SakInntektsmeldinger(new Saksnummer("123123123"));
        var inntektsmelding1 = InntektsmeldingBuilder.builder()
            .medArbeidsgiver(Arbeidsgiver.virksomhet("000000000"))
            .medArbeidsforholdId(InternArbeidsforholdRef.nyRef())
            .medArbeidsforholdId(EksternArbeidsforholdRef.ref("123"))
            .medInnsendingstidspunkt(LocalDateTime.now().minusDays(10))
            .medBeløp(BigDecimal.TEN)
            .medKanalreferanse("AR123")
            .medOppgittFravær(List.of(new PeriodeAndel(LocalDate.now().minusDays(30), LocalDate.now().minusDays(25))))
            .medRefusjon(BigDecimal.TEN)
            .build();
        var inntektsmelding2 = InntektsmeldingBuilder.builder()
            .medArbeidsgiver(Arbeidsgiver.virksomhet("000000001"))
            .medArbeidsforholdId(InternArbeidsforholdRef.nyRef())
            .medArbeidsforholdId(EksternArbeidsforholdRef.ref("123"))
            .medInnsendingstidspunkt(LocalDateTime.now().minusDays(9))
            .medBeløp(BigDecimal.ONE)
            .medKanalreferanse("AR124")
            .medOppgittFravær(List.of(new PeriodeAndel(LocalDate.now().minusDays(26), LocalDate.now().minusDays(25))))
            .medRefusjon(BigDecimal.ONE)
            .build();
        var inntektsmelding3 = InntektsmeldingBuilder.builder()
            .medArbeidsgiver(Arbeidsgiver.virksomhet("000000000"))
            .medArbeidsforholdId(InternArbeidsforholdRef.nyRef())
            .medArbeidsforholdId(EksternArbeidsforholdRef.ref("123"))
            .medInnsendingstidspunkt(LocalDateTime.now().minusDays(9))
            .medBeløp(BigDecimal.ONE)
            .medKanalreferanse("AR125")
            .medOppgittFravær(List.of(new PeriodeAndel(LocalDate.now().minusDays(30), LocalDate.now().minusDays(25))))
            .medRefusjon(BigDecimal.ONE)
            .build();
        var inntektsmelding4 = InntektsmeldingBuilder.builder()
            .medArbeidsgiver(Arbeidsgiver.virksomhet("000000000"))
            .medArbeidsforholdId(InternArbeidsforholdRef.nyRef())
            .medArbeidsforholdId(EksternArbeidsforholdRef.ref("123"))
            .medInnsendingstidspunkt(LocalDateTime.now().minusDays(10))
            .medBeløp(BigDecimal.ONE)
            .medKanalreferanse("AR126")
            .medOppgittFravær(List.of(new PeriodeAndel(LocalDate.now(), LocalDate.now().plusDays(5))))
            .medRefusjon(BigDecimal.ONE)
            .build();

        sakInntektsmeldinger.leggTil(1L, UUID.randomUUID(), LocalDateTime.now(), inntektsmelding1);
        sakInntektsmeldinger.leggTil(2L, UUID.randomUUID(), LocalDateTime.now(), inntektsmelding2);
        sakInntektsmeldinger.leggTil(3L, UUID.randomUUID(), LocalDateTime.now(), inntektsmelding3);
        sakInntektsmeldinger.leggTil(4L, UUID.randomUUID(), LocalDateTime.now(), inntektsmelding4);

        var relevanteInntektsmeldinger = TilKalkulusMapper.utledInntektsmeldingerSomGjelderForPeriode(sakInntektsmeldinger, periode1);

        assertThat(relevanteInntektsmeldinger).containsExactlyInAnyOrder(inntektsmelding1, inntektsmelding2, inntektsmelding3);

        relevanteInntektsmeldinger = TilKalkulusMapper.utledInntektsmeldingerSomGjelderForPeriode(sakInntektsmeldinger, periode2);
        assertThat(relevanteInntektsmeldinger).containsExactlyInAnyOrder(inntektsmelding4);
    }
}
