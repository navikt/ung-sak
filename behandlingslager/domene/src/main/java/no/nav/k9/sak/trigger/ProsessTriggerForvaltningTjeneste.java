package no.nav.k9.sak.trigger;

import java.time.LocalDate;
import java.util.stream.Collectors;

import jakarta.persistence.EntityManager;
import no.nav.k9.kodeverk.behandling.BehandlingÅrsakType;

/**
 * Kun brukt til forvaltning
 */
public class ProsessTriggerForvaltningTjeneste {

    private final ProsessTriggereRepository prosessTriggereRepository;
    private final EntityManager entityManager;

    public ProsessTriggerForvaltningTjeneste(EntityManager entityManager) {
        this.prosessTriggereRepository = new ProsessTriggereRepository(entityManager);
        this.entityManager = entityManager;
    }

    /**
     * Metode for å fjerne prosesstrigger med gitt årsak
     * <p>
     * Denne skal kun brukes i forvaltning og aldri som en del av behandling
     *
     * @param behandlingId        BehandlingId
     * @param skjæringstidspunkt  Skjæringstidspunkt
     * @param behandlingÅrsakType Behandlingsårsak
     */
    public void fjern(Long behandlingId, LocalDate skjæringstidspunkt, BehandlingÅrsakType behandlingÅrsakType) {
        var prosessTriggere = prosessTriggereRepository.hentGrunnlag(behandlingId);

        if (prosessTriggere.isEmpty()) {
            throw new IllegalArgumentException("Fant ingen prosesstriggere");
        }

        var harTriggerPåAktivtGrunnlag = prosessTriggere.stream()
            .flatMap(t -> t.getTriggere().stream())
            .anyMatch(t -> t.getÅrsak().equals(behandlingÅrsakType) && t.getPeriode().getFomDato().equals(skjæringstidspunkt));

        if (!harTriggerPåAktivtGrunnlag) {
            throw new IllegalArgumentException("Hadde ikke prosesstrigger som skulle fjernes på aktivt grunnlag");
        }

        prosessTriggere.get().deaktiver();

        var triggereSomSkalBeholdes = prosessTriggere.stream()
            .flatMap(t -> t.getTriggere().stream())
            .filter(t -> !(t.getÅrsak().equals(behandlingÅrsakType) && t.getPeriode().getFomDato().equals(skjæringstidspunkt)))
            .toList();


        var oppdatert = new ProsessTriggere(behandlingId, new Triggere(triggereSomSkalBeholdes.stream()
            .map(Trigger::new)
            .collect(Collectors.toSet())));

        entityManager.persist(oppdatert.getTriggereEntity());
        entityManager.persist(oppdatert);
        entityManager.flush();
    }
}

