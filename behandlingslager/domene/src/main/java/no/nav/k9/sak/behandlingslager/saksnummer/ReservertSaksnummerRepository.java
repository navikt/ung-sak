package no.nav.k9.sak.behandlingslager.saksnummer;

import java.util.Objects;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;

@Dependent
public class ReservertSaksnummerRepository {

    private EntityManager entityManager;

    ReservertSaksnummerRepository() {
        // for CDI proxy
    }

    @Inject
    public ReservertSaksnummerRepository(EntityManager entityManager) {
        Objects.requireNonNull(entityManager, "entityManager"); //$NON-NLS-1$
        this.entityManager = entityManager;
    }

    public ReservertSaksnummerEntitet hent(String saksnummer) {
        final Query query = entityManager.createNativeQuery("SELECT * FROM RESERVERT_SAKSNUMMER where saksnummer=:saksnummer", ReservertSaksnummerEntitet.class);
        query.setParameter("saksnummer", saksnummer);
        return (ReservertSaksnummerEntitet) query.getSingleResult();
    }

    public void lagre(String saksnummer, FagsakYtelseType ytelseType, String brukerAktørid, String pleietrengendeAktørId) {
        if (hent(saksnummer) != null) {
            throw new IllegalArgumentException("Saksnummer er allerede reservert");
        }
        final var entitet = new ReservertSaksnummerEntitet(saksnummer, ytelseType, brukerAktørid, pleietrengendeAktørId);
        entityManager.persist(entitet);
        entityManager.flush();
    }
}
