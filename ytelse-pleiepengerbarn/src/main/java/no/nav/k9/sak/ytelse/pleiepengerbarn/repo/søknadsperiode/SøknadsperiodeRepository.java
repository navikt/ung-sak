package no.nav.k9.sak.ytelse.pleiepengerbarn.repo.søknadsperiode;

import java.util.Objects;
import java.util.Set;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.persistence.EntityManager;

import no.nav.k9.sak.typer.JournalpostId;

@Dependent
public class SøknadsperiodeRepository {

    private final EntityManager entityManager;

    @Inject
    public SøknadsperiodeRepository(EntityManager entityManager) {
        Objects.requireNonNull(entityManager, "entityManager"); //$NON-NLS-1$
        this.entityManager = entityManager;
    }


    public Set<SøknadsPeriodeDokumenter> hentPerioderKnyttetTilJournalpost(Set<JournalpostId> collect) {

        return null;
    }
}
