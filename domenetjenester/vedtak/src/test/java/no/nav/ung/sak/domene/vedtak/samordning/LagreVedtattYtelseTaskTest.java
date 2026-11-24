package no.nav.ung.sak.domene.vedtak.samordning;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.ung.kodeverk.behandling.BehandlingStatus;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.fagsak.Fagsak;
import no.nav.ung.sak.samordning.VedtakYtelse;
import no.nav.ung.sak.samordning.VedtakYtelseRepository;
import no.nav.ung.sak.typer.AktørId;
import no.nav.ung.sak.typer.Saksnummer;

class LagreVedtattYtelseTaskTest {

    private static final long FAGSAK_ID = 10L;
    private static final long BEHANDLING_ID = 20L;

    private final BehandlingRepository behandlingRepository = mock();
    private final VedtakYtelseUtleder utleder = mock();
    private final VedtakYtelseRepository vedtakYtelseRepository = mock();
    private final Behandling behandling = mock();

    private LagreVedtattYtelseTask task;

    @BeforeEach
    void setUp() {
        task = new LagreVedtattYtelseTask(behandlingRepository, utleder, vedtakYtelseRepository);

        Fagsak fagsak = mock();
        when(fagsak.getSaksnummer()).thenReturn(new Saksnummer("ABC123"));
        when(behandling.getFagsak()).thenReturn(fagsak);
        when(behandling.getFagsakYtelseType()).thenReturn(FagsakYtelseType.AKTIVITETSPENGER);
        when(behandling.getStatus()).thenReturn(BehandlingStatus.AVSLUTTET);
        when(behandlingRepository.hentBehandling(Long.toString(BEHANDLING_ID))).thenReturn(behandling);
    }

    @Test
    void skal_utlede_og_lagre_vedtaket_for_behandlingen() {
        VedtakYtelse vedtak = mock();
        when(utleder.utled(behandling)).thenReturn(vedtak);

        task.doTask(taskData());

        var rekkefølge = inOrder(utleder, vedtakYtelseRepository);
        rekkefølge.verify(utleder).utled(behandling);
        rekkefølge.verify(vedtakYtelseRepository).lagre(vedtak);
    }

    @Test
    void skal_ikke_lagre_noe_når_utledning_feiler() {
        when(utleder.utled(behandling)).thenThrow(new IllegalStateException("Fant ikke vedtak"));

        assertThatThrownBy(() -> task.doTask(taskData())).isInstanceOf(IllegalStateException.class);
        verifyNoInteractions(vedtakYtelseRepository);
    }

    private static ProsessTaskData taskData() {
        var taskData = ProsessTaskData.forProsessTask(LagreVedtattYtelseTask.class);
        taskData.setBehandling(FAGSAK_ID, BEHANDLING_ID, AktørId.dummy().getId());
        return taskData;
    }
}
