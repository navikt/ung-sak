package no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom;

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

import no.nav.k9.sak.db.util.JpaExtension;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.JournalpostId;
import no.nav.vedtak.felles.testutilities.cdi.CdiAwareExtension;

import static org.assertj.core.api.Assertions.assertThat;

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

        final AktørId pleietrengendeAktørId = new AktørId("lala");
        final SykdomDokument dokument = new SykdomDokument(SykdomDokumentType.UKLASSIFISERT, journalpostId, null, endretAv, nå, endretAv, nå);
        repo.lagre(dokument, pleietrengendeAktørId);

        final List<SykdomDokument> dokumenter = repo.hentAlleDokumenterFor(pleietrengendeAktørId);
        assertThat(dokumenter.size()).isEqualTo(1);
        assertThat(repo.hentDokument(dokumenter.get(0).getId(), pleietrengendeAktørId).isPresent());

        final AktørId annenPleietrengendeAktørId = new AktørId("annetBarn");
        assertThat(repo.hentDokument(dokumenter.get(0).getId(), annenPleietrengendeAktørId).isEmpty());
        final SykdomDokument dokument2 = new SykdomDokument(SykdomDokumentType.UKLASSIFISERT, journalpostId, null, endretAv, nå, endretAv, nå);
        repo.lagre(dokument2, annenPleietrengendeAktørId);

        assertThat(repo.hentAlleDokumenterFor(pleietrengendeAktørId).size()).isEqualTo(1);
        assertThat(repo.hentAlleDokumenterFor(annenPleietrengendeAktørId).size()).isEqualTo(1);
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
