package no.nav.k9.sak.ytelse.opplaeringspenger.repo;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.k9.sak.db.util.CdiDbAwareTest;
import no.nav.k9.sak.ytelse.opplaeringspenger.repo.godkjentopplaeringsinstitusjon.GodkjentOpplæringsinstitusjon;
import no.nav.k9.sak.ytelse.opplaeringspenger.repo.godkjentopplaeringsinstitusjon.GodkjentOpplæringsinstitusjonPeriode;
import no.nav.k9.sak.ytelse.opplaeringspenger.repo.godkjentopplaeringsinstitusjon.GodkjentOpplæringsinstitusjonRepository;

@CdiDbAwareTest
public class GodkjentOpplæringsinstitusjonRepositoryTest {

    @Inject
    private EntityManager entityManager;

    @Inject
    private GodkjentOpplæringsinstitusjonRepository godkjentOpplæringsinstitusjonRepository;

    @Test
    public void skalHenteMedUuid() {
        LocalDate idag = LocalDate.now();
        UUID uuid = UUID.randomUUID();
        GodkjentOpplæringsinstitusjonPeriode periode = new GodkjentOpplæringsinstitusjonPeriode(idag, idag.plusYears(99));
        GodkjentOpplæringsinstitusjon godkjentOpplæringsInstitusjon = new GodkjentOpplæringsinstitusjon(uuid, "navn", List.of(periode));
        entityManager.persist(godkjentOpplæringsInstitusjon);
        entityManager.flush();

        Optional<GodkjentOpplæringsinstitusjon> resultat = godkjentOpplæringsinstitusjonRepository.hentMedUuid(godkjentOpplæringsInstitusjon.getUuid());
        assertThat(resultat).isPresent();
        assertThat(resultat.get().getUuid()).isEqualTo(uuid);
        assertThat(resultat.get().getNavn()).isEqualTo("navn");
        assertThat(resultat.get().getPerioder()).hasSize(1);
        assertThat(resultat.get().getPerioder().get(0).getPeriode().getFomDato()).isEqualTo(idag);
        assertThat(resultat.get().getPerioder().get(0).getPeriode().getTomDato()).isEqualTo(idag.plusYears(99));
    }

    @Test
    public void skalHenteAlle() {
        LocalDate idag = LocalDate.now();
        GodkjentOpplæringsinstitusjonPeriode periode1 = new GodkjentOpplæringsinstitusjonPeriode(idag, idag.plusYears(99));
        GodkjentOpplæringsinstitusjonPeriode periode2 = new GodkjentOpplæringsinstitusjonPeriode(idag, idag.plusYears(99));
        GodkjentOpplæringsinstitusjon godkjentOpplæringsinstitusjon1 = new GodkjentOpplæringsinstitusjon(UUID.randomUUID(), "en", List.of(periode1));
        GodkjentOpplæringsinstitusjon godkjentOpplæringsinstitusjon2 = new GodkjentOpplæringsinstitusjon(UUID.randomUUID(), "to", List.of(periode2));
        entityManager.persist(godkjentOpplæringsinstitusjon1);
        entityManager.persist(godkjentOpplæringsinstitusjon2);
        entityManager.flush();

        List<GodkjentOpplæringsinstitusjon> resultat = godkjentOpplæringsinstitusjonRepository.hentAlle();
        assertThat(resultat.size()).isGreaterThanOrEqualTo(2);
    }
}
