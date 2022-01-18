package no.nav.k9.sak.ytelse.pleiepengerbarn.repo.pleietrengende.død;

import no.nav.k9.felles.jpa.HibernateVerktøy;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import java.util.Objects;
import java.util.Optional;

@Dependent
public class RettPleiepengerVedDødRepository {

    private EntityManager entityManager;

    public RettPleiepengerVedDødRepository() {
        // CDI
    }

    @Inject
    public RettPleiepengerVedDødRepository(EntityManager entityManager) {
        this.entityManager = Objects.requireNonNull(entityManager, "entityManager");
    }

    public Optional<RettPleiepengerVedDødGrunnlag> hentHvisEksisterer(Long behandlingId) {
        if (behandlingId == null) {
            return Optional.empty();
        }

        return hentEksisterendeGrunnlag(behandlingId);
    }

    public void lagreOgFlush(Long behandlingId, RettPleiepengerVedDød rettPleiepengerVedDød) {
        Objects.requireNonNull(behandlingId, "behandlingId"); // NOSONAR $NON-NLS-1$
        Objects.requireNonNull(rettPleiepengerVedDød, "rettVedPleietrengendeDød"); // NOSONAR $NON-NLS-1$
        final Optional<RettPleiepengerVedDødGrunnlag> eksisterendeGrunnlag = hentEksisterendeGrunnlag(behandlingId);
        if (eksisterendeGrunnlag.isPresent()) {
            // deaktiver eksisterende grunnlag

            final RettPleiepengerVedDødGrunnlag eksisterendeGrunnlagEntitet = eksisterendeGrunnlag.get();
            eksisterendeGrunnlagEntitet.setAktiv(false);
            entityManager.persist(eksisterendeGrunnlagEntitet);
            entityManager.flush();
        }


        final RettPleiepengerVedDødGrunnlag grunnlagEntitet = new RettPleiepengerVedDødGrunnlag(behandlingId, rettPleiepengerVedDød);
        entityManager.persist(rettPleiepengerVedDød);
        entityManager.persist(grunnlagEntitet);
        entityManager.flush();
    }

    private Optional<RettPleiepengerVedDødGrunnlag> hentEksisterendeGrunnlag(Long id) {
        final TypedQuery<RettPleiepengerVedDødGrunnlag> query = entityManager.createQuery("""
                FROM RettVedPleietrengendeDødGrunnlag g WHERE g.behandlingId = :behandlingId AND g.aktiv = true
            """, RettPleiepengerVedDødGrunnlag.class);

        query.setParameter("behandlingId", id);

        return HibernateVerktøy.hentUniktResultat(query);
    }

    /**
     * Kopierer grunnlag fra en tidligere behandling. Endrer ikke aggregater, en skaper nye referanser til disse.
     */
    public void kopierGrunnlagFraEksisterendeBehandling(Long gammelBehandlingId, Long nyBehandlingId) {
        Optional<RettPleiepengerVedDødGrunnlag> grunnnlag = hentEksisterendeGrunnlag(gammelBehandlingId);
        grunnnlag.ifPresent(entitet -> lagreOgFlush(nyBehandlingId, new RettPleiepengerVedDød(
            entitet.getRettVedPleietrengendeDød().getVurdering(),
            entitet.getRettVedPleietrengendeDød().getRettVedDødType()
        )));
    }
}

