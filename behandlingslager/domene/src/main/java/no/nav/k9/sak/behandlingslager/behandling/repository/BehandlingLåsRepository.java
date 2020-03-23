package no.nav.k9.sak.behandlingslager.behandling.repository;

import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.CacheRetrieveMode;
import javax.persistence.EntityManager;
import javax.persistence.FlushModeType;
import javax.persistence.LockModeType;

import no.nav.k9.sak.behandlingslager.behandling.Behandling;

/**
 * @see BehandlingLås
 */
@ApplicationScoped
public class BehandlingLåsRepository {

    private static final Map<String, Object> BYPASS_PROPS = Map.of("javax.persistence.cache.retrieveMode", CacheRetrieveMode.BYPASS);
    
    private static final Pattern DIGITS_PATTERN = Pattern.compile("\\d+");
    private EntityManager entityManager;

    protected BehandlingLåsRepository() {
        // for CDI proxy
    }

    @Inject
    public BehandlingLåsRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    /** Initialiser lås og ta lock på tilhørende database rader. */
    public BehandlingLås taLås(final Long behandlingId) {
        if (behandlingId != null) {
            låsBehandling(behandlingId);
            BehandlingLås lås = new BehandlingLås(behandlingId);
            return lås;
        } else {
            return new BehandlingLås(null);
        }

    }

    /** Initialiser lås og ta lock på tilhørende database rader. */
    public BehandlingLås taLås(final String behandlingId) {
        if (DIGITS_PATTERN.matcher(behandlingId).matches()) {
            return taLås(Long.parseLong(behandlingId));
        } else {
            return taLås(UUID.fromString(behandlingId));
        }
    }

    public BehandlingLås taLås(final UUID eksternBehandlingRef) {
        if (eksternBehandlingRef != null) {
            Long behandlingId = låsBehandling(eksternBehandlingRef);
            BehandlingLås lås = new BehandlingLås(behandlingId);
            return lås;
        } else {
            return new BehandlingLås(null);
        }
    }

    private void låsBehandling(final Long behandlingId) {
        // bruk native query så vi ikke går i beina på hibernate session cache og transiente data
        entityManager
            .createNativeQuery("select 1 from BEHANDLING beh where beh.id=:id FOR UPDATE") //$NON-NLS-1$
            .setParameter("id", behandlingId) //$NON-NLS-1$
            .setFlushMode(FlushModeType.COMMIT)
            .getSingleResult();
    }

    private Long låsBehandling(final UUID eksternBehandlingRef) {
        // bruk native query så vi ikke går i beina på hibernate session cache og transiente data
        Object result = entityManager
            .createNativeQuery("select beh.id from BEHANDLING beh where beh.uuid=:uuid FOR UPDATE") //$NON-NLS-1$
            .setParameter("uuid", eksternBehandlingRef) //$NON-NLS-1$
            .getSingleResult();
        return Long.valueOf(((Number) result).longValue()); // JPA tar ikke scalar output mapping direkte
    }

    /**
     * Verifiser lås ved å sjekke mot underliggende lager.
     * tvinger inkrementerer versjon på relevante parent entiteteter (fagsak, fagsakrelasjon, behandling) slik
     * at andre vil oppdage endringer og få OptimisticLockException
     */
    public void oppdaterLåsVersjon(BehandlingLås lås) {
        if (lås.getBehandlingId() != null) {
            oppdaterLåsVersjon(lås.getBehandlingId());
        } // else NO-OP (for ny behandling uten id)
    }
  
    private Object oppdaterLåsVersjon(Long id) {      
        var entity = entityManager.find(Behandling.class, id, BYPASS_PROPS);
        if (entity == null) {
            throw BehandlingRepositoryFeil.FACTORY.fantIkkeEntitetForLåsing(Behandling.class.getSimpleName(), id).toException();
        } else {
            LockModeType lockMode = LockModeType.PESSIMISTIC_FORCE_INCREMENT;
            entityManager.lock(entity, lockMode);
        }
        return entity;
    }

}
