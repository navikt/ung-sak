package no.nav.k9.sak.behandlingslager.fagsak;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertTrue;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import javax.inject.Inject;
import javax.persistence.EntityManager;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.db.util.JpaExtension;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.JournalpostId;
import no.nav.k9.sak.typer.Saksnummer;
import no.nav.vedtak.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.vedtak.felles.testutilities.db.Repository;

@ExtendWith(CdiAwareExtension.class)
@ExtendWith(JpaExtension.class)
public class FagsakRepositoryImplTest {

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

    @Test
    public void skal_finne_fagsak_relatert_til_pleietrengende_i_angitt_intervall() {
        AktørId aktørIdSøker1 = AktørId.dummy();
        AktørId aktørIdSøker2 = AktørId.dummy();
        AktørId aktørIdPleietrengende = AktørId.dummy();
        FagsakYtelseType ytelseType = FagsakYtelseType.DAGPENGER;
        LocalDate fom = LocalDate.now();
        LocalDate tom = fom.plusDays(10);

        // Opprett fagsaker
        Fagsak[] fagsaker = {
                new Fagsak(ytelseType, aktørIdSøker1, aktørIdPleietrengende, new Saksnummer("200"), fom, tom),
                new Fagsak(ytelseType, aktørIdSøker1, aktørIdPleietrengende, new Saksnummer("201"), null, fom.minusDays(1)),
                new Fagsak(ytelseType, aktørIdSøker2, aktørIdPleietrengende, new Saksnummer("202"), fom, tom),
                new Fagsak(ytelseType, aktørIdSøker2, aktørIdPleietrengende, new Saksnummer("203"), tom.plusDays(1), null)
        };

        lagre(fagsaker);

        List<Fagsak> list0 = fagsakRepository.finnFagsakRelatertTil(ytelseType, aktørIdPleietrengende, fom.minusDays(10), fom.plusDays(5));
        assertThat(list0).containsOnly(fagsaker[0], fagsaker[1], fagsaker[2]);

        List<Fagsak> list1 = fagsakRepository.finnFagsakRelatertTil(ytelseType, aktørIdPleietrengende, tom, tom);
        assertThat(list1).containsOnly(fagsaker[0], fagsaker[2]);

        List<Fagsak> list2 = fagsakRepository.finnFagsakRelatertTil(ytelseType, aktørIdPleietrengende, tom, null);
        assertThat(list2).containsOnly(fagsaker[0], fagsaker[2], fagsaker[3]);
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
        assertTrue(journalpost.isPresent());

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
