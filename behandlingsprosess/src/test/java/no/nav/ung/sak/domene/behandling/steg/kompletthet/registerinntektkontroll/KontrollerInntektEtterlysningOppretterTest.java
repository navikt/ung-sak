package no.nav.ung.sak.domene.behandling.steg.kompletthet.registerinntektkontroll;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.k9.prosesstask.api.ProsessTaskTjeneste;
import no.nav.ung.sak.behandling.BehandlingReferanse;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.motattdokument.MottatteDokumentRepository;
import no.nav.ung.sak.behandlingslager.behandling.sporing.BehandingprosessSporingRepository;
import no.nav.ung.sak.behandlingslager.etterlysning.EtterlysningRepository;
import no.nav.ung.sak.db.util.JpaExtension;
import no.nav.ung.sak.domene.behandling.steg.registerinntektkontroll.KontrollerInntektInput;
import no.nav.ung.sak.domene.behandling.steg.registerinntektkontroll.KontrollerInntektInputMapper;
import no.nav.ung.sak.domene.iay.modell.InntektArbeidYtelseTjeneste;
import no.nav.ung.sak.etterlysning.EtterlysningTjeneste;
import no.nav.ung.sak.test.util.behandling.TestScenarioBuilder;
import no.nav.ung.sak.ytelse.InntektType;
import no.nav.ung.sak.ytelse.RapportertInntekt;
import no.nav.ung.sak.ytelse.RapporterteInntekter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(JpaExtension.class)
@ExtendWith(CdiAwareExtension.class)
class KontrollerInntektEtterlysningOppretterTest {

    @Inject
    private EntityManager entityManager;

    @Inject
    private EtterlysningRepository etterlysningRepository;
    @Inject
    private BehandingprosessSporingRepository sporingRepository;
    @Inject
    private MottatteDokumentRepository mottatteDokumentRepository;

    private KontrollerInntektInputMapper kontrollerInntektInputMapper = mock(KontrollerInntektInputMapper.class);
    private InntektArbeidYtelseTjeneste iayTjeneste = mock(InntektArbeidYtelseTjeneste.class);

    private KontrollerInntektEtterlysningOppretter oppretter;
    private Behandling behandling;


    @BeforeEach
    void setUp() {
        oppretter = new KontrollerInntektEtterlysningOppretter(
            etterlysningRepository,
            sporingRepository,
            new EtterlysningTjeneste(mottatteDokumentRepository, etterlysningRepository),
            iayTjeneste,
            mock(ProsessTaskTjeneste.class), // ProsessTaskTjeneste er ikke nødvendig for denne testen
            kontrollerInntektInputMapper, // KontrollerInntektInputMapper er ikke nødvendig for denne testen
            100 // Akseptert differanse
        );

        behandling = TestScenarioBuilder.builderMedSøknad().lagre(entityManager);

    }


    @Test
    void skal_utlede_resultat_uten_etterlysning_og_lagre_sporing() {
        // Arrange
        var fom = LocalDate.now().withDayOfMonth(1);
        var tom = fom.plusMonths(1).minusDays(1);
        var rapporterteInntekter = Set.of(new RapportertInntekt(InntektType.ARBEIDSTAKER_ELLER_FRILANSER, BigDecimal.TEN));
        when(kontrollerInntektInputMapper.mapInput(any())).thenReturn(new KontrollerInntektInput(
            new LocalDateTimeline<>(fom, tom, true),
            new LocalDateTimeline<>(fom, tom, new RapporterteInntekter(rapporterteInntekter, rapporterteInntekter)),
            LocalDateTimeline.empty()
        ));

        // Act
        oppretter.opprettEtterlysninger(BehandlingReferanse.fra(behandling));

        // Assert
        var sporinger = sporingRepository.hentSporinger(behandling.getId(), "KontrollerInntektEtterlysningOppretter");
        assertThat(sporinger.size()).isEqualTo(1);
    }
}
