package no.nav.k9.sak.behandlingslager.behandling.uttak;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;

@Dependent
public class OverstyrUttakRepository {

    private EntityManager entityManager;

    @Inject
    public OverstyrUttakRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public boolean harOverstyring(Long behandlingId) {
        return !hentOverstyrtUttak(behandlingId).isEmpty();
    }

    public LocalDateTimeline<OverstyrtUttakPeriode> hentOverstyrtUttak(Long behandlingId) {
        return mapFraEntitetTidslinje(hentSomEntitetTidslinje(behandlingId));
    }

    public void leggTilOverstyring(Long behandlingId, LocalDateInterval periode, OverstyrtUttakPeriode overstyring) {
        fjernOverstyringerForPerioden(behandlingId, periode);

        OverstyrtUttakPeriodeEntitet nyOverstyring = new OverstyrtUttakPeriodeEntitet(behandlingId, DatoIntervallEntitet.fra(periode), overstyring.getSøkersUttaksgrad(), map(overstyring.getOverstyrtUtbetalingsgrad()));
        entityManager.persist(nyOverstyring);

        entityManager.flush();
    }

    private void fjernOverstyringerForPerioden(Long behandlingId, LocalDateInterval periodeSomRyddes) {
        LocalDateTimeline<OverstyrtUttakPeriodeEntitet> eksisterendeOverstyringer = hentSomEntitetTidslinje(behandlingId);

        Set<OverstyrtUttakPeriodeEntitet> påvirkedeEksisterendeOverstyringer = eksisterendeOverstyringer.intersection(periodeSomRyddes).stream().map(LocalDateSegment::getValue).collect(Collectors.toSet());
        påvirkedeEksisterendeOverstyringer.forEach(eksisterendeOverstyring -> {
            eksisterendeOverstyring.deaktiver();
            entityManager.persist(eksisterendeOverstyring);
        });
        LocalDateTimeline<OverstyrtUttakPeriodeEntitet> delvisBevaringsverdigeEksisterendeOverstyringer = tilTidslinje(påvirkedeEksisterendeOverstyringer).disjoint(periodeSomRyddes);
        delvisBevaringsverdigeEksisterendeOverstyringer.stream().forEach(
            segment -> {
                OverstyrtUttakPeriodeEntitet bevaringsverdigPåvirketOverstyring = segment.getValue().kopiMedNyPeriode(DatoIntervallEntitet.fra(segment.getLocalDateInterval()));
                entityManager.persist(bevaringsverdigPåvirketOverstyring);
            });
    }

    public void fjernOverstyring(Long behandlingId, Long overstyrtUttakPeriodeId) {
        OverstyrtUttakPeriodeEntitet entitet = finnOverstyrtPeriode(behandlingId, overstyrtUttakPeriodeId);
        entitet.deaktiver();
        entityManager.persist(entitet);
    }

    private LocalDateTimeline<OverstyrtUttakPeriodeEntitet> hentSomEntitetTidslinje(Long behandlingId) {
        return tilTidslinje(finnOverstyrtePerioder(behandlingId));
    }

    private Collection<OverstyrtUttakPeriodeEntitet> finnOverstyrtePerioder(Long behandlingId) {
        TypedQuery<OverstyrtUttakPeriodeEntitet> query = entityManager.createQuery("from OverstyrtUttakPeriode where behandlingId=:behandlingId and aktiv", OverstyrtUttakPeriodeEntitet.class);
        query.setParameter("behandlingId", behandlingId);
        return query.getResultList();
    }

    private OverstyrtUttakPeriodeEntitet finnOverstyrtPeriode(Long behandlingId, Long overstyrtUttakPeriodeId) {
        TypedQuery<OverstyrtUttakPeriodeEntitet> query = entityManager.createQuery("from OverstyrtUttakPeriode where behandlingId=:behandlingId and id=:id and aktiv", OverstyrtUttakPeriodeEntitet.class);
        query.setParameter("behandlingId", behandlingId);
        query.setParameter("id", overstyrtUttakPeriodeId);
        List<OverstyrtUttakPeriodeEntitet> resultat = query.getResultList();
        if (resultat.size() != 1) {
            throw new IllegalArgumentException("Fant " + resultat.size() + " " + OverstyrtUttakPeriodeEntitet.class.getSimpleName() + " med id=" + overstyrtUttakPeriodeId + " for behandlingen, forventet nøyaktig 1");
        }
        return resultat.get(0);
    }

    private LocalDateTimeline<OverstyrtUttakPeriode> mapFraEntitetTidslinje(LocalDateTimeline<OverstyrtUttakPeriodeEntitet> entiteter) {
        return new LocalDateTimeline<>(entiteter.stream()
            .map(ou -> new LocalDateSegment<>(ou.getFom(), ou.getTom(), new OverstyrtUttakPeriode(ou.getValue().getId(), ou.getValue().getSøkersUttaksgrad(), map(ou.getValue().getOverstyrtUtbetalingsgrad()))))
            .toList());
    }

    private LocalDateTimeline<OverstyrtUttakPeriodeEntitet> tilTidslinje(Collection<OverstyrtUttakPeriodeEntitet> entiteter) {
        return new LocalDateTimeline<>(entiteter.stream()
            .map(ou -> new LocalDateSegment<>(ou.getFom(), ou.getTom(), ou))
            .toList());
    }

    private Set<OverstyrtUttakUtbetalingsgrad> map(List<OverstyrtUttakUtbetalingsgradEntitet> overstyrtUtbetalingsgrad) {
        return overstyrtUtbetalingsgrad.stream()
            .map(this::map)
            .collect(Collectors.toSet());
    }

    private List<OverstyrtUttakUtbetalingsgradEntitet> map(Set<OverstyrtUttakUtbetalingsgrad> overstyrtUtbetalingsgrad) {
        return overstyrtUtbetalingsgrad.stream()
            .map(this::map)
            .toList();
    }

    private OverstyrtUttakUtbetalingsgradEntitet map(OverstyrtUttakUtbetalingsgrad overstyrtUtbetalingsgrad) {
        return new OverstyrtUttakUtbetalingsgradEntitet(overstyrtUtbetalingsgrad.getAktivitetType(), overstyrtUtbetalingsgrad.getArbeidsgiverId(), overstyrtUtbetalingsgrad.getInternArbeidsforholdRef(), overstyrtUtbetalingsgrad.getUtbetalingsgrad());
    }

    private OverstyrtUttakUtbetalingsgrad map(OverstyrtUttakUtbetalingsgradEntitet overstyrtUtbetalingsgrad) {
        return new OverstyrtUttakUtbetalingsgrad(overstyrtUtbetalingsgrad.getAktivitetType(), overstyrtUtbetalingsgrad.getArbeidsgiverId(), overstyrtUtbetalingsgrad.getInternArbeidsforholdRef(), overstyrtUtbetalingsgrad.getUtbetalingsgrad());
    }

}
