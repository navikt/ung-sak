package no.nav.k9.sak.trigger;

import java.time.LocalDate;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.persistence.EntityManager;
import no.nav.k9.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;

/**
 * Kun brukt til forvaltning
 */
public class ProsessTriggerForvaltningTjeneste {

    private Logger LOG = LoggerFactory.getLogger(ProsessTriggerForvaltningTjeneste.class);

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

        var triggerPåAktivtGrunnlag = prosessTriggere.stream()
            .flatMap(t -> t.getTriggere().stream())
            .filter(t -> t.getÅrsak().equals(behandlingÅrsakType) && t.getPeriode().getFomDato().equals(skjæringstidspunkt))
            .toList();

        if (triggerPåAktivtGrunnlag.isEmpty()) {
            throw new IllegalArgumentException("Hadde ikke prosesstrigger som skulle fjernes på aktivt grunnlag");
        }


        LOG.info("Utfører fjerning av trigger for behandling med id " + behandlingId + " og følgende triggere fjernes: " + triggerPåAktivtGrunnlag);

        prosessTriggere.get().deaktiver();
        entityManager.flush();

        var triggereSomSkalBeholdes = prosessTriggere.stream()
            .flatMap(t -> t.getTriggere().stream())
            .filter(t -> !(t.getÅrsak().equals(behandlingÅrsakType) && t.getPeriode().getFomDato().equals(skjæringstidspunkt)))
            .toList();


        if (!triggereSomSkalBeholdes.isEmpty()) {
            var oppdatert = new ProsessTriggere(behandlingId, new Triggere(triggereSomSkalBeholdes.stream()
                .map(Trigger::new)
                .collect(Collectors.toSet())));

            entityManager.persist(oppdatert.getTriggereEntity());
            entityManager.persist(oppdatert);
            entityManager.flush();
        }
    }


    /**
     * Metode for å fjerne prosesstrigger med gitt årsak og periode
     * <p>
     * Denne skal kun brukes i forvaltning og aldri som en del av behandling
     *
     * @param behandlingId        BehandlingId
     * @param periode             periode som fjernes
     * @param behandlingÅrsakType Behandlingsårsak
     */
    public void fjern(Long behandlingId, DatoIntervallEntitet periode, BehandlingÅrsakType behandlingÅrsakType) {
        var prosessTriggere = prosessTriggereRepository.hentGrunnlag(behandlingId);

        if (prosessTriggere.isEmpty()) {
            throw new IllegalArgumentException("Fant ingen prosesstriggere");
        }

        var triggerPåAktivtGrunnlag = prosessTriggere.stream()
            .flatMap(t -> t.getTriggere().stream())
            .filter(t -> skalFjernes(periode, behandlingÅrsakType, t))
            .toList();

        if (triggerPåAktivtGrunnlag.isEmpty()) {
            LOG.info("Fant ingen prosesstrigger for" + behandlingId + " med type " + behandlingÅrsakType + " og periode " + periode);
            return;
        }

        LOG.info("Utfører fjerning av trigger for behandling med id " + behandlingId + " og følgende triggere fjernes: " + triggerPåAktivtGrunnlag);

        prosessTriggere.get().deaktiver();
        entityManager.flush();

        var triggereSomSkalBeholdes = prosessTriggere.stream()
            .flatMap(t -> t.getTriggere().stream())
            .filter(t -> !skalFjernes(periode, behandlingÅrsakType, t))
            .toList();


        if (!triggereSomSkalBeholdes.isEmpty()) {
            var oppdatert = new ProsessTriggere(behandlingId, new Triggere(triggereSomSkalBeholdes.stream()
                .map(Trigger::new)
                .collect(Collectors.toSet())));

            entityManager.persist(oppdatert.getTriggereEntity());
            entityManager.persist(oppdatert);
            entityManager.flush();
        }
    }

    private static boolean skalFjernes(DatoIntervallEntitet periode, BehandlingÅrsakType behandlingÅrsakType, Trigger t) {
        return t.getÅrsak().equals(behandlingÅrsakType) && t.getPeriode().equals(periode);
    }

}

