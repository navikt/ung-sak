package no.nav.ung.sak.domene.vedtak.observer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.ung.kodeverk.vedtak.IverksettingStatus;
import no.nav.ung.kodeverk.vedtak.VedtakResultatType;
import no.nav.k9.prosesstask.api.ProsessTaskGruppe;
import no.nav.k9.prosesstask.api.ProsessTaskTjeneste;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.ung.sak.behandlingslager.behandling.vedtak.BehandlingVedtakEvent;
import no.nav.ung.sak.behandlingslager.behandling.vedtak.BehandlingVedtakRepository;
import no.nav.ung.sak.db.util.JpaExtension;
import no.nav.ung.sak.typer.AktørId;

@ExtendWith(CdiAwareExtension.class)
@ExtendWith(JpaExtension.class)
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class VedtakFattetEventObserverTest {

    @Mock
    private ProsessTaskTjeneste prosessTaskRepository;

    @Mock
    private BehandlingRepository behandlingRepository;

    @Mock
    private BehandlingVedtakRepository vedtakRepository;

    @Captor
    ArgumentCaptor<ProsessTaskGruppe> prosessTaskGruppeCaptorCaptor;

    VedtakFattetEventObserver vedtakFattetEventObserver;

    @BeforeEach
    public void setup() {
        vedtakFattetEventObserver = new VedtakFattetEventObserver(prosessTaskRepository);
    }

    @Test
    public void publisererVedtakForIverksatteVedtak() {
        var behandlingVedtakEvent = lagVedtakEvent(IverksettingStatus.IVERKSATT, VedtakResultatType.INNVILGET);
        vedtakFattetEventObserver.observerBehandlingVedtak(behandlingVedtakEvent);

        verify(prosessTaskRepository, times(1)).lagre(prosessTaskGruppeCaptorCaptor.capture());
        assertThat(prosessTaskGruppeCaptorCaptor.getAllValues().stream().map(ProsessTaskGruppe::getTasks)
            .flatMap(Collection::stream)
            .map(it -> it.getTask().getTaskType()))
            .containsExactlyInAnyOrder(PubliserVedtattYtelseHendelseTask.TASKTYPE, PubliserVedtakHendelseTask.TASKTYPE);
    }

    @Test
    public void publisererIkkeVedtakFørIverksatt() {
        var behandlingVedtakEvent = lagVedtakEvent(IverksettingStatus.IKKE_IVERKSATT, VedtakResultatType.INNVILGET);
        vedtakFattetEventObserver.observerBehandlingVedtak(behandlingVedtakEvent);

        verify(prosessTaskRepository, never()).lagre(any(ProsessTaskGruppe.class));
    }

    @Test
    public void publisererKunGenereltVedtakseventVedAvslag() {
        var behandlingVedtakEvent = lagVedtakEvent(IverksettingStatus.IVERKSATT, VedtakResultatType.AVSLAG);
        vedtakFattetEventObserver.observerBehandlingVedtak(behandlingVedtakEvent);

        verify(prosessTaskRepository, times(1)).lagre(prosessTaskGruppeCaptorCaptor.capture());
        assertThat(prosessTaskGruppeCaptorCaptor.getAllValues().stream().map(ProsessTaskGruppe::getTasks)
            .flatMap(Collection::stream)
            .map(it -> it.getTask().getTaskType()))
            .containsExactly(PubliserVedtakHendelseTask.TASKTYPE, PubliserVedtattYtelseHendelseTask.TASKTYPE);
    }

    private Behandling lagBehandling() {
        Behandling behandling = mock(Behandling.class);
        when(behandling.getId()).thenReturn(123L);
        when(behandling.getFagsakId()).thenReturn(123L);
        when(behandling.getAktørId()).thenReturn(AktørId.dummy());
        when(behandling.erYtelseBehandling()).thenReturn(true);

        when(behandlingRepository.hentBehandlingHvisFinnes(123L)).thenReturn(Optional.of(behandling));
        return behandling;
    }

    private BehandlingVedtakEvent lagVedtakEvent(IverksettingStatus status, VedtakResultatType vedtakResultatType) {
        var vedtak = BehandlingVedtak.builder(123L)
            .medVedtakstidspunkt(LocalDateTime.now())
            .medAnsvarligSaksbehandler("")
            .medIverksettingStatus(status)
            .medVedtakResultatType(vedtakResultatType)
            .build();

        when(vedtakRepository.hentBehandlingVedtakForBehandlingId(any())).thenReturn(Optional.of(vedtak));

        return new BehandlingVedtakEvent(vedtak, lagBehandling());
    }
}
