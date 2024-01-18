package no.nav.k9.sak.behandlingslager.behandling.uttak;

import static java.lang.Boolean.TRUE;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sikkerhet.context.SubjectHandler;

@Dependent
public class OverstyrUttakRepository {

    private EntityManager entityManager;

    @Inject
    public OverstyrUttakRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public LocalDateTimeline<OverstyrtUttakPeriode> hentOverstyrtUttak(Long behandlingId) {
        return mapFraEntitetTidslinje(hentSomEntitetTidslinje(behandlingId));
    }

    public void lagreKopiAvOverstyringAvUttak(Long behandlingId, Collection<OverstyrtUttakPeriodeEntitet> kopiListe) {
        var eksisterende = finnOverstyrtePerioder(behandlingId);
        if (!eksisterende.isEmpty()) {
            throw new IllegalStateException("Kan ikke ta kopi når behandling har eksisterende overstyringer");
        }

        kopiListe.forEach(kopierFra -> {
                OverstyrtUttakPeriodeEntitet nyOverstyring = new OverstyrtUttakPeriodeEntitet(
                    behandlingId,
                    DatoIntervallEntitet.fraOgMedTilOgMed(kopierFra.getFom(), kopierFra.getTom()),
                    kopierFra.getSøkersUttaksgrad(),
                    kopierFra.getOverstyrtUtbetalingsgrad().stream().map(OverstyrtUttakUtbetalingsgradEntitet::new).toList(),
                    kopierFra.getBegrunnelse(),
                    kopierFra.getSaksbehandler());
                entityManager.persist(nyOverstyring);
            }
        );


        entityManager.flush();
    }


    // TODO: Skriv om til å ikkje bruke Id for matching
    public void oppdaterOverstyringAvUttak(Long behandlingId, List<Long> slettes, LocalDateTimeline<OverstyrtUttakPeriode> oppdaterEllerLagre) {
        String saksbehandlerIdent = SubjectHandler.getSubjectHandler().getUid();
        LocalDateTimeline<OverstyrtUttakPeriodeEntitet> eksisterendeOverstyringer = hentSomEntitetTidslinje(behandlingId);
        Map<Long, LocalDateSegment<OverstyrtUttakPeriodeEntitet>> eksisterendeOverstyringerPrId = eksisterendeOverstyringer.stream().collect(Collectors.toMap(segment -> segment.getValue().getId(), segment -> segment));
        slettes.forEach(slettesId -> fjernOverstyring(behandlingId, slettesId));

        List<LocalDateSegment<OverstyrtUttakPeriode>> segmenterSomEndres = new ArrayList<>();
        oppdaterEllerLagre.stream().forEach(segment -> {
            OverstyrtUttakPeriode nyOverstyring = segment.getValue();
            Long eksisterendeOverstyringId = nyOverstyring.getId();
            LocalDateSegment<OverstyrtUttakPeriode> eksisterendeOverstyring = eksisterendeOverstyringId != null ?
                mapFraEntitet(eksisterendeOverstyringerPrId.get(eksisterendeOverstyringId)) :
                LocalDateSegment.emptySegment(segment.getFom(), segment.getTom());
            boolean harEndring = !Objects.equals(segment, eksisterendeOverstyring);
            if (harEndring) {
                segmenterSomEndres.add(segment);
            }
        });

        fjernEksisterendeOverstyringerSomOverlapper(new LocalDateTimeline<>(segmenterSomEndres), eksisterendeOverstyringer);
        segmenterSomEndres.forEach(segment -> {
                Long eksisterendeOverstyringId = segment.getValue().getId();
                if (eksisterendeOverstyringId != null) {
                    fjernOverstyring(behandlingId, eksisterendeOverstyringId);
                }
                OverstyrtUttakPeriode overstyring = segment.getValue();
                OverstyrtUttakPeriodeEntitet nyOverstyring = new OverstyrtUttakPeriodeEntitet(behandlingId, DatoIntervallEntitet.fra(segment.getLocalDateInterval()), overstyring.getSøkersUttaksgrad(), map(overstyring.getOverstyrtUtbetalingsgrad()), overstyring.getBegrunnelse(), saksbehandlerIdent);
                entityManager.persist(nyOverstyring);
            }
        );

        entityManager.flush();
    }

    public void kopierGrunnlagFraEksisterendeBehandling(Long originalBehandlingId, Long nyBehandlingId) {
        var overstyrtePerioder = finnOverstyrtePerioder(originalBehandlingId);
        if (!overstyrtePerioder.isEmpty()) {
            lagreKopiAvOverstyringAvUttak(nyBehandlingId, overstyrtePerioder);
        }
    }

    public void ryddMotVilkår(Long behandlingId, NavigableSet<DatoIntervallEntitet> definerendeVilkårsperioder) {
        var vilkårstidslinje = new LocalDateTimeline<>(definerendeVilkårsperioder.stream().map(p -> new LocalDateSegment<>(p.toLocalDateInterval(), TRUE)).collect(Collectors.toSet())).compress();
        var eksisterendePerioder = finnOverstyrtePerioder(behandlingId);
        var perioderTidslinje = new LocalDateTimeline<>(eksisterendePerioder.stream().map(p -> new LocalDateSegment<>(p.getFom(), p.getTom(), p)).toList());
        var perioderSomMåFjernes = perioderTidslinje.disjoint(vilkårstidslinje, StandardCombinators::leftOnly);
        fjernEksisterendeOverstyringerSomOverlapper(perioderSomMåFjernes, perioderTidslinje);
        entityManager.flush();
    }

    private void fjernEksisterendeOverstyringerSomOverlapper(LocalDateTimeline<?> perioderSomRyddes, LocalDateTimeline<OverstyrtUttakPeriodeEntitet> eksisterendeOverstyringer) {
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

    private void fjernOverstyring(Long behandlingId, Long overstyrtUttakPeriodeId) {
        OverstyrtUttakPeriodeEntitet entitet = finnOverstyrtPeriode(behandlingId, overstyrtUttakPeriodeId);
        entitet.deaktiver();
        entityManager.persist(entitet);
    }

    private LocalDateTimeline<OverstyrtUttakPeriodeEntitet> hentSomEntitetTidslinje(Long behandlingId) {
        Collection<OverstyrtUttakPeriodeEntitet> entiteter = finnOverstyrtePerioder(behandlingId);
        return tilTidslinje(entiteter);
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
            .map(this::mapFraEntitet)
            .toList());
    }

    private LocalDateSegment<OverstyrtUttakPeriode> mapFraEntitet(LocalDateSegment<OverstyrtUttakPeriodeEntitet> ou) {
        OverstyrtUttakPeriodeEntitet entitet = ou.getValue();
        return new LocalDateSegment<>(ou.getLocalDateInterval(), mapFraEntitet(entitet));
    }

    private OverstyrtUttakPeriode mapFraEntitet(OverstyrtUttakPeriodeEntitet entitet) {
        return new OverstyrtUttakPeriode(entitet.getId(), entitet.getSøkersUttaksgrad(), map(entitet.getOverstyrtUtbetalingsgrad()), entitet.getBegrunnelse(), entitet.getSaksbehandler());
    }

    private LocalDateTimeline<OverstyrtUttakPeriodeEntitet> tilTidslinje(Collection<OverstyrtUttakPeriodeEntitet> entiteter) {
        return new LocalDateTimeline<>(entiteter.stream()
            .map(ou -> new LocalDateSegment<>(ou.getFom(), ou.getTom(), ou))
            .toList());
    }

    private Set<OverstyrtUttakUtbetalingsgrad> map(Collection<OverstyrtUttakUtbetalingsgradEntitet> overstyrtUtbetalingsgrad) {
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
        return new OverstyrtUttakUtbetalingsgradEntitet(overstyrtUtbetalingsgrad.getAktivitetType(), overstyrtUtbetalingsgrad.getArbeidsgiver(), overstyrtUtbetalingsgrad.getInternArbeidsforholdRef(), overstyrtUtbetalingsgrad.getUtbetalingsgrad());
    }

    private OverstyrtUttakUtbetalingsgrad map(OverstyrtUttakUtbetalingsgradEntitet overstyrtUtbetalingsgrad) {
        return new OverstyrtUttakUtbetalingsgrad(overstyrtUtbetalingsgrad.getAktivitetType(), overstyrtUtbetalingsgrad.getArbeidsgiver(), overstyrtUtbetalingsgrad.getInternArbeidsforholdRef(), overstyrtUtbetalingsgrad.getUtbetalingsgrad());
    }

}
