package no.nav.k9.sak.behandlingslager.saksnummer;

import java.util.Objects;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import no.nav.k9.sikkerhet.context.SubjectHandler;

/**
 * Hensikten med denne er å kunne lagre kobling mellom aktør og saksnummer før sak er opprettet (reservert saksnummer)
 * JournalpostId er med for å tilrettelegge for bedre sporing i fremtiden
 * */
@Dependent
public class SaksnummerAktorRepository {

    private EntityManager entityManager;

    SaksnummerAktorRepository() {
        // for CDI proxy
    }

    @Inject
    public SaksnummerAktorRepository(EntityManager entityManager) {
        Objects.requireNonNull(entityManager, "entityManager"); //$NON-NLS-1$
        this.entityManager = entityManager;
    }

    //TODO en journalpost kan ikke knyttes til flere saksnumre
    public void lagre(String saksnummer, String aktorId, String journalpostId) {
        final Query query = entityManager.createNativeQuery("INSERT INTO SAKSNUMMER_AKTOR (SAKSNUMMER, AKTOER_ID, JOURNALPOST_ID, OPPRETTET_AV) VALUES (:saksnummer, :aktorId, :journalpostId, :opprettetAv)");
        query.setParameter("saksnummer", saksnummer);
        query.setParameter("aktorId", aktorId);
        query.setParameter("journalpostId", journalpostId);
        query.setParameter("opprettetAv", getCurrentUserId());
    }

    private static String getCurrentUserId() {
        return SubjectHandler.getSubjectHandler().getUid();
    }
}
