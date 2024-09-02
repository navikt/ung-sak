package no.nav.k9.sak.behandlingslager.behandling.merknad;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import no.nav.k9.felles.jpa.HibernateVerktøy;

@Dependent
public class BehandlingMerknadRepository {

    private EntityManager entityManager;

    @Inject
    public BehandlingMerknadRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public Set<BehandlingMerknadType> hentMerknadTyper(Long behandlingId){
        return hentBehandlingMerknad(behandlingId).map(BehandlingMerknad::merknadTyper).orElse(Collections.emptySet());
    }

    public Optional<BehandlingMerknad> hentBehandlingMerknad(Long behandlingId) {
        return finnMerknad(behandlingId)
            .map(entitet -> new BehandlingMerknad(entitet.getMerknadTyper(), entitet.getFritekst()));
    }

    public void registrerMerknadtyper(Long behandlingId, Collection<BehandlingMerknadType> merknadTyper, String fritekst) {
        finnMerknad(behandlingId).ifPresent(this::deaktiver);
        entityManager.persist(new BehandlingMerknadEntitet(behandlingId, EnumSet.copyOf(merknadTyper), fritekst));
        entityManager.flush();
    }

    private void deaktiver(BehandlingMerknadEntitet merknad) {
        merknad.deaktiver();
        entityManager.persist(merknad);
    }

    private Optional<BehandlingMerknadEntitet> finnMerknad(Long behandlingId) {
        TypedQuery<BehandlingMerknadEntitet> query = entityManager.createQuery(
                "SELECT merknad FROM BehandlingMerknad merknad WHERE merknad.behandlingId = :behandling_id AND merknad.aktiv = TRUE", //$NON-NLS-1$
                BehandlingMerknadEntitet.class)
            .setParameter("behandling_id", behandlingId); //$NON-NLS-1$

        return HibernateVerktøy.hentUniktResultat(query);
    }

}
