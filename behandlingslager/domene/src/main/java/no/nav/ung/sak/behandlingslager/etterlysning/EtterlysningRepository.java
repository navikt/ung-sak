package no.nav.ung.sak.behandlingslager.etterlysning;

import java.util.List;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.k9.felles.jpa.HibernateVerktøy;
import no.nav.ung.kodeverk.etterlysning.EtterlysningStatus;
import no.nav.ung.kodeverk.etterlysning.EtterlysningType;

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
        if (etterlysning.getUttalelse() != null) {
            entityManager.persist(etterlysning.getUttalelse());
        }
        entityManager.persist(etterlysning);
        return etterlysning;
    }

    public List<Etterlysning> lagre(List<Etterlysning> etterlysninger) {
        etterlysninger.forEach(this::lagre);
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
            .setParameter("type", type)
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


    public List<Etterlysning> hentUtløpteEtterlysningerSomVenterPåSvar(Long behandlingId) {
        final var etterlysninger = entityManager.createQuery("select e from Etterlysning e " +
                "where e.behandlingId = :behandlingId and e.type = :type and status = :status AND frist < :naa", Etterlysning.class)
            .setParameter("status", EtterlysningStatus.VENTER)
            .setParameter("behandlingId", behandlingId)
            .setParameter("naa", LocalDateTime.now())
            .getResultList();
        return etterlysninger;
    }


    public Etterlysning hentEtterlysningForEksternReferanse(UUID eksternReferanse) {
        return HibernateVerktøy.hentEksaktResultat(
            entityManager.createQuery(
                    "select e from Etterlysning e " +
                    "where e.eksternReferanse = :eksternReferanse", Etterlysning.class)
                .setParameter("eksternReferanse", eksternReferanse)
        );
    }

    public Etterlysning hentEtterlysning(Long id) {
        return HibernateVerktøy.hentEksaktResultat(
            entityManager.createQuery(
                    "select e from Etterlysning e " +
                    "where e.id = :id", Etterlysning.class)
                .setParameter("id", id)
        );
    }
}
