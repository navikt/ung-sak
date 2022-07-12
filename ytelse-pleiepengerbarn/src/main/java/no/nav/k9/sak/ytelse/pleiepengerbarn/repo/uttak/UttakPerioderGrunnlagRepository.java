package no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;
import no.nav.k9.felles.jpa.HibernateVerktøy;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;

@Dependent
public class UttakPerioderGrunnlagRepository {

    private final EntityManager entityManager;

    @Inject
    public UttakPerioderGrunnlagRepository(EntityManager entityManager) {
        Objects.requireNonNull(entityManager, "entityManager"); //$NON-NLS-1$
        this.entityManager = entityManager;
    }

    public Optional<UttaksPerioderGrunnlag> hentGrunnlagBasertPåId(Long grunnlagId) {
        return hentEksisterendeGrunnlagBasertPåGrunnlagId(grunnlagId);
    }

    public Optional<UttaksPerioderGrunnlag> hentGrunnlag(Long behandlingId) {
        return hentEksisterendeGrunnlag(behandlingId);
    }

    public void lagre(Long behandlingId, PerioderFraSøknad perioderFraSøknad) {
        var eksisterendeGrunnlag = hentEksisterendeGrunnlag(behandlingId);
        var nyttGrunnlag = eksisterendeGrunnlag.map(it -> new UttaksPerioderGrunnlag(behandlingId, it))
            .orElse(new UttaksPerioderGrunnlag(behandlingId));
        nyttGrunnlag.leggTil(perioderFraSøknad);

        persister(eksisterendeGrunnlag, nyttGrunnlag);
    }

    public void dedupliserSøktUttak(Long behandlingId) {
        var eksisterendeGrunnlag = hentEksisterendeGrunnlag(behandlingId).orElseThrow();
        UttaksPerioderGrunnlag deduplisertGrunnlag = new UttaksPerioderGrunnlag(behandlingId);
        for (PerioderFraSøknad perioderFraSøknad : eksisterendeGrunnlag.getOppgitteSøknadsperioder().getPerioderFraSøknadene()) {
            deduplisertGrunnlag.leggTil(dedupliserSøktUttak(perioderFraSøknad));
        }
        deduplisertGrunnlag.setRelevanteSøknadsperioder(new UttakPerioderHolder(eksisterendeGrunnlag.getRelevantSøknadsperioder().getPerioderFraSøknadene().stream()
            .map(this::dedupliserSøktUttak)
            .toList()));
        persister(Optional.of(eksisterendeGrunnlag), deduplisertGrunnlag);
    }

    private PerioderFraSøknad dedupliserSøktUttak(PerioderFraSøknad perioderFraSøknad) {
        if (!perioderFraSøknad.getBeredskap().isEmpty()) {
            throw new IllegalArgumentException("beredskap ikke tom");
        }
        if (!perioderFraSøknad.getFerie().isEmpty()) {
            throw new IllegalArgumentException("ferie ikke tom");
        }
        if (!perioderFraSøknad.getNattevåk().isEmpty()) {
            throw new IllegalArgumentException("nattevåk ikke tom");
        }
        if (!perioderFraSøknad.getTilsynsordning().isEmpty()) {
            throw new IllegalArgumentException("tilsynsordning ikke tom");
        }
        if (!perioderFraSøknad.getUtenlandsopphold().isEmpty()) {
            throw new IllegalArgumentException("utenlandshopphold ikke tom");
        }
        Set<ArbeidPeriode> arbeidPerioder = perioderFraSøknad.getArbeidPerioder()
            .stream()
            .map(ArbeidPeriode::new)
            .collect(Collectors.toSet());

        LocalDateTimeline<Duration> tidslinje = new LocalDateTimeline<>(perioderFraSøknad.getUttakPerioder().stream()
            .map(up -> new LocalDateSegment<>(up.getPeriode().toLocalDateInterval(), up.getTimerPleieAvBarnetPerDag()))
            .toList(), StandardCombinators::coalesceLeftHandSide).compress();

        List<UttakPeriode> dedupliserteUttakPerioder = tidslinje.stream()
            .map(segment -> new UttakPeriode(DatoIntervallEntitet.fra(segment.getLocalDateInterval()), segment.getValue()))
            .toList();
        return new PerioderFraSøknad(perioderFraSøknad.getJournalpostId(), dedupliserteUttakPerioder, arbeidPerioder, List.of(), List.of(), List.of(), List.of(), List.of());
    }

    /**
     * Perioder som er knyttet til denne behandlingen. Mao dokumentet periodene kom inn i er knyttet til behandlingen
     * Eventuelt en revurdering som
     *
     * @param behandlingId
     * @param søknadsperioder
     */
    public void lagreRelevantePerioder(Long behandlingId, UttakPerioderHolder søknadsperioder) {
        var eksisterendeGrunnlag = hentEksisterendeGrunnlag(behandlingId);
        var nyttGrunnlag = eksisterendeGrunnlag.map(it -> new UttaksPerioderGrunnlag(behandlingId, it))
            .orElse(new UttaksPerioderGrunnlag(behandlingId));
        nyttGrunnlag.setRelevanteSøknadsperioder(søknadsperioder);

        persister(eksisterendeGrunnlag, nyttGrunnlag);
    }

    private void persister(Optional<UttaksPerioderGrunnlag> eksisterendeGrunnlag, UttaksPerioderGrunnlag nyttGrunnlag) {
        eksisterendeGrunnlag.ifPresent(this::deaktiverEksisterende);

        if (nyttGrunnlag.getOppgitteSøknadsperioder() != null) {
            entityManager.persist(nyttGrunnlag.getOppgitteSøknadsperioder());
        }
        if (nyttGrunnlag.getRelevantSøknadsperioder() != null) {
            entityManager.persist(nyttGrunnlag.getRelevantSøknadsperioder());
        }
        entityManager.persist(nyttGrunnlag);
        entityManager.flush();
    }

    private void deaktiverEksisterende(UttaksPerioderGrunnlag gr) {
        gr.setAktiv(false);
        entityManager.persist(gr);
        entityManager.flush();
    }

    private Optional<UttaksPerioderGrunnlag> hentEksisterendeGrunnlagBasertPåGrunnlagId(Long id) {
        var query = entityManager.createQuery(
            "SELECT s " +
                "FROM UttakPerioderGrunnlag s " +
                "WHERE s.id = :grunnlagId ", UttaksPerioderGrunnlag.class);

        query.setParameter("grunnlagId", id);

        return HibernateVerktøy.hentUniktResultat(query);
    }

    private Optional<UttaksPerioderGrunnlag> hentEksisterendeGrunnlag(Long id) {
        var query = entityManager.createQuery(
            "SELECT s " +
                "FROM UttakPerioderGrunnlag s " +
                "WHERE s.behandlingId = :behandlingId " +
                "AND s.aktiv = true", UttaksPerioderGrunnlag.class);

        query.setParameter("behandlingId", id);

        return HibernateVerktøy.hentUniktResultat(query);
    }

    public void kopierGrunnlagFraEksisterendeBehandling(Long gammelBehandlingId, Long nyBehandlingId) {
        var eksisterendeGrunnlag = hentEksisterendeGrunnlag(gammelBehandlingId);
        eksisterendeGrunnlag.ifPresent(entitet -> {
            persister(Optional.empty(), new UttaksPerioderGrunnlag(nyBehandlingId, entitet));
        });
    }
}
