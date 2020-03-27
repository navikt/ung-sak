package no.nav.k9.sak.ytelse.omsorgspenger.repo;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;

import no.nav.k9.sak.behandlingslager.diff.DiffEntity;
import no.nav.k9.sak.behandlingslager.diff.TraverseEntityGraphFactory;
import no.nav.k9.sak.behandlingslager.diff.TraverseGraph;
import no.nav.vedtak.felles.jpa.HibernateVerktøy;

@ApplicationScoped
public class OmsorgspengerGrunnlagRepository {

    private EntityManager entityManager;

    OmsorgspengerGrunnlagRepository() {
        // CDI
    }

    @Inject
    public OmsorgspengerGrunnlagRepository(EntityManager entityManager) {
        Objects.requireNonNull(entityManager, "entityManager"); //$NON-NLS-1$
        this.entityManager = entityManager;
    }

    public OppgittFravær hentOppgittFravær(UUID behandlingId) {
        return hentOppittFraværHvisEksisterer(behandlingId).orElseThrow(() -> new IllegalStateException("Mangler oppgitt uttak for behandlingId=" + behandlingId));
    }

    public Optional<OppgittFravær> hentOppittFraværHvisEksisterer(UUID behandlingId) {
        if (behandlingId == null) {
            return Optional.empty();
        }
        final var grunnlag = hentGrunnlag(behandlingId);

        return grunnlag.map(OmsorgspengerGrunnlag::getOppgittFravær);
    }

    public OppgittFravær hentOppgittFravær(Long behandlingId) {
        return hentOppgittFraværHvisEksisterer(behandlingId).orElseThrow(() -> new IllegalStateException("Mangler oppgitt uttak for behandlingId=" + behandlingId));
    }

    public Optional<OppgittFravær> hentOppgittFraværHvisEksisterer(Long behandlingId) {
        if (behandlingId == null) {
            return Optional.empty();
        }
        final var grunnlag = hentGrunnlag(behandlingId);

        return grunnlag.map(OmsorgspengerGrunnlag::getOppgittFravær);
    }

    public void lagreOgFlushNyttGrunnlag(Long behandlingId, OmsorgspengerGrunnlag grunnlag) {
        var eksisterendeGrunnlag = hentGrunnlag(behandlingId);

        if (eksisterendeGrunnlag.isPresent()) {
            boolean erForskjellige = differ(false).areDifferent(grunnlag, eksisterendeGrunnlag.orElse(null));
            if (erForskjellige) {
                deaktiverEksisterendeGrunnlag(eksisterendeGrunnlag.orElse(null));
            } else {
                // skip
                return;
            }
        }
        Optional.ofNullable(grunnlag.getOppgittFravær()).ifPresent(entityManager::persist);
        entityManager.persist(grunnlag);
        entityManager.flush();
    }

    private DiffEntity differ(boolean medOnlyCheckTrackedFields) {
        TraverseGraph traverser = TraverseEntityGraphFactory.build(medOnlyCheckTrackedFields);
        return new DiffEntity(traverser);
    }

    public void lagreOgFlushOppgittFravær(Long behandlingId, OppgittFravær input) {
        var eksisterendeGrunnlag = hentGrunnlag(behandlingId);
        entityManager.persist(input);
        deaktiverEksisterendeGrunnlag(eksisterendeGrunnlag.orElse(null));
        lagreOgFlushNyttGrunnlag(new OmsorgspengerGrunnlag(behandlingId, input));
    }

    /**
     * Kopierer grunnlag fra en tidligere behandling. Endrer ikke aggregater, en skaper nye referanser til disse.
     */
    public void kopierGrunnlagFraEksisterendeBehandling(Long gammelBehandlingId, Long nyBehandlingId) {
        Optional<OppgittFravær> søknadEntitet = hentOppgittFraværHvisEksisterer(gammelBehandlingId);
        søknadEntitet.ifPresent(entitet -> lagreOgFlushOppgittFravær(nyBehandlingId, entitet));
    }

    private void lagreOgFlushNyttGrunnlag(OmsorgspengerGrunnlag grunnlagEntitet) {
        entityManager.persist(grunnlagEntitet);
        entityManager.flush();
    }

    public Optional<OmsorgspengerGrunnlag> hentGrunnlag(Long behandlingId) {
        final TypedQuery<OmsorgspengerGrunnlag> query = entityManager.createQuery(
            "SELECT s FROM OmsorgspengerGrunnlag s " +
                "WHERE s.behandlingId = :behandlingId AND s.aktiv = true",
            OmsorgspengerGrunnlag.class);

        query.setParameter("behandlingId", behandlingId);

        return HibernateVerktøy.hentUniktResultat(query);
    }

    public Optional<OmsorgspengerGrunnlag> hentGrunnlag(UUID behandlingId) {
        final TypedQuery<OmsorgspengerGrunnlag> query = entityManager.createQuery(
            "SELECT s FROM OmsorgspengerGrunnlag s INNER JOIN Behandling b on b.id=s.behandlingId " +
                "WHERE b.uuid = :behandlingId AND s.aktiv = true",
            OmsorgspengerGrunnlag.class);

        query.setParameter("behandlingId", behandlingId);

        return HibernateVerktøy.hentUniktResultat(query);
    }

    private void deaktiverEksisterendeGrunnlag(OmsorgspengerGrunnlag eksisterende) {
        if (eksisterende == null) {
            return;
        }
        eksisterende.setAktiv(false);
        lagreOgFlushNyttGrunnlag(eksisterende);
    }

}
