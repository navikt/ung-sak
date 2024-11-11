package no.nav.ung.sak.behandling.prosessering.task;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import no.nav.k9.kodeverk.produksjonsstyring.OrganisasjonsEnhet;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.ung.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.ung.sak.behandlingskontroll.BehandlingskontrollTjeneste;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingLåsRepository;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.produksjonsstyring.behandlingenhet.BehandlendeEnhetTjeneste;
import no.nav.ung.sak.test.util.behandling.TestScenarioBuilder;
import no.nav.ung.sak.typer.AktørId;

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
            .builderMedSøknad();
        Behandling behandling = scenario.lagMocked();
        behandling.setBehandlendeEnhet(organisasjonsEnhet);
        when(mockBehandlingRepository.hentBehandling(Mockito.anyString())).thenReturn(behandling);
        when(mockEnhetsTjeneste.sjekkEnhetEtterEndring(any())).thenReturn(Optional.empty());

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

        behandling.setBehandlendeEnhet(organisasjonsEnhet);
        when(mockBehandlingRepository.hentBehandling(Mockito.anyString())).thenReturn(behandling);
        when(mockBehandlingRepository.lagre(any(Behandling.class), any())).thenReturn(0L);
        when(mockBehandlingskontrollTjeneste.initBehandlingskontroll(Mockito.anyLong())).thenReturn(kontekst);
        when(kontekst.getSkriveLås()).thenReturn(lås);
        when(mockEnhetsTjeneste.sjekkEnhetEtterEndring(any())).thenReturn(Optional.of(enhet));

        ProsessTaskData prosessTaskData =  ProsessTaskData.forProsessTask(GjenopptaBehandlingTask.class);
        prosessTaskData.setBehandling(0L, behandlingId, "0");

        task.doTask(prosessTaskData);

        ArgumentCaptor<OrganisasjonsEnhet> enhetArgumentCaptor = ArgumentCaptor.forClass(OrganisasjonsEnhet.class);
        verify(mockEnhetsTjeneste).oppdaterBehandlendeEnhet(any(), enhetArgumentCaptor.capture(), any(), any());
        assertThat(enhetArgumentCaptor.getValue()).isEqualTo(enhet);
    }
}

