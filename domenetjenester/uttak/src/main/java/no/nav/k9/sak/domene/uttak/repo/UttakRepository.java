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

    public Uttak hentOppgittUttak(Long behandlingId) {
        return hentOppittUttakHvisEksisterer(behandlingId).orElseThrow();
    }

    public Søknadsperioder hentOppgittSøknadsperioder(Long behandlingId) {
        return hentOppgittSøknadsperioderHvisEksisterer(behandlingId).orElseThrow();
    }

    public Optional<Uttak> hentOppittUttakHvisEksisterer(Long behandlingId) {
        if (behandlingId == null) {
            return Optional.empty();
        }
        final var grunnlag = hentEksisterendeGrunnlag(behandlingId);

        return grunnlag.map(UttakGrunnlag::getOppgittUttak);
    }

    public Optional<Søknadsperioder> hentOppgittSøknadsperioderHvisEksisterer(Long behandlingId) {
        if (behandlingId == null) {
            return Optional.empty();
        }
        final var grunnlag = hentEksisterendeGrunnlag(behandlingId);

        return grunnlag.map(UttakGrunnlag::getOppgittSøknadsperioder);
    }

    public void lagreOgFlushOppgittUttak(Long behandlingId, Uttak oppgittUttak) {
        var eksisterendeGrunnlag = deaktiverEksisterendeGrunnlag(behandlingId);
        var fastsattUttak = eksisterendeGrunnlag.map(UttakGrunnlag::getFastsattUttak).orElse(null);
        var søknadsperioder = eksisterendeGrunnlag.map(UttakGrunnlag::getOppgittSøknadsperioder).orElse(null);
        var grunnlagEntitet = new UttakGrunnlag(behandlingId, oppgittUttak, fastsattUttak, søknadsperioder);
        entityManager.persist(oppgittUttak);
        entityManager.persist(grunnlagEntitet);
        entityManager.flush();
    }

    private Optional<UttakGrunnlag> deaktiverEksisterendeGrunnlag(Long behandlingId) {
        Objects.requireNonNull(behandlingId, "behandlingId"); // NOSONAR $NON-NLS-1$
        var eksisterendeGrunnlag = hentEksisterendeGrunnlag(behandlingId);
        if (eksisterendeGrunnlag.isPresent()) {
            // deaktiver eksisterende grunnlag

            var eksisterende = eksisterendeGrunnlag.get();
            eksisterende.setAktiv(false);
            entityManager.persist(eksisterende);
            entityManager.flush();
        }
        return eksisterendeGrunnlag;
    }

    public void lagreOgFlushFastsattUttak(Long behandlingId, Uttak fastsattUttak) {
        var eksisterendeGrunnlag = deaktiverEksisterendeGrunnlag(behandlingId);
        var oppgittUttak = eksisterendeGrunnlag.map(UttakGrunnlag::getOppgittUttak).orElse(null);
        var søknadsperioder = eksisterendeGrunnlag.map(UttakGrunnlag::getOppgittSøknadsperioder).orElse(null);
        var grunnlagEntitet = new UttakGrunnlag(behandlingId, oppgittUttak, fastsattUttak, søknadsperioder);
        entityManager.persist(fastsattUttak);
        entityManager.persist(grunnlagEntitet);
        entityManager.flush();
    }

    public void lagreOgFlushSøknadsperioder(Long behandlingId, Søknadsperioder søknadsperioder) {
        var eksisterendeGrunnlag = deaktiverEksisterendeGrunnlag(behandlingId);
        var oppgittUttak = eksisterendeGrunnlag.map(UttakGrunnlag::getOppgittUttak).orElse(null);
        var fastsattUttak = eksisterendeGrunnlag.map(UttakGrunnlag::getFastsattUttak).orElse(null);
        var grunnlagEntitet = new UttakGrunnlag(behandlingId, oppgittUttak, fastsattUttak, søknadsperioder);
        entityManager.persist(søknadsperioder);
        entityManager.persist(grunnlagEntitet);
        entityManager.flush();
    }

    private Optional<UttakGrunnlag> hentEksisterendeGrunnlag(Long id) {
        final TypedQuery<UttakGrunnlag> query = entityManager.createQuery(
            "FROM UttakGrunnlag s " +
                "WHERE s.behandlingId = :behandlingId AND s.aktiv = true",
            UttakGrunnlag.class);

        query.setParameter("behandlingId", id);

        return HibernateVerktøy.hentUniktResultat(query);
    }

    /**
     * Kopierer grunnlag fra en tidligere behandling. Endrer ikke aggregater, en skaper nye referanser til disse.
     */
    public void kopierGrunnlagFraEksisterendeBehandling(Long gammelBehandlingId, Long nyBehandlingId) {
        Optional<Uttak> søknadEntitet = hentOppittUttakHvisEksisterer(gammelBehandlingId);
        søknadEntitet.ifPresent(entitet -> lagreOgFlushOppgittUttak(nyBehandlingId, entitet));
    }

}
