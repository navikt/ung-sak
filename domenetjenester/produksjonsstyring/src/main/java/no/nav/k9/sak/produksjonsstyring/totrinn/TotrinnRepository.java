package no.nav.k9.sak.produksjonsstyring.totrinn;


import java.util.Collection;
import java.util.Objects;
import java.util.Optional;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;

import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.vedtak.felles.jpa.HibernateVerktøy;

@Dependent
public class TotrinnRepository {

    private EntityManager entityManager;

    TotrinnRepository() {
        // CDI
    }

    @Inject
    public TotrinnRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }


    private void lagreTotrinnsresultatgrunnlag(Totrinnresultatgrunnlag totrinnresultatgrunnlag) {
        entityManager.persist(totrinnresultatgrunnlag);
        entityManager.flush(); // må flushe pga aktiv flagg
    }

    public void lagreOgFlush(Behandling behandling, Totrinnresultatgrunnlag totrinnresultatgrunnlag) {
        Objects.requireNonNull(behandling, "behandling");

        Optional<Totrinnresultatgrunnlag> aktivtTotrinnresultatgrunnlag = getAktivtTotrinnresultatgrunnlag(behandling);
        if (aktivtTotrinnresultatgrunnlag.isPresent()) {
            Totrinnresultatgrunnlag grunnlag = aktivtTotrinnresultatgrunnlag.get();
            grunnlag.deaktiver();
            entityManager.persist(grunnlag);
            entityManager.flush(); // må flushe pga aktiv flagg
        }
        lagreTotrinnsresultatgrunnlag(totrinnresultatgrunnlag);
    }

    public void lagreOgFlush(Behandling behandling, Collection<Totrinnsvurdering> totrinnaksjonspunktvurderinger) {
        Objects.requireNonNull(behandling, "behandling");

        Collection<Totrinnsvurdering> aktiveVurderinger = getAktiveTotrinnaksjonspunktvurderinger(behandling);
        if (!aktiveVurderinger.isEmpty()) {
            aktiveVurderinger.forEach(vurdering -> {
                vurdering.deaktiver();
                entityManager.persist(vurdering);
            });
            entityManager.flush(); // må flushe pga aktiv flagg
        }
        totrinnaksjonspunktvurderinger.forEach(v -> entityManager.persist(v));
        entityManager.flush();
    }


    public Optional<Totrinnresultatgrunnlag> hentTotrinngrunnlag(Behandling behandling) {
        return getAktivtTotrinnresultatgrunnlag(behandling);
    }

    public Collection<Totrinnsvurdering> hentTotrinnaksjonspunktvurderinger(Behandling behandling) {
        return getAktiveTotrinnaksjonspunktvurderinger(behandling);
    }

    protected Optional<Totrinnresultatgrunnlag> getAktivtTotrinnresultatgrunnlag(Behandling behandling) {
        return getAktivtTotrinnresultatgrunnlag(behandling.getId());
    }

    protected Optional<Totrinnresultatgrunnlag> getAktivtTotrinnresultatgrunnlag(Long behandlingId) {
        TypedQuery<Totrinnresultatgrunnlag> query = entityManager.createQuery(
            "SELECT trg FROM Totrinnresultatgrunnlag trg WHERE trg.behandling.id = :behandling_id AND trg.aktiv = TRUE", //$NON-NLS-1$
            Totrinnresultatgrunnlag.class);

        query.setParameter("behandling_id", behandlingId); //$NON-NLS-1$
        return HibernateVerktøy.hentUniktResultat(query);
    }

    protected Collection<Totrinnsvurdering> getAktiveTotrinnaksjonspunktvurderinger(Behandling behandling) {
        return getAktiveTotrinnaksjonspunktvurderinger(behandling.getId());
    }

    protected Collection<Totrinnsvurdering> getAktiveTotrinnaksjonspunktvurderinger(Long behandlingId) {
        TypedQuery<Totrinnsvurdering> query = entityManager.createQuery(
            "SELECT tav FROM Totrinnsvurdering tav WHERE tav.behandling.id = :behandling_id AND tav.aktiv = TRUE", //$NON-NLS-1$
            Totrinnsvurdering.class);

        query.setParameter("behandling_id", behandlingId); //$NON-NLS-1$
        return query.getResultList();
    }
}
