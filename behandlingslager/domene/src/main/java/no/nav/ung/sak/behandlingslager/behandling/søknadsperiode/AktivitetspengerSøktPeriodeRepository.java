package no.nav.ung.sak.behandlingslager.behandling.søknadsperiode;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;

import java.util.Collection;

@Dependent
public class AktivitetspengerSøktPeriodeRepository {


    private final EntityManager entityManager;

    @Inject
    public AktivitetspengerSøktPeriodeRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public void lagreNyPeriode(AktivitetspengerSøktPeriode periode) {
        entityManager.persist(periode);
        entityManager.flush();
    }

    public Collection<AktivitetspengerSøktPeriode> hentSøktePerioder(Long behandlingId) {
        var query = entityManager.createQuery(
            "FROM AktivitetspengerSøktPeriode p WHERE p.behandlingId = :behandlingId",
            AktivitetspengerSøktPeriode.class);
        query.setParameter("behandlingId", behandlingId);
        return query.getResultList();
    }

    public LocalDateTimeline<Boolean> hentSøktePerioderTidslinje(Long behandlingId) {
        return new LocalDateTimeline<>(hentSøktePerioder(behandlingId).stream()
            .map(it -> new LocalDateSegment<>(it.getPeriode().getFomDato(), it.getPeriode().getTomDato(), true))
            .toList(), StandardCombinators::alwaysTrueForMatch);

    }
}
