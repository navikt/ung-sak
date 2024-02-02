package no.nav.k9.sak.behandling.hendelse.innsyn;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.fasterxml.jackson.core.JsonProcessingException;

import jakarta.enterprise.inject.Instance;
import no.nav.k9.innsyn.sak.Aksjonspunkt;
import no.nav.k9.innsyn.sak.Behandling;
import no.nav.k9.innsyn.sak.Fagsak;
import no.nav.k9.innsyn.sak.SøknadInfo;
import no.nav.k9.innsyn.sak.SøknadStatus;
import no.nav.k9.kodeverk.behandling.BehandlingStatus;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.k9.prosesstask.api.ProsessTaskTjeneste;
import no.nav.k9.sak.behandling.saksbehandlingstid.SaksbehandlingsfristUtleder;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.k9.sak.behandlingskontroll.events.BehandlingStatusEvent;
import no.nav.k9.sak.behandlingslager.behandling.motattdokument.MottatteDokumentRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.k9.sak.domene.typer.tid.JsonObjectMapper;
import no.nav.k9.sak.domene.typer.tid.JsonObjectMapperKodeverdiSerializer;
import no.nav.k9.sak.test.util.UnitTestLookupInstanceImpl;
import no.nav.k9.sak.test.util.behandling.TestScenarioBuilder;
import no.nav.k9.sak.typer.AktørId;

class InnsynEventObserverTest {

    private final ProsessTaskTjeneste prosessTaskTjeneste = mock();
    private final SaksbehandlingsfristUtleder fristUtleder = mock();
    private final Instance<SaksbehandlingsfristUtleder> utledere = new UnitTestLookupInstanceImpl<>(fristUtleder);
    private final BrukerdialoginnsynMeldingProducer producer = mock();
    private final MottatteDokumentRepository mottatteDokumentRepository = mock();


    @Test
    void nyBehandlingEvent() {
        var mor = AktørId.dummy();
        var pleietrengende = AktørId.dummy();

        TestScenarioBuilder testScenarioBuilder = TestScenarioBuilder
                .builderMedSøknad(FagsakYtelseType.PLEIEPENGER_SYKT_BARN, mor)
                .medPleietrengende(pleietrengende);

//        testScenarioBuilder.leggTilAksjonspunkt(AksjonspunktDefinisjon.AUTO_MANUELT_SATT_PÅ_VENT);

        var behandling = testScenarioBuilder.lagMocked();
        BehandlingskontrollKontekst kontekst = new BehandlingskontrollKontekst(behandling.getFagsakId(), behandling.getAktørId(), new BehandlingLås(behandling.getId()));
        var event = BehandlingStatusEvent.nyEvent(kontekst, BehandlingStatus.UTREDES, BehandlingStatus.OPPRETTET);

        var observer = new InnsynEventObserver(prosessTaskTjeneste, testScenarioBuilder.mockBehandlingRepository(),
                utledere, producer, mottatteDokumentRepository);

        observer.observerBehandlingStartet(event);

        var captor = ArgumentCaptor.forClass(String.class);
        verify(producer).send(anyString(), captor.capture());

        String json = captor.getValue();
        var b = JsonObjectMapper.fromJson(json, Behandling.class);

        Fagsak sak = b.fagsak();
        assertThat(sak.saksnummer().getVerdi()).isEqualTo(behandling.getFagsak().getSaksnummer().getVerdi());
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
            assertThat(it.mottattTidspunkt()).isEqualTo(ZonedDateTime.now());
            assertThat(it.status()).isEqualTo(SøknadStatus.MOTTATT);
            assertThat(it.søknadId()).isEqualTo("");
        });


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

