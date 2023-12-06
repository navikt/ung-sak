package no.nav.k9.sak.behandlingslager.behandling.uttak;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;

@Dependent
public class UttakNyeReglerRepository {

    private EntityManager entityManager;

    @Inject
    public UttakNyeReglerRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public Optional<LocalDate> finnDatoForNyeRegler(Long behandlingId) {
        return finnUttakNyeReglerEntitet(behandlingId).map(UttakNyeRegler::getVirkningsdato);
    }


    public Optional<LocalDate> finnForrigeDatoForNyeRegler(Long behandlingId) {
        return finnForrigeUttakNyeReglerEntitet(behandlingId).map(UttakNyeRegler::getVirkningsdato);
    }

    public void lagreDatoForNyeRegler(Long behandlingId, LocalDate datoNyeReglerUttak) {
        finnUttakNyeReglerEntitet(behandlingId).ifPresent(eksisterende -> {
            eksisterende.deaktiver();
            entityManager.persist(eksisterende);
            entityManager.flush();
        });
        UttakNyeRegler entitet = new UttakNyeRegler(behandlingId, datoNyeReglerUttak);
        entityManager.persist(entitet);
    }

    public void kopierGrunnlagFraEksisterendeBehandling(Long originalBehandlingId, Long nyBehandlingId) {
        if (finnUttakNyeReglerEntitet(nyBehandlingId).isPresent()) {
            throw new IllegalArgumentException("Kan ikke kopiere til en behandling som allerede har UttakNyeRegler");
        }
        finnUttakNyeReglerEntitet(originalBehandlingId).ifPresent(uttakNyeRegler -> lagreDatoForNyeRegler(nyBehandlingId, uttakNyeRegler.getVirkningsdato()));
    }

    private Optional<UttakNyeRegler> finnUttakNyeReglerEntitet(Long behandlingId) {
        TypedQuery<UttakNyeRegler> query = entityManager.createQuery("from UttakNyeRegler where behandlingId=:behandlingId and aktiv", UttakNyeRegler.class);
        query.setParameter("behandlingId", behandlingId);
        List<UttakNyeRegler> resultat = query.getResultList();
        if (resultat.isEmpty()) {
            return Optional.empty();
        }
        if (resultat.size() == 1) {
            return Optional.of(resultat.get(0));
        }
        throw new IllegalArgumentException("Forventet 0-1 treff for UttakNyeRegler, men fikk " + resultat.size());
    }

    private Optional<UttakNyeRegler> finnForrigeUttakNyeReglerEntitet(Long behandlingId) {
        TypedQuery<UttakNyeRegler> query = entityManager.createQuery("from UttakNyeRegler where behandlingId=:behandlingId and aktiv=false ORDER BY opprettetTidspunkt, id desc", UttakNyeRegler.class);
        query.setParameter("behandlingId", behandlingId);
        query.setMaxResults(1);
        List<UttakNyeRegler> resultat = query.getResultList();
        if (resultat.isEmpty()) {
            return Optional.empty();
        }
        if (resultat.size() == 1) {
            return Optional.of(resultat.get(0));
        }
        throw new IllegalArgumentException("Forventet 0-1 treff for UttakNyeRegler, men fikk " + resultat.size());
    }

}
