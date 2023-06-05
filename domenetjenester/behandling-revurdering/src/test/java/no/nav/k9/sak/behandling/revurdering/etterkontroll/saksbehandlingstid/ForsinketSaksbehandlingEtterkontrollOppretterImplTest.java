package no.nav.k9.sak.behandling.revurdering.etterkontroll.saksbehandlingstid;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import no.nav.k9.sak.behandling.revurdering.etterkontroll.Etterkontroll;
import no.nav.k9.sak.behandling.revurdering.etterkontroll.EtterkontrollRepository;
import no.nav.k9.sak.behandling.revurdering.etterkontroll.KontrollType;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.test.util.UnitTestLookupInstanceImpl;
import no.nav.k9.sak.test.util.behandling.TestScenarioBuilder;

class ForsinketSaksbehandlingEtterkontrollOppretterImplTest {

    private final EtterkontrollRepository etterkontrollRepository = mock();
    private final BehandlingRepository behandlingRepository = mock();
    private final SaksbehandlingsfristUtleder fristUtleder = mock();

    private ForsinketSaksbehandlingEtterkontrollOppretter oppretter = new ForsinketSaksbehandlingEtterkontrollOppretterImpl(
        etterkontrollRepository,
        new UnitTestLookupInstanceImpl<>(fristUtleder),
        behandlingRepository
    );


    @Test
    void skal_opprette_etterkontroll_ved_ny_behandling() {
        LocalDate søknadsdato = LocalDate.now().minusMonths(1);
        var behandling = TestScenarioBuilder.builderMedSøknad().lagMocked();

        when(behandlingRepository.hentBehandling(behandling.getId())).thenReturn(behandling);
        when(fristUtleder.utledFrist(behandling)).thenReturn(søknadsdato.plusWeeks(7).atStartOfDay());

        var captor = ArgumentCaptor.forClass(Etterkontroll.class);
        when(etterkontrollRepository.lagre(captor.capture())).thenReturn(1L);


        oppretter.opprettEtterkontroll(behandling.getId());


        Etterkontroll etterkontroll = captor.getValue();
        assertThat(etterkontroll.getKontrollType()).isEqualTo(KontrollType.FORSINKET_SAKSBEHANDLINGSTID);
        assertThat(etterkontroll.getBehandlingId()).isEqualTo(behandling.getId());
        assertThat(etterkontroll.isBehandlet()).isFalse();
        assertThat(etterkontroll.getKontrollTidspunkt().toLocalDate())
            .isEqualTo(søknadsdato.plusWeeks(7));


    }

    //TODO
    @Test
    @Disabled
    void skal_ignorere_etterkontroll_hvis_frist_allerede_utløpt() {

    }

    //TODO
    @Test
    @Disabled
    void skal_ignorere_etterkontroll_hvis_frist_utløper_om_få_dager() {

    }





}
