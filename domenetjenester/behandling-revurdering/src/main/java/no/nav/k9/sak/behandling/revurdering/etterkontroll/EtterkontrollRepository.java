package no.nav.k9.sak.behandling.revurdering.etterkontroll;

import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;

import no.nav.k9.kodeverk.behandling.BehandlingResultatType;
import no.nav.k9.kodeverk.behandling.BehandlingType;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
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


    public List<Etterkontroll> finnEtterkontrollForFagsak(long fagsakId,KontrollType kontrollType ) {
        List<Etterkontroll> resultList = entityManager.createQuery(
            "from Etterkontroll " +
                "where fagsakId = :fagsakId and kontrollType = :kontrollType", Etterkontroll.class)//$NON-NLS-1$
            .setParameter("fagsakId", fagsakId)
            .setParameter("kontrollType",kontrollType)
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
    public int avflaggDersomEksisterer(Long fagsakId,KontrollType kontrollType){
        int antall=0;
        List<Etterkontroll> etterkontroll=  finnEtterkontrollForFagsak( fagsakId, kontrollType );
        for(Etterkontroll ek : etterkontroll){
            ek.setErBehandlet(true);
            lagre(ek);
            antall++;
        }
        return antall;
    }

    public List<Behandling> finnKandidaterForAutomatiskEtterkontroll(Period etterkontrollTidTilbake) {

        LocalDate datoTilbakeITid = LocalDate.now().minus(etterkontrollTidTilbake);
        java.time.LocalDateTime datoTidTilbake = datoTilbakeITid.atStartOfDay();

        Query query1 = getEntityManager().createQuery(
            " FROM Behandling b WHERE " +
                "  behandlingType in (:ytelsetyper) " +
                "  AND EXISTS (SELECT k FROM Etterkontroll k" +
                "    WHERE k.fagsakId=b.fagsak.id  " +
                "    AND k.erBehandlet = false" +
                "    AND k.kontrollTidspunkt <= :periodeTilbake)" +
                "  AND b.behandlingResultatType NOT IN :henlagtKoder " +
                " ORDER BY b.opprettetTidspunkt DESC" //$NON-NLS-1$
        );
        query1.setParameter("ytelsetyper", BehandlingType.getYtelseBehandlingTyper());
        query1.setParameter("periodeTilbake", datoTidTilbake);
        query1.setParameter("henlagtKoder", BehandlingResultatType.getAlleHenleggelseskoder());
        @SuppressWarnings("unchecked")
        List<Behandling> result = query1.getResultList();

        Collection<Behandling> ret = getSisteBehandlingPerFagsak(result);

        if (ret.isEmpty()) {
            return Collections.emptyList();
        }
        return new ArrayList<>(ret);
    }


    private Collection<Behandling> getSisteBehandlingPerFagsak(List<Behandling> behandlinger) {
        Map<Long, Behandling> fagsakIdToBehandling = new LinkedHashMap<>();
        for (Behandling b :behandlinger ) {
            Long fagsakId = b.getFagsakId(); // NOSONAR
            Behandling tb = fagsakIdToBehandling.get(fagsakId);
            if(tb == null || (b.getOpprettetTidspunkt().compareTo(tb.getOpprettetTidspunkt())) > 0 ) {
                fagsakIdToBehandling.put(fagsakId, b);
            }
        }
        return fagsakIdToBehandling.values();
    }


}
