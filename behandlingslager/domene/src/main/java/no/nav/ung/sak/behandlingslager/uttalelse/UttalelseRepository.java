package no.nav.ung.sak.behandlingslager.uttalelse;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.k9.felles.jpa.HibernateVerktøy;
import no.nav.ung.kodeverk.varsel.EndringType;
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

    public void lagre(Long behandlingId, Collection<UttalelseV2> uttalelser) {
        var eksisterendeGrunnlag = hentEksisterendeGrunnlag(behandlingId);
        var nyttGrunnlag = eksisterendeGrunnlag.map(it -> new UttalelseGrunnlag(behandlingId, it)).orElse(new UttalelseGrunnlag(behandlingId));
        nyttGrunnlag.leggTilUttalelser(uttalelser);
        persister(eksisterendeGrunnlag, nyttGrunnlag);
    }

    public void kopier(Long eksisterendeBehandlingId, Long nyBehandlingId) {
        var eksisterendeGrunnlag = hentEksisterendeGrunnlag(eksisterendeBehandlingId);
        var nyttGrunnlag = eksisterendeGrunnlag.map(it -> new UttalelseGrunnlag(nyBehandlingId, it));
        nyttGrunnlag.ifPresent(gr -> persister(Optional.empty(), gr));
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

    public List<UttalelseV2> hentUttalelser(Long behandlingId, EndringType... typer) {
        final var query = entityManager.createQuery(
            "select ug from UttalelseGrunnlag ug " +
                "where ug.behandlingId = :behandlingId " +
                "and ug.aktiv = true", UttalelseGrunnlag.class);
        query.setParameter("behandlingId", behandlingId);

        return HibernateVerktøy.hentUniktResultat(query).stream().map(UttalelseGrunnlag::getUttalelser).map(Uttalelser::getUttalelser).flatMap(Collection::stream)
            .filter(u -> List.of(typer).contains(u.getType()))
            .toList();
    }

    public Optional<UttalelseGrunnlag> hentGrunnlagBasertPåId(Long id) {
        final var query = entityManager.createQuery(
            "select ug from UttalelseGrunnlag ug " +
                "where ug.id = :id " , UttalelseGrunnlag.class);
        query.setParameter("id", id);

        return HibernateVerktøy.hentUniktResultat(query);
    }

}
