package no.nav.ung.sak.behandling.hendelse.produksjonsstyring;

import jakarta.enterprise.inject.Instance;
import no.nav.ung.kodeverk.behandling.BehandlingStegType;
import no.nav.ung.kodeverk.hendelse.EventHendelse;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingAnsvarligRepository;
import no.nav.ung.sak.søknadsfrist.VurderSøknadsfristTjeneste;
import no.nav.ung.sak.test.util.behandling.ungdomsprogramytelse.TestScenarioBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BehandlingProsessHendelseMapperTest {

    private static final LocalDateTime EVENT_TID = LocalDateTime.of(2026, 10, 30, 12, 0);

    @Mock
    private Instance<VurderSøknadsfristTjeneste<?>> søknadsfristTjenester;
    @Mock
    private BehandlingAnsvarligRepository behandlingAnsvarligRepository;

    private BehandlingProsessHendelseMapper mapper;
    private Behandling behandling;

    @BeforeEach
    void setUp() {
        when(søknadsfristTjenester.select(any(java.lang.annotation.Annotation[].class))).thenReturn(søknadsfristTjenester);
        mapper = new BehandlingProsessHendelseMapper(søknadsfristTjenester, behandlingAnsvarligRepository);
        behandling = spy(TestScenarioBuilder.builderMedSøknad().lagMocked());
    }

    @Test
    void brukerAktivtStegUtenEksplisittOverstyring() {
        when(behandling.getAktivtBehandlingSteg()).thenReturn(BehandlingStegType.FATTE_VEDTAK);

        var dto = mapper.getProduksjonstyringEventDto(EVENT_TID, EventHendelse.AVSLUTTE_NAVKONTOR, behandling, null);

        assertThat(dto.getBehandlingSteg()).isEqualTo(BehandlingStegType.FATTE_VEDTAK.getKode());
    }

    @Test
    void eksplisittStegOverstyrerAktivtStegUavhengigAvHendelse() {
        var dto = mapper.getProduksjonstyringEventDto(EVENT_TID, EventHendelse.BEHANDLINGSKONTROLL_EVENT, behandling, null,
            BehandlingStegType.NAV_KONTOR_BESLUTTER_VILKÅR);

        assertThat(dto.getBehandlingSteg()).isEqualTo(BehandlingStegType.NAV_KONTOR_BESLUTTER_VILKÅR.getKode());
    }

    @Test
    void tillaterManglendeAktivtSteg() {
        when(behandling.getAktivtBehandlingSteg()).thenReturn(null);

        var dto = mapper.getProduksjonstyringEventDto(EVENT_TID, EventHendelse.BEHANDLINGSKONTROLL_EVENT, behandling, null);

        assertThat(dto.getBehandlingSteg()).isNull();
    }
}
