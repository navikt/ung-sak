package no.nav.k9.sak.behandling.revurdering.etterkontroll;

import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;

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


    public List<Etterkontroll> finnEtterkontrollAvTypeForFagsak(long fagsakId, KontrollType kontrollType) {
        List<Etterkontroll> resultList = entityManager.createQuery(
                "from Etterkontroll " +
                    "where fagsakId = :fagsakId and kontrollType = :kontrollType", Etterkontroll.class)//$NON-NLS-1$
            .setParameter("fagsakId", fagsakId)
            .setParameter("kontrollType", kontrollType)
            .getResultList();

        return resultList;

    }

    public List<Etterkontroll> finnEtterkontrollForFagsak(long fagsakId) {
        List<Etterkontroll> resultList = entityManager.createQuery(
                "from Etterkontroll " +
                    "where fagsakId = :fagsakId", Etterkontroll.class)//$NON-NLS-1$
            .setParameter("fagsakId", fagsakId)
            .getResultList();

        return resultList;

    }

    /**
     * Lagrer etterkontroll  på en fagsak
     *
     * @return id for {@link Etterkontroll} opprettet
     */
    public Long lagre(Etterkontroll etterkontroll) {
        getEntityManager().persist(etterkontroll);
        getEntityManager().flush();
        return etterkontroll.getId();
    }


    /**
     * Setter sak til behandlet=Y i etterkontroll slik at batch ikke plukker saken opp for revurdering
     *
     * @param fagsakId id i databasen
     */
    public int avflaggDersomEksisterer(Long fagsakId, KontrollType kontrollType) {
        int antall = 0;
        List<Etterkontroll> etterkontroll = finnEtterkontrollAvTypeForFagsak(fagsakId, kontrollType);
        for (Etterkontroll ek : etterkontroll) {
            ek.setErBehandlet(true);
            lagre(ek);
            antall++;
        }
        return antall;
    }

    public List<Etterkontroll> finnKandidaterForAutomatiskEtterkontroll(Period etterkontrollTidTilbake) {

        LocalDate datoTilbakeITid = LocalDate.now().minus(etterkontrollTidTilbake);
        java.time.LocalDateTime datoTidTilbake = datoTilbakeITid.atStartOfDay();

        Query query1 = getEntityManager().createQuery(
            "SELECT k " +
                "FROM Etterkontroll k" +
                "    WHERE k.erBehandlet = false" +
                "    AND k.kontrollTidspunkt <= :periodeTilbake" //$NON-NLS-1$
        );
        query1.setParameter("periodeTilbake", datoTidTilbake);
        @SuppressWarnings("unchecked")
        List<Etterkontroll> result = query1.getResultList();

        if (result.isEmpty()) {
            return Collections.emptyList();
        }
        return new ArrayList<>(result);
    }

}
