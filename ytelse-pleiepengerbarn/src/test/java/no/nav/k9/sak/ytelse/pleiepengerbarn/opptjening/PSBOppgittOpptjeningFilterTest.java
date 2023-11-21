package no.nav.k9.sak.ytelse.pleiepengerbarn.opptjening;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import no.nav.k9.kodeverk.dokument.Brevkode;
import no.nav.k9.sak.behandlingslager.behandling.motattdokument.MottattDokument;
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

    private final PSBOppgittOpptjeningFilter opptjeningFilter = new PSBOppgittOpptjeningFilter(null, null, null, null, null, true);

    JournalpostId jpId1 = new JournalpostId("1");
    JournalpostId jpId2 = new JournalpostId("2");
    JournalpostId jpId3 = new JournalpostId("3");

    LocalDateTime innsendingstidspunkt1 = LocalDate.now().atStartOfDay();
    LocalDateTime innsendingstidspunkt2 = LocalDate.now().atStartOfDay().plusDays(1);
    LocalDateTime innsendingstidspunkt3 = LocalDate.now().atStartOfDay().plusDays(2);

    KravDokument kravdok1 = new KravDokument(jpId1, innsendingstidspunkt1, KravDokumentType.SØKNAD);
    KravDokument kravdok2 = new KravDokument(jpId2, innsendingstidspunkt2, KravDokumentType.SØKNAD);
    KravDokument kravdok3 = new KravDokument(jpId3, innsendingstidspunkt3, KravDokumentType.SØKNAD);

    Arbeidsgiver arbeidsgiver1 = Arbeidsgiver.virksomhet("123123123");
    Arbeidsgiver arbeidsgiver2 = Arbeidsgiver.virksomhet("234234234");

    Long fagsakId = 1234L;

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

        Set<MottattDokument> mottatteDokumenter = Set.of(byggMottattDokument(fagsakId, getPayloadSøknad(), jpId1, Brevkode.PLEIEPENGER_BARN_SOKNAD));

        // Act
        var resultat = opptjeningFilter.finnOppgittOpptjening(iayGrunnlag, vilkårPeriode, kravDokumenterMedFravær, mottatteDokumenter);

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

        Set<MottattDokument> mottatteDokumenter = Set.of(
            byggMottattDokument(fagsakId, getPayloadSøknad(), jpId1, Brevkode.PLEIEPENGER_BARN_SOKNAD),
            byggMottattDokument(fagsakId, getPayloadSøknad(), jpId2, Brevkode.PLEIEPENGER_BARN_SOKNAD)
        );

        // Act
        var resultat = opptjeningFilter.finnOppgittOpptjening(iayGrunnlag, vilkårPeriodeMaks, kravDokumenterMedFravær, mottatteDokumenter);

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

        Set<MottattDokument> mottatteDokumenter = Set.of(
            byggMottattDokument(fagsakId, getPayloadSøknad(), jpId1, Brevkode.PLEIEPENGER_BARN_SOKNAD),
            byggMottattDokument(fagsakId, getPayloadSøknad(), jpId2, Brevkode.PLEIEPENGER_BARN_SOKNAD)
        );

        // Act
        var resultat = opptjeningFilter.finnOppgittOpptjening(iayGrunnlag, vilkårPeriode, kravDokumenterMedFravær, mottatteDokumenter);

        // Assert
        assertThat(resultat).isPresent();
        assertThat(resultat.get().getEgenNæring().get(0).getOrgnr()).isEqualTo(arbeidsgiver2.getOrgnr());
    }


    @Test
    public void skal_hente_sist_mottatt_søknad_som_oppgav_opptjening() {
        // Arrange
        var fraværFom1 = LocalDate.now();
        var fraværTom1 = LocalDate.now().plusDays(10);
        var fraværFom2 = fraværFom1;
        var fraværTom2 = fraværTom1;
        var fraværFom3 = fraværFom1;
        var fraværTom3 = fraværTom1;

        var vilkårPeriode = DatoIntervallEntitet.fraOgMedTilOgMed(fraværFom1, fraværTom1);

        Map<KravDokument, List<SøktPeriode<Søknadsperiode>>> kravDokumenterMedFravær = Map.of(
            kravdok1, List.of(byggSøktPeriode(fraværFom1, fraværTom1)),
            kravdok2, List.of(byggSøktPeriode(fraværFom2, fraværTom2)),
            kravdok3, List.of(byggSøktPeriode(fraværFom3, fraværTom3))
        );

        OppgittOpptjeningBuilder opptjeningBuilder1 = lagOpptjeningBuilderSN(kravdok1, arbeidsgiver1);
        OppgittOpptjeningBuilder opptjeningBuilder2 = lagOpptjeningBuilderSN(kravdok2, arbeidsgiver2);
        var iayGrunnlag = byggIayGrunnlag(List.of(opptjeningBuilder1, opptjeningBuilder2));

        Set<MottattDokument> mottatteDokumenter = Set.of(
            byggMottattDokument(fagsakId, getPayloadSøknad(), jpId1, Brevkode.PLEIEPENGER_BARN_SOKNAD),
            byggMottattDokument(fagsakId, getPayloadSøknad(), jpId2, Brevkode.PLEIEPENGER_BARN_SOKNAD),
            byggMottattDokument(fagsakId, getPayloadSøknad(), jpId3, Brevkode.PLEIEPENGER_BARN_SOKNAD)
        );

        // Act
        var resultat = opptjeningFilter.finnOppgittOpptjening(iayGrunnlag, vilkårPeriode, kravDokumenterMedFravær, mottatteDokumenter);

        // Assert
        assertThat(resultat).isPresent();
        assertThat(resultat.get().getEgenNæring().get(0).getOrgnr()).isEqualTo(arbeidsgiver2.getOrgnr());
    }

    @Test
    public void skal_ikke_bruke_oppgitt_opptjening_fra_søknad_uten_søknadsperiode_psb() {
        // Arrange
        var fraværFom = LocalDate.now();
        var fraværTom = LocalDate.now().plusDays(10);

        var vilkårPeriode = DatoIntervallEntitet.fraOgMedTilOgMed(fraværFom, fraværTom);

        Map<KravDokument, List<SøktPeriode<Søknadsperiode>>> kravDokumenterMedFravær = Map.of(
            kravdok1, List.of(byggSøktPeriode(fraværFom, fraværTom)),
            kravdok2, List.of(byggSøktPeriode(fraværFom, fraværTom))
        );

        OppgittOpptjeningBuilder opptjeningBuilder1 = lagOpptjeningBuilderSN(kravdok1, arbeidsgiver1);
        OppgittOpptjeningBuilder opptjeningBuilder2 = lagOpptjeningBuilderSN(kravdok2, arbeidsgiver2);
        var iayGrunnlag = byggIayGrunnlag(List.of(opptjeningBuilder1, opptjeningBuilder2));

        Set<MottattDokument> mottatteDokumenter = Set.of(
            byggMottattDokument(fagsakId, getPayloadSøknad(), jpId1, Brevkode.PLEIEPENGER_BARN_SOKNAD),
            byggMottattDokument(fagsakId, getPayloadSøknadUtenSøknadsperiode(), jpId2, Brevkode.PLEIEPENGER_BARN_SOKNAD)
        );

        // Act
        var resultat = opptjeningFilter.finnOppgittOpptjening(iayGrunnlag, vilkårPeriode, kravDokumenterMedFravær, mottatteDokumenter);

        // Assert
        assertThat(resultat).isPresent();
        assertThat(resultat.get().getEgenNæring().get(0).getOrgnr()).isEqualTo(arbeidsgiver1.getOrgnr());
    }

    @Test
    public void skal_bruke_oppgitt_opptjening_fra_søknad_uten_søknadsperiode_psb_hvis_eldre_søknadsversjon() {
        // Arrange
        var fraværFom = LocalDate.now();
        var fraværTom = LocalDate.now().plusDays(10);

        var vilkårPeriode = DatoIntervallEntitet.fraOgMedTilOgMed(fraværFom, fraværTom);

        Map<KravDokument, List<SøktPeriode<Søknadsperiode>>> kravDokumenterMedFravær = Map.of(
            kravdok1, List.of(byggSøktPeriode(fraværFom, fraværTom)),
            kravdok2, List.of(byggSøktPeriode(fraværFom, fraværTom))
        );

        OppgittOpptjeningBuilder opptjeningBuilder1 = lagOpptjeningBuilderSN(kravdok1, arbeidsgiver1);
        OppgittOpptjeningBuilder opptjeningBuilder2 = lagOpptjeningBuilderSN(kravdok2, arbeidsgiver2);
        var iayGrunnlag = byggIayGrunnlag(List.of(opptjeningBuilder1, opptjeningBuilder2));

        Set<MottattDokument> mottatteDokumenter = Set.of(
            byggMottattDokument(fagsakId, getPayloadSøknad(), jpId1, Brevkode.PLEIEPENGER_BARN_SOKNAD),
            byggMottattDokument(fagsakId, getPayloadSøknadUtenSøknadsperiodeEldreVersjon(), jpId2, Brevkode.PLEIEPENGER_BARN_SOKNAD)
        );

        // Act
        var resultat = opptjeningFilter.finnOppgittOpptjening(iayGrunnlag, vilkårPeriode, kravDokumenterMedFravær, mottatteDokumenter);

        // Assert
        assertThat(resultat).isPresent();
        assertThat(resultat.get().getEgenNæring().get(0).getOrgnr()).isEqualTo(arbeidsgiver2.getOrgnr());
    }

    private SøktPeriode<Søknadsperiode> byggSøktPeriode(LocalDate fom, LocalDate tom) {
        return new SøktPeriode<>(DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom), new Søknadsperiode(fom, tom));
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

    private MottattDokument byggMottattDokument(Long fagsakId, String xml, JournalpostId journalpostId, Brevkode brevkode) {
        MottattDokument.Builder builder = new MottattDokument.Builder();
        builder.medType(brevkode);
        builder.medPayload(xml);
        builder.medFagsakId(fagsakId);
        builder.medJournalPostId(journalpostId);
        return builder.build();
    }

    private static String getPayloadSøknad() {
        return "{\"mottattDato\":\"2023-11-09T09:42:19.676Z\",\n" +
            "\"språk\":\"nb\",\n" +
            "\"søker\":{\"norskIdentitetsnummer\":\"08438035460\"},\n" +
            "\"søknadId\":\"7ab49ff6-7994-4690-9476-5bc9285fdb1a\",\n" +
            "\"versjon\":\"1.0.1\",\n" +
            "\"ytelse\":{\n" +
            "\t\"type\":\"PLEIEPENGER_SYKT_BARN\",\n" +
            "\t\"arbeidstid\":{\n" +
            "\t\t\"arbeidstakerList\":[],\n" +
            "\t\t\"frilanserArbeidstidInfo\":null,\n" +
            "\t\t\"selvstendigNæringsdrivendeArbeidstidInfo\":{\"perioder\":{\"2023-08-09/2023-10-09\":{\"faktiskArbeidTimerPerDag\":\"PT0S\",\"jobberNormaltTimerPerDag\":\"PT7H30M\"}}}\n" +
            "\t},\n" +
            "\t\"barn\":{\"fødselsdato\":null,\"norskIdentitetsnummer\":\"13412363366\"},\n" +
            "\t\"lovbestemtFerie\":{\"perioder\":{}},\n" +
            "\t\"opptjeningAktivitet\":{\"selvstendigNæringsdrivende\":[{\"organisasjonsnummer\":\"910909088\",\"perioder\":{\"2022-10-09/2023-11-09\":{\"virksomhetstyper\":[\"ANNEN\"]}},\"virksomhetNavn\":\"Bedriften\"}]},\n" +
            "\t\"søknadsperiode\":[\"2023-08-09/2023-10-09\"],\n" +
            "\t\"trekkKravPerioder\":[],\n" +
            "\t\"uttak\":{\"perioder\":{\"2023-08-09/2023-10-09\":{\"timerPleieAvBarnetPerDag\":\"PT7H30M\"}}}\n" +
            "}}";
    }

    private static String getPayloadSøknadUtenSøknadsperiode() {
        return "{\"mottattDato\":\"2023-11-09T09:42:19.676Z\",\n" +
            "\"språk\":\"nb\",\n" +
            "\"søker\":{\"norskIdentitetsnummer\":\"08438035460\"},\n" +
            "\"søknadId\":\"7ab49ff6-7994-4690-9476-5bc9285fdb1a\",\n" +
            "\"versjon\":\"1.0.1\",\n" +
            "\"ytelse\":{\n" +
            "\t\"type\":\"PLEIEPENGER_SYKT_BARN\",\n" +
            "\t\"barn\":{\"fødselsdato\":null,\"norskIdentitetsnummer\":\"13412363366\"},\n" +
            "\t\"lovbestemtFerie\":{\"perioder\":{\"2023-08-10/2023-09-10\":{\"skalHaFerie\":true}}},\n" +
            "\t\"søknadsperiode\":[],\n" +
            "\t\"trekkKravPerioder\":[],\n" +
            "\t\"uttak\":{\"perioder\":{}}}\n" +
            "}}";
    }

    private static String getPayloadSøknadUtenSøknadsperiodeEldreVersjon() {
        return "{\"mottattDato\":\"2023-11-09T09:42:19.676Z\",\n" +
            "\"språk\":\"nb\",\n" +
            "\"søker\":{\"norskIdentitetsnummer\":\"08438035460\"},\n" +
            "\"søknadId\":\"7ab49ff6-7994-4690-9476-5bc9285fdb1a\",\n" +
            "\"versjon\":\"1.0.0\",\n" +
            "\"ytelse\":{\n" +
            "\t\"type\":\"PLEIEPENGER_SYKT_BARN\",\n" +
            "\t\"barn\":{\"fødselsdato\":null,\"norskIdentitetsnummer\":\"13412363366\"},\n" +
            "\t\"lovbestemtFerie\":{\"perioder\":{\"2023-08-10/2023-09-10\":{\"skalHaFerie\":true}}},\n" +
            "\t\"søknadsperiode\":[],\n" +
            "\t\"trekkKravPerioder\":[],\n" +
            "\t\"uttak\":{\"perioder\":{}}}\n" +
            "}}";
    }
}

