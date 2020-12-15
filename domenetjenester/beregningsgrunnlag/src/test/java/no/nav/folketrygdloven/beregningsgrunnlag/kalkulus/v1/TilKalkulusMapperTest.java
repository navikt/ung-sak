package no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.v1;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

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

    private static PeriodeAndel periode(String fom, String tom) {
        return new PeriodeAndel(dato(fom), dato(tom));
    }

    private static PeriodeAndel periode(String fom, String tom, Duration zero) {
        return new PeriodeAndel(dato(fom), dato(tom), zero);
    }

    private static LocalDate dato(String måneddato) {
        var yr = LocalDate.now().getYear();
        return LocalDate.parse(yr + "-" + måneddato);
    }

    @Test
    public void skal_utlede_inntektsmeldinger_som_gjelder_for_periode() {
        Arbeidsgiver virksomhet = Arbeidsgiver.virksomhet("000000000");
        var periode2327 = periode("03-23", "03-27");
        var periode0609 = periode("04-06", "04-09");
        var periode14 = periode("04-14", "04-14");
        var inntektsmelding1 = InntektsmeldingBuilder.builder()
            .medArbeidsgiver(virksomhet)
            .medJournalpostId("1")
            .medBeløp(BigDecimal.TEN)
            .medKanalreferanse("AR123")
            .medOppgittFravær(List.of(periode14))
            .medRefusjon(BigDecimal.TEN)
            .build();
        var inntektsmelding2 = InntektsmeldingBuilder.builder()
            .medArbeidsgiver(virksomhet)
            .medJournalpostId("2")
            .medBeløp(BigDecimal.ONE)
            .medKanalreferanse("AR124")
            .medOppgittFravær(List.of(periode2327, periode0609, periode14))
            .medRefusjon(BigDecimal.ONE)
            .build();
        var sakInntektsmeldinger = Set.of(inntektsmelding1, inntektsmelding2);
        var vilkårsperiode2 = DatoIntervallEntitet.fraOgMedTilOgMed(dato("04-14"), dato("04-14"));
        var relevanteInntektsmeldinger = TilKalkulusMapper.utledInntektsmeldingerSomGjelderForPeriode(sakInntektsmeldinger, vilkårsperiode2);
        assertThat(relevanteInntektsmeldinger).containsOnly(inntektsmelding2);
    }

    @Test
    public void skal_utlede_inntektsmeldinger_som_gjelder_for_periode_2() {
        Arbeidsgiver virksomhet = Arbeidsgiver.virksomhet("000000000");
        var periode14 = periode("04-14", "04-14");
        var periode1216 = periode("04-12", "04-16");
        var periode1213 = periode("04-12", "04-13", Duration.ZERO);
        var inntektsmelding1 = InntektsmeldingBuilder.builder()
            .medArbeidsgiver(virksomhet)
            .medJournalpostId("1")
            .medBeløp(BigDecimal.TEN)
            .medKanalreferanse("AR123")
            .medOppgittFravær(List.of(periode14))
            .medRefusjon(BigDecimal.TEN)
            .build();
        var inntektsmelding2 = InntektsmeldingBuilder.builder()
            .medArbeidsgiver(virksomhet)
            .medJournalpostId("2")
            .medBeløp(BigDecimal.ONE)
            .medKanalreferanse("AR124")
            .medOppgittFravær(List.of(periode1216))
            .medRefusjon(BigDecimal.ONE)
            .build();
        var inntektsmelding3 = InntektsmeldingBuilder.builder()
            .medArbeidsgiver(virksomhet)
            .medJournalpostId("3")
            .medBeløp(BigDecimal.ONE)
            .medKanalreferanse("AR125")
            .medOppgittFravær(List.of(periode1213))
            .medRefusjon(BigDecimal.ONE)
            .build();

        var sakInntektsmeldinger = Set.of(inntektsmelding1, inntektsmelding2, inntektsmelding3);
        var vilkårsperiode2 = DatoIntervallEntitet.fraOgMedTilOgMed(dato("04-14"), dato("04-14"));
        var relevanteInntektsmeldinger = TilKalkulusMapper.utledInntektsmeldingerSomGjelderForPeriode(sakInntektsmeldinger, vilkårsperiode2);
        assertThat(relevanteInntektsmeldinger).containsOnly(inntektsmelding2);
    }

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

        // assertThat(sortedInntektsmeldinger).has(sortedInntektsmeldinger);

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
