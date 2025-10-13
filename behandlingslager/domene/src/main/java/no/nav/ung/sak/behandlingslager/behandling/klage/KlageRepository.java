package no.nav.ung.sak.behandlingslager.behandling.klage;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import no.nav.k9.felles.jpa.HibernateVerktøy;

import java.util.Objects;
import java.util.Optional;

@Dependent
public class KlageRepository {

    private EntityManager entityManager;

    protected KlageRepository() {
        // for CDI proxy
    }

    @Inject
    public KlageRepository(EntityManager entityManager) {
        Objects.requireNonNull(entityManager, "entityManager"); //$NON-NLS-1$
        this.entityManager = entityManager;
    }

    public KlageUtredningEntitet hentKlageUtredning(Long behandlingId) {
        Objects.requireNonNull(behandlingId, "behandlingId"); // NOSONAR //$NON-NLS-1$

        final TypedQuery<KlageUtredningEntitet> query = entityManager.createQuery(
            "SELECT k FROM KlageUtredning k" +
                "   WHERE k.behandlingId = :behandlingId", //$NON-NLS-1$
            KlageUtredningEntitet.class);// NOSONAR

        query.setParameter("behandlingId", behandlingId);
        return HibernateVerktøy.hentEksaktResultat(query);
    }

    public Optional<KlageUtredningEntitet> finnKlageUtredning(Long behandlingId) {
        Objects.requireNonNull(behandlingId, "behandlingId"); // NOSONAR //$NON-NLS-1$

        final TypedQuery<KlageUtredningEntitet> query = entityManager.createQuery(
            "SELECT k FROM KlageUtredning k " +
                "   WHERE k.behandlingId = :behandlingId", //$NON-NLS-1$
            KlageUtredningEntitet.class);// NOSONAR

        query.setParameter("behandlingId", behandlingId);
        return HibernateVerktøy.hentUniktResultat(query);
    }

    public void lagre(KlageUtredningEntitet klageutredning) {
        entityManager.persist(klageutredning);
        entityManager.flush();
    }
}
