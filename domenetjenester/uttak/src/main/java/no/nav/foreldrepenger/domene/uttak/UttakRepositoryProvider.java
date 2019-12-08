package no.nav.foreldrepenger.domene.uttak;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;

import no.nav.foreldrepenger.behandlingslager.uttak.UttakRepository;
import no.nav.vedtak.felles.jpa.VLPersistenceUnit;

/**
 * Provider for å enklere å kunne hente ut ulike repository uten for mange injection points.
 */
@ApplicationScoped
public class UttakRepositoryProvider {

    private UttakRepository uttakRepository;

    UttakRepositoryProvider() {
        // CDI
    }

    @Inject
    public UttakRepositoryProvider(@VLPersistenceUnit EntityManager entityManager) {
        // behandling aggregater
        this.uttakRepository = new UttakRepository(entityManager);

    }

    public UttakRepository getUttakRepository() {
        return uttakRepository;
    }

}
