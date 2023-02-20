package no.nav.k9.sak.ytelse.pleiepengerbarn.repo.infotrygd;

import java.util.Objects;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import no.nav.k9.sak.typer.AktørId;

@Dependent
public class PsbInfotrygdRepository {

    private EntityManager entityManager;


    @Inject
    public PsbInfotrygdRepository(EntityManager entityManager) {
        Objects.requireNonNull(entityManager, "entityManager");
        this.entityManager = entityManager;
    }


    public void lagre(AktørId aktørId) {
        final Query q = entityManager.createNativeQuery("INSERT INTO PSB_INFOTRYGD_PERSON (AKTOER_ID) VALUES (:aktorId) ON CONFLICT DO NOTHING");
        q.setParameter("aktorId", aktørId.getAktørId());
        q.executeUpdate();
        entityManager.flush();
    }

    public boolean finnes(AktørId aktørId) {
        final Query q = entityManager.createNativeQuery("SELECT AKTOER_ID FROM PSB_INFOTRYGD_PERSON WHERE AKTOER_ID = :aktorId");
        q.setParameter("aktorId", aktørId.getAktørId());
        return q.getResultList().size() > 0;
    }
}
