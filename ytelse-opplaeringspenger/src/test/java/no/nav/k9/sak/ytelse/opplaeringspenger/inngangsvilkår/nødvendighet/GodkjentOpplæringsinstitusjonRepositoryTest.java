package no.nav.k9.sak.ytelse.opplaeringspenger.inngangsvilkår.nødvendighet;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.k9.sak.db.util.CdiDbAwareTest;
import no.nav.k9.sak.ytelse.opplaeringspenger.repo.GodkjentOpplæringsinstitusjon;
import no.nav.k9.sak.ytelse.opplaeringspenger.repo.GodkjentOpplæringsinstitusjonRepository;

@CdiDbAwareTest
public class GodkjentOpplæringsinstitusjonRepositoryTest {

    @Inject
    private EntityManager entityManager;

    @Inject
    private GodkjentOpplæringsinstitusjonRepository godkjentOpplæringsinstitusjonRepository;

    @Test
    public void skalHenteMedUuid() {
        GodkjentOpplæringsinstitusjon godkjentOpplæringsInstitusjon = new GodkjentOpplæringsinstitusjon(UUID.randomUUID(), "navn", LocalDate.now(), LocalDate.now().plusYears(99));
        entityManager.persist(godkjentOpplæringsInstitusjon);
        entityManager.flush();

        Optional<GodkjentOpplæringsinstitusjon> resultat = godkjentOpplæringsinstitusjonRepository.hentMedUuid(godkjentOpplæringsInstitusjon.getUuid());
        assertThat(resultat).isPresent();
        assertThat(resultat.get().getUuid()).isEqualTo(godkjentOpplæringsInstitusjon.getUuid());
        assertThat(resultat.get().getNavn()).isEqualTo(godkjentOpplæringsInstitusjon.getNavn());
        assertThat(resultat.get().getFomDato()).isEqualTo(godkjentOpplæringsInstitusjon.getFomDato());
        assertThat(resultat.get().getTomDato()).isEqualTo(godkjentOpplæringsInstitusjon.getTomDato());
    }

    @Test
    public void skalHenteAlle() {
        GodkjentOpplæringsinstitusjon godkjentOpplæringsinstitusjon1 = new GodkjentOpplæringsinstitusjon(UUID.randomUUID(), "en", LocalDate.now(), LocalDate.now().plusYears(99));
        GodkjentOpplæringsinstitusjon godkjentOpplæringsinstitusjon2 = new GodkjentOpplæringsinstitusjon(UUID.randomUUID(), "to", null, null);
        entityManager.persist(godkjentOpplæringsinstitusjon1);
        entityManager.persist(godkjentOpplæringsinstitusjon2);
        entityManager.flush();

        List<GodkjentOpplæringsinstitusjon> resultat = godkjentOpplæringsinstitusjonRepository.hentAlle();
        assertThat(resultat).hasSize(2);
    }
}
