package no.nav.ung.sak.behandlingslager.perioder;

import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

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

    public Optional<UngdomsprogramPeriodeGrunnlag> hentGrunnlagFraGrunnlagsReferanse(UUID grunnlagsReferanse) {
        return hentEksisterendeGrunnlag(grunnlagsReferanse);
    }

    public void lagre(Long behandlingId, Collection<UngdomsprogramPeriode> ungdomsprogramPerioder) {
        var nyttGrunnlag = new UngdomsprogramPeriodeGrunnlag(behandlingId);
        nyttGrunnlag.leggTil(ungdomsprogramPerioder);

        persister(hentEksisterendeGrunnlag(behandlingId), nyttGrunnlag);
    }

    public void kopier(Long eksisterendeBehandlingId, Long nyBehandlingId) {
        var eksisterendeGrunnlag = hentEksisterendeGrunnlag(eksisterendeBehandlingId);
        var nyttGrunnlag = eksisterendeGrunnlag.map(it -> new UngdomsprogramPeriodeGrunnlag(nyBehandlingId, it));
        nyttGrunnlag.ifPresent(gr -> persister(Optional.empty(), gr));
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

    private Optional<UngdomsprogramPeriodeGrunnlag> hentEksisterendeGrunnlag(UUID grunnlagsReferanse) {
        var query = entityManager.createQuery(
            "SELECT gr " +
                "FROM UngdomsprogramPeriodeGrunnlag gr " +
                "WHERE gr.grunnlagsreferanse = :grunnlagsReferanse " +
                "AND gr.aktiv = true", UngdomsprogramPeriodeGrunnlag.class);

        query.setParameter("grunnlagsReferanse", grunnlagsReferanse);

        return HibernateVerktøy.hentUniktResultat(query);
    }

}
