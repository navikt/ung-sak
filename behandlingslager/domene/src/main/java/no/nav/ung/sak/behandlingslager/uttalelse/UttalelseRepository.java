package no.nav.ung.sak.behandlingslager.uttalelse;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.k9.felles.jpa.HibernateVerktøy;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriodeGrunnlag;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class UttalelseRepository {

    private EntityManager entityManager;

    public UttalelseRepository() {}

    @Inject
    public UttalelseRepository(EntityManager entityManager) { this.entityManager = entityManager; }

    public void lagre(Long behandlingsId, Collection<UttalelseV2> uttalelser) {
        var nyttGrunnlag = new UttalelseGrunnlag(behandlingsId);
        nyttGrunnlag.leggTilUttalelser(uttalelser);
        persister(hentEksisterendeGrunnlag(behandlingsId), nyttGrunnlag);
    }

    private void persister(Optional <UttalelseGrunnlag> eksisterendeGrunnlag, UttalelseGrunnlag nyttGrunnlag) {
        eksisterendeGrunnlag.ifPresent(this::deaktiverEksisterende);
        if (nyttGrunnlag.getUttalelser() != null) {
            entityManager.persist(nyttGrunnlag.getUttalelser());
        }
        entityManager.persist(nyttGrunnlag);
        entityManager.flush();
    }

    private void deaktiverEksisterende(UttalelseGrunnlag gr) {
        gr.setAktiv(false);
        entityManager.persist(gr);
        entityManager.flush();
    }

    public Optional<UttalelseGrunnlag> hentEksisterendeGrunnlag(Long behandlingId) {
        final var query = entityManager.createQuery(
            "select ug from UttalelseGrunnlag ug " +
                "where ug.behandlingId = :behandlingId " +
                "and ug.aktiv = true", UttalelseGrunnlag.class);
        query.setParameter("behandlingId", behandlingId);

        return HibernateVerktøy.hentUniktResultat(query);
    }

    public Optional<UttalelseGrunnlag> hentGrunnlagBasertPåId(Long id) {
        final var query = entityManager.createQuery(
            "select ug from UttalelseGrunnlag ug " +
                "where ug.id = :id " +
                "and ug.aktiv = true", UttalelseGrunnlag.class);
        query.setParameter("id", id);

        return HibernateVerktøy.hentUniktResultat(query);
    }

}
