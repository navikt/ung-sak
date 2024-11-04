package no.nav.k9.sak.web.app.tjenester.behandling.beregningsgrunnlag;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.k9.kodeverk.behandling.BehandlingResultatType;
import no.nav.k9.kodeverk.behandling.BehandlingStatus;
import no.nav.k9.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.vilkår.Utfall;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.prosesstask.impl.ProsessTaskRepositoryImpl;
import no.nav.k9.prosesstask.impl.ProsessTaskTjenesteImpl;
import no.nav.k9.prosesstask.impl.TaskManager;
import no.nav.k9.sak.behandling.FagsakTjeneste;
import no.nav.k9.sak.behandling.prosessering.BehandlingsprosessApplikasjonTjeneste;
import no.nav.k9.sak.behandling.prosessering.ProsesseringAsynkTjeneste;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollTjeneste;
import no.nav.k9.sak.behandlingskontroll.impl.BehandlingskontrollTjenesteImpl;
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
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.produksjonsstyring.behandlingenhet.BehandlendeEnhetTjeneste;
import no.nav.k9.sak.trigger.ProsessTriggereRepository;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.Saksnummer;
import no.nav.k9.sak.web.app.tjenester.behandling.BehandlingsoppretterTjeneste;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.Mockito.mock;

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

    private BehandlendeEnhetTjeneste behandlendeEnhetTjeneste = mock(BehandlendeEnhetTjeneste.class);

    private BehandlingsoppretterTjeneste behandlingsoppretterTjeneste;

    @Inject
    private ProsessTriggereRepository prosessTriggereRepository;
    @Inject
    private BehandlingsprosessApplikasjonTjeneste behandlingsprosessApplikasjonTjeneste;
    @Inject
    private BehandlingskontrollTjeneste behandlingskontrollTjeneste;

    @BeforeEach
    void setUp() {
        ProsessTaskRepositoryImpl prosessTaskRepository = new ProsessTaskRepositoryImpl(entityManager, null, null);
        fagsakRepository = new FagsakRepository(entityManager);
        TaskManager taskManager = mock(TaskManager.class);
        vilkårResultatRepository = new VilkårResultatRepository(entityManager);
        behandlingsoppretterTjeneste = new BehandlingsoppretterTjeneste(new BehandlingRepositoryProvider(entityManager), behandlendeEnhetTjeneste);
        FagsakProsessTaskRepository fagsakProsessTaskRepository = new FagsakProsessTaskRepository(entityManager, new ProsessTaskTjenesteImpl(prosessTaskRepository), taskManager);
        prosessTriggereRepository = new ProsessTriggereRepository(entityManager);
        revurderBeregningTjeneste = new RevurderBeregningTjeneste(behandlingRepository, null, vilkårResultatRepository, new FagsakTjeneste(new BehandlingRepositoryProvider(entityManager), null),
            fagsakProsessTaskRepository,
            null, null, null,
            behandlingsoppretterTjeneste,
            prosessTriggereRepository,
            behandlingsprosessApplikasjonTjeneste, behandlingskontrollTjeneste
        );
    }

    @Test
    void skal_opprette_task_for_revurdering_av_opptjening() {
        var fagsak = lagFagsak();
        var behandling = lagBehandling(fagsak);
        initierVilkår(behandling);

        var gruppeId = revurderBeregningTjeneste.revurderMedÅrsak(SAKSNUMMER, STP, BehandlingÅrsakType.RE_OPPLYSNINGER_OM_OPPTJENING, Optional.empty());

        assertThat(gruppeId).isNotNull();
    }

    @Test
    void revurder_enkeltperiode_fra_gitt_steg_happy_case() {
        var fagsak = lagFagsak();
        lagFagsakMedInnvilgedeBehandlinger(fagsak);
        var fom = STP.minusDays(10);
        var tom = STP.minusDays(4);

        revurderBeregningTjeneste.revurderEnkeltperiodeFraGittSteg(fom, tom, SAKSNUMMER, BehandlingÅrsakType.RE_ENDRET_FORDELING);

    }

    @Test
    void revurder_enkeltperiode_feiler_om_dato_er_i_feil_rekkefølge() {
        var fagsak = lagFagsak();
        lagFagsakMedInnvilgedeBehandlinger(fagsak);
        var iDag = STP;
        var enUkeSiden = STP.minusDays(7);

        assertThatThrownBy(() -> revurderBeregningTjeneste.revurderEnkeltperiodeFraGittSteg(iDag, enUkeSiden, SAKSNUMMER, BehandlingÅrsakType.RE_ENDRET_FORDELING)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void revurder_enkeltperiode_feiler_om_steg_ikke_er_re_endret_fordeling() {
        var fagsak = lagFagsak();
        lagFagsakMedInnvilgedeBehandlinger(fagsak);
        var iDag = STP;
        var enUkeSiden = STP.minusDays(7);

        assertThatThrownBy(() -> revurderBeregningTjeneste.revurderEnkeltperiodeFraGittSteg(enUkeSiden, iDag, SAKSNUMMER, BehandlingÅrsakType.RE_ANNET)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void revurder_enkeltperiode_feiler_om_behandling_ikke_er_avsluttet() {
        var fagsak = lagFagsak();
        lagFagsakMedBehandlingMedStatus(fagsak, BehandlingStatus.UTREDES);

        var iDag = STP;
        var enUkeSiden = STP.minusDays(7);

        assertThatThrownBy(() -> revurderBeregningTjeneste.revurderEnkeltperiodeFraGittSteg(enUkeSiden, iDag, SAKSNUMMER, BehandlingÅrsakType.RE_ENDRET_FORDELING)).isInstanceOf(java.lang.IllegalStateException.class);
    }

    @Test
    void revurder_enkeltperiode_feiler_om_perioden_man_vil_revurdere_ikke_overlapper_med_noen_innvilgede_perioder() {
        var fagsak = lagFagsak();
        lagFagsakMedInnvilgedeBehandlinger(fagsak);

        var iDag = STP;
        var iGår = STP.minusDays(1);

        assertThatThrownBy(() -> revurderBeregningTjeneste.revurderEnkeltperiodeFraGittSteg(iGår, iDag, SAKSNUMMER, BehandlingÅrsakType.RE_ENDRET_FORDELING)).isInstanceOf(IllegalArgumentException.class);
    }

    private void initierVilkår(Behandling behandling) {
        VilkårResultatBuilder vilkårResultatBuilder = new VilkårResultatBuilder();
        var vilkårBuilder = new VilkårBuilder(VilkårType.BEREGNINGSGRUNNLAGVILKÅR);
        var vilkårPeriodeBuilder = vilkårBuilder.hentBuilderFor(STP, STP);
        vilkårPeriodeBuilder.medUtfall(Utfall.IKKE_VURDERT);
        vilkårBuilder.leggTil(vilkårPeriodeBuilder);
        vilkårResultatRepository.lagre(behandling.getId(), vilkårResultatBuilder.leggTil(vilkårBuilder).build());
    }

    private void initierVilkårMedPerioder(Behandling behandling, List<DatoIntervallEntitet> perioder) {
        VilkårResultatBuilder vilkårResultatBuilder = new VilkårResultatBuilder();
        var vilkårBuilder = new VilkårBuilder(VilkårType.BEREGNINGSGRUNNLAGVILKÅR);

        for (DatoIntervallEntitet periode : perioder) {
            var vilkårPeriodeBuilder = vilkårBuilder.hentBuilderFor(periode.getFomDato(), periode.getTomDato());
            vilkårPeriodeBuilder.medUtfall(Utfall.OPPFYLT);
            vilkårBuilder.leggTil(vilkårPeriodeBuilder);
        }

        vilkårResultatRepository.lagre(behandling.getId(), vilkårResultatBuilder.leggTil(vilkårBuilder).build());
    }

    private Behandling lagBehandling(Fagsak fagsak) {
        Behandling.Builder builder = Behandling.forFørstegangssøknad(fagsak);
        Behandling behandling = builder.build();
        behandlingRepository.lagre(behandling, behandlingRepository.taSkriveLås(behandling));
        return behandling;
    }

    private Fagsak lagFagsak() {
        Fagsak fagsak = Fagsak.opprettNy(FagsakYtelseType.PLEIEPENGER_SYKT_BARN, AktørId.dummy(), SAKSNUMMER, STP, STP);
        fagsakRepository.opprettNy(fagsak);
        return fagsak;
    }

    private void lagFagsakMedBehandlingMedStatus(Fagsak fagsak, BehandlingStatus status) {
        var behandling = Behandling.forFørstegangssøknad(fagsak)
            .medBehandlingStatus(status)
            .medOpprettetDato(STP.minusDays(30).atStartOfDay())
            .medAvsluttetDato(STP.minusDays(3).atStartOfDay())
            .build();
        behandlingRepository.lagre(behandling, behandlingRepository.taSkriveLås(behandling));
    }

    private void lagFagsakMedInnvilgedeBehandlinger(Fagsak fagsak) {
        var sisteBehandling = Behandling.forFørstegangssøknad(fagsak)
            .medBehandlingStatus(BehandlingStatus.AVSLUTTET)
            .medOpprettetDato(STP.minusDays(30).atStartOfDay())
            .medAvsluttetDato(STP.minusDays(3).atStartOfDay())
            .medBehandlingResultatType(BehandlingResultatType.INNVILGET)
            .build();
        behandlingRepository.lagre(sisteBehandling, behandlingRepository.taSkriveLås(sisteBehandling));
        initierVilkårMedPerioder(
            sisteBehandling,
            List.of(
                DatoIntervallEntitet.fraOgMedTilOgMed(
                    STP.minusDays(30),
                    STP.minusDays(3)
                )
            )
        );


    }
}
