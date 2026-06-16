package no.nav.ung.sak.behandlingslager.behandling.startdato;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.k9.felles.jpa.HibernateVerktøy;

@Dependent
public class StartdatoRepository {

    private final EntityManager entityManager;

    @Inject
    public StartdatoRepository(EntityManager entityManager) {
        Objects.requireNonNull(entityManager, "entityManager"); //$NON-NLS-1$
        this.entityManager = entityManager;
    }

    public Optional<StartdatoGrunnlag> hentGrunnlag(Long behandlingId) {
        return hentEksisterendeGrunnlag(behandlingId);
    }

    public void lagre(Long behandlingId, List<SøktStartdato> søknadsperioder) {
        var eksisterendeGrunnlag = hentEksisterendeGrunnlag(behandlingId);
        var nyttGrunnlag = eksisterendeGrunnlag.map(it -> new StartdatoGrunnlag(behandlingId, it))
            .orElse(new StartdatoGrunnlag(behandlingId));
        nyttGrunnlag.leggTil(søknadsperioder);

        persister(eksisterendeGrunnlag, nyttGrunnlag);
    }

    public void lagreRelevanteSøknader(Long behandlingId, Startdatoer søknader) {
        var eksisterendeGrunnlag = hentEksisterendeGrunnlag(behandlingId);
        var nyttGrunnlag = eksisterendeGrunnlag.map(it -> new StartdatoGrunnlag(behandlingId, it))
            .orElse(new StartdatoGrunnlag(behandlingId));
        nyttGrunnlag.setRelevanteStartdatoer(søknader);

        persister(eksisterendeGrunnlag, nyttGrunnlag);
    }



    private void persister(Optional<StartdatoGrunnlag> eksisterendeGrunnlag, StartdatoGrunnlag nyttGrunnlag) {
        eksisterendeGrunnlag.ifPresent(this::deaktiverEksisterende);

        if (nyttGrunnlag.getOppgitteStartdatoer() != null) {
            entityManager.persist(nyttGrunnlag.getOppgitteStartdatoer());
        }
        if (nyttGrunnlag.getRelevanteStartdatoer() != null) {
            entityManager.persist(nyttGrunnlag.getRelevanteStartdatoer());
        }
        entityManager.persist(nyttGrunnlag);
        entityManager.flush();
    }

    private void deaktiverEksisterende(StartdatoGrunnlag gr) {
        gr.setAktiv(false);
        entityManager.persist(gr);
        entityManager.flush();
    }

    private Optional<StartdatoGrunnlag> hentEksisterendeGrunnlag(Long id) {
        var query = entityManager.createQuery(
            "SELECT s " +
                "FROM UngdomsytelseStartdatoGrunnlag s " +
                "WHERE s.behandlingId = :behandlingId " +
                "AND s.aktiv = true", StartdatoGrunnlag.class);

        query.setParameter("behandlingId", id);

        return HibernateVerktøy.hentUniktResultat(query);
    }

    public void kopierGrunnlagFraEksisterendeBehandling(Long gammelBehandlingId, Long nyBehandlingId) {
        var eksisterendeGrunnlag = hentEksisterendeGrunnlag(gammelBehandlingId);
        eksisterendeGrunnlag.ifPresent(entitet -> {
            persister(Optional.empty(), new StartdatoGrunnlag(nyBehandlingId, entitet));
        });
    }

    public Optional<StartdatoGrunnlag> hentGrunnlagBasertPåId(Long grunnlagId) {
        var query = entityManager.createQuery(
            "SELECT s " +
                "FROM UngdomsytelseStartdatoGrunnlag s " +
                "WHERE s.id = :grunnlagId", StartdatoGrunnlag.class);

        query.setParameter("grunnlagId", grunnlagId);

        return HibernateVerktøy.hentUniktResultat(query);
    }
}
