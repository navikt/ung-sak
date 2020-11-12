package no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.v1;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.Test;

import no.nav.k9.sak.domene.iay.modell.Inntektsmelding;
import no.nav.k9.sak.domene.iay.modell.InntektsmeldingBuilder;
import no.nav.k9.sak.domene.iay.modell.PeriodeAndel;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.typer.Arbeidsgiver;
import no.nav.k9.sak.typer.EksternArbeidsforholdRef;
import no.nav.k9.sak.typer.InternArbeidsforholdRef;

public class TilKalkulusMapperTest {

    private DatoIntervallEntitet periode1 = DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now().minusDays(30), LocalDate.now().minusDays(25));
    private DatoIntervallEntitet periode2 = DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now(), LocalDate.now().plusDays(10));
    private DatoIntervallEntitet periode3 = DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now(), LocalDate.now().plusDays(4));
    private DatoIntervallEntitet periode4 = DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now(), LocalDate.now().plusDays(8));

    @Test
    public void skal_filtrere_ut_inntektsmeldinger_som_ikke_gjelder_for_vilkårs_periode() {
        var inntektsmelding1 = InntektsmeldingBuilder.builder()
            .medArbeidsgiver(Arbeidsgiver.virksomhet("000000000"))
            .medArbeidsforholdId(InternArbeidsforholdRef.nullRef())
            .medArbeidsforholdId(EksternArbeidsforholdRef.nullRef())
            .medJournalpostId("1")
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
            .medJournalpostId("2")
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
            .medJournalpostId("3")
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
            .medJournalpostId("4")
            .medBeløp(BigDecimal.ONE)
            .medKanalreferanse("AR126")
            .medOppgittFravær(List.of(new PeriodeAndel(LocalDate.now(), LocalDate.now().plusDays(5))))
            .medRefusjon(BigDecimal.ONE)
            .build();

        var sakInntektsmeldinger = Set.of(inntektsmelding1, inntektsmelding2, inntektsmelding3, inntektsmelding4);
        var sortedInntektsmeldinger = sakInntektsmeldinger.stream().sorted(Inntektsmelding.COMP_REKKEFØLGE).collect(Collectors.toList());

        //assertThat(sortedInntektsmeldinger).has(sortedInntektsmeldinger);

        var relevanteInntektsmeldinger = TilKalkulusMapper.utledInntektsmeldingerSomGjelderForPeriode(sakInntektsmeldinger, periode1);

        assertThat(relevanteInntektsmeldinger).containsSequence(inntektsmelding2, inntektsmelding3);

        relevanteInntektsmeldinger = TilKalkulusMapper.utledInntektsmeldingerSomGjelderForPeriode(sakInntektsmeldinger, periode2);
        assertThat(relevanteInntektsmeldinger).containsSequence(inntektsmelding4);
    }

    @Test
    public void skal_filtrere_ut_inntektsmeldinger_som_ikke_gjelder_for_vilkårs_periode_2() {
        var inntektsmelding1 = InntektsmeldingBuilder.builder()
            .medArbeidsgiver(Arbeidsgiver.virksomhet("000000000"))
            .medArbeidsforholdId(InternArbeidsforholdRef.nyRef())
            .medArbeidsforholdId(EksternArbeidsforholdRef.ref("123"))
            .medInnsendingstidspunkt(LocalDateTime.now().minusDays(10))
            .medJournalpostId("1")
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
            .medJournalpostId("2")
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
            .medJournalpostId("3")
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
            .medJournalpostId("4")
            .medBeløp(BigDecimal.ONE)
            .medKanalreferanse("AR126")
            .medOppgittFravær(List.of(new PeriodeAndel(LocalDate.now(), LocalDate.now().plusDays(5))))
            .medRefusjon(BigDecimal.ONE)
            .build();

        var sakInntektsmeldinger = Set.of(inntektsmelding1, inntektsmelding2, inntektsmelding3, inntektsmelding4);

        var relevanteInntektsmeldinger = TilKalkulusMapper.utledInntektsmeldingerSomGjelderForPeriode(sakInntektsmeldinger, periode1);

        assertThat(relevanteInntektsmeldinger).containsExactlyInAnyOrder(inntektsmelding1, inntektsmelding2, inntektsmelding3);

        relevanteInntektsmeldinger = TilKalkulusMapper.utledInntektsmeldingerSomGjelderForPeriode(sakInntektsmeldinger, periode2);
        assertThat(relevanteInntektsmeldinger).containsExactlyInAnyOrder(inntektsmelding4);
    }

    @Test
    public void skal_prioritere_inntektsmelding_nærmest_skjøringstidspunktet() {
        var arbeidsforholdId = InternArbeidsforholdRef.nyRef();
        var ref = EksternArbeidsforholdRef.ref("123");
        var inntektsmelding1 = InntektsmeldingBuilder.builder()
            .medArbeidsgiver(Arbeidsgiver.virksomhet("000000000"))
            .medArbeidsforholdId(arbeidsforholdId)
            .medArbeidsforholdId(ref)
            .medInnsendingstidspunkt(LocalDateTime.now().minusDays(10))
            .medJournalpostId("1")
            .medBeløp(BigDecimal.TEN)
            .medKanalreferanse("AR123")
            .medOppgittFravær(List.of(new PeriodeAndel(LocalDate.now(), LocalDate.now().plusDays(2))))
            .medRefusjon(BigDecimal.TEN)
            .build();
        var inntektsmelding2 = InntektsmeldingBuilder.builder()
            .medArbeidsgiver(Arbeidsgiver.virksomhet("000000000"))
            .medArbeidsforholdId(arbeidsforholdId)
            .medArbeidsforholdId(ref)
            .medInnsendingstidspunkt(LocalDateTime.now().minusDays(9))
            .medJournalpostId("2")
            .medBeløp(BigDecimal.ONE)
            .medKanalreferanse("AR124")
            .medOppgittFravær(List.of(new PeriodeAndel(LocalDate.now().plusDays(1), LocalDate.now().plusDays(4))))
            .medRefusjon(BigDecimal.ONE)
            .build();

        var sakInntektsmeldinger = Set.of(inntektsmelding1, inntektsmelding2);

        var relevanteInntektsmeldinger = TilKalkulusMapper.utledInntektsmeldingerSomGjelderForPeriode(sakInntektsmeldinger, periode3);

        assertThat(relevanteInntektsmeldinger).containsExactlyInAnyOrder(inntektsmelding1);
    }

    @Test
    public void skal_prioritere_inntektsmelding_nærmest_skjøringstidspunktet_2() {
        var arbeidsforholdId = InternArbeidsforholdRef.nyRef();
        var ref = EksternArbeidsforholdRef.ref("123");
        var inntektsmelding1 = InntektsmeldingBuilder.builder()
            .medArbeidsgiver(Arbeidsgiver.virksomhet("000000000"))
            .medArbeidsforholdId(arbeidsforholdId)
            .medArbeidsforholdId(ref)
            .medInnsendingstidspunkt(LocalDateTime.now().minusDays(10))
            .medJournalpostId("1")
            .medBeløp(BigDecimal.TEN)
            .medKanalreferanse("AR123")
            .medOppgittFravær(List.of(new PeriodeAndel(LocalDate.now(), LocalDate.now().plusDays(2)), new PeriodeAndel(LocalDate.now().plusDays(4), LocalDate.now().plusDays(8))))
            .medRefusjon(BigDecimal.TEN)
            .build();
        var inntektsmelding2 = InntektsmeldingBuilder.builder()
            .medArbeidsgiver(Arbeidsgiver.virksomhet("000000000"))
            .medArbeidsforholdId(arbeidsforholdId)
            .medArbeidsforholdId(ref)
            .medInnsendingstidspunkt(LocalDateTime.now().minusDays(9))
            .medJournalpostId("2")
            .medBeløp(BigDecimal.ONE)
            .medKanalreferanse("AR124")
            .medOppgittFravær(List.of(new PeriodeAndel(LocalDate.now().plusDays(3), LocalDate.now().plusDays(3))))
            .medRefusjon(BigDecimal.ONE)
            .build();

        var sakInntektsmeldinger = Set.of(inntektsmelding1, inntektsmelding2);

        var relevanteInntektsmeldinger = TilKalkulusMapper.utledInntektsmeldingerSomGjelderForPeriode(sakInntektsmeldinger, periode4);

        assertThat(relevanteInntektsmeldinger).containsExactlyInAnyOrder(inntektsmelding1);
    }
}
