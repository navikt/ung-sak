package no.nav.foreldrepenger.behandlingsprosess.prosessering;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollTjeneste;
import no.nav.foreldrepenger.behandlingslager.aktør.OrganisasjonsEnhet;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingsprosess.prosessering.task.GjenopptaBehandlingTask;
import no.nav.foreldrepenger.domene.registerinnhenting.RegisterdataEndringshåndterer;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.produksjonsstyring.behandlingenhet.BehandlendeEnhetTjeneste;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.TestScenarioBuilder;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;

public class GjenopptaBehandlingTaskTest {

    private GjenopptaBehandlingTask task; // objektet vi tester

    private BehandlingRepository mockBehandlingRepository;
    private BehandlingskontrollTjeneste mockBehandlingskontrollTjeneste;
    private RegisterdataEndringshåndterer mockRegisterdataEndringshåndterer;
    private BehandlendeEnhetTjeneste mockEnhetsTjeneste;
    private OrganisasjonsEnhet organisasjonsEnhet = new OrganisasjonsEnhet("4802", "NAV Bærum");

    @Before
    public void setup() {
        mockBehandlingRepository = mock(BehandlingRepository.class);
        mockBehandlingskontrollTjeneste = mock(BehandlingskontrollTjeneste.class);
        mockRegisterdataEndringshåndterer = mock(RegisterdataEndringshåndterer.class);
        mockEnhetsTjeneste = mock(BehandlendeEnhetTjeneste.class);

        task = new GjenopptaBehandlingTask(mockBehandlingRepository, mockBehandlingskontrollTjeneste, mockRegisterdataEndringshåndterer, mockEnhetsTjeneste);
    }

    @Test
    public void skal_gjenoppta_behandling() {
        final Long behandlingId = 10L;

        var scenario = TestScenarioBuilder
            .builderMedSøknad();
        Behandling behandling = scenario.lagMocked();
        behandling.setBehandlendeEnhet(organisasjonsEnhet);
        when(mockBehandlingRepository.hentBehandling(any(Long.class))).thenReturn(behandling);
        when(mockEnhetsTjeneste.sjekkEnhetVedGjenopptak(any())).thenReturn(Optional.empty());

        ProsessTaskData prosessTaskData = new ProsessTaskData(GjenopptaBehandlingTask.TASKTYPE);
        prosessTaskData.setBehandling(0L, behandlingId, AktørId.dummy().getId());

        task.doTask(prosessTaskData);

        verify(mockBehandlingskontrollTjeneste).initBehandlingskontroll(Mockito.anyLong());
        verify(mockBehandlingskontrollTjeneste).prosesserBehandling(any());
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
        when(mockBehandlingRepository.hentBehandling(any(Long.class))).thenReturn(behandling);
        when(mockBehandlingRepository.lagre(any(Behandling.class), any())).thenReturn(0L);
        when(mockBehandlingskontrollTjeneste.initBehandlingskontroll(Mockito.anyLong())).thenReturn(kontekst);
        when(kontekst.getSkriveLås()).thenReturn(lås);
        when(mockEnhetsTjeneste.sjekkEnhetVedGjenopptak(any())).thenReturn(Optional.of(enhet));

        ProsessTaskData prosessTaskData = new ProsessTaskData(GjenopptaBehandlingTask.TASKTYPE);
        prosessTaskData.setBehandling(0L, behandlingId, "0");

        task.doTask(prosessTaskData);

        verify(mockBehandlingskontrollTjeneste).initBehandlingskontroll(Mockito.anyLong());
        verify(mockBehandlingskontrollTjeneste).prosesserBehandling(any());
        ArgumentCaptor<OrganisasjonsEnhet> enhetArgumentCaptor = ArgumentCaptor.forClass(OrganisasjonsEnhet.class);
        verify(mockEnhetsTjeneste).oppdaterBehandlendeEnhet(any(), enhetArgumentCaptor.capture(), any(), any());
        assertThat(enhetArgumentCaptor.getValue()).isEqualTo(enhet);
    }
}

