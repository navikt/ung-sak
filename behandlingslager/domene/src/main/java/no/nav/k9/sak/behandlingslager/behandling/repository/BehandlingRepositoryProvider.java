package no.nav.k9.sak.behandlingslager.behandling.repository;

import java.util.Objects;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;

import no.nav.k9.sak.behandlingslager.behandling.beregning.BeregningsresultatRepository;
import no.nav.k9.sak.behandlingslager.behandling.historikk.HistorikkRepository;
import no.nav.k9.sak.behandlingslager.behandling.medlemskap.MedlemskapRepository;
import no.nav.k9.sak.behandlingslager.behandling.opptjening.OpptjeningRepository;
import no.nav.k9.sak.behandlingslager.behandling.personopplysning.PersonopplysningRepository;
import no.nav.k9.sak.behandlingslager.behandling.søknad.SøknadRepository;
import no.nav.k9.sak.behandlingslager.behandling.vedtak.BehandlingVedtakRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakLåsRepository;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakRepository;

/**
 * Provider for å enklere å kunne hente ut ulike repository uten for mange injection points.
 */
@ApplicationScoped
public class BehandlingRepositoryProvider {

    private EntityManager entityManager;
    private BehandlingLåsRepository behandlingLåsRepository;
    private FagsakRepository fagsakRepository;
    private PersonopplysningRepository personopplysningRepository;
    private MedlemskapRepository medlemskapRepository;
    private HistorikkRepository historikkRepository;
    private SøknadRepository søknadRepository;
    private BehandlingVedtakRepository behandlingVedtakRepository;
    private OpptjeningRepository opptjeningRepository;
    private BeregningsresultatRepository beregningsresultatRepository;
    private BehandlingRevurderingRepository behandlingRevurderingRepository;
    private VilkårResultatRepository vilkårResultatRepository;

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
        this.fagsakLåsRepository = new FagsakLåsRepository(entityManager);

        // behandling aggregater
        this.medlemskapRepository = new MedlemskapRepository(entityManager);
        this.personopplysningRepository = new PersonopplysningRepository(entityManager);
        this.søknadRepository = new SøknadRepository(entityManager);

        // behandling resultat aggregater
        this.vilkårResultatRepository = new VilkårResultatRepository(entityManager);
        this.beregningsresultatRepository = new BeregningsresultatRepository(entityManager);
        this.opptjeningRepository = new OpptjeningRepository(entityManager, this.behandlingRepository, vilkårResultatRepository);

        // behandling støtte repositories
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

    public MedlemskapRepository getMedlemskapRepository() {
        return medlemskapRepository;
    }

    public BehandlingLåsRepository getBehandlingLåsRepository() {
        return behandlingLåsRepository;
    }

    public FagsakRepository getFagsakRepository() {
        // bridge metode før sammenkobling medBehandling
        return fagsakRepository;
    }

    public VilkårResultatRepository getVilkårResultatRepository() {
        return vilkårResultatRepository;
    }

    public HistorikkRepository getHistorikkRepository() {
        return historikkRepository;
    }

    public SøknadRepository getSøknadRepository() {
        return søknadRepository;
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

    public BehandlingRevurderingRepository getBehandlingRevurderingRepository() {
        return behandlingRevurderingRepository;
    }

    public FagsakLåsRepository getFagsakLåsRepository() {
        return fagsakLåsRepository;
    }

}
