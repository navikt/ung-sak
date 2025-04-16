package no.nav.ung.sak.domene.arbeidsforhold.testutilities.behandling;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.behandling.søknad.SøknadRepository;
import no.nav.ung.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.ung.sak.domene.abakus.AbakusInMemoryInntektArbeidYtelseTjeneste;
import no.nav.ung.sak.domene.iay.modell.InntektArbeidYtelseTjeneste;

import java.util.Objects;

@Dependent
public class IAYRepositoryProvider {

    private FagsakRepository fagsakRepository;
    private SøknadRepository søknadRepository;
    private BehandlingRepository behandlingRepository;

    @Inject
    public IAYRepositoryProvider(EntityManager entityManager) {
        Objects.requireNonNull(entityManager, "entityManager"); //$NON-NLS-1$

        // behandling repositories
        this.behandlingRepository = new BehandlingRepository(entityManager);
        this.fagsakRepository = new FagsakRepository(entityManager);

        // behandling aggregater
        this.søknadRepository = new SøknadRepository(entityManager);

    }

    public BehandlingRepository getBehandlingRepository() {
        return behandlingRepository;
    }

    public FagsakRepository getFagsakRepository() {
        // bridge metode før sammenkobling medBehandling
        return fagsakRepository;
    }


    public SøknadRepository getSøknadRepository() {
        return søknadRepository;
    }

    InntektArbeidYtelseTjeneste getInntektArbeidYtelseTjeneste() {
        return new AbakusInMemoryInntektArbeidYtelseTjeneste();
    }

}
