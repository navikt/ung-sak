package no.nav.k9.sak.utsatt;

import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.k9.felles.jpa.HibernateVerktøy;
import no.nav.k9.sak.behandlingslager.behandling.EndringsresultatDiff;
import no.nav.k9.sak.behandlingslager.behandling.EndringsresultatSnapshot;
import no.nav.k9.sak.behandlingslager.behandling.RegisterdataDiffsjekker;
import no.nav.k9.sak.behandlingslager.diff.DiffResult;
import no.nav.k9.sak.trigger.ProsessTriggere;

@Dependent
public class UtsattBehandlingAvPeriodeRepository {

    private EntityManager entityManager;

    @Inject
    public UtsattBehandlingAvPeriodeRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public void leggTil(Long behandlingId, Set<UtsattPeriode> utsattePerioder) {
        var prosessTriggere = hentEksisterendeGrunnlag(behandlingId);
        var result = new HashSet<>(utsattePerioder);

        prosessTriggere.ifPresent(it -> result.addAll(it.getPerioder()));

        if (!Objects.equals(result, prosessTriggere.map(UtsattBehandlingAvPeriode::getPerioder).orElse(Set.of()))) {
            prosessTriggere.ifPresent(this::deaktiver);
            var oppdatert = new UtsattBehandlingAvPeriode(behandlingId, new UtsattePerioder(result.stream()
                .map(UtsattPeriode::new)
                .collect(Collectors.toSet())));

            entityManager.persist(oppdatert.getTriggereEntity());
            entityManager.persist(oppdatert);
            entityManager.flush();
        }
    }

    public Optional<UtsattBehandlingAvPeriode> hentGrunnlagBasertPåId(Long grunnlagId) {
        return hentEksisterendeGrunnlagBasertPåGrunnlagId(grunnlagId);
    }

    public Optional<UtsattBehandlingAvPeriode> hentGrunnlag(Long behandlingId) {
        return hentEksisterendeGrunnlag(behandlingId);
    }

    private Optional<UtsattBehandlingAvPeriode> hentEksisterendeGrunnlag(Long behandlingId) {
        var query = entityManager.createQuery(
            "SELECT s " +
                "FROM UtsattBehandlingAvPeriode s " +
                "WHERE s.behandlingId = :behandlingId " +
                "AND s.aktiv = true", UtsattBehandlingAvPeriode.class);

        query.setParameter("behandlingId", behandlingId);

        return HibernateVerktøy.hentUniktResultat(query);
    }

    private Optional<UtsattBehandlingAvPeriode> hentEksisterendeGrunnlagBasertPåGrunnlagId(Long id) {
        var query = entityManager.createQuery(
            "SELECT s " +
                "FROM UtsattBehandlingAvPeriode s " +
                "WHERE s.id = :id", UtsattBehandlingAvPeriode.class);

        query.setParameter("id", id);

        return HibernateVerktøy.hentUniktResultat(query);
    }

    private void deaktiver(UtsattBehandlingAvPeriode it) {
        it.deaktiver();
        entityManager.persist(it);
        entityManager.flush();
    }

    public EndringsresultatSnapshot finnAktivGrunnlagId(Long behandlingId) {
        Optional<Long> funnetId = hentEksisterendeGrunnlag(behandlingId).map(UtsattBehandlingAvPeriode::getId);
        return funnetId
            .map(id -> EndringsresultatSnapshot.medSnapshot(ProsessTriggere.class, id))
            .orElse(EndringsresultatSnapshot.utenSnapshot(ProsessTriggere.class));
    }

    public DiffResult diffResultat(EndringsresultatDiff idEndring, boolean kunSporedeEndringer) {
        var grunnlagId1 = (Long) idEndring.getGrunnlagId1();
        var grunnlagId2 = (Long) idEndring.getGrunnlagId2();
        var grunnlag1 = hentEksisterendeGrunnlagBasertPåGrunnlagId(grunnlagId1)
            .orElse(null);
        var grunnlag2 = hentEksisterendeGrunnlagBasertPåGrunnlagId(grunnlagId2)
            .orElseThrow(() -> new IllegalStateException("id2 ikke kjent"));
        return diff(kunSporedeEndringer, grunnlag1, grunnlag2);
    }

    DiffResult diff(boolean kunSporedeEndringer, UtsattBehandlingAvPeriode grunnlag1, UtsattBehandlingAvPeriode grunnlag2) {
        return new RegisterdataDiffsjekker(kunSporedeEndringer).getDiffEntity().diff(grunnlag1, grunnlag2);
    }
}
