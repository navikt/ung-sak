package no.nav.ung.sak.behandling.revurdering;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;

import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskTjeneste;
import no.nav.ung.kodeverk.behandling.BehandlingDel;
import no.nav.ung.kodeverk.behandling.BehandlingStatus;
import no.nav.ung.kodeverk.behandling.BehandlingType;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.kodeverk.produksjonsstyring.OrganisasjonsEnhet;
import no.nav.ung.kodeverk.vedtak.VedtakResultatType;
import no.nav.ung.kodeverk.vilkår.Utfall;
import no.nav.ung.kodeverk.vilkår.VilkårType;
import no.nav.ung.sak.behandling.prosessering.BehandlingProsesseringTjeneste;
import no.nav.ung.sak.behandling.prosessering.BehandlingsprosessApplikasjonTjeneste;
import no.nav.ung.sak.behandlingskontroll.impl.BehandlingskontrollTjenesteImpl;
import no.nav.ung.sak.behandlingskontroll.spi.BehandlingskontrollServiceProvider;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.historikk.HistorikkinnslagRepository;
import no.nav.ung.sak.behandlingslager.behandling.kontroll.KontrollbehandlingBryter;
import no.nav.ung.sak.behandlingslager.behandling.kontroll.ParallelleBehandlingerRepository;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingAnsvarligRepository;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingLåsRepository;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.ung.sak.behandlingslager.fagsak.FagsakLåsRepository;
import no.nav.ung.sak.behandlingslager.fagsak.FagsakProsessTaskRepository;
import no.nav.ung.sak.db.util.JpaExtension;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.domene.typer.tid.JsonObjectMapper;
import no.nav.ung.sak.produksjonsstyring.behandlingenhet.BehandlendeEnhetTjeneste;
import no.nav.ung.sak.test.util.UnitTestLookupInstanceImpl;
import no.nav.ung.sak.test.util.Whitebox;
import no.nav.ung.sak.test.util.behandling.ungdomsprogramytelse.TestScenarioBuilder;
import no.nav.ung.sak.trigger.ProsessTriggereRepository;
import no.nav.ung.sak.trigger.Trigger;

@ExtendWith(CdiAwareExtension.class)
@ExtendWith(JpaExtension.class)
class OpprettRevurderingEllerOpprettDiffTaskTest {

    private static final DatoIntervallEntitet KONTROLLMÅNED = DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 31));

    @Inject
    private EntityManager entityManager;
    @Inject
    private BeregningRevurderingTestUtil revurderingTestUtil;
    @Inject
    private BehandlingskontrollServiceProvider serviceProvider;
    @Inject
    private HistorikkinnslagRepository historikkRepository;
    @Inject
    private BehandlingAnsvarligRepository behandlingAnsvarligRepository;
    @Inject
    @Any
    private Instance<GrunnlagKopierer> grunnlagKopierer;

    private BehandlingRepositoryProvider repositoryProvider;
    private BehandlingRepository behandlingRepository;
    private ProsessTriggereRepository prosessTriggereRepository;
    private FagsakLåsRepository fagsakLåsRepository;
    private ParallelleBehandlingerRepository parallelleBehandlingerRepository;
    private RevurderingTjeneste revurderingTjeneste;

    private final BehandlingsprosessApplikasjonTjeneste behandlingsprosessApplikasjonTjeneste = mock(BehandlingsprosessApplikasjonTjeneste.class);
    private final BehandlingProsesseringTjeneste behandlingProsesseringTjeneste = mock(BehandlingProsesseringTjeneste.class);
    private final ProsessTaskTjeneste prosessTaskTjeneste = mock(ProsessTaskTjeneste.class);
    private final FagsakProsessTaskRepository fagsakProsessTaskRepository = mock(FagsakProsessTaskRepository.class);

    @BeforeEach
    void setUp() {
        repositoryProvider = new BehandlingRepositoryProvider(entityManager);
        behandlingRepository = repositoryProvider.getBehandlingRepository();
        prosessTriggereRepository = new ProsessTriggereRepository(entityManager);
        fagsakLåsRepository = new FagsakLåsRepository(entityManager);
        parallelleBehandlingerRepository = new ParallelleBehandlingerRepository(entityManager);
        revurderingTjeneste = lagRevurderingTjeneste();
    }

    @Test
    void skal_parse_korrekt_ved_et_element() {
        var perioder = new OpprettRevurderingEllerOpprettDiffTask().parseToPeriodeSet(LocalDate.now() + "/" + LocalDate.now().plusDays(14));

        assertThat(perioder).hasSize(1);
        assertThat(perioder).contains(DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now(), LocalDate.now().plusDays(14)));
    }

    @Test
    void skal_parse_korrekt_ved_flere_element() {
        var perioder = new OpprettRevurderingEllerOpprettDiffTask().parseToPeriodeSet(LocalDate.now() + "/" + LocalDate.now().plusDays(14) + "|" + LocalDate.now().plusDays(50) + "/" + LocalDate.now().plusDays(90));

        assertThat(perioder).hasSize(2);
        assertThat(perioder).contains(DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now(), LocalDate.now().plusDays(14)),
            DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now().plusDays(50), LocalDate.now().plusDays(90)));
    }

    @Test
    void skal_opprette_kontrollbehandling_fra_siste_vedtak_når_bryteren_er_på() {
        var sisteVedtak = lagAvsluttetBehandlingMedVedtak();

        lagTask(true).prosesser(kontrollTask(sisteVedtak));

        var kontrollbehandling = finnKontrollbehandling(sisteVedtak.getFagsakId());
        assertThat(kontrollbehandling.getOriginalBehandlingId()).contains(sisteVedtak.getId());
        assertThat(triggere(kontrollbehandling)).containsExactly(new Trigger(BehandlingÅrsakType.RE_KONTROLL_REGISTER_INNTEKT, KONTROLLMÅNED));
        verify(behandlingsprosessApplikasjonTjeneste).asynkStartBehandlingsprosess(kontrollbehandling);
    }

    @Test
    void skal_legge_kontroll_til_åpen_kontrollbehandling_også_når_bryteren_er_av() {
        var sisteVedtak = lagAvsluttetBehandlingMedVedtak();
        var kontrollbehandling = revurderingTjeneste.opprettKontrollbehandling(sisteVedtak, BehandlingÅrsakType.RE_KONTROLL_REGISTER_INNTEKT, false);

        lagTask(false).prosesser(kontrollTask(sisteVedtak));

        assertThat(behandlingRepository.hentAbsoluttAlleBehandlingerForFagsak(sisteVedtak.getFagsakId())).hasSize(2);
        assertThat(triggere(kontrollbehandling)).containsExactly(new Trigger(BehandlingÅrsakType.RE_KONTROLL_REGISTER_INNTEKT, KONTROLLMÅNED));
        verify(behandlingProsesseringTjeneste).opprettTasksForGjenopptaOppdaterFortsett(kontrollbehandling, false, true);
        verify(behandlingsprosessApplikasjonTjeneste, never()).asynkStartBehandlingsprosess(any());
    }

    @Test
    void skal_opprette_kontrollbehandling_ved_siden_av_åpen_ordinær_behandling_uten_kontrollårsak() {
        var sisteVedtak = lagAvsluttetBehandlingMedVedtak();
        var åpenRevurdering = revurderingTjeneste.opprettAutomatiskRevurdering(sisteVedtak, BehandlingÅrsakType.RE_ANNET);

        lagTask(true).prosesser(kontrollTask(sisteVedtak));

        var kontrollbehandling = finnKontrollbehandling(sisteVedtak.getFagsakId());
        assertThat(triggere(kontrollbehandling)).containsExactly(new Trigger(BehandlingÅrsakType.RE_KONTROLL_REGISTER_INNTEKT, KONTROLLMÅNED));
        assertThat(triggere(åpenRevurdering)).isEmpty();
        assertThat(behandlingRepository.hentBehandling(åpenRevurdering.getId()).getBehandlingÅrsakerTyper()).containsExactly(BehandlingÅrsakType.RE_ANNET);
        verify(behandlingProsesseringTjeneste, never()).opprettTasksForGjenopptaOppdaterFortsett(any(), anyBoolean(), anyBoolean());
    }

    @Test
    void skal_legge_kontroll_til_åpen_ordinær_behandling_som_allerede_kontrollerer_inntekt() {
        var sisteVedtak = lagAvsluttetBehandlingMedVedtak();
        var åpenRevurdering = revurderingTjeneste.opprettAutomatiskRevurdering(sisteVedtak, BehandlingÅrsakType.RE_KONTROLL_REGISTER_INNTEKT);

        lagTask(true).prosesser(kontrollTask(sisteVedtak));

        assertThat(behandlingRepository.hentAbsoluttAlleBehandlingerForFagsak(sisteVedtak.getFagsakId())).noneMatch(Behandling::erKontrollbehandling);
        assertThat(triggere(åpenRevurdering)).containsExactly(new Trigger(BehandlingÅrsakType.RE_KONTROLL_REGISTER_INNTEKT, KONTROLLMÅNED));
        verify(behandlingProsesseringTjeneste).opprettTasksForGjenopptaOppdaterFortsett(åpenRevurdering, false, true);
    }

    @Test
    void skal_opprette_revurdering_når_bryteren_er_av() {
        var sisteVedtak = lagAvsluttetBehandlingMedVedtak();

        lagTask(false).prosesser(kontrollTask(sisteVedtak));

        var behandlinger = behandlingRepository.hentÅpneBehandlingerForFagsakId(sisteVedtak.getFagsakId());
        assertThat(behandlinger).extracting(Behandling::getType).containsExactly(BehandlingType.REVURDERING);
        verify(behandlingsprosessApplikasjonTjeneste).asynkStartBehandlingsprosess(behandlinger.getFirst());
    }

    @Test
    void skal_bruke_ordinær_behandling_når_kontrollen_kommer_sammen_med_andre_årsaker() throws Exception {
        var sisteVedtak = lagAvsluttetBehandlingMedVedtak();
        var task = ProsessTaskData.forProsessTask(OpprettRevurderingEllerOpprettDiffTask.class);
        task.setBehandling(sisteVedtak.getFagsakId(), sisteVedtak.getId());
        task.setPayload(JsonObjectMapper.getJson(new ÅrsakerOgPerioder(List.of(
            new ÅrsakOgPerioder(BehandlingÅrsakType.RE_KONTROLL_REGISTER_INNTEKT, KONTROLLMÅNED),
            new ÅrsakOgPerioder(BehandlingÅrsakType.RE_HENDELSE_OPPHØR_UNGDOMSPROGRAM, KONTROLLMÅNED)))));

        lagTask(true).prosesser(task);

        var behandlinger = behandlingRepository.hentÅpneBehandlingerForFagsakId(sisteVedtak.getFagsakId());
        assertThat(behandlinger).extracting(Behandling::getType).containsExactly(BehandlingType.REVURDERING);
    }

    @Test
    void skal_utsette_til_siste_vedtak_er_iverksatt() {
        var underIverksetting = lagBehandlingMedVedtakUnderIverksetting();
        var task = kontrollTask(underIverksetting);
        when(fagsakProsessTaskRepository.finnAlleÅpneTasksForAngittSøk(eq(underIverksetting.getFagsakId()), eq(underIverksetting.getId()), isNull()))
            .thenReturn(List.of(new ProsessTaskData("iverksetteVedtak.avsluttBehandling")));
        when(prosessTaskTjeneste.finnUferdigForGruppe(any())).thenReturn(List.of(task));

        lagTask(true).prosesser(task);

        var kopi = ArgumentCaptor.forClass(ProsessTaskData.class);
        verify(prosessTaskTjeneste).lagre(kopi.capture());
        assertThat(kopi.getValue().getFagsakId()).isEqualTo(underIverksetting.getFagsakId());
        assertThat(kopi.getValue().getPropertyValue(OpprettRevurderingEllerOpprettDiffTask.BEHANDLING_ÅRSAK)).isEqualTo(BehandlingÅrsakType.RE_KONTROLL_REGISTER_INNTEKT.getKode());
        assertThat(kopi.getValue().getPropertyValue(OpprettRevurderingEllerOpprettDiffTask.PERIODER)).isEqualTo(task.getPropertyValue(OpprettRevurderingEllerOpprettDiffTask.PERIODER));
        assertThat(behandlingRepository.hentAbsoluttAlleBehandlingerForFagsak(underIverksetting.getFagsakId())).hasSize(1);
    }

    @Test
    void skal_feile_når_behandlingen_under_iverksetting_ikke_har_uferdige_tasker() {
        var underIverksetting = lagBehandlingMedVedtakUnderIverksetting();
        var task = kontrollTask(underIverksetting);
        when(fagsakProsessTaskRepository.finnAlleÅpneTasksForAngittSøk(any(), any(), any())).thenReturn(List.of());

        assertThatThrownBy(() -> lagTask(true).prosesser(task))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("ingen uferdige tasker");
        verify(prosessTaskTjeneste, never()).lagre(any(ProsessTaskData.class));
    }

    @Test
    void skal_ikke_opprette_kontrollbehandling_når_fagsaken_mangler_vedtak() {
        var scenario = TestScenarioBuilder.builderMedSøknad(FagsakYtelseType.UNGDOMSYTELSE);
        var førstegangsbehandling = scenario.lagre(repositoryProvider);

        lagTask(true).prosesser(kontrollTask(førstegangsbehandling));

        assertThat(behandlingRepository.hentAbsoluttAlleBehandlingerForFagsak(førstegangsbehandling.getFagsakId()))
            .extracting(Behandling::getId).containsExactly(førstegangsbehandling.getId());
        assertThat(triggere(førstegangsbehandling)).isEmpty();
        verify(behandlingProsesseringTjeneste, never()).opprettTasksForGjenopptaOppdaterFortsett(any(), anyBoolean(), anyBoolean());
        verify(behandlingsprosessApplikasjonTjeneste, never()).asynkStartBehandlingsprosess(any());
    }

    @Test
    void skal_låse_behandlinger_som_ikke_er_avsluttet_og_siste_vedtak() {
        var sisteVedtak = lagAvsluttetBehandlingMedVedtak();
        var åpenRevurdering = revurderingTjeneste.opprettAutomatiskRevurdering(sisteVedtak, BehandlingÅrsakType.RE_ANNET);
        var kontrollbehandling = revurderingTjeneste.opprettKontrollbehandling(sisteVedtak, BehandlingÅrsakType.RE_KONTROLL_REGISTER_INNTEKT, false);
        var task = ProsessTaskData.forProsessTask(OpprettRevurderingEllerOpprettDiffTask.class);
        task.setFagsakId(sisteVedtak.getFagsakId());

        var låste = lagTask(true).identifiserBehandling(task);

        assertThat(låste).containsExactlyInAnyOrder(sisteVedtak.getId().toString(), åpenRevurdering.getId().toString(), kontrollbehandling.getId().toString());
    }

    private OpprettRevurderingEllerOpprettDiffTask lagTask(boolean kontrollbehandlingPå) {
        var ruting = new KontrollbehandlingRuting(behandlingRepository, parallelleBehandlingerRepository, prosessTriggereRepository,
            new KontrollbehandlingBryter(kontrollbehandlingPå, kontrollbehandlingPå));
        return new OpprettRevurderingEllerOpprettDiffTask(repositoryProvider.getFagsakRepository(), behandlingRepository,
            new BehandlingLåsRepository(entityManager), prosessTriggereRepository, fagsakLåsRepository,
            behandlingsprosessApplikasjonTjeneste, behandlingProsesseringTjeneste, prosessTaskTjeneste,
            new UnitTestLookupInstanceImpl<>(revurderingTjeneste), ruting, parallelleBehandlingerRepository, fagsakProsessTaskRepository);
    }

    private RevurderingTjeneste lagRevurderingTjeneste() {
        var behandlendeEnhetTjeneste = mock(BehandlendeEnhetTjeneste.class);
        when(behandlendeEnhetTjeneste.finnBehandlendeEnheterFor(any())).thenReturn(Map.of(BehandlingDel.SENTRAL, new OrganisasjonsEnhet("4863", "Nav test")));
        return new RevurderingTjeneste(new BehandlingskontrollTjenesteImpl(serviceProvider), new RevurderingTjenesteFelles(repositoryProvider),
            grunnlagKopierer, historikkRepository, prosessTriggereRepository, behandlingAnsvarligRepository, behandlendeEnhetTjeneste,
            fagsakLåsRepository, new KontrollbehandlingBryter(true, true));
    }

    private static ProsessTaskData kontrollTask(Behandling behandling) {
        var task = ProsessTaskData.forProsessTask(OpprettRevurderingEllerOpprettDiffTask.class);
        task.setBehandling(behandling.getFagsakId(), behandling.getId());
        task.setProperty(OpprettRevurderingEllerOpprettDiffTask.BEHANDLING_ÅRSAK, BehandlingÅrsakType.RE_KONTROLL_REGISTER_INNTEKT.getKode());
        task.setProperty(OpprettRevurderingEllerOpprettDiffTask.PERIODER, KONTROLLMÅNED.getFomDato() + "/" + KONTROLLMÅNED.getTomDato());
        return task;
    }

    private Behandling lagAvsluttetBehandlingMedVedtak() {
        var behandling = lagBehandlingMedVedtak();
        revurderingTestUtil.avsluttBehandling(behandling);
        return behandling;
    }

    private Behandling lagBehandlingMedVedtakUnderIverksetting() {
        var behandling = lagBehandlingMedVedtak();
        Whitebox.setInternalState(behandling, "status", BehandlingStatus.IVERKSETTER_VEDTAK);
        behandlingRepository.lagre(behandling, behandlingRepository.taSkriveLås(behandling));
        entityManager.flush();
        return behandling;
    }

    private Behandling lagBehandlingMedVedtak() {
        var scenario = TestScenarioBuilder.builderMedSøknad(FagsakYtelseType.UNGDOMSYTELSE);
        scenario.leggTilVilkår(VilkårType.UNGDOMSPROGRAMVILKÅRET, Utfall.OPPFYLT);
        scenario.medBehandlingVedtak()
            .medVedtakstidspunkt(LocalDateTime.now().minusDays(1))
            .medVedtakResultatType(VedtakResultatType.INNVILGET);
        return scenario.lagre(repositoryProvider);
    }

    private Behandling finnKontrollbehandling(Long fagsakId) {
        var kontrollbehandlinger = behandlingRepository.hentAbsoluttAlleBehandlingerForFagsak(fagsakId).stream()
            .filter(Behandling::erKontrollbehandling)
            .toList();
        assertThat(kontrollbehandlinger).hasSize(1);
        return kontrollbehandlinger.getFirst();
    }

    private Set<Trigger> triggere(Behandling behandling) {
        return prosessTriggereRepository.hentGrunnlag(behandling.getId()).map(g -> g.getTriggere()).orElse(Set.of());
    }
}
