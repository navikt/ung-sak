package no.nav.ung.sak.behandling.prosessering.task;

import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.ung.kodeverk.behandling.BehandlingDel;
import no.nav.ung.kodeverk.produksjonsstyring.OrganisasjonsEnhet;
import no.nav.ung.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.ung.sak.behandlingskontroll.BehandlingskontrollTjeneste;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingLåsRepository;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.produksjonsstyring.behandlingenhet.BehandlendeEnhetTjeneste;
import no.nav.ung.sak.test.util.behandling.ungdomsprogramytelse.TestScenarioBuilder;
import no.nav.ung.sak.typer.AktørId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public class GjenopptaBehandlingTaskTest {

    private GjenopptaBehandlingTask task; // objektet vi tester

    private BehandlingRepository mockBehandlingRepository;
    private BehandlingskontrollTjeneste mockBehandlingskontrollTjeneste;
    private BehandlendeEnhetTjeneste mockEnhetsTjeneste;
    private OrganisasjonsEnhet organisasjonsEnhet = new OrganisasjonsEnhet("4802", "NAV Bærum");

    @BeforeEach
    public void setup() {
        mockBehandlingRepository = mock(BehandlingRepository.class);
        mockBehandlingskontrollTjeneste = mock(BehandlingskontrollTjeneste.class);
        mockEnhetsTjeneste = mock(BehandlendeEnhetTjeneste.class);

        task = new GjenopptaBehandlingTask(mockBehandlingRepository, Mockito.mock(BehandlingLåsRepository.class), mockBehandlingskontrollTjeneste, mockEnhetsTjeneste);
    }

    @Test
    public void skal_gjenoppta_behandling() {
        final Long behandlingId = 10L;

        var scenario = TestScenarioBuilder
            .builderMedSøknad()
            .medBehandlendeEnhet(organisasjonsEnhet.getEnhetId());
        Behandling behandling = scenario.lagMocked();
        when(mockBehandlingRepository.hentBehandling(Mockito.anyString())).thenReturn(behandling);
        when(mockEnhetsTjeneste.sjekkEnheterEtterEndring(any())).thenReturn(Map.of());

        ProsessTaskData prosessTaskData =  ProsessTaskData.forProsessTask(GjenopptaBehandlingTask.class);
        prosessTaskData.setBehandling(0L, behandlingId, AktørId.dummy().getId());

        task.doTask(prosessTaskData);
    }

    @Test
    public void skal_gjenoppta_behandling_bytteenhet() {
        final Long behandlingId = 10L;

        OrganisasjonsEnhet enhet = new OrganisasjonsEnhet("2103", "NAV Viken");
        BehandlingLås lås = mock(BehandlingLås.class);
        BehandlingskontrollKontekst kontekst = mock(BehandlingskontrollKontekst.class);
        var scenario = TestScenarioBuilder
            .builderMedSøknad();
        Behandling behandling = scenario.lagMocked();

        //behandling.setBehandlendeEnhet(organisasjonsEnhet);
        when(mockBehandlingRepository.hentBehandling(Mockito.anyString())).thenReturn(behandling);
        when(mockBehandlingRepository.lagre(any(Behandling.class), any())).thenReturn(0L);
        when(mockBehandlingskontrollTjeneste.initBehandlingskontroll(Mockito.anyLong())).thenReturn(kontekst);
        when(kontekst.getSkriveLås()).thenReturn(lås);
        when(mockEnhetsTjeneste.sjekkEnheterEtterEndring(any())).thenReturn(Map.of(BehandlingDel.NAV_KONTOR, enhet));

        ProsessTaskData prosessTaskData =  ProsessTaskData.forProsessTask(GjenopptaBehandlingTask.class);
        prosessTaskData.setBehandling(0L, behandlingId, "0");

        task.doTask(prosessTaskData);

        verify(mockEnhetsTjeneste).oppdaterBehandlendeEnhet(any(), eq(BehandlingDel.NAV_KONTOR), eq(enhet), any(), any());
    }
}

