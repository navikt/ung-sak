package no.nav.ung.sak.behandlingslager.etterlysning;

import java.util.List;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.k9.felles.jpa.HibernateVerktøy;
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

    public EtterlysningEntitet lagre(EtterlysningEntitet etterlysning) {
        if (etterlysning.getUttalelse() != null) {
            entityManager.persist(etterlysning.getUttalelse());
        }
        entityManager.persist(etterlysning);
        return etterlysning;
    }

    public List<EtterlysningEntitet> lagre(List<EtterlysningEntitet> etterlysninger) {
        etterlysninger.forEach(this::lagre);
        return etterlysninger;
    }

    public List<EtterlysningEntitet> hentEtterlysninger(Long behandlingId) {
        final var etterlysninger = entityManager.createQuery("select e from Etterlysning e " +
                                                             "where e.behandlingId = :behandlingId", EtterlysningEntitet.class)
            .setParameter("behandlingId", behandlingId)
            .getResultList();
        return etterlysninger;
    }

    public List<EtterlysningEntitet> hentEtterlysninger(Long behandlingId, EtterlysningType type) {
        final var etterlysninger = entityManager.createQuery("select e from Etterlysning e " +
                                                             "where e.behandlingId = :behandlingId and e.type = :type", EtterlysningEntitet.class)
            .setParameter("behandlingId", behandlingId)
            .setParameter("type", type)
            .getResultList();
        return etterlysninger;
    }


    public List<EtterlysningEntitet> hentOpprettetEtterlysninger(Long behandlingId, EtterlysningType type) {
        final var etterlysninger = entityManager.createQuery("select e from Etterlysning e " +
                                                             "where e.behandlingId = :behandlingId and e.type = :type and status = :status", EtterlysningEntitet.class)
            .setParameter("behandlingId", behandlingId)
            .setParameter("type", type)
            .setParameter("status", EtterlysningStatus.OPPRETTET)
            .getResultList();
        return etterlysninger;
    }

    public List<EtterlysningEntitet> hentEtterlysningerSomSkalAvbrytes(Long behandlingId) {
        final var etterlysninger = entityManager.createQuery("select e from Etterlysning e " +
                                                             "where e.behandlingId = :behandlingId and e.type = :type and status = :status", EtterlysningEntitet.class)
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


    public EtterlysningEntitet hentEtterlysningForEksternReferanse(UUID eksternReferanse) {
        return HibernateVerktøy.hentEksaktResultat(
            entityManager.createQuery(
                    "select e from Etterlysning e " +
                    "where e.eksternReferanse = :eksternReferanse", EtterlysningEntitet.class)
                .setParameter("eksternReferanse", eksternReferanse)
        );
    }

    public EtterlysningEntitet hentEtterlysning(Long id) {
        return HibernateVerktøy.hentEksaktResultat(
            entityManager.createQuery(
                    "select e from Etterlysning e " +
                    "where e.id = :id", EtterlysningEntitet.class)
                .setParameter("id", id)
        );
    }
}
