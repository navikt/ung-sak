package no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom;

import java.time.LocalDateTime;
import java.util.List;

import javax.inject.Inject;

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
}
