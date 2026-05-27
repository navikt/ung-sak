package no.nav.ung.sak.behandlingslager.bosatt;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@ApplicationScoped
public class OpphørResultatRepository {

    private EntityManager entityManager;

    OpphørResultatRepository() {
        // CDI
    }

    @Inject
    public OpphørResultatRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public void lagre(OpphørResultat opphørResultat) {
        deaktiverEksisterende(opphørResultat.getBehandlingId(), opphørResultat.getSkjæringstidspunkt());
        entityManager.persist(opphørResultat);
        entityManager.flush();
    }

    public List<OpphørResultat> hentAktiveForBehandling(Long behandlingId) {
        return entityManager.createQuery(
                "SELECT o FROM OpphørResultat o WHERE o.behandlingId = :behandlingId AND o.aktiv = true",
                OpphørResultat.class)
            .setParameter("behandlingId", behandlingId)
            .getResultList();
    }

    public Map<LocalDate, OpphørResultat> hentAktiveForBehandlingSomMap(Long behandlingId) {
        return hentAktiveForBehandling(behandlingId).stream()
            .collect(java.util.stream.Collectors.toMap(OpphørResultat::getSkjæringstidspunkt, r -> r));
    }

    public Optional<OpphørResultat> hentAktivForBehandlingOgStp(Long behandlingId, LocalDate skjæringstidspunkt) {
        return entityManager.createQuery(
                "SELECT o FROM OpphørResultat o WHERE o.behandlingId = :behandlingId AND o.skjæringstidspunkt = :stp AND o.aktiv = true",
                OpphørResultat.class)
            .setParameter("behandlingId", behandlingId)
            .setParameter("stp", skjæringstidspunkt)
            .getResultStream()
            .findFirst();
    }

    public void deaktiverAlle(Long behandlingId) {
        hentAktiveForBehandling(behandlingId).forEach(OpphørResultat::deaktiver);
        entityManager.flush();
    }

    private void deaktiverEksisterende(Long behandlingId, LocalDate skjæringstidspunkt) {
        hentAktivForBehandlingOgStp(behandlingId, skjæringstidspunkt)
            .ifPresent(o -> {
                o.deaktiver();
                entityManager.flush();
            });
    }
}
