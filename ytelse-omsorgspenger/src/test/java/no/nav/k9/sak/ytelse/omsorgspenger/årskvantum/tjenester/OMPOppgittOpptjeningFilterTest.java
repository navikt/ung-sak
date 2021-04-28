package no.nav.k9.sak.ytelse.omsorgspenger.årskvantum.tjenester;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import no.nav.k9.kodeverk.dokument.Brevkode;
import no.nav.k9.kodeverk.uttak.FraværÅrsak;
import no.nav.k9.kodeverk.uttak.UttakArbeidType;
import no.nav.k9.sak.domene.iay.modell.InntektArbeidYtelseGrunnlagBuilder;
import no.nav.k9.sak.domene.iay.modell.OppgittOpptjeningBuilder;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.mottak.repo.MottattDokument;
import no.nav.k9.sak.typer.Arbeidsgiver;
import no.nav.k9.sak.typer.InternArbeidsforholdRef;
import no.nav.k9.sak.typer.JournalpostId;
import no.nav.k9.sak.ytelse.omsorgspenger.repo.OppgittFraværPeriode;

public class OMPOppgittOpptjeningFilterTest {

    private OMPOppgittOpptjeningFilter opptjeningFilter = new OMPOppgittOpptjeningFilter();

    JournalpostId jpId1 = new JournalpostId("1");
    JournalpostId jpId2 = new JournalpostId("2");
    Arbeidsgiver arbeidsgiver1 = Arbeidsgiver.virksomhet("123123123");
    Arbeidsgiver arbeidsgiver2 = Arbeidsgiver.virksomhet("234234234");

    @Test
    public void skal_hente_matchende_oppgitte_opptjening_for_stp() {
        // Arrange
        var fraværFom = LocalDate.now();
        var fraværTom = LocalDate.now().plusDays(10);
        var vilkårPeriode = DatoIntervallEntitet.fraOgMedTilOgMed(fraværFom, fraværTom);
        var innsendingstidspunkt = LocalDate.now().atStartOfDay();

        var oppgittFraværPerioder = Set.of(new OppgittFraværPeriode(jpId1, fraværFom, fraværTom, UttakArbeidType.SELVSTENDIG_NÆRINGSDRIVENDE,
            arbeidsgiver1, InternArbeidsforholdRef.nullRef(), null, FraværÅrsak.ORDINÆRT_FRAVÆR));

        OppgittOpptjeningBuilder opptjeningBuilder = lagOpptjeningBuilderSN(innsendingstidspunkt, jpId1, arbeidsgiver1);
        var iayGrunnlag = InntektArbeidYtelseGrunnlagBuilder.nytt()
            .medOppgittOpptjeningAggregat(List.of(opptjeningBuilder))
            .build();
        var gyldigeDokumenter = Map.of(jpId1, byggDokument(innsendingstidspunkt, jpId1));

        // Act
        var resultat = opptjeningFilter.finnOppgittOpptjening(iayGrunnlag, vilkårPeriode, oppgittFraværPerioder, gyldigeDokumenter);

        // Assert
        assertThat(resultat).isNotEmpty();
        assertThat(resultat.get().getEgenNæring().get(0).getOrgnr()).isEqualTo(arbeidsgiver1.getOrgnr());
    }

    @Test
    public void skal_hente_siste_mottatte_matchende_oppgitte_opptjening_for_stp() {
        // Arrange
        var fraværFom = LocalDate.now();
        var fraværTom = LocalDate.now().plusDays(10);
        var vilkårPeriode = DatoIntervallEntitet.fraOgMedTilOgMed(fraværFom, fraværTom);
        var innsendingstidspunkt1 = LocalDate.now().atStartOfDay();
        var innsendingstidspunkt2 = LocalDate.now().plusDays(1).atStartOfDay();

        var oppgittFraværPerioder = Set.of(
            new OppgittFraværPeriode(jpId1, fraværFom, fraværTom, UttakArbeidType.SELVSTENDIG_NÆRINGSDRIVENDE, arbeidsgiver1,
                InternArbeidsforholdRef.nullRef(), null, FraværÅrsak.ORDINÆRT_FRAVÆR),
            new OppgittFraværPeriode(jpId2, fraværFom, fraværTom, UttakArbeidType.SELVSTENDIG_NÆRINGSDRIVENDE, arbeidsgiver2,
                InternArbeidsforholdRef.nullRef(), null, FraværÅrsak.ORDINÆRT_FRAVÆR)
        );

        OppgittOpptjeningBuilder opptjeningBuilder1 = lagOpptjeningBuilderSN(innsendingstidspunkt1, jpId1, arbeidsgiver1);
        OppgittOpptjeningBuilder opptjeningBuilder2 = lagOpptjeningBuilderSN(innsendingstidspunkt2, jpId2, arbeidsgiver2);
        var iayGrunnlag = InntektArbeidYtelseGrunnlagBuilder.nytt()
            .medOppgittOpptjeningAggregat(List.of(opptjeningBuilder1, opptjeningBuilder2))
            .build();
        var gyldigeDokumenter = Map.of(
            jpId1, byggDokument(innsendingstidspunkt1, jpId1),
            jpId2, byggDokument(innsendingstidspunkt2, jpId2)
        );

        // Act
        var resultat = opptjeningFilter.finnOppgittOpptjening(iayGrunnlag, vilkårPeriode, oppgittFraværPerioder, gyldigeDokumenter);

        // Assert
        assertThat(resultat).isPresent();
        assertThat(resultat.get().getEgenNæring().get(0).getOrgnr()).isEqualTo(arbeidsgiver2.getOrgnr());
    }

    @Test
    public void skal_hente_siste_mottatte_matchende_oppgitte_opptjening_for_vilkårsperiode() {
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
            new OppgittFraværPeriode(jpId1, fraværFom1, fraværTom1, UttakArbeidType.SELVSTENDIG_NÆRINGSDRIVENDE, arbeidsgiver1,
                InternArbeidsforholdRef.nullRef(), null, FraværÅrsak.ORDINÆRT_FRAVÆR),
            new OppgittFraværPeriode(jpId2, fraværFom2, fraværTom2, UttakArbeidType.SELVSTENDIG_NÆRINGSDRIVENDE, arbeidsgiver2,
                InternArbeidsforholdRef.nullRef(), null, FraværÅrsak.ORDINÆRT_FRAVÆR)
        );

        OppgittOpptjeningBuilder opptjeningBuilder1 = lagOpptjeningBuilderSN(innsendingstidspunkt1, jpId1, arbeidsgiver1);
        OppgittOpptjeningBuilder opptjeningBuilder2 = lagOpptjeningBuilderSN(innsendingstidspunkt2, jpId2, arbeidsgiver2);
        var iayGrunnlag = InntektArbeidYtelseGrunnlagBuilder.nytt()
            .medOppgittOpptjeningAggregat(List.of(opptjeningBuilder1, opptjeningBuilder2))
            .build();
        var gyldigeDokumenter = Map.of(
            jpId1, byggDokument(innsendingstidspunkt1, jpId1),
            jpId2, byggDokument(innsendingstidspunkt2, jpId2)
        );

        // Act
        var resultat = opptjeningFilter.finnOppgittOpptjening(iayGrunnlag, vilkårPeriodeMaks, oppgittFraværPerioder, gyldigeDokumenter);

        // Assert
        assertThat(resultat).isPresent();
        assertThat(resultat.get().getEgenNæring().get(0).getOrgnr()).isEqualTo(arbeidsgiver2.getOrgnr());
    }

    @Test
    public void skal_sammenstille_oppgitt_opptjening_fra_flere_journalposter() {
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
            new OppgittFraværPeriode(jpId1, fraværFom1, fraværTom1, UttakArbeidType.SELVSTENDIG_NÆRINGSDRIVENDE, arbeidsgiver1,
                InternArbeidsforholdRef.nullRef(), null, FraværÅrsak.ORDINÆRT_FRAVÆR),
            new OppgittFraværPeriode(jpId2, fraværFom2, fraværTom2, UttakArbeidType.FRILANSER, arbeidsgiver2,
                InternArbeidsforholdRef.nullRef(), null, FraværÅrsak.ORDINÆRT_FRAVÆR)
        );

        OppgittOpptjeningBuilder opptjeningBuilderSN = lagOpptjeningBuilderSN(innsendingstidspunkt1, jpId1, arbeidsgiver1);
        OppgittOpptjeningBuilder opptjeningBuilderFL = lagOpptjeningBuilderFL(innsendingstidspunkt2, jpId2, true);
        var iayGrunnlag = InntektArbeidYtelseGrunnlagBuilder.nytt()
            .medOppgittOpptjeningAggregat(List.of(opptjeningBuilderSN, opptjeningBuilderFL))
            .build();
        var gyldigeDokumenter = Map.of(
            jpId1, byggDokument(innsendingstidspunkt1, jpId1),
            jpId2, byggDokument(innsendingstidspunkt2, jpId2)
        );

        // Act
        var resultat = opptjeningFilter.finnOppgittOpptjening(iayGrunnlag, vilkårPeriodeMaks, oppgittFraværPerioder, gyldigeDokumenter);

        // Assert
        assertThat(resultat).isPresent();
        assertThat(resultat.get().getEgenNæring().get(0).getOrgnr()).isEqualTo(arbeidsgiver1.getOrgnr());
        assertThat(resultat.get().getFrilans().get().getErNyoppstartet()).isTrue();
    }

    private OppgittOpptjeningBuilder lagOpptjeningBuilderSN(LocalDateTime innsendingstidspunkt, JournalpostId journalpostId, Arbeidsgiver arbeidsgiver) {
        var oppgittOpptjeningBuilder = OppgittOpptjeningBuilder.ny()
            .medJournalpostId(journalpostId)
            .medInnsendingstidspunkt(innsendingstidspunkt);
        var egenNæringBuilder = OppgittOpptjeningBuilder.EgenNæringBuilder.ny()
            .medVirksomhet(arbeidsgiver.getOrgnr());
        oppgittOpptjeningBuilder.leggTilEgneNæringer(List.of(egenNæringBuilder));

        return oppgittOpptjeningBuilder;
    }

    private OppgittOpptjeningBuilder lagOpptjeningBuilderFL(LocalDateTime innsendingstidspunkt, JournalpostId journalpostId, boolean erNyoppstartet) {
        var oppgittOpptjeningBuilder = OppgittOpptjeningBuilder.ny()
            .medJournalpostId(journalpostId)
            .medInnsendingstidspunkt(innsendingstidspunkt);
        var oppgittFrilans = OppgittOpptjeningBuilder.OppgittFrilansBuilder.ny().medErNyoppstartet(erNyoppstartet).build();
        oppgittOpptjeningBuilder.leggTilFrilansOpplysninger(oppgittFrilans);

        return oppgittOpptjeningBuilder;
    }

    private MottattDokument byggDokument(LocalDateTime innsendingstidspunkt, JournalpostId jp) {
        return new MottattDokument.Builder()
            .medJournalPostId(jp)
            .medType(Brevkode.INNTEKTKOMP_FRILANS)
            .medInnsendingstidspunkt(innsendingstidspunkt)
            .medFagsakId(1L)
            .medBehandlingId(1L)
            .build();
    }

}

