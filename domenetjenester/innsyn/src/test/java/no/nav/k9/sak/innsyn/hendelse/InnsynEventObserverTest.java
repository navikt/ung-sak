package no.nav.k9.sak.innsyn.hendelse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;

import no.nav.k9.innsyn.InnsynHendelse;
import no.nav.k9.innsyn.sak.Behandling;
import no.nav.k9.innsyn.sak.Fagsak;
import no.nav.k9.innsyn.sak.SøknadInfo;
import no.nav.k9.innsyn.sak.SøknadStatus;
import no.nav.k9.kodeverk.behandling.BehandlingStatus;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.Venteårsak;
import no.nav.k9.kodeverk.dokument.Brevkode;
import no.nav.k9.kodeverk.dokument.DokumentStatus;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskTjeneste;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.k9.sak.behandlingskontroll.events.BehandlingStatusEvent;
import no.nav.k9.sak.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.k9.sak.behandlingslager.behandling.aksjonspunkt.AksjonspunktTestSupport;
import no.nav.k9.sak.behandlingslager.behandling.motattdokument.MottattDokument;
import no.nav.k9.sak.behandlingslager.behandling.motattdokument.MottatteDokumentRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.k9.sak.domene.person.personopplysning.UtlandVurdererTjeneste;
import no.nav.k9.sak.test.util.behandling.TestScenarioBuilder;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.JournalpostId;
import no.nav.k9.søknad.JsonUtils;

class InnsynEventObserverTest {
    private final ProsessTaskTjeneste prosessTaskTjeneste = mock();
    private final MottatteDokumentRepository mottatteDokumentRepository = mock();
    private final UtlandVurdererTjeneste utlandVurdererTjeneste = mock();
    private final AksjonspunktTestSupport aksjonspunktTestSupport = new AksjonspunktTestSupport();


    @BeforeEach
    void setup() {
        when(utlandVurdererTjeneste.erUtenlandssak(any())).thenReturn(false);
    }

    @Test
    void nyBehandlingEvent() {
        var mor = AktørId.dummy();
        var pleietrengende = AktørId.dummy();
        var now = LocalDateTime.now();
        var søknadJpId = "123";
        var venteFrist = now.plusWeeks(4);

        TestScenarioBuilder testScenarioBuilder = TestScenarioBuilder
            .builderMedSøknad(FagsakYtelseType.PLEIEPENGER_SYKT_BARN, mor)
            .medPleietrengende(pleietrengende);

        var behandling = testScenarioBuilder.lagMocked();
        Aksjonspunkt aksjonspunkt = aksjonspunktTestSupport.leggTilAksjonspunkt(behandling, AksjonspunktDefinisjon.KONTROLLER_LEGEERKLÆRING);
        aksjonspunktTestSupport.setFrist(aksjonspunkt,  venteFrist, Venteårsak.MEDISINSKE_OPPLYSNINGER, null);

        no.nav.k9.sak.behandlingslager.fagsak.Fagsak fagsak = behandling.getFagsak();

        //TODO egen test for mottatt, behandler og gyldig + ugyldig?
        when(mottatteDokumentRepository.hentMottatteDokumentForBehandling(
            anyLong(),
            anyLong(),
            anyList(),
            anyBoolean(),
            ArgumentMatchers.any(DokumentStatus[].class)))
            .thenReturn(List.of(byggMottattDokument(fagsak.getId(), behandling.getId(), now, søknadJpId, Brevkode.PLEIEPENGER_BARN_SOKNAD)));

        BehandlingskontrollKontekst kontekst = new BehandlingskontrollKontekst(behandling.getFagsakId(), behandling.getAktørId(), new BehandlingLås(behandling.getId()));
        var event = BehandlingStatusEvent.nyEvent(kontekst, BehandlingStatus.UTREDES, BehandlingStatus.OPPRETTET);


        var observer = new InnsynEventObserver(prosessTaskTjeneste,
            testScenarioBuilder.mockBehandlingRepository(),
            mottatteDokumentRepository,
            true,
            utlandVurdererTjeneste);

        observer.observerBehandlingStartet(event);

        var captor = ArgumentCaptor.forClass(ProsessTaskData.class);
        verify(prosessTaskTjeneste).lagre(captor.capture());

        ProsessTaskData pd = captor.getValue();
        assertThat(pd.getTaskType()).isEqualTo(PubliserInnsynEventTask.TASKTYPE);
        assertThat(pd.getSaksnummer()).isEqualTo(fagsak.getSaksnummer().getVerdi());

        String json = pd.getPayloadAsString();
        InnsynHendelse<Behandling> behandlingInnsynHendelse = JsonUtils.fromString(json, InnsynHendelse.class);
        assertThat(behandlingInnsynHendelse.getOppdateringstidspunkt()).isNotNull();

        var b = behandlingInnsynHendelse.getData();

        Fagsak sak = b.fagsak();
        assertThat(sak.saksnummer().getVerdi()).isEqualTo(fagsak.getSaksnummer().getVerdi());
        assertThat(sak.søkerAktørId()).isEqualTo(mor);
        assertThat(sak.ytelseType()).isEqualTo(FagsakYtelseType.PLEIEPENGER_SYKT_BARN);
        assertThat(sak.pleietrengendeAktørId()).isEqualTo(pleietrengende);

        assertThat(b.behandlingsId()).isEqualTo(behandling.getUuid());
        assertThat(b.erUtenlands()).isEqualTo(false);
        assertThat(b.status()).isEqualTo(no.nav.k9.innsyn.sak.BehandlingStatus.PÅ_VENT);
        assertThat(b.avsluttetTidspunkt()).isNull();
        assertThat(b.opprettetTidspunkt()).isEqualTo(behandling.getOpprettetDato().atZone(ZoneId.systemDefault()));

        var aksjonspunkter = b.aksjonspunkter();
        assertThat(aksjonspunkter).hasSize(1);
        assertThat(aksjonspunkter).allSatisfy(it -> {
            assertThat(it.tidsfrist()).isCloseTo(venteFrist.atZone(ZoneId.systemDefault()), within(1, ChronoUnit.MILLIS));
            assertThat(it.venteårsak()).isEqualTo(no.nav.k9.innsyn.sak.Aksjonspunkt.Venteårsak.MEDISINSK_DOKUMENTASJON);
        });

        Set<SøknadInfo> søknader = b.søknader();
        assertThat(søknader).hasSize(1);
        assertThat(søknader).allSatisfy(it -> {
            assertThat(it.mottattTidspunkt()).isCloseTo(now.atZone(ZoneId.systemDefault()), within(1, ChronoUnit.MILLIS));
            assertThat(it.status()).isEqualTo(SøknadStatus.MOTTATT);
            assertThat(it.journalpostId()).isEqualTo(søknadJpId);
        });


    }

    public static MottattDokument byggMottattDokument(Long fagsakId, Long behandlingId, LocalDateTime mottattDato, String journalpostId, Brevkode brevkode) {
        MottattDokument.Builder builder = new MottattDokument.Builder();
        builder.medMottattTidspunkt(mottattDato);
        builder.medType(brevkode);
        builder.medPayload("payload");
        builder.medFagsakId(fagsakId);
        if (journalpostId != null) {
            builder.medJournalPostId(new JournalpostId(journalpostId));
        }
        builder.medBehandlingId(behandlingId);
        return builder.build();
    }
}
