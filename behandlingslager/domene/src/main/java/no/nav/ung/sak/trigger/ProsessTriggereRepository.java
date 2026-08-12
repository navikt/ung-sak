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
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.sak.behandlingslager.behandling.EndringsresultatDiff;
import no.nav.ung.sak.behandlingslager.behandling.EndringsresultatSnapshot;
import no.nav.ung.sak.behandlingslager.behandling.RegisterdataDiffsjekker;
import no.nav.ung.sak.diff.DiffResult;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Dependent
public class ProsessTriggereRepository {

    private static final Logger log = LoggerFactory.getLogger(ProsessTriggereRepository.class);

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

    /**
     * Fjerner en spesifikk trigger (identifisert ved årsak + periode) fra aktivt grunnlag for en behandling.
     * Brukes bl.a. av forvaltning for å nøytralisere en trigger som ble opprettet av et dokument som i
     * ettertid er markert ugyldig (f.eks. en duplikat søknad), slik at den ikke lenger forsøkes matchet
     * mot søknadsperioder ved senere vurdering.
     * <p>
     * Gjør ingenting (no-op) dersom det ikke finnes noe aktivt grunnlag, eller ingen trigger matcher
     * angitt årsak+periode - dette logges eksplisitt siden det er en stille situasjon som ellers er
     * vanskelig å oppdage.
     */
    public void fjern(Long behandlingId, BehandlingÅrsakType årsak, DatoIntervallEntitet periode) {
        var eksisterende = hentEksisterendeGrunnlag(behandlingId);
        if (eksisterende.isEmpty()) {
            log.warn("Fant ingen aktivt ProsessTriggere-grunnlag for behandlingId={}, ingenting å fjerne (årsak={}, periode={})", behandlingId, årsak, periode);
            return;
        }

        var eksisterendeTriggere = eksisterende.get().getTriggere();
        var gjenværende = eksisterendeTriggere.stream()
            .filter(t -> !(t.getÅrsak() == årsak && t.getPeriode().equals(periode)))
            .map(Trigger::new)
            .collect(Collectors.toSet());

        if (gjenværende.size() == eksisterendeTriggere.size()) {
            log.warn("Fant ingen trigger som matchet årsak={} og periode={} for behandlingId={} - ingen endring gjort. Eksisterende triggere: {}",
                årsak, periode, behandlingId, eksisterendeTriggere);
            return;
        }

        deaktiver(eksisterende.get());
        var oppdatert = new ProsessTriggere(behandlingId, new Triggere(gjenværende));
        entityManager.persist(oppdatert.getTriggereEntity());
        entityManager.persist(oppdatert);
        entityManager.flush();

        log.info("Fjernet trigger årsak={} periode={} fra behandlingId={}. Gjenværende triggere: {}", årsak, periode, behandlingId, gjenværende);
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
