package no.nav.k9.sak.behandlingslager.saksnummer;

import java.math.BigInteger;
import java.util.Objects;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.Query;

import no.nav.vedtak.util.env.Environment;

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
        BigInteger tall = (BigInteger) query.getSingleResult();

        if (Environment.current().isDev()) {
            //legg merke til at SEQ_SAKSNUMMER starter på samme verdi og øker med 50 i alle miljøer
            //for å unngå å generere samme saksnummer i q og p legger vi til 1
            tall = tall.add(BigInteger.ONE);
        }

        return Long.toString(tall.longValue(), 36).toUpperCase().replace("O", "o").replace("I", "i");
    }
}
