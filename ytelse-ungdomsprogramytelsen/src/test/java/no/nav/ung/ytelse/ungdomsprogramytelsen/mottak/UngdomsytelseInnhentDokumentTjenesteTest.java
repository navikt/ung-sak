package no.nav.ung.ytelse.ungdomsprogramytelsen.mottak;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.k9.oppgave.OppgaveBekreftelse;
import no.nav.k9.oppgave.bekreftelse.ung.inntekt.InntektBekreftelse;
import no.nav.k9.prosesstask.api.ProsessTaskGruppe;
import no.nav.k9.prosesstask.api.ProsessTaskTjeneste;
import no.nav.k9.søknad.Søknad;
import no.nav.k9.søknad.felles.Versjon;
import no.nav.k9.søknad.felles.personopplysninger.Søker;
import no.nav.k9.søknad.felles.type.NorskIdentitetsnummer;
import no.nav.k9.søknad.felles.type.SøknadId;
import no.nav.ung.kodeverk.behandling.BehandlingDel;
import no.nav.ung.kodeverk.behandling.BehandlingStatus;
import no.nav.ung.kodeverk.behandling.BehandlingStegType;
import no.nav.ung.kodeverk.behandling.BehandlingType;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.kodeverk.dokument.Brevkode;
import no.nav.ung.kodeverk.dokument.DokumentStatus;
import no.nav.ung.kodeverk.produksjonsstyring.OrganisasjonsEnhet;
import no.nav.ung.kodeverk.vedtak.VedtakResultatType;
import no.nav.ung.sak.behandling.revurdering.KontrollbehandlingRuting;
import no.nav.ung.sak.behandling.prosessering.BehandlingProsesseringTjeneste;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.aksjonspunkt.AksjonspunktTestSupport;
import no.nav.ung.sak.behandlingslager.behandling.kontroll.KontrollbehandlingBryter;
import no.nav.ung.sak.behandlingslager.behandling.kontroll.ParallelleBehandlingerRepository;
import no.nav.ung.sak.behandlingslager.behandling.motattdokument.MottattDokument;
import no.nav.ung.sak.behandlingslager.behandling.motattdokument.MottatteDokumentRepository;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.ung.sak.behandlingslager.etterlysning.Etterlysning;
import no.nav.ung.sak.behandlingslager.etterlysning.EtterlysningRepository;
import no.nav.ung.sak.behandlingslager.fagsak.Fagsak;
import no.nav.ung.sak.behandlingslager.fagsak.FagsakProsessTaskRepository;
import no.nav.ung.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.ung.sak.db.util.JpaExtension;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.mottak.Behandlingsoppretter;
import no.nav.ung.sak.mottak.dokumentmottak.Dokumentmottaker;
import no.nav.ung.sak.mottak.dokumentmottak.MottattDokumentRuting;
import no.nav.ung.sak.mottak.dokumentmottak.SøknadParser;
import no.nav.ung.sak.mottak.dokumentmottak.Trigger;
import no.nav.ung.sak.mottak.dokumentmottak.oppgavebekreftelse.OppgaveBekreftelseParser;
import no.nav.ung.sak.produksjonsstyring.behandlingenhet.BehandlendeEnhetTjeneste;
import no.nav.ung.sak.test.util.UnitTestLookupInstanceImpl;
import no.nav.ung.sak.test.util.Whitebox;
import no.nav.ung.sak.test.util.behandling.ungdomsprogramytelse.TestScenarioBuilder;
import no.nav.ung.sak.trigger.ProsessTriggereRepository;
import no.nav.ung.sak.typer.AktørId;
import no.nav.ung.sak.typer.Saksnummer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static java.time.LocalDate.now;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.*;

@ExtendWith(CdiAwareExtension.class)
@ExtendWith(JpaExtension.class)
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class UngdomsytelseInnhentDokumentTjenesteTest {

    @Inject
    private BehandlingRepositoryProvider repositoryProvider;
    @Inject
    private BehandlingRepository behandlingRepository;
    @Inject
    private FagsakRepository fagsakRepository;
    @Inject
    private MottatteDokumentRepository mottatteDokumentRepository;

    private AksjonspunktTestSupport aksjonspunktRepository;

    @Mock
    private ProsessTaskTjeneste prosessTaskTjeneste;
    @Mock
    private BehandlendeEnhetTjeneste behandlendeEnhetTjeneste;
    @Mock
    private Behandlingsoppretter behandlingsoppretter;
    @Mock
    private ProsessTriggereRepository prosessTriggereRepository;
    @Mock
    private Dokumentmottaker dokumentmottaker;
    @Mock
    private BehandlingProsesseringTjeneste behandlingProsesseringTjeneste;
    @Mock
    private FagsakProsessTaskRepository fagsakProsessTaskRepository;
    @Mock
    private SøknadParser søknadParser;
    @Mock
    private OppgaveBekreftelseParser oppgaveBekreftelseParser;
    @Mock
    private EtterlysningRepository etterlysningRepository;
    @Inject
    private EntityManager entityManager;

    private UngdomsytelseInnhentDokumentTjeneste innhentDokumentTjeneste;

    @BeforeEach
    public void oppsett() {
        aksjonspunktRepository = new AksjonspunktTestSupport();

        MockitoAnnotations.initMocks(this);

        innhentDokumentTjeneste = lagInnhentDokumentTjeneste(false);

        OrganisasjonsEnhet enhet = new OrganisasjonsEnhet("0312", "enhetNavn");
        when(behandlendeEnhetTjeneste.finnBehandlendeEnhetFor(any(Fagsak.class), eq(BehandlingDel.SENTRAL))).thenReturn(enhet);
        when(behandlingProsesseringTjeneste.opprettTaskGruppeForGjenopptaOppdaterFortsett(any(Behandling.class), anyBoolean(), anyBoolean())).thenReturn(new ProsessTaskGruppe());

        when(dokumentmottaker.getTriggere(ArgumentMatchers.anyList())).thenReturn(List.of(new Trigger(DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now(), LocalDate.now()), BehandlingÅrsakType.RE_RAPPORTERING_INNTEKT)));
    }


    @Test
    public void skal_lagre_dokument__dersom_inntektrapportering_på_åpen_behandling() {
        // Arrange - opprette åpen behandling
        var scenario = TestScenarioBuilder.builderMedSøknad()
            .medBehandlingStegStart(BehandlingStegType.INNHENT_REGISTEROPP);
        Behandling behandling = scenario.lagre(repositoryProvider);

        // Arrange - bygg dok
        MottattDokument mottattDokument = DokumentmottakTestUtil.byggMottattDokument(behandling.getFagsakId(), "", now(), "123", Brevkode.UNGDOMSYTELSE_INNTEKTRAPPORTERING);

        // Act
        innhentDokumentTjeneste.mottaDokument(behandling.getFagsak(), List.of(mottattDokument));

        // Assert - sjekk flyt
        verify(behandlingProsesseringTjeneste).opprettTaskGruppeForGjenopptaOppdaterFortsett(behandling, false, false);
        verify(dokumentmottaker).lagreDokumentinnhold(List.of(mottattDokument), behandling);
    }

    @Test
    public void skal_opprette_revurdering_dersom_inntektrapportering_på_avsluttet_behandling() {
        // Arrange - opprette avsluttet førstegangsbehandling
        var scenario = TestScenarioBuilder.builderMedSøknad();
        Behandling behandling = scenario.lagre(repositoryProvider);
        behandling.avsluttBehandling();
        BehandlingLås behandlingLås = behandlingRepository.taSkriveLås(behandling);
        behandlingRepository.lagre(behandling, behandlingLås);

        Behandling revurdering = mock(Behandling.class);
        when(revurdering.getId()).thenReturn(10L);
        when(revurdering.getFagsakId()).thenReturn(behandling.getFagsakId());
        when(revurdering.getFagsak()).thenReturn(behandling.getFagsak());
        when(revurdering.getAktørId()).thenReturn(behandling.getAktørId());

        MottattDokument mottattDokument = DokumentmottakTestUtil.byggMottattDokument(behandling.getFagsakId(), "", now(), "123", Brevkode.UNGDOMSYTELSE_INNTEKTRAPPORTERING);
        when(behandlingsoppretter.opprettRevurdering(behandling, BehandlingÅrsakType.RE_RAPPORTERING_INNTEKT)).thenReturn(revurdering);

        // Act
        innhentDokumentTjeneste.mottaDokument(behandling.getFagsak(), List.of(mottattDokument));

        // Assert
        verify(dokumentmottaker).lagreDokumentinnhold(List.of(mottattDokument), revurdering);
    }

    @Test
    public void skal_opprette_førstegangsbehandling() {

        Fagsak fagsak = DokumentmottakTestUtil.byggFagsak(AktørId.dummy(), new Saksnummer("123"), fagsakRepository);
        MottattDokument mottattDokument = DokumentmottakTestUtil.byggMottattDokument(123L, "", now(), "123", Brevkode.UNGDOMSYTELSE_INNTEKTRAPPORTERING);
        Behandling førstegangsbehandling = mock(Behandling.class);
        when(førstegangsbehandling.getFagsak()).thenReturn(fagsak);
        when(førstegangsbehandling.getAktørId()).thenReturn(AktørId.dummy());
        when(førstegangsbehandling.getFagsakId()).thenReturn(fagsak.getId());
        when(behandlingsoppretter.opprettFørstegangsbehandling(fagsak, BehandlingÅrsakType.UDEFINERT, Optional.empty())).thenReturn(førstegangsbehandling);

        // Act
        innhentDokumentTjeneste.mottaDokument(fagsak, List.of(mottattDokument));

        // Assert
        verify(behandlingsoppretter).opprettFørstegangsbehandling(fagsak, BehandlingÅrsakType.UDEFINERT, Optional.empty());
        verify(dokumentmottaker).lagreDokumentinnhold(List.of(mottattDokument), førstegangsbehandling);
    }

    @Test
    public void skal_ignorere_søknad_som_allerede_er_mottatt_på_fagsaken() {
        var scenario = TestScenarioBuilder.builderMedSøknad();
        scenario.medSøknad().medSøknadId("søknad-1");
        Behandling behandling = scenario.lagre(repositoryProvider);
        behandling.avsluttBehandling();
        behandlingRepository.lagre(behandling, behandlingRepository.taSkriveLås(behandling));

        MottattDokument duplikat = lagreSøknadDokument(behandling.getFagsakId(), "100", "søknad-1");

        innhentDokumentTjeneste.mottaDokument(behandling.getFagsak(), List.of(duplikat));

        assertThat(duplikat.getStatus()).isEqualTo(DokumentStatus.UGYLDIG);
        assertThat(duplikat.getFeilmelding()).contains("søknad-1");
        verify(behandlingsoppretter, never()).opprettRevurdering(any(), any());
        verify(dokumentmottaker, never()).lagreDokumentinnhold(any(), any());
        verify(prosessTaskTjeneste, never()).lagre(any(ProsessTaskGruppe.class));
    }

    @Test
    public void skal_behandle_søknad_med_ny_søknadId_som_vanlig() {
        var scenario = TestScenarioBuilder.builderMedSøknad()
            .medBehandlingStegStart(BehandlingStegType.INNHENT_REGISTEROPP);
        scenario.medSøknad().medSøknadId("søknad-1");
        Behandling behandling = scenario.lagre(repositoryProvider);

        MottattDokument ny = lagreSøknadDokument(behandling.getFagsakId(), "100", "søknad-2");

        innhentDokumentTjeneste.mottaDokument(behandling.getFagsak(), List.of(ny));

        assertThat(ny.getStatus()).isEqualTo(DokumentStatus.BEHANDLER);
        verify(dokumentmottaker).lagreDokumentinnhold(List.of(ny), behandling);
    }

    @Test
    public void skal_ikke_regne_søknad_uten_søknadId_som_duplikat() {
        var scenario = TestScenarioBuilder.builderMedSøknad()
            .medBehandlingStegStart(BehandlingStegType.INNHENT_REGISTEROPP);
        Behandling behandling = scenario.lagre(repositoryProvider);

        MottattDokument utenId = lagreSøknadDokument(behandling.getFagsakId(), "100", null);

        innhentDokumentTjeneste.mottaDokument(behandling.getFagsak(), List.of(utenId));

        verify(dokumentmottaker).lagreDokumentinnhold(List.of(utenId), behandling);
    }

    @Test
    public void skal_ignorere_duplikat_søknad_i_samme_batch() {
        var scenario = TestScenarioBuilder.builderMedSøknad()
            .medBehandlingStegStart(BehandlingStegType.INNHENT_REGISTEROPP);
        Behandling behandling = scenario.lagre(repositoryProvider);

        MottattDokument første = lagreSøknadDokument(behandling.getFagsakId(), "100", "søknad-3");
        MottattDokument andre = lagreSøknadDokument(behandling.getFagsakId(), "101", "søknad-3");

        innhentDokumentTjeneste.mottaDokument(behandling.getFagsak(), List.of(første, andre));

        assertThat(andre.getStatus()).isEqualTo(DokumentStatus.UGYLDIG);
        assertThat(første.getStatus()).isEqualTo(DokumentStatus.BEHANDLER);
        verify(dokumentmottaker).lagreDokumentinnhold(List.of(første), behandling);
    }

    @Test
    public void skal_opprette_kontrollbehandling_fra_siste_vedtak_for_inntektsrapportering_når_bryteren_er_på() {
        var tjeneste = lagInnhentDokumentTjeneste(true);
        var vedtak = lagAvsluttetBehandlingMedVedtak();
        var kontrollbehandling = mockNyBehandling(vedtak, 20L);
        when(behandlingsoppretter.opprettKontrollbehandling(vedtak, BehandlingÅrsakType.RE_RAPPORTERING_INNTEKT)).thenReturn(kontrollbehandling);
        var rapportering = DokumentmottakTestUtil.byggMottattDokument(vedtak.getFagsakId(), "", now(), "123", Brevkode.UNGDOMSYTELSE_INNTEKTRAPPORTERING);

        tjeneste.mottaDokument(vedtak.getFagsak(), List.of(rapportering));

        verify(dokumentmottaker).lagreDokumentinnhold(List.of(rapportering), kontrollbehandling);
        verify(behandlingsoppretter, never()).opprettRevurdering(any(), any());
    }

    @Test
    public void skal_legge_inntektsrapportering_til_åpen_ordinær_behandling_med_rapporteringstrigger() {
        var tjeneste = lagInnhentDokumentTjeneste(true);
        var vedtak = lagAvsluttetBehandlingMedVedtak();
        var revurdering = lagÅpenBehandling(vedtak, BehandlingType.REVURDERING);
        new ProsessTriggereRepository(entityManager).leggTil(revurdering.getId(), Set.of(
            new no.nav.ung.sak.trigger.Trigger(BehandlingÅrsakType.RE_RAPPORTERING_INNTEKT, DatoIntervallEntitet.fraOgMedTilOgMed(now().minusMonths(1), now()))));
        var rapportering = DokumentmottakTestUtil.byggMottattDokument(vedtak.getFagsakId(), "", now(), "123", Brevkode.UNGDOMSYTELSE_INNTEKTRAPPORTERING);

        tjeneste.mottaDokument(vedtak.getFagsak(), List.of(rapportering));

        verify(dokumentmottaker).lagreDokumentinnhold(List.of(rapportering), revurdering);
        verify(behandlingsoppretter, never()).opprettKontrollbehandling(any(), any());
    }

    @Test
    public void skal_legge_inntektsrapportering_til_åpen_kontrollbehandling_også_når_bryteren_er_av() {
        var tjeneste = lagInnhentDokumentTjeneste(false);
        var vedtak = lagAvsluttetBehandlingMedVedtak();
        var kontrollbehandling = lagÅpenBehandling(vedtak, BehandlingType.KONTROLL);
        var rapportering = DokumentmottakTestUtil.byggMottattDokument(vedtak.getFagsakId(), "", now(), "123", Brevkode.UNGDOMSYTELSE_INNTEKTRAPPORTERING);

        tjeneste.mottaDokument(vedtak.getFagsak(), List.of(rapportering));

        verify(behandlingProsesseringTjeneste).opprettTaskGruppeForGjenopptaOppdaterFortsett(kontrollbehandling, false, false);
        verify(dokumentmottaker).lagreDokumentinnhold(List.of(rapportering), kontrollbehandling);
        verify(behandlingsoppretter, never()).opprettRevurdering(any(), any());
    }

    @Test
    public void skal_sende_svar_på_etterlysning_til_kontrollbehandlingen_som_eier_etterlysningen() {
        var tjeneste = lagInnhentDokumentTjeneste(true);
        var vedtak = lagAvsluttetBehandlingMedVedtak();
        var revurdering = lagÅpenBehandling(vedtak, BehandlingType.REVURDERING);
        var kontrollbehandling = lagÅpenBehandling(vedtak, BehandlingType.KONTROLL);
        var svarTilKontroll = lagSvarPåEtterlysning(kontrollbehandling, "123");
        var svarTilRevurdering = lagSvarPåEtterlysning(revurdering, "124");

        tjeneste.mottaDokument(vedtak.getFagsak(), List.of(svarTilKontroll, svarTilRevurdering));

        verify(dokumentmottaker).lagreDokumentinnhold(List.of(svarTilKontroll), kontrollbehandling);
        verify(dokumentmottaker).lagreDokumentinnhold(List.of(svarTilRevurdering), revurdering);
    }

    @Test
    public void skal_sende_søknad_til_ordinær_behandling_og_inntektsrapportering_til_åpen_kontrollbehandling() {
        var tjeneste = lagInnhentDokumentTjeneste(true);
        var vedtak = lagAvsluttetBehandlingMedVedtak();
        var kontrollbehandling = lagÅpenBehandling(vedtak, BehandlingType.KONTROLL);
        var revurdering = mockNyBehandling(vedtak, 10L);
        when(behandlingsoppretter.opprettRevurdering(vedtak, BehandlingÅrsakType.RE_RAPPORTERING_INNTEKT)).thenReturn(revurdering);
        var søknad = lagreSøknadDokument(vedtak.getFagsakId(), "100", null);
        var rapportering = DokumentmottakTestUtil.byggMottattDokument(vedtak.getFagsakId(), "", now(), "123", Brevkode.UNGDOMSYTELSE_INNTEKTRAPPORTERING);

        tjeneste.mottaDokument(vedtak.getFagsak(), List.of(søknad, rapportering));

        verify(dokumentmottaker).lagreDokumentinnhold(List.of(søknad), revurdering);
        verify(dokumentmottaker).lagreDokumentinnhold(List.of(rapportering), kontrollbehandling);
        verify(behandlingsoppretter, never()).opprettKontrollbehandling(any(), any());
    }

    @Test
    public void skal_vente_med_å_opprette_kontrollbehandling_når_siste_vedtak_er_under_iverksetting() {
        var tjeneste = lagInnhentDokumentTjeneste(true);
        var scenario = TestScenarioBuilder.builderMedSøknad();
        scenario.medBehandlingVedtak().medVedtakResultatType(VedtakResultatType.INNVILGET);
        var vedtak = scenario.lagre(repositoryProvider);
        Whitebox.setInternalState(vedtak, "status", BehandlingStatus.IVERKSETTER_VEDTAK);
        behandlingRepository.lagre(vedtak, behandlingRepository.taSkriveLås(vedtak));
        entityManager.flush();
        var rapportering = DokumentmottakTestUtil.byggMottattDokument(vedtak.getFagsakId(), "", now(), "123", Brevkode.UNGDOMSYTELSE_INNTEKTRAPPORTERING);

        assertThatThrownBy(() -> tjeneste.mottaDokument(vedtak.getFagsak(), List.of(rapportering)))
            .hasMessageContaining("K9-653312");
        verify(behandlingsoppretter, never()).opprettKontrollbehandling(any(), any());
        verify(dokumentmottaker, never()).lagreDokumentinnhold(any(), any());
    }

    private UngdomsytelseInnhentDokumentTjeneste lagInnhentDokumentTjeneste(boolean kontrollbehandlingPå) {
        var parallelleBehandlingerRepository = new ParallelleBehandlingerRepository(entityManager);
        var kontrollbehandlingRuting = new KontrollbehandlingRuting(behandlingRepository, parallelleBehandlingerRepository,
            new ProsessTriggereRepository(entityManager), new KontrollbehandlingBryter(kontrollbehandlingPå, kontrollbehandlingPå));
        var mottattDokumentRuting = new MottattDokumentRuting(kontrollbehandlingRuting, parallelleBehandlingerRepository,
            oppgaveBekreftelseParser, etterlysningRepository);
        return Mockito.spy(new UngdomsytelseInnhentDokumentTjeneste(
            new UnitTestLookupInstanceImpl<>(dokumentmottaker),
            behandlingsoppretter,
            repositoryProvider,
            behandlingProsesseringTjeneste,
            prosessTaskTjeneste,
            fagsakProsessTaskRepository,
            prosessTriggereRepository,
            mottatteDokumentRepository,
            søknadParser,
            mottattDokumentRuting,
            parallelleBehandlingerRepository));
    }

    private Behandling lagAvsluttetBehandlingMedVedtak() {
        var scenario = TestScenarioBuilder.builderMedSøknad();
        scenario.medBehandlingVedtak().medVedtakResultatType(VedtakResultatType.INNVILGET);
        var behandling = scenario.lagre(repositoryProvider);
        behandling.avsluttBehandling();
        behandlingRepository.lagre(behandling, behandlingRepository.taSkriveLås(behandling));
        return behandling;
    }

    private Behandling lagÅpenBehandling(Behandling vedtak, BehandlingType type) {
        var behandling = Behandling.fraTidligereBehandling(vedtak, type).build();
        behandlingRepository.lagre(behandling, behandlingRepository.taSkriveLås(behandling));
        entityManager.flush();
        return behandling;
    }

    private static Behandling mockNyBehandling(Behandling vedtak, long behandlingId) {
        var behandling = mock(Behandling.class);
        when(behandling.getId()).thenReturn(behandlingId);
        when(behandling.getFagsakId()).thenReturn(vedtak.getFagsakId());
        when(behandling.getFagsak()).thenReturn(vedtak.getFagsak());
        when(behandling.getAktørId()).thenReturn(vedtak.getAktørId());
        return behandling;
    }

    private MottattDokument lagSvarPåEtterlysning(Behandling eier, String journalpostId) {
        var svar = DokumentmottakTestUtil.byggMottattDokument(eier.getFagsakId(), "", now(), journalpostId, Brevkode.UNGDOMSYTELSE_VARSEL_UTTALELSE);
        var oppgaveReferanse = UUID.randomUUID();
        when(oppgaveBekreftelseParser.parseOppgaveBekreftelse(svar)).thenReturn(new OppgaveBekreftelse(
            new SøknadId("456"),
            Versjon.of("1"),
            ZonedDateTime.now(),
            new Søker(NorskIdentitetsnummer.of("12345678910")),
            new InntektBekreftelse(oppgaveReferanse, false, null)));
        var etterlysning = mock(Etterlysning.class);
        when(etterlysning.getBehandlingId()).thenReturn(eier.getId());
        when(etterlysningRepository.hentEtterlysningForEksternReferanse(oppgaveReferanse)).thenReturn(etterlysning);
        return svar;
    }

    private MottattDokument lagreSøknadDokument(Long fagsakId, String journalpostId, String søknadId) {
        var dokument = DokumentmottakTestUtil.byggMottattDokument(fagsakId, "{}", now(), journalpostId, Brevkode.UNGDOMSYTELSE_SOKNAD);
        mottatteDokumentRepository.lagre(dokument, DokumentStatus.BEHANDLER);
        var søknad = mock(Søknad.class);
        when(søknad.getSøknadId()).thenReturn(søknadId == null ? null : new SøknadId(søknadId));
        when(søknadParser.parseSøknad(dokument)).thenReturn(søknad);
        return dokument;
    }

}
