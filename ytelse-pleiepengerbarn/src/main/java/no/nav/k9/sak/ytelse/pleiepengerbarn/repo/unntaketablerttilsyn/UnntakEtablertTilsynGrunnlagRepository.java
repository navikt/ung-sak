package no.nav.k9.sak.ytelse.pleiepengerbarn.repo.unntaketablerttilsyn;

import no.nav.k9.felles.jpa.HibernateVerktøy;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import java.util.Objects;
import java.util.Optional;

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

    public void lagre(UnntakEtablertTilsynGrunnlag unntakEtablertTilsynGrunnlag) {
        entityManager.persist(unntakEtablertTilsynGrunnlag);
        entityManager.flush();
    }

    public UnntakEtablertTilsynGrunnlag lagre(Long behandlingId, UnntakEtablertTilsynForPleietrengende unntakEtablertTilsynForPleietrengende) {
        final Optional<UnntakEtablertTilsynGrunnlag> eksisterendeGrunnlag = hentEksisterendeGrunnlag(behandlingId);
        return lagre(behandlingId, unntakEtablertTilsynForPleietrengende, eksisterendeGrunnlag);
    }

    public void kopierGrunnlagFraEksisterendeBehandling(Long gammelBehandlingId, Long nyBehandlingId) {
        Optional<UnntakEtablertTilsynGrunnlag> grunnlag = hentEksisterendeGrunnlag(gammelBehandlingId);
        grunnlag.ifPresent(entitet -> {
            lagre(nyBehandlingId, new UnntakEtablertTilsynForPleietrengende(entitet.getUnntakEtablertTilsynForPleietrengende()));
        });
    }

    private UnntakEtablertTilsynGrunnlag lagre(Long behandlingId, UnntakEtablertTilsynForPleietrengende unntakEtablertTilsynForPleietrengende, Optional<UnntakEtablertTilsynGrunnlag> eksisterendeGrunnlag) {
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
