package no.nav.k9.sak.ytelse.pleiepengerbarn.repo.unntaketablerttilsyn;

import java.util.Objects;
import java.util.Optional;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;

import no.nav.k9.felles.jpa.HibernateVerktøy;
import no.nav.k9.sak.typer.AktørId;

@Dependent
public class UnntakEtablertTilsynGrunnlagRepository {

    EntityManager entityManager;

    @Inject
    public UnntakEtablertTilsynGrunnlagRepository(EntityManager entityManager) {
        Objects.requireNonNull(entityManager, "entityManager"); //$NON-NLS-1$
        this.entityManager = entityManager;
    }

    public UnntakEtablertTilsynGrunnlag hent(Long behandlingId) {
        return hentHvisEksisterer(behandlingId).orElseThrow();
    }

    public Optional<UnntakEtablertTilsynGrunnlag> hentHvisEksisterer(Long behandlingId) {
        if (behandlingId == null) {
            return Optional.empty();
        }

        return hentEksisterendeGrunnlag(behandlingId);
    }

    public Optional<UnntakEtablertTilsynForPleietrengende> hentHvisEksistererUnntakPleietrengende(AktørId pleietrengende) {
        final TypedQuery<UnntakEtablertTilsynForPleietrengende> query = entityManager.createQuery(
            "SELECT s FROM UnntakEtablertTilsynForPleietrengende s " +
                "WHERE s.pleietrengendeAktørId = :pleietrengendeAktørId AND s.aktiv = true", UnntakEtablertTilsynForPleietrengende.class);

        query.setParameter("pleietrengendeAktørId", pleietrengende);

        return HibernateVerktøy.hentUniktResultat(query);
    }

    public void lagre(Long behandlingId, UnntakEtablertTilsynForPleietrengende unntakEtablertTilsynForPleietrengende) {
        final Optional<UnntakEtablertTilsynForPleietrengende> eksisterendeGrunnlag = hentHvisEksistererUnntakPleietrengende(unntakEtablertTilsynForPleietrengende.getPleietrengendeAktørId());
        lagre(behandlingId, unntakEtablertTilsynForPleietrengende, eksisterendeGrunnlag);
    }

    private void lagre(Long behandlingId, UnntakEtablertTilsynForPleietrengende unntakEtablertTilsynForPleietrengende, Optional<UnntakEtablertTilsynForPleietrengende> eksisterendeGrunnlag) {
        if (eksisterendeGrunnlag.isPresent()) {
            // deaktiver eksisterende grunnlag
            final UnntakEtablertTilsynForPleietrengende eksisterendeGrunnlagEntitet = eksisterendeGrunnlag.get();
            eksisterendeGrunnlagEntitet.setAktiv(false);
            entityManager.persist(eksisterendeGrunnlagEntitet);
            entityManager.flush();
        }

        entityManager.persist(unntakEtablertTilsynForPleietrengende);
        entityManager.flush();
    }

    public void lagreGrunnlag(Long behandlingId, UnntakEtablertTilsynForPleietrengende unntakEtablertTilsynForPleietrengende) {
        final Optional<UnntakEtablertTilsynGrunnlag> eksisterendeGrunnlag = hentHvisEksisterer(behandlingId);
        lagreGrunnlag(behandlingId, unntakEtablertTilsynForPleietrengende, eksisterendeGrunnlag);
    }
    private UnntakEtablertTilsynGrunnlag lagreGrunnlag(Long behandlingId, UnntakEtablertTilsynForPleietrengende unntakEtablertTilsynForPleietrengende, Optional<UnntakEtablertTilsynGrunnlag> eksisterendeGrunnlag) {
        if (eksisterendeGrunnlag.isPresent()) {
            // deaktiver eksisterende grunnlag
            final UnntakEtablertTilsynGrunnlag eksisterendeGrunnlagEntitet = eksisterendeGrunnlag.get();
            eksisterendeGrunnlagEntitet.setAktiv(false);
            entityManager.persist(eksisterendeGrunnlagEntitet);
            entityManager.flush();
        }

        var grunnlagEntitet = new UnntakEtablertTilsynGrunnlag(behandlingId, unntakEtablertTilsynForPleietrengende);

        entityManager.persist(grunnlagEntitet);
        entityManager.flush();
        return grunnlagEntitet;
    }

    private Optional<UnntakEtablertTilsynGrunnlag> hentEksisterendeGrunnlag(Long behandlingId) {
        final TypedQuery<UnntakEtablertTilsynGrunnlag> query = entityManager.createQuery(
            "SELECT s FROM UnntakEtablertTilsynGrunnlag s " +
                "WHERE s.behandlingId = :behandlingId AND s.aktiv = true", UnntakEtablertTilsynGrunnlag.class);

        query.setParameter("behandlingId", behandlingId);

        return HibernateVerktøy.hentUniktResultat(query);
    }

}
