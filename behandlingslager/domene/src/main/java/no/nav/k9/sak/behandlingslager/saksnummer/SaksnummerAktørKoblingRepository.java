package no.nav.k9.sak.behandlingslager.saksnummer;

import java.util.Objects;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;

/**
 * Hensikten med denne er å kunne lagre kobling mellom aktør og saksnummer før sak er opprettet (reservert saksnummer)
 * JournalpostId er med for å tilrettelegge for bedre sporing i fremtiden
 * */
@Dependent
public class SaksnummerAktørKoblingRepository {

    private EntityManager entityManager;

    SaksnummerAktørKoblingRepository() {
        // for CDI proxy
    }

    @Inject
    public SaksnummerAktørKoblingRepository(EntityManager entityManager) {
        Objects.requireNonNull(entityManager, "entityManager"); //$NON-NLS-1$
        this.entityManager = entityManager;
    }

    public SaksnummerAktørKoblingEntitet hent(String saksnummer, String aktørId, String journalpostId) {
        final Query query = entityManager.createNativeQuery("SELECT * FROM SAKSNUMMER_AKTOR where saksnummer=:saksnummer and aktoer_id=:aktorId and journalpost_id=:journalpostId and slettet=false", SaksnummerAktørKoblingEntitet.class);
        query.setParameter("saksnummer", saksnummer);
        query.setParameter("aktorId", aktørId);
        query.setParameter("journalpostId", journalpostId);
        final var result = query.getResultList();
        if (result.isEmpty()) {
            return null;
        }
        return (SaksnummerAktørKoblingEntitet) result.get(0);
    }

    //TODO hva med å sjekke at aktørid/saksnummer/journalpostid er gyldig?
    public void lagre(String saksnummer, String aktørId, String journalpostId) {
        if (hent(saksnummer, aktørId, journalpostId) != null) {
            return; //Unngår exception ved duplikat
        }

        final Query query = entityManager.createNativeQuery("SELECT * FROM SAKSNUMMER_AKTOR where saksnummer!=:saksnummer and journalpost_id=:journalpostId and slettet=false", SaksnummerAktørKoblingEntitet.class);
        query.setParameter("saksnummer", saksnummer);
        query.setParameter("journalpostId", journalpostId);
        final var result = query.getResultList();
        if (!result.isEmpty()) {
            throw new IllegalArgumentException("Kan ikke koble en journalpost til flere saksnumre");
        }

        final var entitet = new SaksnummerAktørKoblingEntitet(saksnummer, aktørId, journalpostId);
        entityManager.persist(entitet);
        entityManager.flush();
    }

    public void slett(String saksnummer, String aktørId, String journalpostId) {
        final Query query = entityManager.createNativeQuery("SELECT * FROM SAKSNUMMER_AKTOR where saksnummer=:saksnummer and aktoer_id=:aktorId and journalpost_id=:journalpostId and slettet=false", SaksnummerAktørKoblingEntitet.class);
        query.setParameter("saksnummer", saksnummer);
        query.setParameter("aktorId", aktørId);
        query.setParameter("journalpostId", journalpostId);
        final var result = query.getResultList();
        if (!result.isEmpty()) {
            var entitet = (SaksnummerAktørKoblingEntitet) result.get(0);
            entitet.setSlettet();
            entityManager.persist(entitet);
            entityManager.flush();
        }
    }
}
