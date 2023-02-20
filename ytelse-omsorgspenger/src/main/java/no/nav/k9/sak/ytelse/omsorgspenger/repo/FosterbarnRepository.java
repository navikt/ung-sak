package no.nav.k9.sak.ytelse.omsorgspenger.repo;

import java.util.Objects;
import java.util.Optional;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import no.nav.k9.felles.jpa.HibernateVerktøy;

/**
 * Lagring av Fosterbarn for Omsorgspenger
 * Dette er en midlertidig løsning i påvente av et felles Fosterbarn-register for NAV
 */
@Dependent
public class FosterbarnRepository {

    private EntityManager entityManager;

    @Inject
    public FosterbarnRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public Optional<FosterbarnGrunnlag> hentHvisEksisterer(Long behandlingId) {
        Objects.requireNonNull(behandlingId, "behandlingId");
        return hentEksisterendeGrunnlag(behandlingId);
    }

    private Optional<FosterbarnGrunnlag> hentEksisterendeGrunnlag(Long id) {
        final TypedQuery<FosterbarnGrunnlag> query = entityManager.createQuery("""
                FROM FosterbarnGrunnlag g WHERE g.behandlingId = :behandlingId AND g.aktiv = true
            """, FosterbarnGrunnlag.class);

        query.setParameter("behandlingId", id);

        return HibernateVerktøy.hentUniktResultat(query);
    }

    public void lagreOgFlush(Long behandlingId, Fosterbarna fosterbarna) {
        Objects.requireNonNull(behandlingId, "behandlingId");
        Objects.requireNonNull(fosterbarna, "fosterbarna");

        Optional<FosterbarnGrunnlag> eksisterendeGrunnlag = hentEksisterendeGrunnlag(behandlingId);
        if (eksisterendeGrunnlag.isPresent()) {
            // deaktiver eksisterende grunnlag
            FosterbarnGrunnlag eksisterendeGrunnlagEntitet = eksisterendeGrunnlag.get();
            eksisterendeGrunnlagEntitet.setAktiv(false);
            entityManager.persist(eksisterendeGrunnlagEntitet);
            entityManager.flush();
        }

        var grunnlagEntitet = new FosterbarnGrunnlag(behandlingId, fosterbarna);
        entityManager.persist(fosterbarna);
        entityManager.persist(grunnlagEntitet);
        entityManager.flush();
    }

    /**
     * Kopierer grunnlag fra en tidligere behandling. Endrer ikke aggregater, en skaper nye referanser til disse.
     */
    public void kopierGrunnlagFraEksisterendeBehandling(Long eksisterendeBehandlingId, Long nyBehandlingId) {
        Optional<FosterbarnGrunnlag> eksisterendeGrunnlag = hentEksisterendeGrunnlag(eksisterendeBehandlingId);
        eksisterendeGrunnlag.ifPresent(entitet -> lagreOgFlush(nyBehandlingId, entitet.getFosterbarna()));
    }

}
