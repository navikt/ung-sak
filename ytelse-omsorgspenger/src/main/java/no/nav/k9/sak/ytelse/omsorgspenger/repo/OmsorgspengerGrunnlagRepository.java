package no.nav.k9.sak.ytelse.omsorgspenger.repo;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;

import no.nav.k9.felles.jpa.HibernateVerktøy;

@Dependent
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

    public Optional<OppgittFravær> hentOppgittFraværFraSøknadHvisEksisterer(Long behandlingId) {
        if (behandlingId == null) {
            return Optional.empty();
        }
        final var grunnlag = hentGrunnlag(behandlingId);

        return grunnlag.map(OmsorgspengerGrunnlag::getOppgittFraværFraSøknad);
    }

    public void lagreOgFlushOppgittFravær(Long behandlingId, OppgittFravær input) {
        var eksisterendeGrunnlag = hentGrunnlag(behandlingId);
        var eksisterendeFraværFraSøknad = eksisterendeGrunnlag.map(OmsorgspengerGrunnlag::getOppgittFraværFraSøknad).orElse(null);
        entityManager.persist(input);
        deaktiverEksisterendeGrunnlag(eksisterendeGrunnlag.orElse(null));
        lagreOgFlushNyttGrunnlag(new OmsorgspengerGrunnlag(behandlingId, input, eksisterendeFraværFraSøknad));
    }

    public void lagreOgFlushOppgittFraværFraSøknad(Long behandlingId, OppgittFravær input) {
        var eksisterendeGrunnlag = hentGrunnlag(behandlingId);
        var eksisterendeFravær = eksisterendeGrunnlag.map(OmsorgspengerGrunnlag::getOppgittFravær).orElse(null);
        entityManager.persist(input);
        deaktiverEksisterendeGrunnlag(eksisterendeGrunnlag.orElse(null));
        lagreOgFlushNyttGrunnlag(new OmsorgspengerGrunnlag(behandlingId, eksisterendeFravær, input));
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
