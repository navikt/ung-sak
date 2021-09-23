package no.nav.k9.sak.ytelse.pleiepengerbarn.opptjening;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import no.nav.k9.sak.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.k9.sak.domene.iay.modell.InntektArbeidYtelseGrunnlagBuilder;
import no.nav.k9.sak.domene.iay.modell.OppgittOpptjeningBuilder;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.perioder.KravDokument;
import no.nav.k9.sak.perioder.KravDokumentType;
import no.nav.k9.sak.perioder.SøktPeriode;
import no.nav.k9.sak.typer.Arbeidsgiver;
import no.nav.k9.sak.typer.JournalpostId;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.søknadsperiode.Søknadsperiode;

public class PSBOppgittOpptjeningFilterTest {

    private final PSBOppgittOpptjeningFilter opptjeningFilter = new PSBOppgittOpptjeningFilter();

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

        Map<KravDokument, List<SøktPeriode<Søknadsperiode>>> kravDokumenterMedFravær = Map.of(
            kravdok1, List.of(byggSøktPeriode(fraværFom, fraværTom)));

        OppgittOpptjeningBuilder opptjeningBuilder = lagOpptjeningBuilderSN(kravdok1, arbeidsgiver1);
        var iayGrunnlag = byggIayGrunnlag(List.of(opptjeningBuilder));

        // Act
        var resultat = opptjeningFilter.finnOppgittOpptjeningLansert(iayGrunnlag, vilkårPeriode, kravDokumenterMedFravær);

        // Assert
        assertThat(resultat).isNotEmpty();
        assertThat(resultat.get().getEgenNæring().get(0).getOrgnr()).isEqualTo(arbeidsgiver1.getOrgnr());
    }

    @Test
    public void skal_hente_søknad_nærmest_stp() {
        // Arrange
        var fraværFom1 = LocalDate.now();
        var fraværTom1 = LocalDate.now().plusDays(10);
        var fraværFom2 = LocalDate.now().plusDays(5);
        var fraværTom2 = LocalDate.now().plusDays(15);

        // Maks vilkårsperiode overlapper begge fraværsperioder
        var vilkårPeriodeMaks = DatoIntervallEntitet.fraOgMedTilOgMed(fraværFom1, fraværFom2);

        Map<KravDokument, List<SøktPeriode<Søknadsperiode>>> kravDokumenterMedFravær = Map.of(
            kravdok1, List.of(byggSøktPeriode(fraværFom1, fraværTom1)),
            kravdok2, List.of(byggSøktPeriode(fraværFom2, fraværTom2))
        );

        OppgittOpptjeningBuilder opptjeningBuilder1 = lagOpptjeningBuilderSN(kravdok1, arbeidsgiver1);
        OppgittOpptjeningBuilder opptjeningBuilder2 = lagOpptjeningBuilderSN(kravdok2, arbeidsgiver2);
        var iayGrunnlag = byggIayGrunnlag(List.of(opptjeningBuilder1, opptjeningBuilder2));

        // Act
        var resultat = opptjeningFilter.finnOppgittOpptjeningLansert(iayGrunnlag, vilkårPeriodeMaks, kravDokumenterMedFravær);

        // Assert
        assertThat(resultat).isPresent();
        assertThat(resultat.get().getEgenNæring().get(0).getOrgnr()).isEqualTo(arbeidsgiver1.getOrgnr());
    }

    @Test
    public void skal_hente_sist_mottatt_søknad_dersom_samme_stp() {
        // Arrange
        var fraværFom1 = LocalDate.now();
        var fraværTom1 = LocalDate.now().plusDays(10);
        var fraværFom2 = fraværFom1;
        var fraværTom2 = fraværTom1;

        var vilkårPeriode = DatoIntervallEntitet.fraOgMedTilOgMed(fraværFom1, fraværFom2);

        Map<KravDokument, List<SøktPeriode<Søknadsperiode>>> kravDokumenterMedFravær = Map.of(
            kravdok1, List.of(byggSøktPeriode(fraværFom1, fraværTom1)),
            kravdok2, List.of(byggSøktPeriode(fraværFom2, fraværTom2))
        );

        OppgittOpptjeningBuilder opptjeningBuilder1 = lagOpptjeningBuilderSN(kravdok1, arbeidsgiver1);
        OppgittOpptjeningBuilder opptjeningBuilder2 = lagOpptjeningBuilderSN(kravdok2, arbeidsgiver2);
        var iayGrunnlag = byggIayGrunnlag(List.of(opptjeningBuilder1, opptjeningBuilder2));

        // Act
        var resultat = opptjeningFilter.finnOppgittOpptjeningLansert(iayGrunnlag, vilkårPeriode, kravDokumenterMedFravær);

        // Assert
        assertThat(resultat).isPresent();
        assertThat(resultat.get().getEgenNæring().get(0).getOrgnr()).isEqualTo(arbeidsgiver2.getOrgnr());
    }

    private SøktPeriode<Søknadsperiode> byggSøktPeriode(LocalDate fom, LocalDate tom) {
        var søktPeriode = new SøktPeriode<>(DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom), new Søknadsperiode(fom, tom));

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

    private InntektArbeidYtelseGrunnlag byggIayGrunnlag(List<OppgittOpptjeningBuilder> opptjeningBuilder12) {
        return InntektArbeidYtelseGrunnlagBuilder.nytt()
            .medOppgittOpptjeningAggregat(opptjeningBuilder12)
            .build();
    }

}

