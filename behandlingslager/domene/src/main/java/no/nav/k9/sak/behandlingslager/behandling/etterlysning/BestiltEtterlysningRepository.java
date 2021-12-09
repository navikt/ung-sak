package no.nav.k9.sak.behandlingslager.behandling.etterlysning;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.persistence.EntityManager;

@Dependent
public class BestiltEtterlysningRepository {

    private EntityManager entityManager;

    @Inject
    public BestiltEtterlysningRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public void lagre(BestiltEtterlysning bestiltEtterlysning) {
        var bestiltEtterlysninger = hentFor(bestiltEtterlysning.getFagsakId());
        if (bestiltEtterlysninger.stream().noneMatch(it -> it.getPeriode().overlapper(bestiltEtterlysning.getPeriode())
            && Objects.equals(it.getDokumentMal(), bestiltEtterlysning.getDokumentMal()))) {
            entityManager.persist(bestiltEtterlysning);
            entityManager.flush();
        }
    }

    public void lagre(Set<BestiltEtterlysning> bestiltEtterlysninger) {
        if (bestiltEtterlysninger.isEmpty()) {
            return;
        }
        var fagsakId = finnFagsakId(bestiltEtterlysninger);
        var tidligereBestiltEtterlysninger = hentFor(fagsakId);
        var persistedSomething = false;
        for (BestiltEtterlysning bestiltEtterlysning : bestiltEtterlysninger) {
            if (tidligereBestiltEtterlysninger.stream().noneMatch(it -> it.getPeriode().overlapper(bestiltEtterlysning.getPeriode())
                && Objects.equals(it.getDokumentMal(), bestiltEtterlysning.getDokumentMal()))) {
                entityManager.persist(bestiltEtterlysning);
                persistedSomething = true;
            }
        }
        if (persistedSomething) {
            entityManager.flush();
        }
    }

    private Long finnFagsakId(Set<BestiltEtterlysning> bestiltEtterlysninger) {
        var fagsakIder = bestiltEtterlysninger.stream()
            .map(BestiltEtterlysning::getFagsakId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
        if (fagsakIder.size() == 1) {
            return fagsakIder.iterator().next();
        }
        throw new IllegalStateException("Fant flere fagsakIder");
    }

    public List<BestiltEtterlysning> hentFor(Long fagsakId) {
        var query = entityManager.createQuery("SELECT e " +
            "FROM BestiltEtterlysning e " +
            "WHERE e.fagsakId = :fagsakId", BestiltEtterlysning.class);
        query.setParameter("fagsakId", fagsakId);

        return query.getResultList();
    }
}
