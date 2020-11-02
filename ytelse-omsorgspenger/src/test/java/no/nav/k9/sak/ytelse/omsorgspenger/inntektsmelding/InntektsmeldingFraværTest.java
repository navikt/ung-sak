package no.nav.k9.sak.ytelse.omsorgspenger.inntektsmelding;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import org.junit.Test;

import no.nav.k9.sak.domene.iay.modell.InntektsmeldingBuilder;
import no.nav.k9.sak.domene.iay.modell.PeriodeAndel;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.typer.Arbeidsgiver;
import no.nav.k9.sak.typer.EksternArbeidsforholdRef;
import no.nav.k9.sak.typer.InternArbeidsforholdRef;
import no.nav.k9.sak.ytelse.omsorgspenger.repo.OppgittFraværPeriode;

public class InntektsmeldingFraværTest {

    @Test
    public void skal_bygge_tidslinje_av_fravær_fra_inntektsmeldinger() {
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

        var inntektsmeldinger = Set.of(inntektsmelding1, inntektsmelding2, inntektsmelding3, inntektsmelding4);

        var oppgittFraværPeriode = new InntektsmeldingFravær().trekkUtAlleFraværOgValiderOverlapp(inntektsmeldinger);

        assertThat(oppgittFraværPeriode).hasSize(3);
        assertThat(oppgittFraværPeriode.stream().map(WrappedOppgittFraværPeriode::getPeriode).filter(it -> it.getArbeidsgiver().getOrgnr().equals("000000000"))).hasSize(2);
        assertThat(oppgittFraværPeriode.stream().map(WrappedOppgittFraværPeriode::getPeriode).filter(it -> it.getArbeidsgiver().getOrgnr().equals("000000001"))).hasSize(1);
    }

    @Test
    public void skal_rydde_i_berørte_tidslinjer() {
        var arbeidsforholdId = InternArbeidsforholdRef.nyRef();
        var inntektsmelding1 = InntektsmeldingBuilder.builder()
            .medArbeidsgiver(Arbeidsgiver.virksomhet("000000000"))
            .medArbeidsforholdId(arbeidsforholdId)
            .medArbeidsforholdId(EksternArbeidsforholdRef.ref("asdf"))
            .medJournalpostId("1")
            .medInnsendingstidspunkt(LocalDateTime.now().minusDays(10))
            .medBeløp(BigDecimal.TEN)
            .medKanalreferanse("AR123")
            .medOppgittFravær(List.of(new PeriodeAndel(LocalDate.now().minusDays(30), LocalDate.now().minusDays(25))))
            .medRefusjon(BigDecimal.TEN)
            .build();
        var inntektsmelding2 = InntektsmeldingBuilder.builder()
            .medArbeidsgiver(Arbeidsgiver.virksomhet("000000000"))
            .medArbeidsforholdId(InternArbeidsforholdRef.nullRef())
            .medArbeidsforholdId(EksternArbeidsforholdRef.nullRef())
            .medInnsendingstidspunkt(LocalDateTime.now().minusDays(9))
            .medJournalpostId("2")
            .medBeløp(BigDecimal.ONE)
            .medKanalreferanse("AR124")
            .medOppgittFravær(List.of(new PeriodeAndel(LocalDate.now().minusDays(28), LocalDate.now().minusDays(25))))
            .medRefusjon(BigDecimal.ONE)
            .build();

        var inntektsmeldinger = Set.of(inntektsmelding1, inntektsmelding2);
        var oppgittFraværPeriode = new InntektsmeldingFravær().trekkUtAlleFraværOgValiderOverlapp(inntektsmeldinger);

        assertThat(oppgittFraværPeriode).hasSize(2);
        assertThat(oppgittFraværPeriode.stream().map(WrappedOppgittFraværPeriode::getPeriode).filter(it -> it.getArbeidsgiver().getOrgnr().equals("000000000") && it.getArbeidsforholdRef().equals(arbeidsforholdId))).hasSize(1);
        assertThat(oppgittFraværPeriode.stream()
            .map(WrappedOppgittFraværPeriode::getPeriode)
            .filter(it -> it.getArbeidsgiver().getOrgnr().equals("000000000") && it.getArbeidsforholdRef().equals(arbeidsforholdId))
            .map(OppgittFraværPeriode::getPeriode))
            .contains(DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now().minusDays(30), LocalDate.now().minusDays(29)));
        assertThat(oppgittFraværPeriode.stream()
            .map(WrappedOppgittFraværPeriode::getPeriode)
            .filter(it -> it.getArbeidsgiver().getOrgnr().equals("000000000") && it.getArbeidsforholdRef().equals(InternArbeidsforholdRef.nullRef())))
            .hasSize(1);
        assertThat(oppgittFraværPeriode.stream()
            .map(WrappedOppgittFraværPeriode::getPeriode)
            .filter(it -> it.getArbeidsgiver().getOrgnr().equals("000000000") && it.getArbeidsforholdRef().equals(InternArbeidsforholdRef.nullRef()))
            .map(OppgittFraværPeriode::getPeriode))
            .contains(DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now().minusDays(28), LocalDate.now().minusDays(25)));
    }

    @Test
    public void skal_rydde_i_berørte_tidslinjer_reverse() {
        var arbeidsforholdId = InternArbeidsforholdRef.nyRef();
        var inntektsmelding1 = InntektsmeldingBuilder.builder()
            .medArbeidsgiver(Arbeidsgiver.virksomhet("000000000"))
            .medArbeidsforholdId(arbeidsforholdId)
            .medArbeidsforholdId(EksternArbeidsforholdRef.ref("asdf"))
            .medJournalpostId("1")
            .medInnsendingstidspunkt(LocalDateTime.now().minusDays(9))
            .medBeløp(BigDecimal.TEN)
            .medKanalreferanse("AR124")
            .medOppgittFravær(List.of(new PeriodeAndel(LocalDate.now().minusDays(30), LocalDate.now().minusDays(25))))
            .medRefusjon(BigDecimal.TEN)
            .build();
        var inntektsmelding2 = InntektsmeldingBuilder.builder()
            .medArbeidsgiver(Arbeidsgiver.virksomhet("000000000"))
            .medArbeidsforholdId(InternArbeidsforholdRef.nullRef())
            .medArbeidsforholdId(EksternArbeidsforholdRef.nullRef())
            .medInnsendingstidspunkt(LocalDateTime.now().minusDays(10))
            .medJournalpostId("2")
            .medBeløp(BigDecimal.ONE)
            .medKanalreferanse("AR123")
            .medOppgittFravær(List.of(new PeriodeAndel(LocalDate.now().minusDays(28), LocalDate.now().minusDays(25))))
            .medRefusjon(BigDecimal.ONE)
            .build();

        var inntektsmeldinger = Set.of(inntektsmelding1, inntektsmelding2);
        var oppgittFraværPeriode = new InntektsmeldingFravær().trekkUtAlleFraværOgValiderOverlapp(inntektsmeldinger);

        assertThat(oppgittFraværPeriode).hasSize(1);
        assertThat(oppgittFraværPeriode.stream()
            .map(WrappedOppgittFraværPeriode::getPeriode)
            .filter(it -> it.getArbeidsgiver().getOrgnr().equals("000000000") && it.getArbeidsforholdRef().equals(arbeidsforholdId))).hasSize(1);
        assertThat(oppgittFraværPeriode.stream()
            .map(WrappedOppgittFraværPeriode::getPeriode)
            .filter(it -> it.getArbeidsgiver().getOrgnr().equals("000000000") && it.getArbeidsforholdRef().equals(arbeidsforholdId))
            .map(OppgittFraværPeriode::getPeriode))
            .contains(DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now().minusDays(30), LocalDate.now().minusDays(25)));
        assertThat(oppgittFraværPeriode.stream()
            .map(WrappedOppgittFraværPeriode::getPeriode)
            .filter(it -> it.getArbeidsgiver().getOrgnr().equals("000000000") && it.getArbeidsforholdRef().equals(InternArbeidsforholdRef.nullRef())))
            .hasSize(0);
    }

    @Test
    public void skal_rydde_i_berørte_tidslinjer_reverse_2() {
        var arbeidsforholdId = InternArbeidsforholdRef.nyRef();
        var inntektsmelding1 = InntektsmeldingBuilder.builder()
            .medArbeidsgiver(Arbeidsgiver.virksomhet("000000000"))
            .medArbeidsforholdId(arbeidsforholdId)
            .medArbeidsforholdId(EksternArbeidsforholdRef.ref("asdf"))
            .medJournalpostId("1")
            .medInnsendingstidspunkt(LocalDateTime.now().minusDays(9))
            .medBeløp(BigDecimal.TEN)
            .medKanalreferanse("AR124")
            .medOppgittFravær(List.of(new PeriodeAndel(LocalDate.now().minusDays(30), LocalDate.now().minusDays(25))))
            .medRefusjon(BigDecimal.TEN)
            .build();
        var inntektsmelding2 = InntektsmeldingBuilder.builder()
            .medArbeidsgiver(Arbeidsgiver.virksomhet("000000000"))
            .medArbeidsforholdId(InternArbeidsforholdRef.nullRef())
            .medArbeidsforholdId(EksternArbeidsforholdRef.nullRef())
            .medInnsendingstidspunkt(LocalDateTime.now().minusDays(10))
            .medJournalpostId("2")
            .medBeløp(BigDecimal.ONE)
            .medKanalreferanse("AR123")
            .medOppgittFravær(List.of(new PeriodeAndel(LocalDate.now().minusDays(28), LocalDate.now().minusDays(22))))
            .medRefusjon(BigDecimal.ONE)
            .build();

        var inntektsmeldinger = Set.of(inntektsmelding1, inntektsmelding2);

        var oppgittFraværPeriode = new InntektsmeldingFravær().trekkUtAlleFraværOgValiderOverlapp(inntektsmeldinger);

        assertThat(oppgittFraværPeriode).hasSize(2);
        assertThat(oppgittFraværPeriode.stream()
            .map(WrappedOppgittFraværPeriode::getPeriode)
            .filter(it -> it.getArbeidsgiver().getOrgnr().equals("000000000") && it.getArbeidsforholdRef().equals(arbeidsforholdId))).hasSize(1);
        assertThat(oppgittFraværPeriode.stream()
            .map(WrappedOppgittFraværPeriode::getPeriode)
            .filter(it -> it.getArbeidsgiver().getOrgnr().equals("000000000") && it.getArbeidsforholdRef().equals(arbeidsforholdId))
            .map(OppgittFraværPeriode::getPeriode))
            .contains(DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now().minusDays(30), LocalDate.now().minusDays(25)));
        assertThat(oppgittFraværPeriode.stream()
            .map(WrappedOppgittFraværPeriode::getPeriode)
            .filter(it -> it.getArbeidsgiver().getOrgnr().equals("000000000") && it.getArbeidsforholdRef().equals(InternArbeidsforholdRef.nullRef())))
            .hasSize(1);
        assertThat(oppgittFraværPeriode.stream()
            .map(WrappedOppgittFraværPeriode::getPeriode)
            .filter(it -> it.getArbeidsgiver().getOrgnr().equals("000000000") && it.getArbeidsforholdRef().equals(InternArbeidsforholdRef.nullRef()))
            .map(OppgittFraværPeriode::getPeriode))
            .contains(DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now().minusDays(24), LocalDate.now().minusDays(22)));
    }
}
