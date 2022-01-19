package no.nav.k9.sak.ytelse.pleiepengerbarn.repo.etablerttilsyn.sak;

import java.util.Objects;
import java.util.Optional;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;

import no.nav.k9.felles.jpa.HibernateVerktøy;

@Dependent
public class EtablertTilsynRepository {

    private EntityManager entityManager;


    @Inject
    public EtablertTilsynRepository(EntityManager entityManager) {
        Objects.requireNonNull(entityManager, "entityManager"); //$NON-NLS-1$
        this.entityManager = entityManager;
    }

    
    public EtablertTilsynGrunnlag hent(Long behandlingId) {
        return hentHvisEksisterer(behandlingId).orElseThrow();
    }

    public Optional<EtablertTilsynGrunnlag> hentHvisEksisterer(Long behandlingId) {
        if (behandlingId == null) {
            return Optional.empty();
        }

        return hentEksisterendeGrunnlag(behandlingId);
    }

    public void lagre(Long behandlingId, EtablertTilsyn etablertTilsyn) {
        final Optional<EtablertTilsynGrunnlag> eksisterendeGrunnlag = hentEksisterendeGrunnlag(behandlingId);
        lagre(behandlingId, etablertTilsyn, eksisterendeGrunnlag);
    }
    
    private void lagre(Long behandlingId, EtablertTilsyn etablertTilsyn, Optional<EtablertTilsynGrunnlag> eksisterendeGrunnlag) {
        if (eksisterendeGrunnlag.isPresent()) {
            // deaktiver eksisterende grunnlag

            final EtablertTilsynGrunnlag eksisterendeGrunnlagEntitet = eksisterendeGrunnlag.get();
            eksisterendeGrunnlagEntitet.setAktiv(false);
            entityManager.persist(eksisterendeGrunnlagEntitet);
            entityManager.flush();
        }

        final EtablertTilsynGrunnlag grunnlagEntitet = new EtablertTilsynGrunnlag(behandlingId, etablertTilsyn);
        if (etablertTilsyn != null) {
            entityManager.persist(etablertTilsyn);
        }

        entityManager.persist(grunnlagEntitet);
        entityManager.flush();
    }

    private Optional<EtablertTilsynGrunnlag> hentEksisterendeGrunnlag(Long id) {
        final TypedQuery<EtablertTilsynGrunnlag> query = entityManager.createQuery(
            "FROM EtablertTilsynGrunnlag g " +
                "WHERE g.behandlingId = :behandlingId AND g.aktiv = true", EtablertTilsynGrunnlag.class);

        query.setParameter("behandlingId", id);

        return HibernateVerktøy.hentUniktResultat(query);
    }
}
