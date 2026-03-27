package no.nav.ung.sak.behandlingslager.behandling.repository;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.ung.kodeverk.produksjonsstyring.OrganisasjonsEnhet;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.BehandlingAnsvarlig;
import no.nav.ung.kodeverk.behandling.BehandlingDel;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Dependent
public class BehandlingAnsvarligRepository {

    private final EntityManager entityManager;
    private final BehandlingRepository behandlingRepository;

    @Inject
    public BehandlingAnsvarligRepository(EntityManager entityManager, BehandlingRepository behandlingRepository) {
        this.entityManager = entityManager;
        this.behandlingRepository = behandlingRepository;
    }

    public Optional<BehandlingAnsvarlig> hentBehandlingAnsvarlig(Long behandlingId) {
        return hentBehandlingAnsvarlig(behandlingId, BehandlingDel.SENTRAL);
    }

    public Optional<BehandlingAnsvarlig> hentBehandlingAnsvarlig(Long behandlingId, BehandlingDel behandlingDel) {
        Objects.requireNonNull(behandlingId, "behandlingId");
        Objects.requireNonNull(behandlingDel, "behandlingDel");
        List<BehandlingAnsvarlig> resultat = entityManager.createQuery("SELECT ba FROM BehandlingAnsvarlig ba WHERE ba.behandlingDel = :behandlingDel AND ba.behandlingId = :behandlingId", BehandlingAnsvarlig.class)
            .setParameter("behandlingDel", behandlingDel)
            .setParameter("behandlingId", behandlingId)
            .getResultList();
        if (resultat.isEmpty()) {
            return Optional.empty();
        } else if (resultat.size() == 1) {
            return Optional.of(resultat.getFirst());
        } else {
            throw new IllegalStateException("Fant mer enn en BehandlingAnsvarlig for behandlingId " + behandlingId + " og behandlingDel " + behandlingDel);
        }
    }

    public Map<Long, BehandlingAnsvarlig> hentBehandlingAnsvarlig(List<Long> behandlingIder) {
        return hentBehandlingAnsvarlig(behandlingIder, BehandlingDel.SENTRAL);
    }

    public Map<Long, BehandlingAnsvarlig> hentBehandlingAnsvarlig(List<Long> behandlingIder, BehandlingDel behandlingDel) {
        return entityManager.createQuery("SELECT ba FROM BehandlingAnsvarlig ba WHERE ba.behandlingDel = :behandlingDel AND ba.behandlingId in (:behandlingId)", BehandlingAnsvarlig.class)
            .setParameter("behandlingDel", behandlingDel)
            .setParameter("behandlingId", behandlingIder)
            .getResultList()
            .stream()
            .collect(Collectors.toMap(BehandlingAnsvarlig::getBehandlingId, Function.identity()));
    }

    public void setBehandlendeEnhet(Long behandlingId, OrganisasjonsEnhet enhet) {
        setBehandlendeEnhet(behandlingId, BehandlingDel.SENTRAL, enhet, null);
    }

    public void setBehandlendeEnhet(Long behandlingId, OrganisasjonsEnhet enhet, String årsak) {
        setBehandlendeEnhet(behandlingId, BehandlingDel.SENTRAL, enhet, årsak);
    }

    private void setBehandlendeEnhet(Long behandlingId, BehandlingDel behandlingDel, OrganisasjonsEnhet enhet, String årsak) {
        BehandlingAnsvarlig behandlingAnsvarlig = hentEllerOpprett(behandlingId, behandlingDel);
        behandlingAnsvarlig.setBehandlendeEnhet(enhet);
        behandlingAnsvarlig.setBehandlendeEnhetÅrsak(årsak);
        lagre(behandlingAnsvarlig);
    }

    public void setAnsvarligBeslutter(Long behandlingId, String ansvarligBeslutterIdent) {
        setAnsvarligBeslutter(behandlingId, BehandlingDel.SENTRAL, ansvarligBeslutterIdent);
    }

    public void setAnsvarligBeslutter(Long behandlingId, BehandlingDel behandlingDel, String ansvarligBeslutterIdent) {
        BehandlingAnsvarlig behandlingAnsvarlig = hentEllerOpprett(behandlingId, behandlingDel);
        behandlingAnsvarlig.setAnsvarligBeslutter(ansvarligBeslutterIdent);
        lagre(behandlingAnsvarlig);
    }

    public void setAnsvarligSaksbehandler(Long behandlingId, String ansvarligSaksbehandlerIdent) {
        setAnsvarligSaksbehandler(behandlingId, BehandlingDel.SENTRAL, ansvarligSaksbehandlerIdent);
    }

    public void setAnsvarligSaksbehandler(Long behandlingId, BehandlingDel behandlingDel, String ansvarligSaksbehandlerIdent) {
        BehandlingAnsvarlig behandlingAnsvarlig = hentEllerOpprett(behandlingId, behandlingDel);
        behandlingAnsvarlig.setAnsvarligSaksbehandler(ansvarligSaksbehandlerIdent);
        lagre(behandlingAnsvarlig);
    }

    public boolean erTotrinnsBehandling(Long behandlingId) {
        return erTotrinnsBehandling(behandlingId, BehandlingDel.SENTRAL);
    }

    public boolean erTotrinnsBehandling(Long behandlingId, BehandlingDel behandlingDel) {
        return hentBehandlingAnsvarlig(behandlingId, behandlingDel)
            .map(BehandlingAnsvarlig::erTotrinnsBehandling)
            .orElse(false);
    }

    public String hentAnsvarligSaksbehandler(Long behandlingId) {
        return hentAnsvarligSaksbehandler(behandlingId, BehandlingDel.SENTRAL);
    }

    private String hentAnsvarligSaksbehandler(Long behandlingId, BehandlingDel behandlingDel) {
        return hentBehandlingAnsvarlig(behandlingId, behandlingDel)
            .map(BehandlingAnsvarlig::getAnsvarligSaksbehandler)
            .orElse(null);
    }

    public void nullstillToTrinnsBehandling(Long behandlingId) {
        nullstillToTrinnsBehandling(behandlingId, BehandlingDel.SENTRAL);
    }

    private void nullstillToTrinnsBehandling(Long behandlingId, BehandlingDel behandlingDel) {
        hentBehandlingAnsvarlig(behandlingId, behandlingDel).ifPresent(
            behandlingAnsvarlig -> {
                behandlingAnsvarlig.setToTrinnsBehandling(false);
                behandlingAnsvarlig.setAnsvarligBeslutter(null);
                lagre(behandlingAnsvarlig);
            }
        );
    }

    public void setToTrinnsbehandling(Long behandlingId) {
        setToTrinnsbehandling(behandlingId, BehandlingDel.SENTRAL);
    }

    public void setToTrinnsbehandling(Long behandlingId, BehandlingDel behandlingDel) {
        BehandlingAnsvarlig behandlingAnsvarlig = hentEllerOpprett(behandlingId, behandlingDel);
        behandlingAnsvarlig.setToTrinnsBehandling(true);
        lagre(behandlingAnsvarlig);
    }

    private BehandlingAnsvarlig hentEllerOpprett(Long behandlingId, BehandlingDel behandlingDel) {
        return hentBehandlingAnsvarlig(behandlingId, behandlingDel)
            .orElse(new BehandlingAnsvarlig(behandlingId, behandlingDel));
    }

    private void guardTilstandPåBehandling(Long behandlingId) {
        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);
        guardTilstandPåBehandling(behandling);
    }

    private void guardTilstandPåBehandling(Behandling behandling) {
        if (behandling.erSaksbehandlingAvsluttet()) {
            throw new IllegalStateException("Utvikler-feil: kan ikke endre tilstand på en behandling som er avsluttet.");
        }
    }

    private void lagre(BehandlingAnsvarlig behandlingAnsvarlig) {
        guardTilstandPåBehandling(behandlingAnsvarlig.getBehandlingId());

        entityManager.persist(behandlingAnsvarlig);
        entityManager.flush();

    }
}
