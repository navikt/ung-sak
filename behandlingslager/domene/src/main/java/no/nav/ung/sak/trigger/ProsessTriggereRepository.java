package no.nav.ung.sak.trigger;

import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.k9.felles.jpa.HibernateVerktøy;
import no.nav.ung.sak.behandlingslager.behandling.EndringsresultatDiff;
import no.nav.ung.sak.behandlingslager.behandling.EndringsresultatSnapshot;
import no.nav.ung.sak.behandlingslager.behandling.RegisterdataDiffsjekker;
import no.nav.ung.sak.behandlingslager.diff.DiffResult;

@Dependent
public class ProsessTriggereRepository {

    private EntityManager entityManager;

    @Inject
    public ProsessTriggereRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public void leggTil(Long behandlingId, Set<Trigger> triggere) {
        var prosessTriggere = hentEksisterendeGrunnlag(behandlingId);
        var result = new HashSet<>(triggere);

        prosessTriggere.ifPresent(it -> result.addAll(it.getTriggere()));

        if (!Objects.equals(result, prosessTriggere.map(ProsessTriggere::getTriggere).orElse(Set.of()))) {
            prosessTriggere.ifPresent(this::deaktiver);
            var oppdatert = new ProsessTriggere(behandlingId, new Triggere(result.stream()
                .map(Trigger::new)
                .collect(Collectors.toSet())));

            entityManager.persist(oppdatert.getTriggereEntity());
            entityManager.persist(oppdatert);
            entityManager.flush();
        }
    }

    public Optional<ProsessTriggere> hentGrunnlagBasertPåId(Long grunnlagId) {
        return hentEksisterendeGrunnlagBasertPåGrunnlagId(grunnlagId);
    }

    public Optional<ProsessTriggere> hentGrunnlag(Long behandlingId) {
        return hentEksisterendeGrunnlag(behandlingId);
    }

    private Optional<ProsessTriggere> hentEksisterendeGrunnlag(Long behandlingId) {
        var query = entityManager.createQuery(
            "SELECT s " +
                "FROM ProsessTriggere s " +
                "WHERE s.behandlingId = :behandlingId " +
                "AND s.aktiv = true", ProsessTriggere.class);

        query.setParameter("behandlingId", behandlingId);

        return HibernateVerktøy.hentUniktResultat(query);
    }

    private Optional<ProsessTriggere> hentEksisterendeGrunnlagBasertPåGrunnlagId(Long id) {
        var query = entityManager.createQuery(
            "SELECT s " +
                "FROM ProsessTriggere s " +
                "WHERE s.id = :id", ProsessTriggere.class);

        query.setParameter("id", id);

        return HibernateVerktøy.hentUniktResultat(query);
    }

    private void deaktiver(ProsessTriggere it) {
        it.deaktiver();
        entityManager.persist(it);
        entityManager.flush();
    }

    public EndringsresultatSnapshot finnAktivGrunnlagId(Long behandlingId) {
        Optional<Long> funnetId = hentEksisterendeGrunnlag(behandlingId).map(ProsessTriggere::getId);
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

    DiffResult diff(boolean kunSporedeEndringer, ProsessTriggere grunnlag1, ProsessTriggere grunnlag2) {
        return new RegisterdataDiffsjekker(kunSporedeEndringer).getDiffEntity().diff(grunnlag1, grunnlag2);
    }
}
