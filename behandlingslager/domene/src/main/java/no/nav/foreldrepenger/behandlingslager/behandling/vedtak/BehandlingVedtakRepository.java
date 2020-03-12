package no.nav.foreldrepenger.behandlingslager.behandling.vedtak;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingLåsRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;

@ApplicationScoped
public class BehandlingVedtakRepository {

    private EntityManager entityManager;
    private BehandlingRepository behandlingRepository;

    public BehandlingVedtakRepository() {
        // for CDI proxy
    }

    @Inject
    public BehandlingVedtakRepository(EntityManager entityManager,
                                          BehandlingRepository behandlingRepository) {
        this.entityManager = entityManager;
        this.behandlingRepository = behandlingRepository;
    }

    public BehandlingVedtakRepository(EntityManager entityManager) {
        this(entityManager, new BehandlingRepository(entityManager));
    }

    protected EntityManager getEntityManager() {
        return entityManager;
    }

    public Optional<BehandlingVedtak> hentBehandlingVedtakForBehandlingId(Long behandlingId) {
        Objects.requireNonNull(behandlingId, "behandlingId"); // NOSONAR //$NON-NLS-1$
        TypedQuery<BehandlingVedtak> query = getEntityManager().createQuery("from BehandlingVedtak where behandlingId=:behandlingId", BehandlingVedtak.class);
        query.setParameter("behandlingId", behandlingId); // $NON-NLS-1$
        return optionalFirstVedtak(query.getResultList());
    }
    
    public Optional<BehandlingVedtak> hentBehandlingVedtakFor(UUID behandlingUuid) {
        Objects.requireNonNull(behandlingUuid, "behandlingUuid"); // NOSONAR //$NON-NLS-1$
        TypedQuery<BehandlingVedtak> query = getEntityManager().createQuery("Select bv from BehandlingVedtak bv INNER JOIN Behandling b ON b.id=bv.behandlingId where b.uuid=:uuid", BehandlingVedtak.class);
        query.setParameter("uuid", behandlingUuid); // $NON-NLS-1$
        return optionalFirstVedtak(query.getResultList());
    }

    public BehandlingVedtak hentBehandlingVedtakFraRevurderingensOriginaleBehandling(Behandling behandling) {
        if (!behandling.erRevurdering()) {
            throw new IllegalStateException("Utviklerfeil: Metoden skal bare kalles for revurderinger");
        }
        Behandling originalBehandling = behandling.getOriginalBehandling()
            .orElseThrow(() -> new IllegalStateException("Utviklerfeil: Original behandling mangler på revurdering - skal ikke skje"));
        return hentBehandlingVedtakForBehandlingId(originalBehandling.getId())
            .orElseThrow(() -> new IllegalStateException("Utviklerfeil: Original behandling har ikke behandlingsvedtak - skal ikke skje"));
    }

    /**
     * Lagrer vedtak på behandling. Sørger for at samtidige oppdateringer på samme Behandling, eller andre Behandlinger
     * på samme Fagsak ikke kan gjøres samtidig.
     *
     * @see BehandlingLås
     */
    public Long lagre(BehandlingVedtak vedtak, BehandlingLås lås) {
        getEntityManager().persist(vedtak);
        verifiserBehandlingLås(lås);
        getEntityManager().flush();
        return vedtak.getId();
    }

    public Optional<BehandlingVedtak> hentGjeldendeVedtak(Fagsak fagsak) {
        List<Behandling> avsluttedeIkkeHenlagteBehandlinger = behandlingRepository.finnAlleAvsluttedeIkkeHenlagteBehandlinger(fagsak.getId());
        if (avsluttedeIkkeHenlagteBehandlinger.isEmpty()) {
            return Optional.empty();
        }

        List<Behandling> behandlingerMedSisteVedtakstidspunkt = behandlingMedSisteVedtakstidspunkt(avsluttedeIkkeHenlagteBehandlinger);
        if (behandlingerMedSisteVedtakstidspunkt.size() > 1) {
            Behandling behandlingMedGjeldendeVedtak = sisteEndretVedtak(behandlingerMedSisteVedtakstidspunkt);
            return hentBehandlingVedtakForBehandlingId(behandlingMedGjeldendeVedtak.getId());
        } else {
            return hentBehandlingVedtakForBehandlingId(behandlingerMedSisteVedtakstidspunkt.get(0).getId());
        }
    }

    private List<Behandling> behandlingMedSisteVedtakstidspunkt(List<Behandling> behandlinger) {
        LocalDateTime senestVedtak = LocalDateTime.MIN;
        List<Behandling> resultat = new ArrayList<>();
        for (Behandling behandling : behandlinger) {
            LocalDateTime vedtakstidspunkt = vedtakstidspunktForBehandling(behandling);
            if (vedtakstidspunkt.isEqual(senestVedtak)) {
                resultat.add(behandling);
            } else if (vedtakstidspunkt.isAfter(senestVedtak)) {
                senestVedtak = vedtakstidspunkt;
                resultat.clear();
                resultat.add(behandling);
            }
        }
        return resultat;
    }

    private LocalDateTime vedtakstidspunktForBehandling(Behandling behandling) {
        return hentBehandlingVedtakForBehandlingId(behandling.getId()).orElseThrow().getVedtakstidspunkt();
    }

    private Behandling sisteEndretVedtak(List<Behandling> behandlinger) {
        if (behandlinger.isEmpty()) {
            throw new IllegalArgumentException("Behandlinger må ha minst ett element");
        }
        BehandlingVedtak sistEndretVedtak = null;
        Behandling sistEndretVedtakBehandling = null;
        for (Behandling behandling : behandlinger) {
            BehandlingVedtak vedtak = hentBehandlingVedtakForBehandlingId(behandling.getId()).orElseThrow();
            if (sistEndretVedtak == null || vedtak.getOpprettetTidspunkt().isAfter(sistEndretVedtak.getOpprettetTidspunkt())) {
                sistEndretVedtak = vedtak;
                sistEndretVedtakBehandling = behandling; 
            }
        }
        return sistEndretVedtakBehandling;
    }

    // sjekk lås og oppgrader til skriv
    private void verifiserBehandlingLås(BehandlingLås lås) {
        BehandlingLåsRepository låsHåndterer = new BehandlingLåsRepository(getEntityManager());
        låsHåndterer.oppdaterLåsVersjon(lås);
    }

    private static Optional<BehandlingVedtak> optionalFirstVedtak(List<BehandlingVedtak> behandlinger) {
        return behandlinger.isEmpty() ? Optional.empty() : Optional.of(behandlinger.get(0));
    }

    

}
