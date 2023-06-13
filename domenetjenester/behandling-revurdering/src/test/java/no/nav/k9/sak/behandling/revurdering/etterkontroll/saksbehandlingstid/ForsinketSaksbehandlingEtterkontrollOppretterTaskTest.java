package no.nav.k9.sak.behandling.revurdering.etterkontroll.saksbehandlingstid;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.sak.behandling.revurdering.etterkontroll.Etterkontroll;
import no.nav.k9.sak.behandling.revurdering.etterkontroll.EtterkontrollRepository;
import no.nav.k9.sak.behandling.revurdering.etterkontroll.KontrollType;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.test.util.UnitTestLookupInstanceImpl;
import no.nav.k9.sak.test.util.behandling.TestScenarioBuilder;

class ForsinketSaksbehandlingEtterkontrollOppretterTaskTest {

    private final EtterkontrollRepository etterkontrollRepository = mock();
    private final BehandlingRepository behandlingRepository = mock();
    private final SaksbehandlingsfristUtleder fristUtleder = mock();

    private final ForsinketSaksbehandlingEtterkontrollOppretterTask oppretter = new ForsinketSaksbehandlingEtterkontrollOppretterTask(
        etterkontrollRepository,
        new UnitTestLookupInstanceImpl<>(fristUtleder),
        behandlingRepository
    );


    @Test
    void skal_opprette_etterkontroll_ved_ny_behandling() {
        LocalDate søknadsdato = LocalDate.now().minusMonths(1);
        var behandling = TestScenarioBuilder.builderMedSøknad().lagMocked();

        when(behandlingRepository.hentBehandling(behandling.getId().toString())).thenReturn(behandling);
        when(fristUtleder.utledFrist(behandling)).thenReturn(Optional.of(søknadsdato.plusWeeks(7).atStartOfDay()));

        var captor = ArgumentCaptor.forClass(Etterkontroll.class);
        when(etterkontrollRepository.lagre(captor.capture())).thenReturn(1L);

        oppretter.doTask(lagTask(behandling));

        Etterkontroll etterkontroll = captor.getValue();
        assertThat(etterkontroll.getKontrollType()).isEqualTo(KontrollType.FORSINKET_SAKSBEHANDLINGSTID);
        assertThat(etterkontroll.getBehandlingId()).isEqualTo(behandling.getId());
        assertThat(etterkontroll.isBehandlet()).isFalse();
        assertThat(etterkontroll.getKontrollTidspunkt().toLocalDate())
            .isEqualTo(søknadsdato.plusWeeks(7));


    }

    @Test
    void skal_ikke_opprette_etterkontroll_hvis_frist_mangler() {
        var behandling = TestScenarioBuilder.builderMedSøknad().lagMocked();

        when(behandlingRepository.hentBehandling(behandling.getId().toString())).thenReturn(behandling);
        when(fristUtleder.utledFrist(behandling)).thenReturn(Optional.empty());

        oppretter.doTask(lagTask(behandling));

        verifyNoInteractions(etterkontrollRepository);


    }

    private static ProsessTaskData lagTask(Behandling behandling) {
        ProsessTaskData prosessTaskData = ProsessTaskData.forProsessTask(ForsinketSaksbehandlingEtterkontrollOppretterTask.class);
        prosessTaskData.setBehandling(behandling.getFagsakId(), behandling.getId(), behandling.getAktørId().getAktørId());
        return prosessTaskData;
    }


}
