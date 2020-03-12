package no.nav.k9.sak.domene.uttak.repo;

import java.util.Objects;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;

import no.nav.vedtak.felles.jpa.HibernateVerktøy;

@ApplicationScoped
public class UttakRepository {

    private EntityManager entityManager;

    UttakRepository() {
        // CDI
    }

    @Inject
    public UttakRepository(EntityManager entityManager) {
        Objects.requireNonNull(entityManager, "entityManager"); //$NON-NLS-1$
        this.entityManager = entityManager;
    }

    public Uttak hent(Long behandlingId) {
        return hentHvisEksisterer(behandlingId).orElseThrow();
    }

    public Optional<Uttak> hentHvisEksisterer(Long behandlingId) {
        if (behandlingId == null) {
            return Optional.empty();
        }
        final var fordelingGrunnlagEntitet = hentEksisterendeGrunnlag(behandlingId);

        return fordelingGrunnlagEntitet.map(UttakGrunnlag::getOppgittFordeling);
    }

    public void lagreOgFlush(Long behandlingId, Uttak uttak) {
        Objects.requireNonNull(behandlingId, "behandlingId"); // NOSONAR $NON-NLS-1$
        final Optional<UttakGrunnlag> eksisterendeGrunnlag = hentEksisterendeGrunnlag(behandlingId);
        if (eksisterendeGrunnlag.isPresent()) {
            // deaktiver eksisterende grunnlag

            final UttakGrunnlag eksisterendeGrunnlagEntitet = eksisterendeGrunnlag.get();
            eksisterendeGrunnlagEntitet.setAktiv(false);
            entityManager.persist(eksisterendeGrunnlagEntitet);
            entityManager.flush();
        }

        final UttakGrunnlag grunnlagEntitet = new UttakGrunnlag(behandlingId, uttak);
        entityManager.persist(uttak);
        entityManager.persist(grunnlagEntitet);
        entityManager.flush();
    }

    private Optional<UttakGrunnlag> hentEksisterendeGrunnlag(Long id) {
        final TypedQuery<UttakGrunnlag> query = entityManager.createQuery(
            "FROM UttakGrunnlag s " +
                "WHERE s.behandlingId = :behandlingId AND s.aktiv = true", UttakGrunnlag.class);

        query.setParameter("behandlingId", id);

        return HibernateVerktøy.hentUniktResultat(query);
    }

    /**
     * Kopierer grunnlag fra en tidligere behandling. Endrer ikke aggregater, en skaper nye referanser til disse.
     */
    public void kopierGrunnlagFraEksisterendeBehandling(Long gammelBehandlingId, Long nyBehandlingId) {
        Optional<Uttak> søknadEntitet = hentHvisEksisterer(gammelBehandlingId);
        søknadEntitet.ifPresent(entitet -> lagreOgFlush(nyBehandlingId, entitet));
    }
}
