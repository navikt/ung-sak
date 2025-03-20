package no.nav.ung.sak.behandlingslager.etterlysning;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.ung.kodeverk.etterlysning.EtterlysningStatus;
import no.nav.ung.kodeverk.etterlysning.EtterlysningType;

import java.util.List;

@ApplicationScoped
public class EtterlysningRepository {

    private EntityManager entityManager;


    public EtterlysningRepository() {
    }

    @Inject
    public EtterlysningRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public Etterlysning lagre(Etterlysning etterlysning) {
        entityManager.persist(etterlysning);
        return etterlysning;
    }

    public List<Etterlysning> lagre(List<Etterlysning> etterlysninger) {
        etterlysninger.forEach(entityManager::persist);
        return etterlysninger;
    }

    public List<Etterlysning> hentEtterlysninger(Long behandlingId) {
        final var etterlysninger = entityManager.createQuery("select e from Etterlysning e " +
                "where e.behandlingId = :behandlingId", Etterlysning.class)
            .setParameter("behandlingId", behandlingId)
            .getResultList();
        return etterlysninger;
    }

    public List<Etterlysning> hentEtterlysninger(Long behandlingId, EtterlysningType type) {
        final var etterlysninger = entityManager.createQuery("select e from Etterlysning e " +
                "where e.behandlingId = :behandlingId and e.type = :type", Etterlysning.class)
            .setParameter("behandlingId", behandlingId)
            .setParameter("type", type)
            .getResultList();
        return etterlysninger;
    }


    public List<Etterlysning> hentOpprettetEtterlysninger(Long behandlingId, EtterlysningType type) {
        final var etterlysninger = entityManager.createQuery("select e from Etterlysning e " +
                "where e.behandlingId = :behandlingId and e.type = :type and status = :status", Etterlysning.class)
            .setParameter("behandlingId", behandlingId)
            .setParameter("type", type.getKode())
            .setParameter("status", EtterlysningStatus.OPPRETTET)
            .getResultList();
        return etterlysninger;
    }

    public List<Etterlysning> hentEtterlysningerSomSkalAvbrytes(Long behandlingId) {
        final var etterlysninger = entityManager.createQuery("select e from Etterlysning e " +
                "where e.behandlingId = :behandlingId and e.type = :type and status = :status", Etterlysning.class)
            .setParameter("behandlingId", behandlingId)
            .setParameter("status", EtterlysningStatus.SKAL_AVBRYTES)
            .getResultList();
        return etterlysninger;
    }


}
