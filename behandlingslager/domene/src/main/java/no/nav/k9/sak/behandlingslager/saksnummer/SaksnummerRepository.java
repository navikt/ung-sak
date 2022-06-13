package no.nav.k9.sak.behandlingslager.saksnummer;

import java.math.BigInteger;
import java.util.Objects;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;

@Dependent
public class SaksnummerRepository {

    private EntityManager entityManager;

    SaksnummerRepository() {
        // for CDI proxy
    }

    @Inject
    public SaksnummerRepository(EntityManager entityManager) {
        Objects.requireNonNull(entityManager, "entityManager"); //$NON-NLS-1$
        this.entityManager = entityManager;
    }

    public String genererNyttSaksnummer() {
        final Query query = entityManager.createNativeQuery("SELECT nextval('SEQ_SAKSNUMMER')");
        final BigInteger tall = (BigInteger) query.getSingleResult();

        return Long.toString(tall.longValue(), 36).toUpperCase().replace("O", "o").replace("I", "i");
    }
}
