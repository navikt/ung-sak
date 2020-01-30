package no.nav.foreldrepenger.behandlingslager.behandling.fordeling;

import java.util.Objects;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.vedtak.felles.jpa.HibernateVerktøy;

@ApplicationScoped
public class FordelingRepository {

    private EntityManager entityManager;

    FordelingRepository() {
        // CDI
    }

    @Inject
    public FordelingRepository(EntityManager entityManager) {
        Objects.requireNonNull(entityManager, "entityManager"); //$NON-NLS-1$
        this.entityManager = entityManager;
    }

    public Fordeling hent(Long behandlingId) {
        return hentHvisEksisterer(behandlingId).orElseThrow();
    }

    public Optional<Fordeling> hentHvisEksisterer(Long behandlingId) {
        if (behandlingId == null) {
            return Optional.empty();
        }
        final var fordelingGrunnlagEntitet = hentEksisterendeGrunnlag(behandlingId);

        return fordelingGrunnlagEntitet.map(FordelingGrunnlagEntitet::getOppgittFordeling);
    }

    public void lagreOgFlush(Behandling behandling, Fordeling fordeling) {
        Objects.requireNonNull(behandling, "behandling"); // NOSONAR $NON-NLS-1$
        final Optional<FordelingGrunnlagEntitet> eksisterendeGrunnlag = hentEksisterendeGrunnlag(behandling.getId());
        if (eksisterendeGrunnlag.isPresent()) {
            // deaktiver eksisterende grunnlag

            final FordelingGrunnlagEntitet eksisterendeGrunnlagEntitet = eksisterendeGrunnlag.get();
            eksisterendeGrunnlagEntitet.setAktiv(false);
            entityManager.persist(eksisterendeGrunnlagEntitet);
            entityManager.flush();
        }

        final FordelingGrunnlagEntitet grunnlagEntitet = new FordelingGrunnlagEntitet(behandling, fordeling);
        entityManager.persist(fordeling);
        entityManager.persist(grunnlagEntitet);
        entityManager.flush();
    }

    private Optional<FordelingGrunnlagEntitet> hentEksisterendeGrunnlag(Long id) {
        final TypedQuery<FordelingGrunnlagEntitet> query = entityManager.createQuery(
            "FROM FordelingGrunnlag s " +
                "WHERE s.behandling.id = :behandlingId AND s.aktiv = true", FordelingGrunnlagEntitet.class);

        query.setParameter("behandlingId", id);

        return HibernateVerktøy.hentUniktResultat(query);
    }

    /**
     * Kopierer grunnlag fra en tidligere behandling. Endrer ikke aggregater, en skaper nye referanser til disse.
     */
    public void kopierGrunnlagFraEksisterendeBehandling(Behandling gammelBehandling, Behandling nyBehandling) {
        Optional<Fordeling> søknadEntitet = hentHvisEksisterer(gammelBehandling.getId());
        søknadEntitet.ifPresent(entitet -> lagreOgFlush(nyBehandling, entitet));
    }
}
