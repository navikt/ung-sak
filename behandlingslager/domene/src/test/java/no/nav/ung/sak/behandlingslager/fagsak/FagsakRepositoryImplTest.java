package no.nav.ung.sak.behandlingslager.fagsak;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.sak.db.util.JpaExtension;
import no.nav.ung.sak.db.util.Repository;
import no.nav.ung.sak.typer.AktørId;
import no.nav.ung.sak.typer.JournalpostId;
import no.nav.ung.sak.typer.Saksnummer;

@ExtendWith(CdiAwareExtension.class)
public class FagsakRepositoryImplTest {
    @RegisterExtension
    public static final JpaExtension repoRule = new JpaExtension();
    @Inject
    private EntityManager entityManager;

    private Repository repository;
    private FagsakRepository fagsakRepository;

    @BeforeEach
    public void setup() {
        repository = new Repository(entityManager);
        fagsakRepository  = new FagsakRepository(entityManager);
    }

    @Test
    public void skal_finne_eksakt_fagsak_gitt_id() {
        AktørId aktørId = AktørId.dummy();
        Saksnummer saksnummer = new Saksnummer("200");
        Fagsak fagsak = opprettFagsak(saksnummer, aktørId);

        Fagsak resultat = fagsakRepository.finnEksaktFagsak(fagsak.getId());

        Assertions.assertThat(resultat).isNotNull();
    }

    @Test
    public void skal_finne_unik_fagsak_gitt_id() {
        AktørId aktørId = AktørId.dummy();
        Saksnummer saksnummer = new Saksnummer("200");
        Fagsak fagsak = opprettFagsak(saksnummer, aktørId);

        Optional<Fagsak> resultat = fagsakRepository.finnUnikFagsak(fagsak.getId());

        Assertions.assertThat(resultat).isPresent();
    }

    @Test
    public void skal_finne_fagsak_gitt_saksnummer() {
        AktørId aktørId = AktørId.dummy();
        Saksnummer saksnummer = new Saksnummer("200");

        opprettFagsak(saksnummer, aktørId);
        Optional<Fagsak> optional = fagsakRepository.hentSakGittSaksnummer(saksnummer);

        Assertions.assertThat(optional).isPresent();
    }

    @Test
    public void skal_finne_fagsak_gitt_aktør_id() {
        AktørId aktørId = AktørId.dummy();
        Saksnummer saksnummer = new Saksnummer("200");

        opprettFagsak(saksnummer, aktørId);
        List<Fagsak> list = fagsakRepository.hentForBruker(aktørId);

        Assertions.assertThat(list).hasSize(1);
    }

    private void lagre(Fagsak... fagsaker) {
        for (var f : fagsaker) {
            repository.lagre(f);
            repository.flush();
        }
        repository.flushAndClear();

    }

    @Test
    public void skal_finne_journalpost_gitt_journalpost_id() {
        AktørId aktørId = AktørId.dummy();
        Saksnummer saksnummer = new Saksnummer("200");
        JournalpostId journalpostId = new JournalpostId("30000");

        opprettJournalpost(journalpostId, saksnummer, aktørId);

        Optional<Journalpost> journalpost = fagsakRepository.hentJournalpost(journalpostId);
        assertThat(journalpost).isPresent();

    }

    private Fagsak opprettFagsak(Saksnummer saksnummer, AktørId aktørId) {
        // Opprett fagsak
        Fagsak fagsak = Fagsak.opprettNy(FagsakYtelseType.DAGPENGER, aktørId, saksnummer);
        lagre(fagsak);
        return fagsak;
    }

    private Journalpost opprettJournalpost(JournalpostId journalpostId, Saksnummer saksnummer, AktørId aktørId) {
        Fagsak fagsak = opprettFagsak(saksnummer, aktørId);

        Journalpost journalpost = new Journalpost(journalpostId, fagsak);
        repository.lagre(journalpost);
        repository.flushAndClear();
        return journalpost;
    }
}
