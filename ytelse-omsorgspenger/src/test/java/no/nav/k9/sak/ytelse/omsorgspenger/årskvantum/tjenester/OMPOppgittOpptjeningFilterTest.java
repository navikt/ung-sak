package no.nav.k9.sak.ytelse.omsorgspenger.årskvantum.tjenester;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import no.nav.k9.kodeverk.uttak.FraværÅrsak;
import no.nav.k9.kodeverk.uttak.UttakArbeidType;
import no.nav.k9.sak.domene.iay.modell.InntektArbeidYtelseGrunnlagBuilder;
import no.nav.k9.sak.domene.iay.modell.OppgittOpptjeningBuilder;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.typer.Arbeidsgiver;
import no.nav.k9.sak.typer.InternArbeidsforholdRef;
import no.nav.k9.sak.typer.JournalpostId;
import no.nav.k9.sak.ytelse.omsorgspenger.repo.OppgittFraværPeriode;

public class OMPOppgittOpptjeningFilterTest {

    private OMPOppgittOpptjeningFilter opptjeningFilter = new OMPOppgittOpptjeningFilter();

    JournalpostId jpId1 = new JournalpostId("1");
    JournalpostId jpId2 = new JournalpostId("2");
    Arbeidsgiver arbeidsgiver = Arbeidsgiver.virksomhet("123123123");

    @Test
    public void skal_hente_matchende_oppgitte_opptjening_for_stp() {
        // Arrange
        var fraværFom = LocalDate.now();
        var fraværTom = LocalDate.now().plusDays(10);
        var vilkårPeriode = DatoIntervallEntitet.fraOgMedTilOgMed(fraværFom, fraværTom);
        var innsendingstidspunkt = LocalDate.now().atStartOfDay();

        var oppgittFraværPerioder = Set.of(new OppgittFraværPeriode(jpId1, fraværFom, fraværTom, UttakArbeidType.SELVSTENDIG_NÆRINGSDRIVENDE,
            arbeidsgiver, InternArbeidsforholdRef.nullRef(), null, FraværÅrsak.ORDINÆRT_FRAVÆR));

        OppgittOpptjeningBuilder oppgittOpptjeningBuilder = lagOppgittOpptjeningBuilder(innsendingstidspunkt, jpId1);
        var iayGrunnlag = InntektArbeidYtelseGrunnlagBuilder.nytt()
            .medOppgittOpptjeningAggregat(List.of(oppgittOpptjeningBuilder))
            .build();

        // Act
        var resultat = opptjeningFilter.finnOppgittOpptjening(iayGrunnlag, vilkårPeriode, oppgittFraværPerioder);

        // Assert
        var forventet = iayGrunnlag.getOppgittOpptjeningAggregat().get().getOppgitteOpptjeninger().get(0);
        assertThat(resultat).hasValue(forventet);
    }

    @Test
    public void skal_hente_første_mottatte_matchende_oppgitte_opptjening_for_stp() {
        // Arrange
        var fraværFom = LocalDate.now();
        var fraværTom = LocalDate.now().plusDays(10);
        var vilkårPeriode = DatoIntervallEntitet.fraOgMedTilOgMed(fraværFom, fraværTom);
        var innsendingstidspunkt1 = LocalDate.now().atStartOfDay();
        var innsendingstidspunkt2 = LocalDate.now().plusDays(1).atStartOfDay();

        var oppgittFraværPerioder = Set.of(
            new OppgittFraværPeriode(jpId1, fraværFom, fraværTom, UttakArbeidType.SELVSTENDIG_NÆRINGSDRIVENDE, arbeidsgiver,
                InternArbeidsforholdRef.nullRef(), null, FraværÅrsak.ORDINÆRT_FRAVÆR),
            new OppgittFraværPeriode(jpId2, fraværFom, fraværTom, UttakArbeidType.SELVSTENDIG_NÆRINGSDRIVENDE, arbeidsgiver,
                InternArbeidsforholdRef.nullRef(), null, FraværÅrsak.ORDINÆRT_FRAVÆR)
        );

        OppgittOpptjeningBuilder oppgittOpptjeningBuilder1 = lagOppgittOpptjeningBuilder(innsendingstidspunkt1, jpId1);
        OppgittOpptjeningBuilder oppgittOpptjeningBuilder2 = lagOppgittOpptjeningBuilder(innsendingstidspunkt2, jpId2);
        var iayGrunnlag = InntektArbeidYtelseGrunnlagBuilder.nytt()
            .medOppgittOpptjeningAggregat(List.of(oppgittOpptjeningBuilder1, oppgittOpptjeningBuilder2))
            .build();

        // Act
        var resultat = opptjeningFilter.finnOppgittOpptjening(iayGrunnlag, vilkårPeriode, oppgittFraværPerioder);

        // Assert
        assertThat(resultat).isPresent();
        assertThat(resultat.get().getJournalpostId()).isEqualTo(jpId1);
    }

    @Test
    public void skal_hente_første_mottatte_matchende_oppgitte_opptjening_for_vilkårsperiode() {
        // Arrange
        var fraværFom1 = LocalDate.now();
        var fraværTom1 = LocalDate.now().plusDays(10);
        var fraværFom2 = LocalDate.now().plusDays(20);
        var fraværTom2 = LocalDate.now().plusDays(30);
        // Maks vilkårsperiode overlapper begge fraværsperioder
        var vilkårPeriodeMaks = DatoIntervallEntitet.fraOgMedTilOgMed(fraværFom1, fraværFom2);

        var innsendingstidspunkt1 = LocalDate.now().atStartOfDay();
        var innsendingstidspunkt2 = LocalDate.now().plusDays(1).atStartOfDay();

        var oppgittFraværPerioder = Set.of(
            new OppgittFraværPeriode(jpId1, fraværFom1, fraværTom1, UttakArbeidType.SELVSTENDIG_NÆRINGSDRIVENDE, arbeidsgiver,
                InternArbeidsforholdRef.nullRef(), null, FraværÅrsak.ORDINÆRT_FRAVÆR),
            new OppgittFraværPeriode(jpId2, fraværFom2, fraværTom2, UttakArbeidType.SELVSTENDIG_NÆRINGSDRIVENDE, arbeidsgiver,
                InternArbeidsforholdRef.nullRef(), null, FraværÅrsak.ORDINÆRT_FRAVÆR)
        );

        OppgittOpptjeningBuilder oppgittOpptjeningBuilder1 = lagOppgittOpptjeningBuilder(innsendingstidspunkt1, jpId1);
        OppgittOpptjeningBuilder oppgittOpptjeningBuilder2 = lagOppgittOpptjeningBuilder(innsendingstidspunkt2, jpId2);
        var iayGrunnlag = InntektArbeidYtelseGrunnlagBuilder.nytt()
            .medOppgittOpptjeningAggregat(List.of(oppgittOpptjeningBuilder1, oppgittOpptjeningBuilder2))
            .build();

        // Act
        var resultat = opptjeningFilter.finnOppgittOpptjening(iayGrunnlag, vilkårPeriodeMaks, oppgittFraværPerioder);

        // Assert
        assertThat(resultat).isPresent();
        assertThat(resultat.get().getJournalpostId()).isEqualTo(jpId1);
    }

    private OppgittOpptjeningBuilder lagOppgittOpptjeningBuilder(LocalDateTime innsendingstidspunkt, JournalpostId journalpostId) {
        var oppgittOpptjeningBuilder = OppgittOpptjeningBuilder.ny()
            .medJournalpostId(journalpostId)
            .medInnsendingstidspunkt(innsendingstidspunkt);
        return oppgittOpptjeningBuilder;
    }

}

