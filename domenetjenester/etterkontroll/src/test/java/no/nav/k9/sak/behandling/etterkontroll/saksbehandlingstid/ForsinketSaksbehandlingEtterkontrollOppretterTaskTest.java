package no.nav.k9.sak.behandling.etterkontroll.saksbehandlingstid;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.sak.behandling.etterkontroll.Etterkontroll;
import no.nav.k9.sak.behandling.etterkontroll.EtterkontrollRepository;
import no.nav.k9.sak.behandling.etterkontroll.KontrollType;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.test.util.UnitTestLookupInstanceImpl;
import no.nav.k9.sak.test.util.behandling.TestScenarioBuilder;

class ForsinketSaksbehandlingEtterkontrollOppretterTaskTest {

    private final EtterkontrollRepository etterkontrollRepository = mock();
    private final BehandlingRepository behandlingRepository = mock();
    private final SaksbehandlingsfristUtleder fristUtleder = mock();




    @Test
    void skal_opprette_etterkontroll_ved_ny_behandling() {
        var behandling = TestScenarioBuilder.builderMedSøknad().lagMocked();
        var frist = LocalDate.now().minusMonths(1).plusWeeks(7);

        when(behandlingRepository.hentBehandling(behandling.getId().toString())).thenReturn(behandling);
        when(fristUtleder.utledFrist(behandling)).thenReturn(Optional.of(frist.atStartOfDay()));

        var captor = ArgumentCaptor.forClass(Etterkontroll.class);
        when(etterkontrollRepository.lagre(captor.capture())).thenReturn(1L);
        var oppretter = lagOppretter(null);

        oppretter.doTask(lagTask(behandling));

        Etterkontroll etterkontroll = captor.getValue();
        assertThat(etterkontroll.getKontrollType()).isEqualTo(KontrollType.FORSINKET_SAKSBEHANDLINGSTID);
        assertThat(etterkontroll.getBehandlingId()).isEqualTo(behandling.getId());
        assertThat(etterkontroll.isBehandlet()).isFalse();
        assertThat(etterkontroll.getKontrollTidspunkt().toLocalDate())
            .isEqualTo(frist);


    }

    @Test
    void skal_opprette_etterkontroll_X_dager_etter_i_dag_hvis_frist_allerede_er_utgått() {
        var behandling = TestScenarioBuilder.builderMedSøknad().lagMocked();
        var now = LocalDateTime.now();
        var frist1 = now.minusMonths(1);

        when(behandlingRepository.hentBehandling(behandling.getId().toString())).thenReturn(behandling);
        when(fristUtleder.utledFrist(behandling)).thenReturn(Optional.of(frist1));

        var captor = ArgumentCaptor.forClass(Etterkontroll.class);
        when(etterkontrollRepository.lagre(captor.capture())).thenReturn(1L);
        var oppretter = lagOppretter("P3D");

        oppretter.doTask(lagTask(behandling));

        var etterkontroll = captor.getValue();
        assertThat(etterkontroll.getKontrollTidspunkt().toLocalDate())
            .isEqualTo(now.toLocalDate().plusDays(3));

        //grensetilfelle dagens dato
        var frist2 = now;
        when(fristUtleder.utledFrist(behandling)).thenReturn(Optional.of(frist2));

        oppretter.doTask(lagTask(behandling));

        etterkontroll = captor.getValue();
        assertThat(etterkontroll.getKontrollTidspunkt().toLocalDate())
            .isEqualTo(now.toLocalDate().plusDays(3));

    }

    @Test
    void skal_opprette_etterkontroll_på_frist_hvis_frist_ikke_gått_ut() {
        var behandling = TestScenarioBuilder.builderMedSøknad().lagMocked();
        var now = LocalDateTime.now();
        var frist = now.plusDays(1);

        when(behandlingRepository.hentBehandling(behandling.getId().toString())).thenReturn(behandling);
        when(fristUtleder.utledFrist(behandling)).thenReturn(Optional.of(frist));

        var captor = ArgumentCaptor.forClass(Etterkontroll.class);
        when(etterkontrollRepository.lagre(captor.capture())).thenReturn(1L);

        lagOppretter("P3D").doTask(lagTask(behandling));

        var etterkontroll = captor.getValue();
        assertThat(etterkontroll.getKontrollTidspunkt().toLocalDate())
            .isEqualTo(frist.toLocalDate());

    }

    private ForsinketSaksbehandlingEtterkontrollOppretterTask lagOppretter(String ventetid) {
        return new ForsinketSaksbehandlingEtterkontrollOppretterTask(
            etterkontrollRepository,
            new UnitTestLookupInstanceImpl<>(fristUtleder),
            behandlingRepository,
            ventetid != null ? Period.parse(ventetid) : Period.ofDays(0)
        );
    }

    @Test
    void skal_ikke_opprette_etterkontroll_hvis_frist_mangler() {
        var behandling = TestScenarioBuilder.builderMedSøknad().lagMocked();

        when(behandlingRepository.hentBehandling(behandling.getId().toString())).thenReturn(behandling);
        when(fristUtleder.utledFrist(behandling)).thenReturn(Optional.empty());

        lagOppretter(null).doTask(lagTask(behandling));

        verifyNoInteractions(etterkontrollRepository);


    }

    private static ProsessTaskData lagTask(Behandling behandling) {
        ProsessTaskData prosessTaskData = ProsessTaskData.forProsessTask(ForsinketSaksbehandlingEtterkontrollOppretterTask.class);
        prosessTaskData.setBehandling(behandling.getFagsakId(), behandling.getId(), behandling.getAktørId().getAktørId());
        return prosessTaskData;
    }


}
