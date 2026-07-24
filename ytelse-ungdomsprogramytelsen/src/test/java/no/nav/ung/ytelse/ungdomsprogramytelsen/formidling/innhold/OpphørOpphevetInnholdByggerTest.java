package no.nav.ung.ytelse.ungdomsprogramytelsen.formidling.innhold;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.ung.kodeverk.formidling.TemplateType;
import no.nav.ung.sak.db.util.JpaExtension;
import no.nav.ung.sak.test.util.behandling.ungdomsprogramytelse.TestScenarioBuilder;
import no.nav.ung.sak.test.util.behandling.ungdomsprogramytelse.UngTestRepositories;
import no.nav.ung.sak.test.util.behandling.ungdomsprogramytelse.UngTestScenario;
import no.nav.ung.ytelse.ungdomsprogramytelsen.formidling.BrevTestUtils;
import no.nav.ung.ytelse.ungdomsprogramytelsen.formidling.dto.OpphørOpphevetDto;
import no.nav.ung.ytelse.ungdomsprogramytelsen.formidling.scenarioer.EndringProgramPeriodeScenarioer;
import no.nav.ung.ytelse.ungdomsprogramytelsen.formidling.scenarioer.FørstegangsbehandlingScenarioer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(CdiAwareExtension.class)
@ExtendWith(JpaExtension.class)
class OpphørOpphevetInnholdByggerTest {

    private static final LocalDate FOM = LocalDate.of(2026, 1, 1);
    private static final LocalDate TIDLIGERE_OPPHØRSDATO = LocalDate.of(2026, 10, 15);
    private static final LocalDate MAKSDATO = LocalDate.of(2027, 3, 1);

    @Inject
    private EntityManager entityManager;

    private UngTestRepositories ungTestRepositories;
    private OpphørOpphevetInnholdBygger bygger;

    @BeforeEach
    void setUp() {
        ungTestRepositories = BrevTestUtils.lagAlleUngTestRepositories(entityManager);
        bygger = new OpphørOpphevetInnholdBygger(
            ungTestRepositories.ungdomsprogramPeriodeRepository(),
            ungTestRepositories.prosessTriggereRepository());
    }

    @Test
    void skal_utlede_tidligere_sluttdato_fra_triggerperiode_selv_når_opphør_og_opphevelse_er_slått_sammen_på_samme_behandling() {
        // Simulerer sammenslåing: forrige behandling har IKKE fått persistert det opprinnelige opphøret
        // (dvs. periodegrunnlaget på forrige behandling ville gitt feil svar), men triggerperioden på
        // DENNE behandlingen reflekterer korrekt hvilken opphørsdato som oppheves.
        UngTestScenario scenario = EndringProgramPeriodeScenarioer.opphevingAvOpphør(FOM, TIDLIGERE_OPPHØRSDATO, MAKSDATO);
        var behandling = TestScenarioBuilder.builderMedSøknad()
            .medUngTestGrunnlag(scenario)
            .buildOgLagreMedUng(ungTestRepositories);

        var resultat = bygger.bygg(behandling, LocalDateTimeline.empty());

        assertThat(resultat.templateType()).isEqualTo(TemplateType.OPPHOR_OPPHEVET);
        var dto = (OpphørOpphevetDto) resultat.templateInnholdDto();
        assertThat(dto.tidligereSluttdato()).isEqualTo(TIDLIGERE_OPPHØRSDATO);
        assertThat(dto.maksdato()).isEqualTo(MAKSDATO);
    }

    @Test
    void skal_kaste_feil_dersom_prosesstrigger_for_opphevelse_mangler() {
        // Vanlig førstegangsbehandling mangler opphevelse-triggeren byggeren trenger.
        UngTestScenario scenario = FørstegangsbehandlingScenarioer.innvilget19år(FOM);
        var behandling = TestScenarioBuilder.builderMedSøknad()
            .medUngTestGrunnlag(scenario)
            .buildOgLagreMedUng(ungTestRepositories);

        assertThatThrownBy(() -> bygger.bygg(behandling, LocalDateTimeline.empty()))
            .isInstanceOf(IllegalStateException.class);
    }

}
