package no.nav.foreldrepenger.domene.arbeidsforhold.testutilities.behandling;

import java.util.Objects;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.persistence.EntityManager;

import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.OpptjeningRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.MottatteDokumentRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingslager.virksomhet.VirksomhetRepository;
import no.nav.foreldrepenger.domene.abakus.AbakusInMemoryInntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.vedtak.felles.jpa.VLPersistenceUnit;

@Dependent
public class IAYRepositoryProvider {

    private FagsakRepository fagsakRepository;
    private AksjonspunktRepository aksjonspunktRepository;
    private PersonopplysningRepository personopplysningRepository;
    private SøknadRepository søknadRepository;
    private VirksomhetRepository virksomhetRepository;
    private OpptjeningRepository opptjeningRepository;
    private MottatteDokumentRepository mottatteDokumentRepository;
    private BehandlingRepository behandlingRepository;

    @Inject
    public IAYRepositoryProvider(@VLPersistenceUnit EntityManager entityManager) {
        Objects.requireNonNull(entityManager, "entityManager"); //$NON-NLS-1$

        // behandling repositories
        this.behandlingRepository = new BehandlingRepository(entityManager);
        this.fagsakRepository = new FagsakRepository(entityManager);
        this.aksjonspunktRepository = new AksjonspunktRepository(entityManager);

        // behandling aggregater
        this.opptjeningRepository = new OpptjeningRepository(entityManager, this.behandlingRepository);
        this.personopplysningRepository = new PersonopplysningRepository(entityManager);
        this.søknadRepository = new SøknadRepository(entityManager, this.behandlingRepository);

        // inntekt arbeid ytelser
        this.virksomhetRepository = new VirksomhetRepository();

        // behandling støtte repositories
        this.mottatteDokumentRepository = new MottatteDokumentRepository(entityManager);

    }

    public AksjonspunktRepository getAksjonspunktRepository() {
        return aksjonspunktRepository;
    }

    public BehandlingRepository getBehandlingRepository() {
        return behandlingRepository;
    }

    public FagsakRepository getFagsakRepository() {
        // bridge metode før sammenkobling medBehandling
        return fagsakRepository;
    }

    public MottatteDokumentRepository getMottatteDokumentRepository() {
        return mottatteDokumentRepository;
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

    PersonopplysningRepository getPersonopplysningRepository() {
        return personopplysningRepository;
    }
}
