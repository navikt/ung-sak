package no.nav.foreldrepenger.behandlingslager.behandling.repository;

import java.util.Objects;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;

import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapVilkårPeriodeRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.OpptjeningRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtakRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakLåsRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakRepository;
import no.nav.foreldrepenger.behandlingslager.virksomhet.VirksomhetRepository;

/**
 * Provider for å enklere å kunne hente ut ulike repository uten for mange injection points.
 */
@ApplicationScoped
public class BehandlingRepositoryProvider {

    private EntityManager entityManager;
    private BehandlingLåsRepository behandlingLåsRepository;
    private FagsakRepository fagsakRepository;
    private AksjonspunktRepository aksjonspunktRepository;
    private PersonopplysningRepository personopplysningRepository;
    private MedlemskapRepository medlemskapRepository;
    private MedlemskapVilkårPeriodeRepository medlemskapVilkårPeriodeRepository;
    private HistorikkRepository historikkRepository;
    private SøknadRepository søknadRepository;
    private UttakRepository uttakRepository;
    private VirksomhetRepository virksomhetRepository;
    private BehandlingVedtakRepository behandlingVedtakRepository;
    private OpptjeningRepository opptjeningRepository;
    private BeregningsresultatRepository beregningsresultatRepository;
    private MottatteDokumentRepository mottatteDokumentRepository;
    private BehandlingRevurderingRepository behandlingRevurderingRepository;
    private BehandlingsresultatRepository behandlingsresultatRepository;

    private BehandlingRepository behandlingRepository;
    private FagsakLåsRepository fagsakLåsRepository;

    BehandlingRepositoryProvider() {
        // for CDI proxy
    }

    @Inject
    public BehandlingRepositoryProvider(EntityManager entityManager) {
        Objects.requireNonNull(entityManager, "entityManager"); //$NON-NLS-1$
        this.entityManager = entityManager;

        // VIS HENSYN - IKKE FORSØPLE MER FLERE REPOS HER. TA GJERNE NOEN UT HVIS DU SER DETTE.

        // behandling repositories
        this.behandlingRepository = new BehandlingRepository(entityManager);
        this.behandlingLåsRepository = new BehandlingLåsRepository(entityManager);
        this.fagsakRepository = new FagsakRepository(entityManager);
        this.aksjonspunktRepository = new AksjonspunktRepository(entityManager);
        this.fagsakLåsRepository = new FagsakLåsRepository(entityManager);

        // behandling aggregater
        this.medlemskapRepository = new MedlemskapRepository(entityManager);
        this.medlemskapVilkårPeriodeRepository = new MedlemskapVilkårPeriodeRepository(entityManager);
        this.opptjeningRepository = new OpptjeningRepository(entityManager, this.behandlingRepository);
        this.personopplysningRepository = new PersonopplysningRepository(entityManager);
        this.søknadRepository = new SøknadRepository(entityManager, this.behandlingRepository);
        this.uttakRepository = new UttakRepository(entityManager);
        this.behandlingsresultatRepository = new BehandlingsresultatRepository(entityManager);

        // inntekt arbeid ytelser
        this.virksomhetRepository = new VirksomhetRepository();

        // behandling resultat aggregater
        this.beregningsresultatRepository = new BeregningsresultatRepository(entityManager);

        // behandling støtte repositories
        this.mottatteDokumentRepository = new MottatteDokumentRepository(entityManager);
        this.historikkRepository = new HistorikkRepository(entityManager);
        this.behandlingVedtakRepository = new BehandlingVedtakRepository(entityManager, behandlingRepository);
        this.behandlingRevurderingRepository = new BehandlingRevurderingRepository(entityManager, behandlingRepository, søknadRepository,
            behandlingLåsRepository);

        // ********
        // VIS HENSYN - IKKE FORSØPLE MER FLERE REPOS HER. DET SKAPER AVHENGIGHETER. TA GJERNE NOEN UT HVIS DU SER DETTE.
        // ********
    }

    public EntityManager getEntityManager() {
        return entityManager;
    }

    public BehandlingRepository getBehandlingRepository() {
        return behandlingRepository;
    }

    public PersonopplysningRepository getPersonopplysningRepository() {
        return personopplysningRepository;
    }

    public AksjonspunktRepository getAksjonspunktRepository() {
        return aksjonspunktRepository;
    }

    public MedlemskapRepository getMedlemskapRepository() {
        return medlemskapRepository;
    }

    public MedlemskapVilkårPeriodeRepository getMedlemskapVilkårPeriodeRepository() {
        return medlemskapVilkårPeriodeRepository;
    }

    public BehandlingLåsRepository getBehandlingLåsRepository() {
        return behandlingLåsRepository;
    }

    public FagsakRepository getFagsakRepository() {
        // bridge metode før sammenkobling medBehandling
        return fagsakRepository;
    }

    public HistorikkRepository getHistorikkRepository() {
        return historikkRepository;
    }

    public SøknadRepository getSøknadRepository() {
        return søknadRepository;
    }

    public UttakRepository getUttakRepository() {
        return uttakRepository;
    }

    public VirksomhetRepository getVirksomhetRepository() {
        return virksomhetRepository;
    }

    public BehandlingVedtakRepository getBehandlingVedtakRepository() {
        return behandlingVedtakRepository;
    }

    public OpptjeningRepository getOpptjeningRepository() {
        return opptjeningRepository;
    }

    public BeregningsresultatRepository getBeregningsresultatRepository() {
        return beregningsresultatRepository;
    }

    public MottatteDokumentRepository getMottatteDokumentRepository() {
        return mottatteDokumentRepository;
    }

    public BehandlingRevurderingRepository getBehandlingRevurderingRepository() {
        return behandlingRevurderingRepository;
    }

    public FagsakLåsRepository getFagsakLåsRepository() {
        return fagsakLåsRepository;
    }

    public BehandlingsresultatRepository getBehandlingsresultatRepository() {
        return behandlingsresultatRepository;
    }

}
