package no.nav.k9.sak.ytelse.ung.periode;

import java.util.Collection;
import java.util.Objects;
import java.util.Optional;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.k9.felles.jpa.HibernateVerktøy;

@Dependent
public class UngdomsprogramPeriodeRepository {

    private final EntityManager entityManager;

    @Inject
    public UngdomsprogramPeriodeRepository(EntityManager entityManager) {
        Objects.requireNonNull(entityManager, "entityManager"); //$NON-NLS-1$
        this.entityManager = entityManager;
    }

    public Optional<UngdomsprogramPeriodeGrunnlag> hentGrunnlag(Long behandlingId) {
        return hentEksisterendeGrunnlag(behandlingId);
    }

    public void lagre(Long behandlingId, Collection<UngdomsprogramPeriode> ungdomsprogramPerioder) {
        var eksisterendeGrunnlag = hentEksisterendeGrunnlag(behandlingId);
        var nyttGrunnlag = eksisterendeGrunnlag.map(it -> new UngdomsprogramPeriodeGrunnlag(behandlingId, it))
            .orElse(new UngdomsprogramPeriodeGrunnlag(behandlingId));
        nyttGrunnlag.leggTil(ungdomsprogramPerioder);

        persister(eksisterendeGrunnlag, nyttGrunnlag);
    }


    private void persister(Optional<UngdomsprogramPeriodeGrunnlag> eksisterendeGrunnlag, UngdomsprogramPeriodeGrunnlag nyttGrunnlag) {
        eksisterendeGrunnlag.ifPresent(this::deaktiverEksisterende);

        if (nyttGrunnlag.getUngdomsprogramPerioder() != null) {
            entityManager.persist(nyttGrunnlag.getUngdomsprogramPerioder());
        }

        entityManager.persist(nyttGrunnlag);
        entityManager.flush();
    }

    private void deaktiverEksisterende(UngdomsprogramPeriodeGrunnlag gr) {
        gr.setAktiv(false);
        entityManager.persist(gr);
        entityManager.flush();
    }

    private Optional<UngdomsprogramPeriodeGrunnlag> hentEksisterendeGrunnlag(Long id) {
        var query = entityManager.createQuery(
            "SELECT gr " +
                "FROM UngdomsprogramPeriodeGrunnlag gr " +
                "WHERE gr.behandlingId = :behandlingId " +
                "AND gr.aktiv = true", UngdomsprogramPeriodeGrunnlag.class);

        query.setParameter("behandlingId", id);

        return HibernateVerktøy.hentUniktResultat(query);
    }


}