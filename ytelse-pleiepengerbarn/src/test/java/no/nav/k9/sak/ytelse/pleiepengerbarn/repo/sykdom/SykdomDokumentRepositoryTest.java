package no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.k9.sak.db.util.JpaExtension;
import no.nav.k9.sak.kontrakt.sykdom.dokument.SykdomDokumentType;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.JournalpostId;

@ExtendWith(CdiAwareExtension.class)
@ExtendWith(JpaExtension.class)
class SykdomDokumentRepositoryTest {
    @Inject
    private SykdomDokumentRepository repo;


    @Test
    void lagreDokumentOgLesInnIgjen() {
        final String endretAv = "saksbehandler";
        final LocalDateTime nå = LocalDateTime.now();
        final JournalpostId journalpostId = new JournalpostId("journalpostId");
        final JournalpostId journalpostId2 = new JournalpostId("journalpostId2");

        final AktørId pleietrengendeAktørId = new AktørId("lala");
        final SykdomDokumentInformasjon informasjon = new SykdomDokumentInformasjon(SykdomDokumentType.UKLASSIFISERT, false, nå.toLocalDate(), nå, 0L, endretAv, nå);
        final SykdomDokument dokument = new SykdomDokument(journalpostId, null, informasjon, null, null, null, endretAv, nå);
        repo.lagre(dokument, pleietrengendeAktørId);

        final List<SykdomDokument> dokumenter = repo.hentAlleDokumenterFor(pleietrengendeAktørId);
        assertThat(dokumenter.size()).isEqualTo(1);
        assertThat(repo.hentDokument(dokumenter.get(0).getId(), pleietrengendeAktørId).isPresent());

        final AktørId annenPleietrengendeAktørId = new AktørId("annetBarn");
        assertThat(repo.hentDokument(dokumenter.get(0).getId(), annenPleietrengendeAktørId).isEmpty());
        final SykdomDokumentInformasjon informasjon2 = new SykdomDokumentInformasjon(SykdomDokumentType.UKLASSIFISERT, false, nå.toLocalDate(), nå, 0L, endretAv, nå);
        final SykdomDokument dokument2 = new SykdomDokument(journalpostId2, null, informasjon2, null, null, null, endretAv, nå);
        
        assertThat(repo.finnesSykdomDokument(journalpostId2, null)).isFalse();
        repo.lagre(dokument2, annenPleietrengendeAktørId);
        assertThat(repo.finnesSykdomDokument(journalpostId2, null)).isTrue();

        assertThat(repo.hentAlleDokumenterFor(pleietrengendeAktørId).size()).isEqualTo(1);
        assertThat(repo.hentAlleDokumenterFor(annenPleietrengendeAktørId).size()).isEqualTo(1);
    }

    @Test
    void lagreDokumentOgOppdaterInformasjon() {
        final String endretAv = "saksbehandler";
        final LocalDateTime nå = LocalDateTime.now();
        final JournalpostId journalpostId = new JournalpostId("journalpostId");

        final AktørId pleietrengendeAktørId = new AktørId("lala");
        final SykdomDokumentInformasjon informasjon = new SykdomDokumentInformasjon(SykdomDokumentType.UKLASSIFISERT, false, nå.toLocalDate(), nå, 0L, endretAv, nå);
        SykdomDokument dokument = new SykdomDokument(journalpostId, null, informasjon, null, null, null, endretAv, nå);
        repo.lagre(dokument, pleietrengendeAktørId);

        final List<SykdomDokument> dokumenter = repo.hentAlleDokumenterFor(pleietrengendeAktørId);
        assertThat(dokumenter.size()).isEqualTo(1);
        assertThat(repo.hentDokument(dokumenter.get(0).getId(), pleietrengendeAktørId).isPresent());
        assertThat(repo.hentAlleDokumenterFor(pleietrengendeAktørId).size()).isEqualTo(1);

        dokument = dokumenter.get(0);
        final SykdomDokumentInformasjon nyInformasjon = new SykdomDokumentInformasjon(SykdomDokumentType.LEGEERKLÆRING_SYKEHUS, false, nå.toLocalDate(), nå, 1L, endretAv, nå);
        dokument.setInformasjon(nyInformasjon);

        repo.oppdater(dokument.getInformasjon());
        List<SykdomDokument> oppdaterteDokumenter = repo.hentAlleDokumenterFor(pleietrengendeAktørId);
        assertThat(oppdaterteDokumenter.size()).isEqualTo(1);
        final SykdomDokumentInformasjon oppdatertInformasjon = oppdaterteDokumenter.get(0).getInformasjon();

        assertThat(oppdatertInformasjon.getVersjon()).isEqualTo(1L);
        assertThat(oppdatertInformasjon.getType()).isEqualTo(SykdomDokumentType.LEGEERKLÆRING_SYKEHUS);
        assertThat(repo.hentDokument(dokumenter.get(0).getId(), pleietrengendeAktørId).isPresent());
        assertThat(repo.hentAlleDokumenterFor(pleietrengendeAktørId).size()).isEqualTo(1);
    }

    @Test
    void lagreDokumentSomDuplikat() {
        final String endretAv = "saksbehandler";
        final LocalDateTime nå = LocalDateTime.now();
        final JournalpostId journalpostId = new JournalpostId("journalpostId");
        final JournalpostId duplikatJournalpostId = new JournalpostId("duplikatJournalpostId");

        final AktørId pleietrengendeAktørId = new AktørId("lala");
        final SykdomDokumentInformasjon informasjon = new SykdomDokumentInformasjon(SykdomDokumentType.LEGEERKLÆRING_SYKEHUS, false, nå.toLocalDate(), nå, 0L, endretAv, nå);
        SykdomDokument dokument = new SykdomDokument(journalpostId, null, informasjon, null, null, null, endretAv, nå);
        repo.lagre(dokument, pleietrengendeAktørId);

        final List<SykdomDokument> dokumenter = repo.hentAlleDokumenterFor(pleietrengendeAktørId);
        assertThat(dokumenter.size()).isEqualTo(1);
        assertThat(repo.hentDokument(dokumenter.get(0).getId(), pleietrengendeAktørId).isPresent());
        assertThat(repo.hentAlleDokumenterFor(pleietrengendeAktørId).size()).isEqualTo(1);

        dokument = dokumenter.get(0);

        final SykdomDokumentInformasjon duplikatInformasjon = new SykdomDokumentInformasjon(SykdomDokumentType.LEGEERKLÆRING_SYKEHUS, false, nå.toLocalDate(), nå, 0L, endretAv, nå);
        duplikatInformasjon.setDuplikatAvDokument(dokument);
        SykdomDokument duplikatDokument = new SykdomDokument(duplikatJournalpostId, null, duplikatInformasjon, null, null, null, endretAv, nå);
        repo.lagre(duplikatDokument, pleietrengendeAktørId);

        List<SykdomDokument> oppdaterteDokumenter = repo.hentAlleDokumenterFor(pleietrengendeAktørId);
        assertThat(oppdaterteDokumenter.size()).isEqualTo(2);
        assertThat(oppdaterteDokumenter.get(1).getDuplikatAvDokument()).isNotNull();

        assertThat(repo.isDokumentBruktIVurdering(dokument.getId())).isFalse();
        assertThat(repo.hentDuplikaterAv(dokument.getId())).isNotEmpty();
    }

    @Test
    void lagreHentOgOppdaterDiagnosekoder() {
        final String opprettetAv = "saksbehandler";
        final LocalDateTime nå = LocalDateTime.now();
        final AktørId pleietrengende = new AktørId(123L);

        SykdomDiagnosekoder diagnosekoder = lagDiagnosekoder(opprettetAv, nå);

        repo.opprettEllerOppdaterDiagnosekoder(diagnosekoder, pleietrengende);
        diagnosekoder = repo.hentDiagnosekoder(pleietrengende);

        assertThat(diagnosekoder.getVersjon()).isEqualTo(0L);
        assertThat(diagnosekoder.getOpprettetAv()).isEqualTo(opprettetAv);

        List<SykdomDiagnosekode> kodelisteLagret = diagnosekoder.getDiagnosekoder();
        assertThat(kodelisteLagret.size()).isEqualTo(2);
        assertThat(kodelisteLagret.get(0).getDiagnosekode()).isEqualTo("testsykdom1");
        assertThat(kodelisteLagret.get(1).getDiagnosekode()).isEqualTo("testsykdom2");

        diagnosekoder = kopierDiagnosekoder(diagnosekoder);
        diagnosekoder.leggTilDiagnosekode(
            new SykdomDiagnosekode(
                "testsykdom3",
                opprettetAv,
                nå.plusHours(1)));

        repo.opprettEllerOppdaterDiagnosekoder(diagnosekoder, pleietrengende);

        diagnosekoder = repo.hentDiagnosekoder(pleietrengende);

        kodelisteLagret = diagnosekoder.getDiagnosekoder();
        assertThat(kodelisteLagret.size()).isEqualTo(3);
        assertThat(kodelisteLagret.get(0).getDiagnosekode()).isEqualTo("testsykdom1");
        assertThat(kodelisteLagret.get(1).getDiagnosekode()).isEqualTo("testsykdom2");
        assertThat(kodelisteLagret.get(2).getDiagnosekode()).isEqualTo("testsykdom3");

    }

    @Test
    void sykdomDokumentKvittereUtEksisterendeVurderingerIEtterkant() {
        final String endretAv = "saksbehandler";
        final JournalpostId journalpostId = new JournalpostId("journalpostId");
        final LocalDateTime nå = LocalDateTime.now();
        final AktørId pleietrengendeAktørId = new AktørId("lala");
        final SykdomDokumentInformasjon informasjon = new SykdomDokumentInformasjon(SykdomDokumentType.UKLASSIFISERT, false, nå.toLocalDate(), nå, 0L, endretAv, nå);

        final SykdomDokument dokument = new SykdomDokument(journalpostId, null, informasjon, null, null, null, endretAv, nå);
        repo.lagre(dokument, pleietrengendeAktørId);

        SykdomDokument lagretDokument = repo.hentAlleDokumenterFor(pleietrengendeAktørId).get(0);

        assertFalse(repo.harKvittertDokumentForEksisterendeVurderinger(lagretDokument));

        repo.kvitterDokumenterMedOppdatertEksisterendeVurderinger(new SykdomDokumentHarOppdatertEksisterendeVurderinger(lagretDokument, endretAv, nå));

        lagretDokument = repo.hentAlleDokumenterFor(pleietrengendeAktørId).get(0);

        assertTrue(repo.harKvittertDokumentForEksisterendeVurderinger(lagretDokument));
    }

    @Test
    void lagreHentOgOppdaterInnleggelse() {
        final String opprettetAv = "saksbehandler";
        final LocalDateTime nå = LocalDateTime.now();
        final AktørId pleietrengende = new AktørId(123L);

        SykdomInnleggelser innleggelser = lagInnleggelser(opprettetAv, nå);

        repo.opprettEllerOppdaterInnleggelser(innleggelser, pleietrengende);
        innleggelser = repo.hentInnleggelse(pleietrengende);

        assertThat(innleggelser.getVersjon()).isEqualTo(0L);
        assertThat(innleggelser.getOpprettetAv()).isEqualTo(opprettetAv);

        List<SykdomInnleggelsePeriode> perioderLagret = innleggelser.getPerioder();

        assertThat(perioderLagret.size()).isEqualTo(2);
        assertThat(perioderLagret.get(0).getInnleggelser()).isEqualTo(innleggelser);
        assertThat(perioderLagret.get(0).getFom()).isEqualTo(LocalDate.of(2021, 1, 1));
        assertThat(perioderLagret.get(0).getTom()).isEqualTo(LocalDate.of(2021, 1, 10));
        assertThat(perioderLagret.get(1).getInnleggelser()).isEqualTo(innleggelser);
        assertThat(perioderLagret.get(1).getFom()).isEqualTo(LocalDate.of(2021, 1, 15));
        assertThat(perioderLagret.get(1).getTom()).isEqualTo(LocalDate.of(2021, 1, 20));

        innleggelser = kopierInnleggelser(innleggelser);
        innleggelser.leggTilPeriode(
            new SykdomInnleggelsePeriode(
                LocalDate.of(2021, 2, 1),
                LocalDate.of(2021, 2, 10),
                opprettetAv,
                nå.plusHours(1)));

        repo.opprettEllerOppdaterInnleggelser(innleggelser, pleietrengende);

        innleggelser = repo.hentInnleggelse(pleietrengende);
        assertThat(innleggelser.getVersjon()).isEqualTo(1L);

        perioderLagret = innleggelser.getPerioder();

        assertThat(perioderLagret.size()).isEqualTo(3);
        assertThat(perioderLagret.get(0).getInnleggelser()).isEqualTo(innleggelser);
        assertThat(perioderLagret.get(0).getFom()).isEqualTo(LocalDate.of(2021, 1, 1));
        assertThat(perioderLagret.get(0).getTom()).isEqualTo(LocalDate.of(2021, 1, 10));
        assertThat(perioderLagret.get(1).getInnleggelser()).isEqualTo(innleggelser);
        assertThat(perioderLagret.get(1).getFom()).isEqualTo(LocalDate.of(2021, 1, 15));
        assertThat(perioderLagret.get(1).getTom()).isEqualTo(LocalDate.of(2021, 1, 20));
        assertThat(perioderLagret.get(2).getInnleggelser()).isEqualTo(innleggelser);
        assertThat(perioderLagret.get(2).getFom()).isEqualTo(LocalDate.of(2021, 2, 1));
        assertThat(perioderLagret.get(2).getTom()).isEqualTo(LocalDate.of(2021, 2, 10));
    }


    @Test
    void lagreKolliderendeVersjonsnummerDiagnosekoderSkalFeile() {
        final String opprettetAv = "saksbehandler";
        final LocalDateTime nå = LocalDateTime.now();
        final AktørId pleietrengende = new AktørId(123L);

        SykdomDiagnosekoder diagnosekoder = lagDiagnosekoder(opprettetAv, nå);

        repo.opprettEllerOppdaterDiagnosekoder(diagnosekoder, pleietrengende);
        diagnosekoder = repo.hentDiagnosekoder(pleietrengende);

        assertThat(diagnosekoder.getVersjon()).isEqualTo(0L);
        assertThat(diagnosekoder.getOpprettetAv()).isEqualTo(opprettetAv);

        diagnosekoder = kopierDiagnosekoder(diagnosekoder);
        diagnosekoder.leggTilDiagnosekode(
            new SykdomDiagnosekode(
                "testsykdom3",
                opprettetAv,
                nå.plusHours(1)));

        repo.opprettEllerOppdaterDiagnosekoder(diagnosekoder, pleietrengende);

        diagnosekoder = repo.hentDiagnosekoder(pleietrengende);

        diagnosekoder.setVersjon(diagnosekoder.getVersjon()-1);

        SykdomDiagnosekoder finalDiagnosekoder = diagnosekoder;
        Assertions.assertThrows(IllegalStateException.class, () -> repo.opprettEllerOppdaterDiagnosekoder(finalDiagnosekoder, pleietrengende));
    }

    @Test
    void lagreKolliderendeVersjonsnummerInnleggelserSkalFeile() {
        final String opprettetAv = "saksbehandler";
        final LocalDateTime nå = LocalDateTime.now();
        final AktørId pleietrengende = new AktørId(123L);

        SykdomInnleggelser innleggelser = lagInnleggelser(opprettetAv, nå);

        repo.opprettEllerOppdaterInnleggelser(innleggelser, pleietrengende);
        innleggelser = repo.hentInnleggelse(pleietrengende);

        assertThat(innleggelser.getVersjon()).isEqualTo(0L);
        assertThat(innleggelser.getOpprettetAv()).isEqualTo(opprettetAv);

        innleggelser = kopierInnleggelser(innleggelser);
        innleggelser.leggTilPeriode(
            new SykdomInnleggelsePeriode(
                LocalDate.of(2021, 2, 1),
                LocalDate.of(2021, 2, 10),
                opprettetAv,
                nå.plusHours(1)));

        repo.opprettEllerOppdaterInnleggelser(innleggelser, pleietrengende);

        innleggelser = repo.hentInnleggelse(pleietrengende);

        innleggelser.setVersjon(innleggelser.getVersjon()-1);

        SykdomInnleggelser finalInnleggelser = innleggelser;
        Assertions.assertThrows(IllegalStateException.class, () -> repo.opprettEllerOppdaterInnleggelser(finalInnleggelser, pleietrengende));
    }

    private SykdomDiagnosekoder lagDiagnosekoder(String opprettetAv, LocalDateTime nå) {
        List<SykdomDiagnosekode> koder = Arrays.asList(
            new SykdomDiagnosekode("testsykdom1", opprettetAv, nå),
            new SykdomDiagnosekode("testsykdom2", opprettetAv, nå)
        );

        return new SykdomDiagnosekoder(null, koder, opprettetAv, nå);
    }

    private SykdomDiagnosekoder kopierDiagnosekoder(SykdomDiagnosekoder i) {
        List<SykdomDiagnosekode> koder = i.getDiagnosekoder()
            .stream()
            .map(k -> new SykdomDiagnosekode(k.getDiagnosekode(), k.getOpprettetAv(), k.getOpprettetTidspunkt()))
            .collect(Collectors.toCollection(ArrayList::new));

        return new SykdomDiagnosekoder(i.getVersjon(), i.getVurderinger(), koder, i.getOpprettetAv(), i.getOpprettetTidspunkt());
    }

    @NotNull
    private SykdomInnleggelser lagInnleggelser(String opprettetAv, LocalDateTime nå) {
        List<SykdomInnleggelsePeriode> perioder = Arrays.asList(
            new SykdomInnleggelsePeriode(
                LocalDate.of(2021, 1, 1),
                LocalDate.of(2021, 1, 10),
                opprettetAv,
                nå),
            new SykdomInnleggelsePeriode(
                LocalDate.of(2021, 1, 15),
                LocalDate.of(2021, 1, 20),
                opprettetAv,
                nå));
        final SykdomInnleggelser innleggelser = new SykdomInnleggelser(null, null, perioder, opprettetAv, nå);
        return innleggelser;
    }

    private SykdomInnleggelser kopierInnleggelser(SykdomInnleggelser i) {
        List<SykdomInnleggelsePeriode> perioder = i.getPerioder()
            .stream()
            .map(p -> new SykdomInnleggelsePeriode(p.getFom(), p.getTom(), p.getOpprettetAv(), p.getOpprettetTidspunkt()))
            .collect(Collectors.toCollection(ArrayList::new));

        return new SykdomInnleggelser(i.getVersjon(), i.getVurderinger(), perioder, i.getOpprettetAv(), i.getOpprettetTidspunkt());
    }
}
