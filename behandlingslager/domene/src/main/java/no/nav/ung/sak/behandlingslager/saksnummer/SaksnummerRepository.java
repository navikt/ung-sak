package no.nav.ung.sak.behandlingslager.saksnummer;

import java.util.Objects;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import no.nav.ung.sak.typer.Saksnummer;

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
        var tall = (Number) query.getSingleResult();

        return Long.toString(tall.longValue(), 36).toUpperCase().replace("O", "o").replace("I", "i");
    }

}
