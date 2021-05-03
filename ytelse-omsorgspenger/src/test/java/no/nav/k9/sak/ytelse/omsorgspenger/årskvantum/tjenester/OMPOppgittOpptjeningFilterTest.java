package no.nav.k9.sak.ytelse.omsorgspenger.årskvantum.tjenester;

import static no.nav.k9.kodeverk.uttak.UttakArbeidType.FRILANSER;
import static no.nav.k9.kodeverk.uttak.UttakArbeidType.SELVSTENDIG_NÆRINGSDRIVENDE;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import no.nav.k9.kodeverk.uttak.FraværÅrsak;
import no.nav.k9.kodeverk.uttak.UttakArbeidType;
import no.nav.k9.sak.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.k9.sak.domene.iay.modell.InntektArbeidYtelseGrunnlagBuilder;
import no.nav.k9.sak.domene.iay.modell.OppgittOpptjeningBuilder;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.perioder.KravDokument;
import no.nav.k9.sak.perioder.KravDokumentType;
import no.nav.k9.sak.perioder.SøktPeriode;
import no.nav.k9.sak.typer.Arbeidsgiver;
import no.nav.k9.sak.typer.InternArbeidsforholdRef;
import no.nav.k9.sak.typer.JournalpostId;
import no.nav.k9.sak.ytelse.omsorgspenger.repo.OppgittFraværPeriode;

public class OMPOppgittOpptjeningFilterTest {

    private final OMPOppgittOpptjeningFilter opptjeningFilter = new OMPOppgittOpptjeningFilter();

    JournalpostId jpId1 = new JournalpostId("1");
    JournalpostId jpId2 = new JournalpostId("2");

    LocalDateTime innsendingstidspunkt1 = LocalDate.now().atStartOfDay();
    LocalDateTime innsendingstidspunkt2 = LocalDate.now().atStartOfDay().plusDays(1);

    KravDokument kravdok1 = new KravDokument(jpId1, innsendingstidspunkt1, KravDokumentType.SØKNAD);
    KravDokument kravdok2 = new KravDokument(jpId2, innsendingstidspunkt2, KravDokumentType.SØKNAD);

    Arbeidsgiver arbeidsgiver1 = Arbeidsgiver.virksomhet("123123123");
    Arbeidsgiver arbeidsgiver2 = Arbeidsgiver.virksomhet("234234234");

    @Test
    public void skal_hente_matchende_oppgitte_opptjening_for_stp() {
        // Arrange
        var fraværFom = LocalDate.now();
        var fraværTom = LocalDate.now().plusDays(10);
        var vilkårPeriode = DatoIntervallEntitet.fraOgMedTilOgMed(fraværFom, fraværTom);

        var kravDokumenterMedFravær = Map.of(
            kravdok1, List.of(byggSøktPeriode(kravdok1, arbeidsgiver1, fraværFom, fraværTom, SELVSTENDIG_NÆRINGSDRIVENDE)));

        OppgittOpptjeningBuilder opptjeningBuilder = lagOpptjeningBuilderSN(kravdok1, arbeidsgiver1);
        var iayGrunnlag = byggIayGrunnlag(List.of(opptjeningBuilder));

        // Act
        var resultat = opptjeningFilter.finnOppgittOpptjening(iayGrunnlag, vilkårPeriode, kravDokumenterMedFravær);

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

        var kravDokumenterMedFravær = Map.of(
            kravdok1, List.of(byggSøktPeriode(kravdok1, arbeidsgiver1, fraværFom, fraværTom, SELVSTENDIG_NÆRINGSDRIVENDE)),
            kravdok2, List.of(byggSøktPeriode(kravdok2, arbeidsgiver2, fraværFom, fraværTom, SELVSTENDIG_NÆRINGSDRIVENDE))
            );

        OppgittOpptjeningBuilder opptjeningBuilder1 = lagOpptjeningBuilderSN(kravdok1, arbeidsgiver1);
        OppgittOpptjeningBuilder opptjeningBuilder2 = lagOpptjeningBuilderSN(kravdok2, arbeidsgiver2);
        var iayGrunnlag = byggIayGrunnlag(List.of(opptjeningBuilder1, opptjeningBuilder2));

        // Act
        var resultat = opptjeningFilter.finnOppgittOpptjening(iayGrunnlag, vilkårPeriode, kravDokumenterMedFravær);

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

        var kravDokumenterMedFravær = Map.of(
            kravdok1, List.of(byggSøktPeriode(kravdok1, arbeidsgiver1, fraværFom1, fraværTom1, SELVSTENDIG_NÆRINGSDRIVENDE)),
            kravdok2, List.of(byggSøktPeriode(kravdok2, arbeidsgiver2, fraværFom2, fraværTom2, SELVSTENDIG_NÆRINGSDRIVENDE))
        );

        OppgittOpptjeningBuilder opptjeningBuilder1 = lagOpptjeningBuilderSN(kravdok1, arbeidsgiver1);
        OppgittOpptjeningBuilder opptjeningBuilder2 = lagOpptjeningBuilderSN(kravdok2, arbeidsgiver2);
        var iayGrunnlag = byggIayGrunnlag(List.of(opptjeningBuilder1, opptjeningBuilder2));

        // Act
        var resultat = opptjeningFilter.finnOppgittOpptjening(iayGrunnlag, vilkårPeriodeMaks, kravDokumenterMedFravær);

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

        var kravDokumenterMedFravær = Map.of(
            kravdok1, List.of(byggSøktPeriode(kravdok1, arbeidsgiver1, fraværFom1, fraværTom1, SELVSTENDIG_NÆRINGSDRIVENDE)),
            kravdok2, List.of(byggSøktPeriode(kravdok2, arbeidsgiver2, fraværFom2, fraværTom2, FRILANSER))
        );

        OppgittOpptjeningBuilder opptjeningBuilderSN = lagOpptjeningBuilderSN(kravdok1, arbeidsgiver1);
        OppgittOpptjeningBuilder opptjeningBuilderFL = lagOpptjeningBuilderFL(kravdok2, true);
        var iayGrunnlag = byggIayGrunnlag(List.of(opptjeningBuilderSN, opptjeningBuilderFL));

        // Act
        var resultat = opptjeningFilter.finnOppgittOpptjening(iayGrunnlag, vilkårPeriodeMaks, kravDokumenterMedFravær);

        // Assert
        assertThat(resultat).isPresent();
        assertThat(resultat.get().getEgenNæring().get(0).getOrgnr()).isEqualTo(arbeidsgiver1.getOrgnr());
        assertThat(resultat.get().getFrilans().get().getErNyoppstartet()).isTrue();
    }

    private SøktPeriode<OppgittFraværPeriode> byggSøktPeriode(KravDokument kravdok, Arbeidsgiver arbeidsgiver, LocalDate fom, LocalDate tom, UttakArbeidType arbeidType) {
        var fraværÅrsakDummy = FraværÅrsak.ORDINÆRT_FRAVÆR;
        var arbeidRefDummy = InternArbeidsforholdRef.nullRef();

        var journalpostId = kravdok.getJournalpostId();
        var søktPeriode = new SøktPeriode<>(DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom),
            new OppgittFraværPeriode(journalpostId, fom, tom, arbeidType, arbeidsgiver, arbeidRefDummy, null, fraværÅrsakDummy));

        return søktPeriode;
    }

    private OppgittOpptjeningBuilder lagOpptjeningBuilderSN(KravDokument kravdok, Arbeidsgiver arbeidsgiver) {
        var oppgittOpptjeningBuilder = OppgittOpptjeningBuilder.ny()
            .medJournalpostId(kravdok.getJournalpostId())
            .medInnsendingstidspunkt(kravdok.getInnsendingsTidspunkt());
        var egenNæringBuilder = OppgittOpptjeningBuilder.EgenNæringBuilder.ny()
            .medVirksomhet(arbeidsgiver.getOrgnr());
        oppgittOpptjeningBuilder.leggTilEgneNæringer(List.of(egenNæringBuilder));

        return oppgittOpptjeningBuilder;
    }

    private OppgittOpptjeningBuilder lagOpptjeningBuilderFL(KravDokument kravdok2, boolean erNyoppstartet) {
        var oppgittOpptjeningBuilder = OppgittOpptjeningBuilder.ny()
            .medJournalpostId(kravdok2.getJournalpostId())
            .medInnsendingstidspunkt(kravdok2.getInnsendingsTidspunkt());
        var oppgittFrilans = OppgittOpptjeningBuilder.OppgittFrilansBuilder.ny().medErNyoppstartet(erNyoppstartet).build();
        oppgittOpptjeningBuilder.leggTilFrilansOpplysninger(oppgittFrilans);

        return oppgittOpptjeningBuilder;
    }

    private InntektArbeidYtelseGrunnlag byggIayGrunnlag(List<OppgittOpptjeningBuilder> opptjeningBuilder12) {
        return InntektArbeidYtelseGrunnlagBuilder.nytt()
            .medOppgittOpptjeningAggregat(opptjeningBuilder12)
            .build();
    }

}

