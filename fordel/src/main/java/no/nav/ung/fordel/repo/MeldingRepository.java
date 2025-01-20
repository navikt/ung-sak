package no.nav.ung.fordel.repo;

import java.util.Objects;
import java.util.Optional;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

@Dependent
public class MeldingRepository {

    private EntityManager entityManager;

    @Inject
    public MeldingRepository(EntityManager entityManager) {
        this.entityManager = Objects.requireNonNull(entityManager, "entityManager");
    }

    public void lagre(MottattMeldingEntitet melding) {
        entityManager.persist(melding);
        entityManager.flush();
    }

    public void lagre(ProduksjonsstyringOppgaveEntitet oppgave) {
        entityManager.persist(oppgave);
        entityManager.flush();
    }

    @SuppressWarnings("unchecked")
    public Optional<MottattMeldingEntitet> finnMottattMelding(String journalpostId) {
        return entityManager.createQuery("select mm from MottattMelding mm where mm.journalpostId=:journalpostId")
            .setParameter("journalpostId", journalpostId)
            .getResultStream().findFirst();
    }

}
