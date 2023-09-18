package no.nav.k9.sak.behandlingslager.behandling.uttak;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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

    public void oppdaterOverstyringer(Long behandlingId, List<Long> slettes, LocalDateTimeline<OverstyrtUttakPeriode> oppdaterEllerLagre) {
        LocalDateTimeline<OverstyrtUttakPeriode> eksisterendeOverstyringer = hentOverstyrtUttak(behandlingId);
        Map<Long, LocalDateSegment<OverstyrtUttakPeriode>> eksisterendeOverstyringerPrId = eksisterendeOverstyringer.stream().collect(Collectors.toMap(segment -> segment.getValue().getId(), segment -> segment));
        slettes.forEach(slettesId -> fjernOverstyring(behandlingId, slettesId));

        List<LocalDateSegment<OverstyrtUttakPeriode>> segmenterSomEndres =new ArrayList<>();
        oppdaterEllerLagre.stream().forEach(segment -> {
            OverstyrtUttakPeriode nyOverstyring = segment.getValue();
            Long eksisterendeOverstyringId = nyOverstyring.getId();
            LocalDateSegment<OverstyrtUttakPeriode> eksisterendeOverstyring = eksisterendeOverstyringId != null ? eksisterendeOverstyringerPrId.get(eksisterendeOverstyringId) : null;
            boolean harEndring = !Objects.equals(nyOverstyring, eksisterendeOverstyring);
            if (harEndring && eksisterendeOverstyringId != null){
                fjernOverstyring(behandlingId, eksisterendeOverstyringId);
            }
            if (harEndring){
                segmenterSomEndres.add(segment);
            }
        });

        fjernOverstyringerSomOverlapper(behandlingId, new LocalDateTimeline<>(segmenterSomEndres));
        entityManager.flush();
    }

    public void leggTilOverstyring(Long behandlingId, LocalDateInterval periode, OverstyrtUttakPeriode overstyring) {
        fjernOverstyringerSomOverlapper(behandlingId, new LocalDateTimeline<>(periode, true));

        OverstyrtUttakPeriodeEntitet nyOverstyring = new OverstyrtUttakPeriodeEntitet(behandlingId, DatoIntervallEntitet.fra(periode), overstyring.getSøkersUttaksgrad(), map(overstyring.getOverstyrtUtbetalingsgrad()), overstyring.getBegrunnelse());
        entityManager.persist(nyOverstyring);

        entityManager.flush();
    }

    private void fjernOverstyringerSomOverlapper(Long behandlingId, LocalDateTimeline<?> perioderSomRyddes) {
        LocalDateTimeline<OverstyrtUttakPeriodeEntitet> eksisterendeOverstyringer = hentSomEntitetTidslinje(behandlingId);

        Set<OverstyrtUttakPeriodeEntitet> påvirkedeEksisterendeOverstyringer = eksisterendeOverstyringer.intersection(perioderSomRyddes).stream().map(LocalDateSegment::getValue).collect(Collectors.toSet());
        påvirkedeEksisterendeOverstyringer.forEach(eksisterendeOverstyring -> {
            eksisterendeOverstyring.deaktiver();
            entityManager.persist(eksisterendeOverstyring);
        });
        LocalDateTimeline<OverstyrtUttakPeriodeEntitet> delvisBevaringsverdigeEksisterendeOverstyringer = tilTidslinje(påvirkedeEksisterendeOverstyringer).disjoint(perioderSomRyddes);
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
            .map(ou -> new LocalDateSegment<>(ou.getFom(), ou.getTom(), new OverstyrtUttakPeriode(ou.getValue().getId(), ou.getValue().getSøkersUttaksgrad(), map(ou.getValue().getOverstyrtUtbetalingsgrad()), ou.getValue().getBegrunnelse())))
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
