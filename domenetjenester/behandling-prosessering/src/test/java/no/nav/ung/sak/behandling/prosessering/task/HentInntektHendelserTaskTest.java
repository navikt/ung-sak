package no.nav.ung.sak.behandling.prosessering.task;

import jakarta.inject.Inject;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskTjeneste;
import no.nav.ung.sak.behandling.FagsakTjeneste;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.db.util.CdiDbAwareTest;
import no.nav.ung.sak.domene.registerinnhenting.InntektAbonnentTjeneste;
import no.nav.ung.sak.typer.AktørId;
import no.nav.ung.sak.typer.Periode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@CdiDbAwareTest
class HentInntektHendelserTaskTest {

    @Inject
    private FagsakTjeneste fagsakTjeneste;

    @Inject
    private BehandlingRepository behandlingRepository;

    private final InntektAbonnentTjeneste inntektAbonnentTjeneste = mock(InntektAbonnentTjeneste.class);
    private final ProsessTaskTjeneste prosessTaskTjeneste = mock(ProsessTaskTjeneste.class);
    private HentInntektHendelserTask task;

    @BeforeEach
    void setUp() {
        task = new HentInntektHendelserTask(
            inntektAbonnentTjeneste,
            fagsakTjeneste,
            behandlingRepository,
            prosessTaskTjeneste,
            false,
            "PT1M"
        );
    }

    @Test
    void skal_hente_første_sekvensnummer_når_task_mangler_sekvensnummer_og_opprette_neste_task() {
        var prosessTaskData = ProsessTaskData.forProsessTask(HentInntektHendelserTask.class);
        when(inntektAbonnentTjeneste.hentFørsteSekvensnummer()).thenReturn(Optional.of(1000L));
        when(inntektAbonnentTjeneste.hentNyeInntektHendelser(1000L)).thenReturn(List.of());

        task.doTask(prosessTaskData);

        verify(inntektAbonnentTjeneste).hentFørsteSekvensnummer();
        verify(inntektAbonnentTjeneste).hentNyeInntektHendelser(1000L);

        ArgumentCaptor<ProsessTaskData> taskCaptor = ArgumentCaptor.forClass(ProsessTaskData.class);
        verify(prosessTaskTjeneste).lagre(taskCaptor.capture());

        ProsessTaskData nesteTask = taskCaptor.getValue();
        assertThat(nesteTask.getPropertyValue(HentInntektHendelserTask.SEKVENSNUMMER_KEY)).isEqualTo("1000");
        assertThat(nesteTask.getNesteKjøringEtter()).isNotNull();
    }

    @Test
    void skal_hente_nye_hendelser_basert_på_sekvensnummer_fra_task_og_opprette_neste_task_med_oppdatert_sekvensnummer() {
        var prosessTaskData = ProsessTaskData.forProsessTask(HentInntektHendelserTask.class);
        prosessTaskData.setProperty(HentInntektHendelserTask.SEKVENSNUMMER_KEY, "1000");

        var hendelse1 = new InntektAbonnentTjeneste.InntektHendelse(1000L, new AktørId("12345678901"), periode());
        var hendelse2 = new InntektAbonnentTjeneste.InntektHendelse(1005L, new AktørId("12345678902"), periode());
        var hendelse3 = new InntektAbonnentTjeneste.InntektHendelse(1003L, new AktørId("12345678903"), periode());

        when(inntektAbonnentTjeneste.hentNyeInntektHendelser(1000L))
            .thenReturn(List.of(hendelse1, hendelse2, hendelse3));

        task.doTask(prosessTaskData);

        verify(inntektAbonnentTjeneste, never()).hentFørsteSekvensnummer();
        verify(inntektAbonnentTjeneste).hentNyeInntektHendelser(1000L);

        ArgumentCaptor<ProsessTaskData> taskCaptor = ArgumentCaptor.forClass(ProsessTaskData.class);
        verify(prosessTaskTjeneste).lagre(taskCaptor.capture());

        ProsessTaskData nesteTask = taskCaptor.getValue();
        assertThat(nesteTask.getPropertyValue(HentInntektHendelserTask.SEKVENSNUMMER_KEY)).isEqualTo("1006");
        assertThat(nesteTask.getNesteKjøringEtter()).isNotNull();
    }

    @Test
    void skal_alltid_reschedule_seg_selv_selv_når_ingen_hendelser_returneres() {
        var prosessTaskData = ProsessTaskData.forProsessTask(HentInntektHendelserTask.class);
        prosessTaskData.setProperty(HentInntektHendelserTask.SEKVENSNUMMER_KEY, "1000");

        when(inntektAbonnentTjeneste.hentNyeInntektHendelser(1000L)).thenReturn(List.of());

        task.doTask(prosessTaskData);

        verify(inntektAbonnentTjeneste).hentNyeInntektHendelser(1000L);

        ArgumentCaptor<ProsessTaskData> taskCaptor = ArgumentCaptor.forClass(ProsessTaskData.class);
        verify(prosessTaskTjeneste).lagre(taskCaptor.capture());

        ProsessTaskData nesteTask = taskCaptor.getValue();
        assertThat(nesteTask.getPropertyValue(HentInntektHendelserTask.SEKVENSNUMMER_KEY)).isEqualTo("1000");
        assertThat(nesteTask.getNesteKjøringEtter()).isNotNull();
        assertThat(nesteTask.getNesteKjøringEtter()).isAfter(java.time.LocalDateTime.now());
    }

    private Periode periode() {
        return new Periode(LocalDate.now(), LocalDate.now().plusMonths(1));
    }
}

