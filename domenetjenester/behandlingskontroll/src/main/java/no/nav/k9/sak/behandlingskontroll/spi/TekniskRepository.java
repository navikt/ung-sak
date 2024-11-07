package no.nav.k9.sak.behandlingskontroll.spi;

import jakarta.persistence.EntityManager;

import no.nav.k9.felles.jpa.savepoint.RunWithSavepoint;
import no.nav.k9.felles.jpa.savepoint.Work;

public class TekniskRepository {

    private EntityManager entityManager;

    public TekniskRepository(EntityManager em) {
        this.entityManager = em;
    }

    public <V> V doWorkInSavepoint(Work<V> work) {
        RunWithSavepoint setJdbcSavepoint = new RunWithSavepoint(entityManager);
        return setJdbcSavepoint.doWork(work);
    }
 }
