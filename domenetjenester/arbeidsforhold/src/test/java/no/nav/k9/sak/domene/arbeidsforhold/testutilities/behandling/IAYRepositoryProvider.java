package no.nav.k9.sak.domene.arbeidsforhold.testutilities.behandling;

import java.util.Objects;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.persistence.EntityManager;

import no.nav.k9.sak.behandlingslager.behandling.opptjening.OpptjeningRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.søknad.SøknadRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.k9.sak.behandlingslager.virksomhet.VirksomhetRepository;
import no.nav.k9.sak.domene.abakus.AbakusInMemoryInntektArbeidYtelseTjeneste;
import no.nav.k9.sak.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;

@Dependent
public class IAYRepositoryProvider {

    private FagsakRepository fagsakRepository;
    private SøknadRepository søknadRepository;
    private VirksomhetRepository virksomhetRepository;
    private OpptjeningRepository opptjeningRepository;
    private BehandlingRepository behandlingRepository;

    @Inject
    public IAYRepositoryProvider(EntityManager entityManager) {
        Objects.requireNonNull(entityManager, "entityManager"); //$NON-NLS-1$

        // behandling repositories
        this.behandlingRepository = new BehandlingRepository(entityManager);
        this.fagsakRepository = new FagsakRepository(entityManager);

        // behandling aggregater
        this.opptjeningRepository = new OpptjeningRepository(entityManager, this.behandlingRepository, new VilkårResultatRepository(entityManager));
        this.søknadRepository = new SøknadRepository(entityManager);

        // inntekt arbeid ytelser
        this.virksomhetRepository = new VirksomhetRepository();

    }

    public BehandlingRepository getBehandlingRepository() {
        return behandlingRepository;
    }

    public FagsakRepository getFagsakRepository() {
        // bridge metode før sammenkobling medBehandling
        return fagsakRepository;
    }

    public OpptjeningRepository getOpptjeningRepository() {
        return opptjeningRepository;
    }

    public SøknadRepository getSøknadRepository() {
        return søknadRepository;
    }

    public VirksomhetRepository getVirksomhetRepository() {
        return virksomhetRepository;
    }

    InntektArbeidYtelseTjeneste getInntektArbeidYtelseTjeneste() {
        return new AbakusInMemoryInntektArbeidYtelseTjeneste();
    }

}
