package no.nav.k9.sak.behandling.revurdering.etterkontroll.saksbehandlingstid;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import no.nav.k9.kodeverk.behandling.BehandlingType;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.søknad.SøknadEntitet;
import no.nav.k9.sak.behandlingslager.behandling.søknad.SøknadRepository;
import no.nav.k9.sak.test.util.behandling.TestScenarioBuilder;

public class PsbSaksbehandlingsfristUtlederTest {

    private final SøknadRepository søknadRepository = mock();

    @Test
    void skal_utlede_frist() {
        var søknadsdato = LocalDateTime.now().toLocalDate();
        TestScenarioBuilder testScenarioBuilder = lagScenario(BehandlingType.FØRSTEGANGSSØKNAD, søknadsdato);
        Behandling behandling = testScenarioBuilder.lagMocked();
        SøknadEntitet søknadEntitet = testScenarioBuilder.medSøknad().build();

        when(søknadRepository.hentSøknadHvisEksisterer(behandling.getId())).thenReturn(Optional.of(søknadEntitet));

        var fristEnUke = lagFristUtleder("P1W").utledFrist(behandling);
        assertThat(fristEnUke).get().isEqualTo(søknadsdato.plusWeeks(1).atStartOfDay());

        var frist0Uker = lagFristUtleder("P0W").utledFrist(behandling);
        assertThat(frist0Uker).get().isEqualTo(søknadsdato.atStartOfDay());
    }

    @Test
    void skal_ikke_utlede_frist_for_revurderinger() {
        var søknadsdato = LocalDateTime.now().toLocalDate();
        TestScenarioBuilder testScenarioBuilder = lagScenario(BehandlingType.REVURDERING, søknadsdato);
        Behandling behandling = testScenarioBuilder.lagMocked();

        var fristEnUke = lagFristUtleder("P1W").utledFrist(behandling);
        assertThat(fristEnUke).isEmpty();

    }

    @Test
    void skal_ikke_utlede_frist_for_automatiske_utenlandssaker() {
        var søknadsdato = LocalDateTime.now().toLocalDate();
        TestScenarioBuilder builder = lagScenario(BehandlingType.FØRSTEGANGSSØKNAD, søknadsdato);

        builder.leggTilAksjonspunkt(
            AksjonspunktDefinisjon.AUTOMATISK_MARKERING_AV_UTENLANDSSAK,
            AksjonspunktDefinisjon.AUTOMATISK_MARKERING_AV_UTENLANDSSAK.getBehandlingSteg());

        Behandling behandling = builder.lagMocked();

        var fristEnUke = lagFristUtleder("P1W").utledFrist(behandling);
        assertThat(fristEnUke).isEmpty();

    }

    @Test
    void skal_ikke_utlede_frist_for_manuelle_utenlandssaker() {
        var søknadsdato = LocalDateTime.now().toLocalDate();
        TestScenarioBuilder builder = lagScenario(BehandlingType.FØRSTEGANGSSØKNAD, søknadsdato);

        builder.leggTilAksjonspunkt(
            AksjonspunktDefinisjon.MANUELL_MARKERING_AV_UTLAND_SAKSTYPE,
            AksjonspunktDefinisjon.MANUELL_MARKERING_AV_UTLAND_SAKSTYPE.getBehandlingSteg());

        Behandling behandling = builder.lagMocked();

        var fristEnUke = lagFristUtleder("P1W").utledFrist(behandling);
        assertThat(fristEnUke).isEmpty();

    }

    private static TestScenarioBuilder lagScenario(BehandlingType førstegangssøknad, LocalDate søknadsdato) {
        return TestScenarioBuilder
            .builderMedSøknad()
            .medBehandlingType(førstegangssøknad)
            .medSøknadDato(søknadsdato);
    }


    private PsbSaksbehandlingsfristUtleder lagFristUtleder(String periode) {
        return new PsbSaksbehandlingsfristUtleder(søknadRepository, periode);
    }
}
