package no.nav.k9.sak.innsyn.hendelse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
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

import jakarta.enterprise.inject.Instance;
import no.nav.k9.innsyn.InnsynHendelse;
import no.nav.k9.innsyn.sak.Behandling;
import no.nav.k9.innsyn.sak.Fagsak;
import no.nav.k9.innsyn.sak.SøknadInfo;
import no.nav.k9.innsyn.sak.SøknadStatus;
import no.nav.k9.kodeverk.behandling.BehandlingStatus;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.dokument.Brevkode;
import no.nav.k9.kodeverk.dokument.DokumentStatus;
import no.nav.k9.prosesstask.api.ProsessTaskTjeneste;
import no.nav.k9.sak.behandling.saksbehandlingstid.SaksbehandlingsfristUtleder;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.k9.sak.behandlingskontroll.events.BehandlingStatusEvent;
import no.nav.k9.sak.behandlingslager.behandling.motattdokument.MottattDokument;
import no.nav.k9.sak.behandlingslager.behandling.motattdokument.MottatteDokumentRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.k9.sak.domene.person.personopplysning.UtlandVurdererTjeneste;
import no.nav.k9.sak.domene.typer.tid.JsonObjectMapper;
import no.nav.k9.sak.innsyn.BrukerdialoginnsynMeldingProducer;
import no.nav.k9.sak.test.util.UnitTestLookupInstanceImpl;
import no.nav.k9.sak.test.util.behandling.TestScenarioBuilder;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.JournalpostId;

class InnsynEventObserverTest {
    private final ProsessTaskTjeneste prosessTaskTjeneste = mock();
    private final SaksbehandlingsfristUtleder fristUtleder = mock();
    private final Instance<SaksbehandlingsfristUtleder> utledere = new UnitTestLookupInstanceImpl<>(fristUtleder);
    private final BrukerdialoginnsynMeldingProducer producer = mock();
    private final MottatteDokumentRepository mottatteDokumentRepository = mock();
    private UtlandVurdererTjeneste utlandVurdererTjeneste = mock();


    @BeforeEach
    void setup() {
        when(utlandVurdererTjeneste.erUtenlandssak(any())).thenReturn(false);
    }

    @Test
    void nyBehandlingEvent() {
        var mor = AktørId.dummy();
        var pleietrengende = AktørId.dummy();

        TestScenarioBuilder testScenarioBuilder = TestScenarioBuilder
            .builderMedSøknad(FagsakYtelseType.PLEIEPENGER_SYKT_BARN, mor)
            .medPleietrengende(pleietrengende);
        var behandling = testScenarioBuilder.lagMocked();
        no.nav.k9.sak.behandlingslager.fagsak.Fagsak fagsak = behandling.getFagsak();
        var now = LocalDateTime.now();
        var søknadJpId = "123";

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
            utledere,
            producer,
            mottatteDokumentRepository,
            true, utlandVurdererTjeneste);

        observer.observerBehandlingStartet(event);

        var captor = ArgumentCaptor.forClass(String.class);
        verify(producer).send(anyString(), captor.capture());

        String json = captor.getValue();
        InnsynHendelse<Behandling> behandlingInnsynHendelse = JsonObjectMapper.fromJson(json, InnsynHendelse.class);
        assertThat(behandlingInnsynHendelse.getOppdateringstidspunkt()).isNotNull();

        var b = behandlingInnsynHendelse.getData();

        Fagsak sak = b.fagsak();
        assertThat(sak.saksnummer().getVerdi()).isEqualTo(fagsak.getSaksnummer().getVerdi());
        assertThat(sak.søkerAktørId()).isEqualTo(mor);
        assertThat(sak.ytelseType()).isEqualTo(FagsakYtelseType.PLEIEPENGER_SYKT_BARN);
        assertThat(sak.pleietrengendeAktørId()).isEqualTo(pleietrengende);

        assertThat(b.behandlingsId()).isEqualTo(behandling.getUuid());
        assertThat(b.erUtenlands()).isEqualTo(false);
        assertThat(b.status()).isEqualTo(no.nav.k9.innsyn.sak.BehandlingStatus.UNDER_BEHANDLING);

        var aksjonspunkter = b.aksjonspunkter();
        assertThat(aksjonspunkter).isEmpty();

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


    @Test
    void test() {
    }
    // fanger behandling opprettet
    // lager prosesstask for å oppdatere innsyn med hva slags event som ble trigget?

    //event opprettet
    // prosesstask:
    // henter behandling
    // utleder frist
    // status = under behandling
    // event type = opprettet
    // lager prosesstask for å kafka

    //event: vent
    // prosesstask:
    // henter behandling
    // utleder frist
    // status = på vent + hvorfor
    // event type = på vent
    // lager prosesstask for å kafka

    //event: ut av vent
    // prosesstask:
    // henter behandling
    // utleder frist
    // status = under behandling
    // event type = gjennopptatt
    // lager prosesstask for å kafka

    //event: fattet vedtak
    // prosesstask:
    // henter behandling
    // utleder frist
    // status = avsluttet
    // event type = vedtak fattet
    // lager prosesstask for å kafka

    // Utenom event observer:
    //event: endret frist
    // henter behandlinger
    // utleder frist
    // status = fra behandling
    // event type = frist endret
    // lager prosesstask(er?) for å kafka

    //Utenom event observer:
    //event: migrering
    // henter behandlinger
    // utleder frist
    // status = fra behandling
    // event type = migrering
    // lager prosesstask(er?) for å kafka
}
