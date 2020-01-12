package no.nav.folketrygdloven.beregningsgrunnlag;

import java.util.Objects;

import javax.persistence.EntityManager;

import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingslager.virksomhet.VirksomhetRepository;

/**
 * Provider for å enklere å kunne hente ut ulike repository uten for mange injection points.
 * Kun for test, ikke for injection
 */
public class RepositoryProvider {

    private final VirksomhetRepository virksomhetRepository;
    private EntityManager entityManager;

    public RepositoryProvider(EntityManager entityManager) {
        Objects.requireNonNull(entityManager, "entityManager"); //$NON-NLS-1$
        this.entityManager = entityManager;
        this.virksomhetRepository = new VirksomhetRepository();
    }

    public BehandlingRepository getBehandlingRepository() {
        return new BehandlingRepository(entityManager);
    }

    public FagsakRepository getFagsakRepository() {
        // bridge metode før sammenkobling medBehandling
        return new FagsakRepository(entityManager);
    }

    public HistorikkRepository getHistorikkRepository() {
        return new HistorikkRepository(entityManager);
    }

    public BeregningsgrunnlagRepository getBeregningsgrunnlagRepository() {
        return new BeregningsgrunnlagRepository(entityManager);
    }

    public VirksomhetRepository getVirksomhetRepository() {
        return virksomhetRepository;
    }

}
