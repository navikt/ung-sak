package no.nav.ung.sak.domene.behandling.steg.parallellebehandlinger;

import static no.nav.ung.kodeverk.behandling.BehandlingStegType.BEREGN_YTELSE;
import static no.nav.ung.kodeverk.behandling.BehandlingStegType.KONTROLLER_REGISTER_INNTEKT;
import static no.nav.ung.kodeverk.behandling.BehandlingStegType.VURDER_KOMPLETTHET;
import static no.nav.ung.kodeverk.behandling.BehandlingStegType.VURDER_PARALLELLE_BEHANDLINGER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskGruppe;
import no.nav.k9.prosesstask.api.TaskType;
import no.nav.ung.kodeverk.behandling.BehandlingResultatType;
import no.nav.ung.kodeverk.behandling.BehandlingStegType;
import no.nav.ung.kodeverk.behandling.BehandlingType;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.ung.kodeverk.kontroll.KontrollertInntektKilde;
import no.nav.ung.kodeverk.varsel.EndringType;
import no.nav.ung.kodeverk.vedtak.IverksettingStatus;
import no.nav.ung.kodeverk.vedtak.VedtakResultatType;
import no.nav.ung.kodeverk.vilkår.Utfall;
import no.nav.ung.kodeverk.vilkår.VilkårType;
import no.nav.ung.sak.behandling.prosessering.BehandlingProsesseringTjeneste;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.InternalManipulerBehandling;
import no.nav.ung.sak.behandlingslager.behandling.aksjonspunkt.AksjonspunktTestSupport;
import no.nav.ung.sak.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.ung.sak.behandlingslager.behandling.historikk.HistorikkinnslagRepository;
import no.nav.ung.sak.behandlingslager.behandling.kontroll.ParallelleBehandlingerRepository;
import no.nav.ung.sak.behandlingslager.behandling.personopplysning.PersonopplysningRepository;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.ung.sak.behandlingslager.behandling.startdato.StartdatoRepository;
import no.nav.ung.sak.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.ung.sak.behandlingslager.behandling.vedtak.BehandlingVedtakRepository;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.VilkårBuilder;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.Vilkårene;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriode;
import no.nav.ung.sak.behandlingslager.fagsak.FagsakProsessTaskRepository;
import no.nav.ung.sak.behandlingslager.tilkjentytelse.KontrollertInntektPeriode;
import no.nav.ung.sak.behandlingslager.tilkjentytelse.TilkjentYtelseRepository;
import no.nav.ung.sak.behandlingslager.uttalelse.UttalelseRepository;
import no.nav.ung.sak.behandlingslager.uttalelse.UttalelseV2;
import no.nav.ung.sak.db.util.JpaExtension;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.kontroll.RelevanteKontrollperioderUtleder;
import no.nav.ung.sak.test.util.UnitTestLookupInstanceImpl;
import no.nav.ung.sak.test.util.behandling.ungdomsprogramytelse.TestScenarioBuilder;
import no.nav.ung.sak.typer.JournalpostId;

@ExtendWith(CdiAwareExtension.class)
@ExtendWith(JpaExtension.class)
class SynkroniserÅpneBehandlingerTjenesteTest {

    private static final DatoIntervallEntitet AUGUST = DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.of(2025, 8, 1), LocalDate.of(2025, 8, 31));

    @Inject
    private EntityManager entityManager;

    private BehandlingRepository behandlingRepository;
    private BehandlingVedtakRepository behandlingVedtakRepository;
    private VilkårResultatRepository vilkårResultatRepository;
    private UttalelseRepository uttalelseRepository;
    private TilkjentYtelseRepository tilkjentYtelseRepository;
    private HistorikkinnslagRepository historikkinnslagRepository;

    private BehandlingProsesseringTjeneste behandlingProsesseringTjeneste;
    private FagsakProsessTaskRepository fagsakProsessTaskRepository;
    private SynkroniserÅpneBehandlingerTaskOppretter synkroniseringTaskOppretter;
    private RelevanteKontrollperioderUtleder relevanteKontrollperioderUtleder;
    private YtelsesgrunnlagSynkroniserer ytelsesgrunnlagSynkroniserer;
    private SynkroniserÅpneBehandlingerTjeneste tjeneste;

    private Behandling førstegangsbehandling;
    private LocalDate fagsakFom;
    private LocalDateTime nesteVedtakstidspunkt = LocalDateTime.now().minusDays(1);

    @BeforeEach
    void setUp() {
        var repositoryProvider = new BehandlingRepositoryProvider(entityManager);
        behandlingRepository = repositoryProvider.getBehandlingRepository();
        behandlingVedtakRepository = new BehandlingVedtakRepository(entityManager, behandlingRepository);
        vilkårResultatRepository = new VilkårResultatRepository(entityManager);
        uttalelseRepository = new UttalelseRepository(entityManager);
        tilkjentYtelseRepository = new TilkjentYtelseRepository(entityManager);
        historikkinnslagRepository = new HistorikkinnslagRepository(entityManager);

        behandlingProsesseringTjeneste = mock(BehandlingProsesseringTjeneste.class);
        when(behandlingProsesseringTjeneste.opprettTaskGruppeForSynkronisering(any()))
            .thenAnswer(invocation -> gjenopptaGruppe());
        fagsakProsessTaskRepository = mock(FagsakProsessTaskRepository.class);
        synkroniseringTaskOppretter = mock(SynkroniserÅpneBehandlingerTaskOppretter.class);
        relevanteKontrollperioderUtleder = mock(RelevanteKontrollperioderUtleder.class);
        when(relevanteKontrollperioderUtleder.utledPerioderForKontrollAvInntekt(anyLong(), any()))
            .thenReturn(new LocalDateTimeline<>(AUGUST.getFomDato(), AUGUST.getTomDato(), Set.of(BehandlingÅrsakType.RE_KONTROLL_REGISTER_INNTEKT)));
        ytelsesgrunnlagSynkroniserer = mock(YtelsesgrunnlagSynkroniserer.class);

        var parallelleBehandlingerRepository = new ParallelleBehandlingerRepository(entityManager);
        tjeneste = new SynkroniserÅpneBehandlingerTjeneste(behandlingRepository, parallelleBehandlingerRepository,
            new ParallelleBehandlingerTjeneste(parallelleBehandlingerRepository,
                VurderParallelleBehandlingerStegTest.behandlingskontrollMedModell(VurderParallelleBehandlingerStegTest.MODELL_MED_PORT)),
            behandlingProsesseringTjeneste, fagsakProsessTaskRepository, synkroniseringTaskOppretter, relevanteKontrollperioderUtleder,
            historikkinnslagRepository, behandlingVedtakRepository, vilkårResultatRepository, uttalelseRepository, tilkjentYtelseRepository,
            new StartdatoRepository(entityManager), new PersonopplysningRepository(entityManager),
            new UnitTestLookupInstanceImpl<>(ytelsesgrunnlagSynkroniserer));

        førstegangsbehandling = TestScenarioBuilder.builderMedSøknad().lagre(repositoryProvider);
        fagsakFom = førstegangsbehandling.getFagsak().getPeriode().getFomDato();
        avslutt(førstegangsbehandling);
    }

    @Test
    void kontrollbehandling_får_vedtatte_data_fra_ordinær_behandling_og_beholder_egne_kontrolldata() {
        var kontroll = nyBehandling(BehandlingType.KONTROLL, VURDER_KOMPLETTHET);
        lagreVilkår(kontroll, fagsakFom.plusMonths(6));
        lagreUttalelse(kontroll, EndringType.ENDRET_STARTDATO, 1);
        lagreUttalelse(kontroll, EndringType.ENDRET_INNTEKT, 2);
        tilkjentYtelseRepository.lagre(kontroll.getId(), List.of(kontrollperiode(BigDecimal.TEN)));

        var revurdering = nyBehandling(BehandlingType.REVURDERING, KONTROLLER_REGISTER_INNTEKT);
        lagreVilkår(revurdering, fagsakFom.plusMonths(3));
        lagreUttalelse(revurdering, EndringType.ENDRET_PERIODE, 3);
        lagreUttalelse(revurdering, EndringType.ENDRET_INNTEKT, 4);
        tilkjentYtelseRepository.lagre(revurdering.getId(), List.of(kontrollperiode(BigDecimal.ONE)));
        avslutt(revurdering);

        tjeneste.synkroniser(synkTask());

        assertThat(vilkårsperiode(kontroll).getPeriode()).isEqualTo(DatoIntervallEntitet.fraOgMedTilOgMed(fagsakFom, fagsakFom.plusMonths(3)));
        assertThat(uttalelseRepository.hentUttalelser(kontroll.getId(), EndringType.values()))
            .extracting(UttalelseV2::getType, UttalelseV2::getSvarJournalpostId)
            .containsExactlyInAnyOrder(
                org.assertj.core.groups.Tuple.tuple(EndringType.ENDRET_PERIODE, new JournalpostId(3L)),
                org.assertj.core.groups.Tuple.tuple(EndringType.ENDRET_INNTEKT, new JournalpostId(2L)));
        assertThat(kontrollertInntekt(kontroll)).isEqualByComparingTo(BigDecimal.TEN);
        assertThat(behandlingRepository.hentBehandling(kontroll.getId()).getOriginalBehandlingId()).contains(revurdering.getId());
        verify(ytelsesgrunnlagSynkroniserer).erstattYtelsesgrunnlagFraOrdinærBehandling(revurdering.getId(), kontroll.getId());
        assertThat(historikkinnslagRepository.hent(kontroll.getId())).extracting(Historikkinnslag::getTittel)
            .contains(SynkroniserÅpneBehandlingerTjeneste.HISTORIKK_TITTEL);
        assertThat(lagretGruppe(kontroll).getTasks()).extracting(e -> e.getTask().getTaskType())
            .containsExactly("test.gjenoppta", "test.diff", "test.fortsett");
    }

    @Test
    void snapshot_til_diffen_tas_før_kopieringen() {
        var kontroll = nyBehandling(BehandlingType.KONTROLL, VURDER_KOMPLETTHET);
        var revurdering = nyBehandling(BehandlingType.REVURDERING, KONTROLLER_REGISTER_INNTEKT);
        avslutt(revurdering);

        tjeneste.synkroniser(synkTask());

        var rekkefølge = inOrder(behandlingProsesseringTjeneste, ytelsesgrunnlagSynkroniserer, fagsakProsessTaskRepository);
        rekkefølge.verify(behandlingProsesseringTjeneste).opprettTaskGruppeForSynkronisering(kontroll);
        rekkefølge.verify(ytelsesgrunnlagSynkroniserer).erstattYtelsesgrunnlagFraOrdinærBehandling(revurdering.getId(), kontroll.getId());
        rekkefølge.verify(fagsakProsessTaskRepository).lagreNyGruppeKunHvisIkkeAlleredeFinnesOgIngenHarFeilet(eq(kontroll.getFagsakId()), eq(kontroll.getId()), any(ProsessTaskGruppe.class));
    }

    @Test
    void ordinær_behandling_får_bare_kontrolldata_fra_kontrollbehandling() {
        var revurdering = nyBehandling(BehandlingType.REVURDERING, VURDER_PARALLELLE_BEHANDLINGER);
        settPåVentIPorten(revurdering);
        lagreVilkår(revurdering, fagsakFom.plusMonths(3));
        lagreUttalelse(revurdering, EndringType.ENDRET_PERIODE, 1);
        lagreUttalelse(revurdering, EndringType.ENDRET_INNTEKT, 2);
        tilkjentYtelseRepository.lagre(revurdering.getId(), List.of(kontrollperiode(BigDecimal.ONE)));

        var kontroll = nyBehandling(BehandlingType.KONTROLL, KONTROLLER_REGISTER_INNTEKT);
        lagreVilkår(kontroll, fagsakFom.plusMonths(6));
        lagreUttalelse(kontroll, EndringType.ENDRET_STARTDATO, 3);
        lagreUttalelse(kontroll, EndringType.ENDRET_INNTEKT, 4);
        tilkjentYtelseRepository.lagre(kontroll.getId(), List.of(kontrollperiode(BigDecimal.TEN)));
        avslutt(kontroll);

        tjeneste.synkroniser(synkTask());

        assertThat(kontrollertInntekt(revurdering)).isEqualByComparingTo(BigDecimal.TEN);
        assertThat(uttalelseRepository.hentUttalelser(revurdering.getId(), EndringType.values()))
            .extracting(UttalelseV2::getType, UttalelseV2::getSvarJournalpostId)
            .containsExactlyInAnyOrder(
                org.assertj.core.groups.Tuple.tuple(EndringType.ENDRET_PERIODE, new JournalpostId(1L)),
                org.assertj.core.groups.Tuple.tuple(EndringType.ENDRET_INNTEKT, new JournalpostId(4L)));
        assertThat(vilkårsperiode(revurdering).getPeriode()).isEqualTo(DatoIntervallEntitet.fraOgMedTilOgMed(fagsakFom, fagsakFom.plusMonths(3)));
        assertThat(behandlingRepository.hentBehandling(revurdering.getId()).getOriginalBehandlingId()).contains(kontroll.getId());
        verifyNoInteractions(ytelsesgrunnlagSynkroniserer);
        verify(fagsakProsessTaskRepository).lagreNyGruppeKunHvisIkkeAlleredeFinnesOgIngenHarFeilet(eq(revurdering.getFagsakId()), eq(revurdering.getId()), any(ProsessTaskGruppe.class));
        verify(behandlingProsesseringTjeneste, never()).opprettTasksForGjenopptaOppdaterFortsett(any(), anyBoolean());
    }

    @Test
    void kontrollperiodene_i_ordinær_behandling_fjernes_når_kontrollbehandlingen_ikke_har_kontrollperioder() {
        var revurdering = nyBehandling(BehandlingType.REVURDERING, VURDER_KOMPLETTHET);
        tilkjentYtelseRepository.lagre(revurdering.getId(), List.of(kontrollperiode(BigDecimal.ONE)));
        var kontroll = nyBehandling(BehandlingType.KONTROLL, KONTROLLER_REGISTER_INNTEKT);
        avslutt(kontroll);

        tjeneste.synkroniser(synkTask());

        assertThat(tilkjentYtelseRepository.hentKontrollertInntektPerioder(revurdering.getId())).isEmpty();
    }

    @Test
    void behandling_som_bygger_på_siste_vedtak_og_venter_i_porten_tas_bare_av_vent() {
        var revurdering = nyBehandling(BehandlingType.REVURDERING, VURDER_PARALLELLE_BEHANDLINGER);
        settPåVentIPorten(revurdering);
        var kontroll = nyBehandling(BehandlingType.KONTROLL, VURDER_KOMPLETTHET);

        tjeneste.synkroniser(synkTask());

        verify(behandlingProsesseringTjeneste).opprettTasksForGjenopptaOppdaterFortsett(revurdering, false);
        verify(behandlingProsesseringTjeneste, never()).opprettTasksForGjenopptaOppdaterFortsett(eq(kontroll), anyBoolean());
        verify(behandlingProsesseringTjeneste, never()).opprettTaskGruppeForSynkronisering(any());
        verify(fagsakProsessTaskRepository, never()).lagreNyGruppeKunHvisIkkeAlleredeFinnesOgIngenHarFeilet(anyLong(), anyLong(), any());
        assertThat(behandlingRepository.hentBehandling(revurdering.getId()).getOriginalBehandlingId()).contains(førstegangsbehandling.getId());
    }

    @Test
    void feiler_når_behandling_som_ikke_bygger_på_siste_vedtak_er_forbi_porten() {
        var revurdering = nyBehandling(BehandlingType.REVURDERING, BEREGN_YTELSE);
        var kontroll = nyBehandling(BehandlingType.KONTROLL, KONTROLLER_REGISTER_INNTEKT);
        avslutt(kontroll);

        assertThatThrownBy(() -> tjeneste.synkroniser(synkTask()))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining(String.valueOf(revurdering.getId()));

        verify(fagsakProsessTaskRepository, never()).lagreNyGruppeKunHvisIkkeAlleredeFinnesOgIngenHarFeilet(anyLong(), anyLong(), any());
        assertThat(behandlingRepository.hentBehandling(revurdering.getId()).getOriginalBehandlingId()).contains(førstegangsbehandling.getId());
    }

    @Test
    void utsetter_synkroniseringen_når_en_åpen_behandling_har_uferdige_tasker() {
        var kontroll = nyBehandling(BehandlingType.KONTROLL, VURDER_KOMPLETTHET);
        var revurdering = nyBehandling(BehandlingType.REVURDERING, KONTROLLER_REGISTER_INNTEKT);
        avslutt(revurdering);
        when(fagsakProsessTaskRepository.finnAlleÅpneTasksForAngittSøk(kontroll.getFagsakId(), kontroll.getId(), null))
            .thenReturn(List.of(task("test.fortsett")));

        tjeneste.synkroniser(synkTask());

        verify(synkroniseringTaskOppretter).opprett(kontroll.getFagsakId());
        verify(behandlingProsesseringTjeneste, never()).opprettTaskGruppeForSynkronisering(any());
        verify(fagsakProsessTaskRepository, never()).lagreNyGruppeKunHvisIkkeAlleredeFinnesOgIngenHarFeilet(anyLong(), anyLong(), any());
        verifyNoInteractions(ytelsesgrunnlagSynkroniserer);
        assertThat(behandlingRepository.hentBehandling(kontroll.getId()).getOriginalBehandlingId()).contains(førstegangsbehandling.getId());
    }

    @Test
    void kontrollbehandling_uten_måneder_igjen_får_henleggelse_først_i_gruppen() {
        var kontroll = nyBehandling(BehandlingType.KONTROLL, VURDER_KOMPLETTHET);
        var revurdering = nyBehandling(BehandlingType.REVURDERING, KONTROLLER_REGISTER_INNTEKT);
        avslutt(revurdering);
        when(relevanteKontrollperioderUtleder.utledPerioderForKontrollAvInntekt(eq(kontroll.getId()), any())).thenReturn(LocalDateTimeline.empty());

        tjeneste.synkroniser(synkTask());

        var tasker = lagretGruppe(kontroll).getTasks();
        assertThat(tasker).extracting(e -> e.getTask().getTaskType())
            .containsExactly(HenleggKontrollbehandlingUtenMånederTask.TASKTYPE, "test.gjenoppta", "test.diff", "test.fortsett");
        var henleggelse = tasker.getFirst().getTask();
        assertThat(henleggelse.getPropertyValue(HenleggKontrollbehandlingUtenMånederTask.KONTROLLBEHANDLING_ID)).isEqualTo(kontroll.getId().toString());
        assertThat(henleggelse.getBehandlingId()).isNull();
    }

    @Test
    void henleggelse_først_beholder_parallelle_tasker_i_gruppen() {
        var gjenopptaGruppe = new ProsessTaskGruppe();
        gjenopptaGruppe.addNesteSekvensiell(task("test.gjenoppta"));
        gjenopptaGruppe.addNesteParallell(List.of(task("test.innhent1"), task("test.innhent2")));
        gjenopptaGruppe.addNesteSekvensiell(task("test.fortsett"));

        var gruppe = SynkroniserÅpneBehandlingerTjeneste.medHenleggelseFørst(task("test.henlegg"), gjenopptaGruppe);

        assertThat(gruppe.getTasks()).extracting(e -> e.getTask().getTaskType(), ProsessTaskGruppe.Entry::getSekvens)
            .containsExactly(
                org.assertj.core.groups.Tuple.tuple("test.henlegg", "1"),
                org.assertj.core.groups.Tuple.tuple("test.gjenoppta", "2"),
                org.assertj.core.groups.Tuple.tuple("test.innhent1", "3"),
                org.assertj.core.groups.Tuple.tuple("test.innhent2", "3"),
                org.assertj.core.groups.Tuple.tuple("test.fortsett", "4"));
    }

    private ProsessTaskGruppe lagretGruppe(Behandling behandling) {
        var captor = ArgumentCaptor.forClass(ProsessTaskGruppe.class);
        verify(fagsakProsessTaskRepository).lagreNyGruppeKunHvisIkkeAlleredeFinnesOgIngenHarFeilet(eq(behandling.getFagsakId()), eq(behandling.getId()), captor.capture());
        return captor.getValue();
    }

    private static ProsessTaskGruppe gjenopptaGruppe() {
        var gruppe = new ProsessTaskGruppe();
        gruppe.addNesteSekvensiell(task("test.gjenoppta"));
        gruppe.addNesteSekvensiell(task("test.diff"));
        gruppe.addNesteSekvensiell(task("test.fortsett"));
        return gruppe;
    }

    private static ProsessTaskData task(String taskType) {
        return ProsessTaskData.forTaskType(new TaskType(taskType));
    }

    private ProsessTaskData synkTask() {
        var task = ProsessTaskData.forProsessTask(SynkroniserÅpneBehandlingerTask.class);
        task.setFagsakId(førstegangsbehandling.getFagsakId());
        return task;
    }

    private VilkårPeriode vilkårsperiode(Behandling behandling) {
        var perioder = vilkårResultatRepository.hentHvisEksisterer(behandling.getId()).orElseThrow()
            .getVilkår(VilkårType.UNGDOMSPROGRAMVILKÅRET).orElseThrow()
            .getPerioder();
        assertThat(perioder).hasSize(1);
        return perioder.getFirst();
    }

    private BigDecimal kontrollertInntekt(Behandling behandling) {
        var perioder = tilkjentYtelseRepository.hentKontrollertInntektPerioder(behandling.getId()).orElseThrow().getPerioder();
        assertThat(perioder).hasSize(1);
        return perioder.getFirst().getInntekt();
    }

    private void lagreVilkår(Behandling behandling, LocalDate tom) {
        var vilkårBuilder = new VilkårBuilder(VilkårType.UNGDOMSPROGRAMVILKÅRET);
        vilkårBuilder.leggTil(vilkårBuilder.hentBuilderFor(fagsakFom, tom).medUtfall(Utfall.OPPFYLT));
        vilkårResultatRepository.lagre(behandling.getId(), Vilkårene.builder().leggTil(vilkårBuilder).build());
    }

    private void lagreUttalelse(Behandling behandling, EndringType type, long journalpostId) {
        uttalelseRepository.lagre(behandling.getId(),
            new UttalelseV2(true, "Begrunnelse", AUGUST, new JournalpostId(journalpostId), type, UUID.randomUUID()));
    }

    private static KontrollertInntektPeriode kontrollperiode(BigDecimal inntekt) {
        return KontrollertInntektPeriode.ny()
            .medPeriode(AUGUST)
            .medInntekt(inntekt)
            .medKilde(KontrollertInntektKilde.REGISTER)
            .build();
    }

    private void settPåVentIPorten(Behandling behandling) {
        new AksjonspunktTestSupport().leggTilAksjonspunkt(behandling, AksjonspunktDefinisjon.AUTO_SATT_PÅ_VENT_PARALLELL_BEHANDLING, VURDER_PARALLELLE_BEHANDLINGER);
        lagreOgFlush(behandling);
    }

    private Behandling nyBehandling(BehandlingType type, BehandlingStegType aktivtSteg) {
        var behandling = Behandling.fraTidligereBehandling(førstegangsbehandling, type).build();
        behandlingRepository.lagre(behandling, behandlingRepository.taSkriveLås(behandling));
        new InternalManipulerBehandling().forceOppdaterBehandlingSteg(behandling, aktivtSteg);
        lagreOgFlush(behandling);
        return behandling;
    }

    private void avslutt(Behandling behandling) {
        nesteVedtakstidspunkt = nesteVedtakstidspunkt.plusMinutes(1);
        var vedtak = BehandlingVedtak.builder(behandling.getId())
            .medVedtakstidspunkt(nesteVedtakstidspunkt)
            .medAnsvarligSaksbehandler("Z000000")
            .medVedtakResultatType(VedtakResultatType.INNVILGET)
            .medIverksettingStatus(IverksettingStatus.IVERKSATT)
            .build();
        behandlingVedtakRepository.lagre(vedtak, behandlingRepository.taSkriveLås(behandling));
        behandling.setBehandlingResultatType(BehandlingResultatType.INNVILGET);
        behandling.avsluttBehandling();
        lagreOgFlush(behandling);
    }

    private void lagreOgFlush(Behandling behandling) {
        behandlingRepository.lagre(behandling, behandlingRepository.taSkriveLås(behandling));
        entityManager.flush();
    }
}
