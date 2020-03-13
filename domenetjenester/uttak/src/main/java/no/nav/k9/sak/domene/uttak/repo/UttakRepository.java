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

    public UttakAktivitet hentOppgittUttak(Long behandlingId) {
        return hentOppittUttakHvisEksisterer(behandlingId).orElseThrow();
    }
    
    public Ferie hentOppgittFerie(Long behandlingId) {
        return hentOppgittFerieHvisEksisterer(behandlingId).orElseThrow();
    }

    public Søknadsperioder hentOppgittSøknadsperioder(Long behandlingId) {
        return hentOppgittSøknadsperioderHvisEksisterer(behandlingId).orElseThrow();
    }
    
    public UttakAktivitet hentFastsattUttak(Long behandlingId) {
        return hentFastsattUttakHvisEksisterer(behandlingId).orElseThrow();
    }

    public Optional<UttakAktivitet> hentOppittUttakHvisEksisterer(Long behandlingId) {
        if (behandlingId == null) {
            return Optional.empty();
        }
        final var grunnlag = hentEksisterendeGrunnlag(behandlingId);

        return grunnlag.map(UttakGrunnlag::getOppgittUttak);
    }
    
    public Optional<UttakAktivitet> hentFastsattUttakHvisEksisterer(Long behandlingId) {
        if (behandlingId == null) {
            return Optional.empty();
        }
        final var grunnlag = hentEksisterendeGrunnlag(behandlingId);

        return grunnlag.map(UttakGrunnlag::getFastsattUttak);
    }

    public Optional<Ferie> hentOppgittFerieHvisEksisterer(Long behandlingId) {
        if (behandlingId == null) {
            return Optional.empty();
        }
        final var grunnlag = hentEksisterendeGrunnlag(behandlingId);

        return grunnlag.map(UttakGrunnlag::getOppgittFerie);
    }

    
    public Optional<Søknadsperioder> hentOppgittSøknadsperioderHvisEksisterer(Long behandlingId) {
        if (behandlingId == null) {
            return Optional.empty();
        }
        final var grunnlag = hentEksisterendeGrunnlag(behandlingId);

        return grunnlag.map(UttakGrunnlag::getOppgittSøknadsperioder);
    }

    public void lagreOgFlushOppgittUttak(Long behandlingId, UttakAktivitet input) {
        var eksisterendeGrunnlag = deaktiverEksisterendeGrunnlag(behandlingId);
        var fastsattUttak = eksisterendeGrunnlag.map(UttakGrunnlag::getFastsattUttak).orElse(null);
        var søknadsperioder = eksisterendeGrunnlag.map(UttakGrunnlag::getOppgittSøknadsperioder).orElse(null);
        var ferie = eksisterendeGrunnlag.map(UttakGrunnlag::getOppgittFerie).orElse(null);
        var grunnlagEntitet = new UttakGrunnlag(behandlingId, input, fastsattUttak, søknadsperioder, ferie);
        entityManager.persist(input);
        entityManager.persist(grunnlagEntitet);
        entityManager.flush();
    }

    public void lagreOgFlushFastsattUttak(Long behandlingId, UttakAktivitet input) {
        var eksisterendeGrunnlag = deaktiverEksisterendeGrunnlag(behandlingId);
        var oppgittUttak = eksisterendeGrunnlag.map(UttakGrunnlag::getOppgittUttak).orElse(null);
        var søknadsperioder = eksisterendeGrunnlag.map(UttakGrunnlag::getOppgittSøknadsperioder).orElse(null);
        var ferie = eksisterendeGrunnlag.map(UttakGrunnlag::getOppgittFerie).orElse(null);
        var grunnlagEntitet = new UttakGrunnlag(behandlingId, oppgittUttak, input, søknadsperioder, ferie);
        entityManager.persist(input);
        entityManager.persist(grunnlagEntitet);
        entityManager.flush();
    }

    public void lagreOgFlushSøknadsperioder(Long behandlingId, Søknadsperioder input) {
        var eksisterendeGrunnlag = deaktiverEksisterendeGrunnlag(behandlingId);
        var oppgittUttak = eksisterendeGrunnlag.map(UttakGrunnlag::getOppgittUttak).orElse(null);
        var fastsattUttak = eksisterendeGrunnlag.map(UttakGrunnlag::getFastsattUttak).orElse(null);
        var ferie = eksisterendeGrunnlag.map(UttakGrunnlag::getOppgittFerie).orElse(null);
        var grunnlagEntitet = new UttakGrunnlag(behandlingId, oppgittUttak, fastsattUttak, input, ferie);
        entityManager.persist(input);
        entityManager.persist(grunnlagEntitet);
        entityManager.flush();
    }
    
    public void lagreOgFlushOppgittFerie(Long behandlingId, Ferie input) {
        var eksisterendeGrunnlag = deaktiverEksisterendeGrunnlag(behandlingId);
        var oppgittUttak = eksisterendeGrunnlag.map(UttakGrunnlag::getOppgittUttak).orElse(null);
        var fastsattUttak = eksisterendeGrunnlag.map(UttakGrunnlag::getFastsattUttak).orElse(null);
        var søknadsperioder = eksisterendeGrunnlag.map(UttakGrunnlag::getOppgittSøknadsperioder).orElse(null);
        var grunnlagEntitet = new UttakGrunnlag(behandlingId, oppgittUttak, fastsattUttak, søknadsperioder, input);
        entityManager.persist(input);
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

    /**
     * Kopierer grunnlag fra en tidligere behandling. Endrer ikke aggregater, en skaper nye referanser til disse.
     */
    public void kopierGrunnlagFraEksisterendeBehandling(Long gammelBehandlingId, Long nyBehandlingId) {
        Optional<UttakAktivitet> søknadEntitet = hentOppittUttakHvisEksisterer(gammelBehandlingId);
        søknadEntitet.ifPresent(entitet -> lagreOgFlushOppgittUttak(nyBehandlingId, entitet));
    }

}
