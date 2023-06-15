package no.nav.k9.sak.behandling.etterkontroll;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.persistence.TypedQuery;
import no.nav.k9.felles.jpa.HibernateVerktøy;

/**
 * Oppdatering av tilstand for etterkontroll av behandling.
 */
@Dependent
public class EtterkontrollRepository {

    private EntityManager entityManager;

    protected EtterkontrollRepository() {
        // for CDI proxy
    }

    @Inject
    public EtterkontrollRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    protected EntityManager getEntityManager() {
        return entityManager;
    }


    /**
     * Lagrer etterkontroll
     *
     * @return id for {@link Etterkontroll} opprettet
     */
    public Long lagre(Etterkontroll etterkontroll) {
        getEntityManager().persist(etterkontroll);
        getEntityManager().flush();
        return etterkontroll.getId();
    }


    public List<Etterkontroll> finnKandidaterForAutomatiskEtterkontroll() {

        Query query1 = getEntityManager().createQuery(
            "SELECT k " +
                "FROM Etterkontroll k" +
                "    WHERE k.erBehandlet = false" +
                "    AND k.kontrollTidspunkt <= :periodeTilbake" //$NON-NLS-1$
        );
        query1.setParameter("periodeTilbake", LocalDate.now().atStartOfDay());
        @SuppressWarnings("unchecked")
        List<Etterkontroll> result = query1.getResultList();

        if (result.isEmpty()) {
            return Collections.emptyList();
        }
        return new ArrayList<>(result);
    }

    public Etterkontroll hent(String id) {
        TypedQuery<Etterkontroll> query = getEntityManager().createQuery("from Etterkontroll k where id=:etterkontrollId", Etterkontroll.class);
        query.setParameter("etterkontrollId", id);
        return HibernateVerktøy.hentEksaktResultat(query);
    }
}
