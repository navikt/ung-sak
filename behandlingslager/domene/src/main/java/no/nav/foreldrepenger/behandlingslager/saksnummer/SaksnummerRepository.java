package no.nav.foreldrepenger.behandlingslager.saksnummer;

import no.nav.vedtak.felles.jpa.VLPersistenceUnit;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.Query;
import java.math.BigInteger;
import java.util.Objects;

@ApplicationScoped
public class SaksnummerRepository {

    private EntityManager entityManager;

    SaksnummerRepository() {
        // for CDI proxy
    }

    @Inject
    public SaksnummerRepository(@VLPersistenceUnit EntityManager entityManager) {
        Objects.requireNonNull(entityManager, "entityManager"); //$NON-NLS-1$
        this.entityManager = entityManager;
    }

    public String genererNyttSaksnummer() {
        final Query query = entityManager.createNativeQuery("SELECT nextval('SEQ_SAKSNUMMER')");
        final BigInteger tall = (BigInteger) query.getSingleResult();

        return Long.toString(tall.longValue(), 36).toUpperCase().replace("O", "o").replace("I", "i");
    }
}
