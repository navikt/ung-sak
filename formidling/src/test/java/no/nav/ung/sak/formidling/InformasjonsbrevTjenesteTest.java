package no.nav.ung.sak.formidling;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.ung.kodeverk.behandling.BehandlingResultatType;
import no.nav.ung.kodeverk.behandling.BehandlingType;
import no.nav.ung.kodeverk.formidling.IdType;
import no.nav.ung.kodeverk.formidling.InformasjonsbrevMalType;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.db.util.JpaExtension;
import no.nav.ung.sak.kontrakt.formidling.informasjonsbrev.InformasjonsbrevMottakerDto;
import no.nav.ung.sak.kontrakt.formidling.informasjonsbrev.InformasjonsbrevValgDto;
import no.nav.ung.sak.test.util.UngTestRepositories;
import no.nav.ung.sak.test.util.behandling.TestScenarioBuilder;
import no.nav.ung.sak.test.util.behandling.UngTestScenario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(CdiAwareExtension.class)
@ExtendWith(JpaExtension.class)
class InformasjonsbrevTjenesteTest {

    @Inject
    private EntityManager entityManager;

    private UngTestRepositories ungTestRepositories;
    private InformasjonsbrevTjeneste informasjonsbrevTjeneste;


    @BeforeEach
    void setup() {
        ungTestRepositories = BrevTestUtils.lagAlleUngTestRepositories(entityManager);
        informasjonsbrevTjeneste = new InformasjonsbrevTjeneste();
    }

    @Test
    @DisplayName("Skal verifisere at innvilget BrevScenario gir InformasjonsbrevValg med generelt fritekst")
    void skal_få_generelt_fritekstbrev() {
        // Given
        LocalDate fom = LocalDate.of(2024, 12, 1);
        UngTestScenario scenario = BrevScenarioer.innvilget19år(fom);

        Behandling behandling = TestScenarioBuilder.builderMedSøknad()
            .medBehandlingType(BehandlingType.FØRSTEGANGSSØKNAD)
            .medUngTestGrunnlag(scenario)
            .buildOgLagreMedUng(ungTestRepositories);

        behandling.setBehandlingResultatType(BehandlingResultatType.INNVILGET);
        behandling.avsluttBehandling();

        // When
        List<InformasjonsbrevValgDto> informasjonsbrevValg = informasjonsbrevTjeneste.informasjonsbrevValg(behandling.getId());

        // Then
        assertThat(informasjonsbrevValg.size()).isEqualTo(1);
        InformasjonsbrevValgDto first = informasjonsbrevValg.getFirst();
        assertThat(first.malType()).isEqualTo(InformasjonsbrevMalType.GENERELT_FRITEKSTBREV);

        assertThat(first.mottakere()).isEqualTo(List.of(new InformasjonsbrevMottakerDto(
            behandling.getFagsak().getAktørId().getId(),
            IdType.AKTØRID, null))
        );

        assertThat(first.støtterFritekst()).isFalse();
        assertThat(first.støtterTittelOgFritekst()).isTrue();
        assertThat(first.støtterTredjepartsMottaker()).isFalse();
    }

}
