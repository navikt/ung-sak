package no.nav.ung.sak.behandlingslager.fagsak;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;

import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepositoryFeil;

@Dependent
public class FagsakLåsRepository {

    private EntityManager entityManager;

    protected FagsakLåsRepository() {
    }

    @Inject
    public FagsakLåsRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    /**
     * Tar lås på underliggende rader
     *
     * @param fagsakId fagsaken
     * @return låsen
     */
    public FagsakLås taLås(final Long fagsakIdIn) {
        final LockModeType lockModeType = LockModeType.PESSIMISTIC_WRITE;

        FagsakLås lås = new FagsakLås(fagsakIdIn);

        // sjekker om fagsakId != null slik at det fungerer som no-op for ferske, unpersisted entiteter
        if (fagsakIdIn != null) {
            // bruker enkle queries slik at unngår laste eager associations og entiteter her
            låsFagsak(fagsakIdIn, lockModeType);
        }

        return lås;
    }

    /**
     * Tar lås på underliggende rader
     * Kaller bare på {@link #taLås(Long)}
     *
     * @param fagsak fagsaken
     * @return låsen
     */
    public FagsakLås taLås(Fagsak fagsak) {
        return taLås(fagsak.getId());
    }

    private Long låsFagsak(final Long fagsakId, LockModeType lockModeType) {
        Object[] resultFs = (Object[]) entityManager
            .createQuery("select fs.id, fs.versjon from Fagsak fs where fs.id=:id") //$NON-NLS-1$
            .setParameter("id", fagsakId) //$NON-NLS-1$
            .setLockMode(lockModeType)
            .getSingleResult();
        return (Long) resultFs[0];
    }

    /**
     * tvinger inkrementerer versjon på relevante parent entiteteter (fagsak, fagsakrelasjon, behandling) slik
     * at andre vil oppdage endringer og få OptimisticLockException
     */
    public void oppdaterLåsVersjon(FagsakLås lås) {
        if (lås.getFagsakId() != null) {
            verifisertLås(lås);
        }
    }

    private void verifisertLås(FagsakLås lås) {
        Long id = lås.getFagsakId();
        // NB - Oracle syntax
        Object versjon = entityManager.createNativeQuery("select versjon from FAGSAK where id =:fagsakId for update nowait")
            .setParameter("fagsakId", id)
            .getSingleResult();

        if (versjon == null) {
            throw BehandlingRepositoryFeil.FACTORY.fantIkkeEntitetForLåsing(Fagsak.class.getSimpleName(), id).toException();
        } else {
            int updated = entityManager.createNativeQuery("update FAGSAK set versjon=versjon+1 where id=:fagsakId and versjon=:versjon")
                .setParameter("fagsakId", id)
                .setParameter("versjon", versjon)
                .executeUpdate();
            if (updated != 1) {
                throw new IllegalStateException("Kunne ikke verifisere lås på Fagsak: " + id + ", fikk ikke oppdatert versjon " + versjon);
            }
        }
    }
}
