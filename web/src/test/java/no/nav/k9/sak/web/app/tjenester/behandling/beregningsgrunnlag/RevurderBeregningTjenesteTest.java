package no.nav.k9.sak.web.app.tjenester.behandling.beregningsgrunnlag;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.k9.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.vilkår.Utfall;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.prosesstask.impl.ProsessTaskRepositoryImpl;
import no.nav.k9.prosesstask.impl.ProsessTaskTjenesteImpl;
import no.nav.k9.prosesstask.impl.TaskManager;
import no.nav.k9.sak.behandling.FagsakTjeneste;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårBuilder;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatBuilder;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakProsessTaskRepository;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.k9.sak.db.util.CdiDbAwareTest;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.Saksnummer;

@CdiDbAwareTest
class RevurderBeregningTjenesteTest {

    public static final Saksnummer SAKSNUMMER = new Saksnummer("AAAAA");
    public static final LocalDate STP = LocalDate.now();
    @Inject
    private EntityManager entityManager;

    private RevurderBeregningTjeneste revurderBeregningTjeneste;

    @Inject
    private BehandlingRepository behandlingRepository;

    private VilkårResultatRepository vilkårResultatRepository;

    private FagsakRepository fagsakRepository;
    private Behandling behandling;

    @BeforeEach
    void setUp() {
        ProsessTaskRepositoryImpl prosessTaskRepository = new ProsessTaskRepositoryImpl(entityManager, null, null);
        fagsakRepository = new FagsakRepository(entityManager);
        TaskManager taskManager = Mockito.mock(TaskManager.class);
        vilkårResultatRepository = new VilkårResultatRepository(entityManager);

        FagsakProsessTaskRepository fagsakProsessTaskRepository = new FagsakProsessTaskRepository(entityManager, new ProsessTaskTjenesteImpl(prosessTaskRepository), taskManager);
        revurderBeregningTjeneste = new RevurderBeregningTjeneste(behandlingRepository, null, vilkårResultatRepository, new FagsakTjeneste(new BehandlingRepositoryProvider(entityManager), null),
            fagsakProsessTaskRepository,
            null, null, null
        );

        Fagsak fagsak = lagFagsak();
        lagBehandling(fagsak);
    }

    @Test
    void skal_opprette_task_for_revurdering_av_opptjening() {
        initierVilkår();

        var gruppeId = revurderBeregningTjeneste.revurderMedÅrsak(SAKSNUMMER, STP, BehandlingÅrsakType.RE_OPPLYSNINGER_OM_OPPTJENING, Optional.empty());

        assertThat(gruppeId).isNotNull();
    }

    private void initierVilkår() {
        VilkårResultatBuilder vilkårResultatBuilder = new VilkårResultatBuilder();
        var vilkårBuilder = new VilkårBuilder(VilkårType.BEREGNINGSGRUNNLAGVILKÅR);
        var vilkårPeriodeBuilder = vilkårBuilder.hentBuilderFor(STP, STP);
        vilkårPeriodeBuilder.medUtfall(Utfall.IKKE_VURDERT);
        vilkårBuilder.leggTil(vilkårPeriodeBuilder);
        vilkårResultatRepository.lagre(behandling.getId(), vilkårResultatBuilder.leggTil(vilkårBuilder).build());
    }

    private void lagBehandling(Fagsak fagsak) {
        Behandling.Builder builder = Behandling.forFørstegangssøknad(fagsak);
        behandling = builder.build();
        behandlingRepository.lagre(behandling, behandlingRepository.taSkriveLås(behandling));
    }

    private Fagsak lagFagsak() {
        Fagsak fagsak = Fagsak.opprettNy(FagsakYtelseType.PLEIEPENGER_SYKT_BARN, AktørId.dummy(), SAKSNUMMER, STP, STP);
        fagsakRepository.opprettNy(fagsak);
        return fagsak;
    }

}
