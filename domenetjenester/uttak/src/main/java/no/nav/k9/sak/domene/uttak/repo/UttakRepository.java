package no.nav.k9.sak.domene.uttak.repo;

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
        return hentOppittUttakHvisEksisterer(behandlingId).orElseThrow(() -> new IllegalStateException("Mangler oppgitt uttak for behandlingId=" + behandlingId));
    }
    
    public UttakAktivitet hentOppgittUttak(UUID behandlingId) {
        return hentOppittUttakHvisEksisterer(behandlingId).orElseThrow(() -> new IllegalStateException("Mangler oppgitt uttak for behandlingId=" + behandlingId));
    }

    public Ferie hentOppgittFerie(Long behandlingId) {
        return hentOppgittFerieHvisEksisterer(behandlingId).orElseThrow(() -> new IllegalStateException("Mangler oppgitt ferie for behandlingId=" + behandlingId));
    }
    
    public Ferie hentOppgittFerie(UUID behandlingId) {
        return hentOppgittFerieHvisEksisterer(behandlingId).orElseThrow(() -> new IllegalStateException("Mangler oppgitt ferie for behandlingId=" + behandlingId));
    }

    public Søknadsperioder hentOppgittSøknadsperioder(Long behandlingId) {
        return hentOppgittSøknadsperioderHvisEksisterer(behandlingId).orElseThrow(() -> new IllegalStateException("Mangler oppgitt søknadsperioder for behandlingId=" + behandlingId));
    }
    
    public Søknadsperioder hentOppgittSøknadsperioder(UUID behandlingId) {
        return hentOppgittSøknadsperioderHvisEksisterer(behandlingId).orElseThrow(() -> new IllegalStateException("Mangler oppgitt søknadsperioder for behandlingId=" + behandlingId));
    }


    public OppgittTilsynsordning hentOppgittTilsynsordning(Long behandlingId) {
        return hentOppgittTilsynsordningHvisEksisterer(behandlingId).orElseThrow(() -> new IllegalStateException("Mangler tilsynsordning for behandlingId=" + behandlingId));
    }
    
    public OppgittTilsynsordning hentOppgittTilsynsordning(UUID behandlingId) {
        return hentOppgittTilsynsordningHvisEksisterer(behandlingId).orElseThrow(() -> new IllegalStateException("Mangler tilsynsordning for behandlingId=" + behandlingId));
    }


    public UttakAktivitet hentFastsattUttak(Long behandlingId) {
        return hentFastsattUttakHvisEksisterer(behandlingId).orElseThrow(() -> new IllegalStateException("Mangler fastsatt uttak for behandlingId=" + behandlingId));
    }

    public Optional<UttakAktivitet> hentOppittUttakHvisEksisterer(Long behandlingId) {
        if (behandlingId == null) {
            return Optional.empty();
        }
        final var grunnlag = hentGrunnlag(behandlingId);

        return grunnlag.map(UttakGrunnlag::getOppgittUttak);
    }
    
    public Optional<UttakAktivitet> hentOppittUttakHvisEksisterer(UUID behandlingId) {
        if (behandlingId == null) {
            return Optional.empty();
        }
        final var grunnlag = hentGrunnlag(behandlingId);

        return grunnlag.map(UttakGrunnlag::getOppgittUttak);
    }

    public Optional<UttakAktivitet> hentFastsattUttakHvisEksisterer(Long behandlingId) {
        if (behandlingId == null) {
            return Optional.empty();
        }
        final var grunnlag = hentGrunnlag(behandlingId);

        return grunnlag.map(UttakGrunnlag::getFastsattUttak);
    }

    public Optional<Ferie> hentOppgittFerieHvisEksisterer(Long behandlingId) {
        if (behandlingId == null) {
            return Optional.empty();
        }
        final var grunnlag = hentGrunnlag(behandlingId);

        return grunnlag.map(UttakGrunnlag::getOppgittFerie);
    }

    public Optional<Ferie> hentOppgittFerieHvisEksisterer(UUID behandlingId) {
        if (behandlingId == null) {
            return Optional.empty();
        }
        final var grunnlag = hentGrunnlag(behandlingId);

        return grunnlag.map(UttakGrunnlag::getOppgittFerie);
    }
    
    public Optional<Søknadsperioder> hentOppgittSøknadsperioderHvisEksisterer(Long behandlingId) {
        if (behandlingId == null) {
            return Optional.empty();
        }
        final var grunnlag = hentGrunnlag(behandlingId);

        return grunnlag.map(UttakGrunnlag::getOppgittSøknadsperioder);
    }
    
    public Optional<Søknadsperioder> hentOppgittSøknadsperioderHvisEksisterer(UUID behandlingId) {
        if (behandlingId == null) {
            return Optional.empty();
        }
        final var grunnlag = hentGrunnlag(behandlingId);

        return grunnlag.map(UttakGrunnlag::getOppgittSøknadsperioder);
    }

    public Optional<OppgittTilsynsordning> hentOppgittTilsynsordningHvisEksisterer(Long behandlingId) {
        if (behandlingId == null) {
            return Optional.empty();
        }
        final var grunnlag = hentGrunnlag(behandlingId);

        return grunnlag.map(UttakGrunnlag::getOppgittTilsynsordning);
    }

    public Optional<OppgittTilsynsordning> hentOppgittTilsynsordningHvisEksisterer(UUID behandlingId) {
        if (behandlingId == null) {
            return Optional.empty();
        }
        final var grunnlag = hentGrunnlag(behandlingId);

        return grunnlag.map(UttakGrunnlag::getOppgittTilsynsordning);
    }
    
    public void lagreOgFlushNyttGrunnlag(Long behandlingId, UttakGrunnlag grunnlag) {
        var eksisterendeGrunnlag = hentGrunnlag(behandlingId);
        
        if (eksisterendeGrunnlag.isPresent()) {
            boolean erForskjellige = differ(true).areDifferent(grunnlag, eksisterendeGrunnlag.orElse(null));
            if (erForskjellige) {
                deaktiverEksisterendeGrunnlag(eksisterendeGrunnlag.orElse(null));
            } else {
                // skip lagring - ingen endring
                return;
            }
        }
        
        Optional.ofNullable(grunnlag.getOppgittUttak()).ifPresent(entityManager::persist);
        Optional.ofNullable(grunnlag.getOppgittFerie()).ifPresent(entityManager::persist);
        Optional.ofNullable(grunnlag.getOppgittSøknadsperioder()).ifPresent(entityManager::persist);
        Optional.ofNullable(grunnlag.getOppgittTilsynsordning()).ifPresent(entityManager::persist);
        
        Optional.ofNullable(grunnlag.getFastsattUttak()).ifPresent(entityManager::persist);

        entityManager.persist(grunnlag);
        entityManager.flush();
    }

    public void lagreOgFlushOppgittUttak(Long behandlingId, UttakAktivitet input) {
        var eksisterendeGrunnlag = hentGrunnlag(behandlingId);
        var fastsattUttak = eksisterendeGrunnlag.map(UttakGrunnlag::getFastsattUttak).orElse(null);
        var søknadsperioder = eksisterendeGrunnlag.map(UttakGrunnlag::getOppgittSøknadsperioder).orElse(null);
        var ferie = eksisterendeGrunnlag.map(UttakGrunnlag::getOppgittFerie).orElse(null);
        var tilsynsordning = eksisterendeGrunnlag.map(UttakGrunnlag::getOppgittTilsynsordning).orElse(null);
        entityManager.persist(input);
        deaktiverEksisterendeGrunnlag(eksisterendeGrunnlag.orElse(null));
        lagreOgFlushNyttGrunnlag(new UttakGrunnlag(behandlingId, input, fastsattUttak, søknadsperioder, ferie, tilsynsordning));
    }

    public void lagreOgFlushFastsattUttak(Long behandlingId, UttakAktivitet input) {
        var eksisterendeGrunnlag = hentGrunnlag(behandlingId);
        var oppgittUttak = eksisterendeGrunnlag.map(UttakGrunnlag::getOppgittUttak).orElse(null);
        var søknadsperioder = eksisterendeGrunnlag.map(UttakGrunnlag::getOppgittSøknadsperioder).orElse(null);
        var ferie = eksisterendeGrunnlag.map(UttakGrunnlag::getOppgittFerie).orElse(null);
        var tilsynsordning = eksisterendeGrunnlag.map(UttakGrunnlag::getOppgittTilsynsordning).orElse(null);
        entityManager.persist(input);
        deaktiverEksisterendeGrunnlag(eksisterendeGrunnlag.orElse(null));
        lagreOgFlushNyttGrunnlag(new UttakGrunnlag(behandlingId, oppgittUttak, input, søknadsperioder, ferie, tilsynsordning));
    }

    public void lagreOgFlushSøknadsperioder(Long behandlingId, Søknadsperioder input) {
        var eksisterendeGrunnlag = hentGrunnlag(behandlingId);
        var oppgittUttak = eksisterendeGrunnlag.map(UttakGrunnlag::getOppgittUttak).orElse(null);
        var fastsattUttak = eksisterendeGrunnlag.map(UttakGrunnlag::getFastsattUttak).orElse(null);
        var ferie = eksisterendeGrunnlag.map(UttakGrunnlag::getOppgittFerie).orElse(null);
        var tilsynsordning = eksisterendeGrunnlag.map(UttakGrunnlag::getOppgittTilsynsordning).orElse(null);
        entityManager.persist(input);
        deaktiverEksisterendeGrunnlag(eksisterendeGrunnlag.orElse(null));
        lagreOgFlushNyttGrunnlag(new UttakGrunnlag(behandlingId, oppgittUttak, fastsattUttak, input, ferie, tilsynsordning));
    }

    public void lagreOgFlushOppgittFerie(Long behandlingId, Ferie input) {
        var eksisterendeGrunnlag = hentGrunnlag(behandlingId);
        var oppgittUttak = eksisterendeGrunnlag.map(UttakGrunnlag::getOppgittUttak).orElse(null);
        var fastsattUttak = eksisterendeGrunnlag.map(UttakGrunnlag::getFastsattUttak).orElse(null);
        var søknadsperioder = eksisterendeGrunnlag.map(UttakGrunnlag::getOppgittSøknadsperioder).orElse(null);
        var tilsynsordning = eksisterendeGrunnlag.map(UttakGrunnlag::getOppgittTilsynsordning).orElse(null);
        entityManager.persist(input);
        deaktiverEksisterendeGrunnlag(eksisterendeGrunnlag.orElse(null));
        lagreOgFlushNyttGrunnlag(new UttakGrunnlag(behandlingId, oppgittUttak, fastsattUttak, søknadsperioder, input, tilsynsordning));
    }

    public void lagreOgFlushOppgittTilsynsordning(Long behandlingId, OppgittTilsynsordning input) {
        var eksisterendeGrunnlag = hentGrunnlag(behandlingId);
        var oppgittUttak = eksisterendeGrunnlag.map(UttakGrunnlag::getOppgittUttak).orElse(null);
        var fastsattUttak = eksisterendeGrunnlag.map(UttakGrunnlag::getFastsattUttak).orElse(null);
        var ferie = eksisterendeGrunnlag.map(UttakGrunnlag::getOppgittFerie).orElse(null);
        var søknadsperioder = eksisterendeGrunnlag.map(UttakGrunnlag::getOppgittSøknadsperioder).orElse(null);
        entityManager.persist(input);
        deaktiverEksisterendeGrunnlag(eksisterendeGrunnlag.orElse(null));
        lagreOgFlushNyttGrunnlag(new UttakGrunnlag(behandlingId, oppgittUttak, fastsattUttak, søknadsperioder, ferie, input));
    }

    /**
     * Kopierer grunnlag fra en tidligere behandling. Endrer ikke aggregater, en skaper nye referanser til disse.
     */
    public void kopierGrunnlagFraEksisterendeBehandling(Long gammelBehandlingId, Long nyBehandlingId) {
        Optional<UttakAktivitet> søknadEntitet = hentOppittUttakHvisEksisterer(gammelBehandlingId);
        søknadEntitet.ifPresent(entitet -> lagreOgFlushOppgittUttak(nyBehandlingId, entitet));
    }

    private void lagreOgFlushNyttGrunnlag(UttakGrunnlag grunnlagEntitet) {
        entityManager.persist(grunnlagEntitet);
        entityManager.flush();
    }

    public Optional<UttakGrunnlag> hentGrunnlag(Long behandlingId) {
        final TypedQuery<UttakGrunnlag> query = entityManager.createQuery(
            "SELECT s FROM UttakGrunnlag s " +
                "WHERE s.behandlingId = :behandlingId AND s.aktiv = true",
            UttakGrunnlag.class);

        query.setParameter("behandlingId", behandlingId);

        return HibernateVerktøy.hentUniktResultat(query);
    }
    
    public Optional<UttakGrunnlag> hentGrunnlag(UUID behandlingId) {
        final TypedQuery<UttakGrunnlag> query = entityManager.createQuery(
            "SELECT s FROM UttakGrunnlag s INNER JOIN Behandling b on b.id=s.behandlingId " +
                "WHERE b.uuid = :behandlingId AND s.aktiv = true",
            UttakGrunnlag.class);

        query.setParameter("behandlingId", behandlingId);

        return HibernateVerktøy.hentUniktResultat(query);
    }

    private void deaktiverEksisterendeGrunnlag(UttakGrunnlag eksisterende) {
        if (eksisterende == null) {
            return;
        }
        eksisterende.setAktiv(false);
        lagreOgFlushNyttGrunnlag(eksisterende);
    }

    private DiffEntity differ(boolean medOnlyCheckTrackedFields) {
        TraverseGraph traverser = TraverseEntityGraphFactory.build(medOnlyCheckTrackedFields);
        return new DiffEntity(traverser);
    }

}
