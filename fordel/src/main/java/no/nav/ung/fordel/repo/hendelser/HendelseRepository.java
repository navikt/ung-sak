package no.nav.ung.fordel.repo.hendelser;

import java.util.List;
import java.util.Objects;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;


/**
 * Repository for InngåendeHendelse
 */
@ApplicationScoped
public class HendelseRepository {


    private EntityManager entityManager;

    HendelseRepository() {
        // for CDI proxy
    }

    @Inject
    public HendelseRepository(EntityManager entityManager) {
        Objects.requireNonNull(entityManager, "entityManager");
        this.entityManager = entityManager;
    }

    public InngåendeHendelseEntitet finnEksaktHendelse(Long inngåendeHendelseId) {
        return entityManager.find(InngåendeHendelseEntitet.class, inngåendeHendelseId);
    }

    public List<InngåendeHendelseEntitet> finnUhåndterteHendelser(InngåendeHendelseEntitet inngåendeHendelse) {
        TypedQuery<InngåendeHendelseEntitet> query = entityManager.createQuery(
            "from InngåendeHendelse " +
                "where hendelseType = :hendelseType " +
                "and aktørId = :aktørId " +
                "and håndtertStatus = :håndtertStatus " +
                "order by opprettetTidspunkt DESC",
            InngåendeHendelseEntitet.class);
        query.setParameter("hendelseType", inngåendeHendelse.getHendelseType().getKode());
        query.setParameter("aktørId", inngåendeHendelse.getAktørId());
        query.setParameter("håndtertStatus", InngåendeHendelseEntitet.HåndtertStatusType.MOTTATT.getDbKode());
        return query.getResultList();
    }


    public void lagreInngåendeHendelse(InngåendeHendelseEntitet inngåendeHendelse) {
        entityManager.persist(inngåendeHendelse);
        entityManager.flush();
    }

    public void oppdaterHåndtertStatus(InngåendeHendelseEntitet inngåendeHendelse, String håndtertAvHendelseId, InngåendeHendelseEntitet.HåndtertStatusType håndtertStatus) {
        inngåendeHendelse.setHåndtertStatus(håndtertStatus);
        inngåendeHendelse.setHåndtertAvHendelseId(håndtertAvHendelseId);
        entityManager.flush();
    }
}
