package no.nav.foreldrepenger.behandlingslager.behandling;

import java.util.Objects;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;

import no.nav.vedtak.felles.jpa.HibernateVerktøy;

@ApplicationScoped
public class BehandlingsresultatRepository {

    private EntityManager entityManager;

    @Inject
    public BehandlingsresultatRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    protected BehandlingsresultatRepository() {
    }

    public Optional<Behandlingsresultat> hentHvisEksisterer(Long behandlingId) {
        TypedQuery<Behandlingsresultat> query = entityManager
            .createQuery("from Behandlingsresultat where behandlingId = :behandlingId", Behandlingsresultat.class);
        query.setParameter("behandlingId", behandlingId);
        return HibernateVerktøy.hentUniktResultat(query);
    }

    public Behandlingsresultat hent(Long behandlingId) {
        return hentHvisEksisterer(behandlingId).orElseThrow(() -> new IllegalStateException("Forventet behandlingsresultat"));
    }

    public void lagre(Long behandlingId, Behandlingsresultat resultat) {
        Objects.requireNonNull(behandlingId, "behandlingId");
        Objects.requireNonNull(resultat, "behandlingsresultat");
        resultat.setBehandling(behandlingId);
        entityManager.persist(resultat);
        entityManager.flush();
    }

}
