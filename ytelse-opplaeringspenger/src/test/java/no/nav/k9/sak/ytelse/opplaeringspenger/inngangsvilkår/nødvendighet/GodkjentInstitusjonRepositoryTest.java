package no.nav.k9.sak.ytelse.opplaeringspenger.inngangsvilkår.nødvendighet;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.k9.sak.db.util.CdiDbAwareTest;
import no.nav.k9.sak.ytelse.opplaeringspenger.repo.GodkjentInstitusjon;
import no.nav.k9.sak.ytelse.opplaeringspenger.repo.GodkjentInstitusjonRepository;

@CdiDbAwareTest
public class GodkjentInstitusjonRepositoryTest {

    @Inject
    private EntityManager entityManager;

    @Inject
    private GodkjentInstitusjonRepository godkjentInstitusjonRepository;

    @Test
    public void skalHenteMedNavn() {
        GodkjentInstitusjon godkjentInstitusjon = new GodkjentInstitusjon("navn", LocalDate.now(), LocalDate.now().plusYears(99));
        entityManager.persist(godkjentInstitusjon);
        entityManager.flush();

        Optional<GodkjentInstitusjon> resultat = godkjentInstitusjonRepository.hentMedNavn(godkjentInstitusjon.getNavn());
        assertThat(resultat).isPresent();
        assertThat(resultat.get().getNavn()).isEqualTo(godkjentInstitusjon.getNavn());
        assertThat(resultat.get().getFomDato()).isEqualTo(godkjentInstitusjon.getFomDato());
        assertThat(resultat.get().getTomDato()).isEqualTo(godkjentInstitusjon.getTomDato());
    }

    @Test
    public void skalHenteAlle() {
        GodkjentInstitusjon godkjentInstitusjon1 = new GodkjentInstitusjon("en", LocalDate.now(), LocalDate.now().plusYears(99));
        GodkjentInstitusjon godkjentInstitusjon2 = new GodkjentInstitusjon("to", null, null);
        entityManager.persist(godkjentInstitusjon1);
        entityManager.persist(godkjentInstitusjon2);
        entityManager.flush();

        List<GodkjentInstitusjon> resultat = godkjentInstitusjonRepository.hentAlle();
        assertThat(resultat).hasSize(2);
    }
}
