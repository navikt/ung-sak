package no.nav.ung.ytelse.ungdomsprogramytelsen.formidling;

import no.nav.ung.kodeverk.behandling.BehandlingResultatType;
import no.nav.ung.kodeverk.behandling.BehandlingType;
import no.nav.ung.kodeverk.formidling.TemplateType;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.formidling.GenerertBrev;
import no.nav.ung.sak.test.util.behandling.ungdomsprogramytelse.TestScenarioBuilder;
import no.nav.ung.sak.test.util.behandling.ungdomsprogramytelse.UngTestScenario;
import no.nav.ung.ytelse.ungdomsprogramytelsen.formidling.scenarioer.EndringProgramPeriodeScenarioer;
import no.nav.ung.ytelse.ungdomsprogramytelsen.formidling.scenarioer.FørstegangsbehandlingScenarioer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static no.nav.ung.ytelse.ungdomsprogramytelsen.formidling.HtmlAssert.assertThatHtml;
import static org.assertj.core.api.Assertions.assertThat;


class EndringOpphørTest extends AbstractUngdomsytelseVedtaksbrevInnholdByggerTest {

    private static final LocalDate DAGENS_DATO = LocalDate.of(2025, 8, 15);




    EndringOpphørTest() {
        super(1, "Du får ikke lenger ungdomsprogramytelse");
    }

    @BeforeAll
    static void beforeAll() {
        System.setProperty("BREV_DAGENS_DATO_TEST", DAGENS_DATO.toString());
    }

    @AfterAll
    static void afterAll() {
        System.clearProperty("BREV_DAGENS_DATO_TEST");
    }

    @Test
    void standardOpphørsbrev() {
        LocalDate sluttdato = LocalDate.of(2025, 8, 15);
        var forventet = VedtaksbrevVerifikasjon.medHeaderOgFooter(fnr,
            """
                Du får ikke lenger ungdomsprogramytelse \
                Fra 16. august 2025 får du ikke lenger penger gjennom ungdomsprogramytelsen. \
                Det er fordi du ikke lenger er med i ungdomsprogrammet. \
                Den siste utbetalingen får du før den 12. september 2025. \
                Vedtaket er gjort etter arbeidsmarkedsloven §§ 12 tredje ledd og 13 fjerde ledd og forskrift om forsøk med ungdomsprogram og ungdomsprogramytelse § 8 jf. § 3. \
                """);


        var behandling = lagOpphørsbehandling(sluttdato);

        GenerertBrev generertBrev = genererVedtaksbrev(behandling.getId());
        assertThat(generertBrev.templateType()).isEqualTo(TemplateType.OPPHØR);

        var brevtekst = generertBrev.dokument().html();

        assertThatHtml(brevtekst)
            .asPlainTextIsEqualTo(forventet)
            .containsHtmlSubSequenceOnce(
                "<h1>Du får ikke lenger ungdomsprogramytelse</h1>"
            );

    }


    @Test
    void opphør_tilbake_i_tid() {
        LocalDate sluttdato = LocalDate.of(2025, 6, 15);
        var behandling = lagOpphørsbehandling(sluttdato);

        var forventet = VedtaksbrevVerifikasjon.medHeaderOgFooter(fnr,
            """
                Du får ikke lenger ungdomsprogramytelse \
                Fra 16. juni 2025 får du ikke lenger penger gjennom ungdomsprogramytelsen. \
                Det er fordi du ikke lenger er med i ungdomsprogrammet. \
                Vedtaket er gjort etter arbeidsmarkedsloven §§ 12 tredje ledd og 13 fjerde ledd og forskrift om forsøk med ungdomsprogram og ungdomsprogramytelse § 8 jf. § 3. \
                """);

        GenerertBrev generertBrev = genererVedtaksbrev(behandling.getId());
        assertThat(generertBrev.templateType()).isEqualTo(TemplateType.OPPHØR);

        var brevtekst = generertBrev.dokument().html();

        assertThatHtml(brevtekst)
            .asPlainTextIsEqualTo(forventet)
            .containsHtmlSubSequenceOnce(
                "<h1>Du får ikke lenger ungdomsprogramytelse</h1>"
            );

    }

    @Test
    void opphørMaksDato() {
        var forventet = VedtaksbrevVerifikasjon.medHeaderOgFooter(fnr,
            """
                Ungdomsprogramytelsen din opphører \
                Fra 16. august 2025 får du ikke lenger penger gjennom ungdomsprogramytelsen. \
                Det er fordi du har brukt opp alle dagene dine i ungdomsprogrammet. \
                Den siste utbetalingen får du før den 12. september 2025. \
                Vedtaket er gjort etter arbeidsmarkedsloven §§ 12 tredje ledd og 13 fjerde ledd og forskrift om forsøk med ungdomsprogram og ungdomsprogramytelse § 8 jf. § 3. \
                """);


        var behandling = lagOpphørVedMaksDatoBehandling();

        GenerertBrev generertBrev = genererVedtaksbrev(behandling.getId());
        assertThat(generertBrev.templateType()).isEqualTo(TemplateType.OPPHØR);

        var brevtekst = generertBrev.dokument().html();

        assertThatHtml(brevtekst)
            .asPlainTextIsEqualTo(forventet)
            .containsHtmlSubSequenceOnce(
                "<h1>Du får ikke lenger ungdomsprogramytelse</h1>"
            );

    }

    private Behandling lagOpphørsbehandling(LocalDate sluttdato) {
        var forrigeBehandlingGrunnlag = FørstegangsbehandlingScenarioer.innvilget19år(LocalDate.of(2025, 1, 1));
        var revurderingGrunnlag = EndringProgramPeriodeScenarioer.endringOpphør(forrigeBehandlingGrunnlag.programPerioder().getFirst().getPeriode().toLocalDateInterval(), sluttdato);

        var behandling = lagRevurderingMedOriginalBehandling(forrigeBehandlingGrunnlag, revurderingGrunnlag);


        return behandling;
    }

    private Behandling lagOpphørVedMaksDatoBehandling() {
        LocalDate fom = LocalDate.of(2025, 1, 1);
        var forrigeBehandlingGrunnlag = FørstegangsbehandlingScenarioer.innvilget19år(fom);
        LocalDate maksdato = fom.plusWeeks(52).minusDays(1);
        var revurderingGrunnlag = EndringProgramPeriodeScenarioer.opphørMaksDato(fom, maksdato);


        return lagRevurderingMedOriginalBehandling(forrigeBehandlingGrunnlag, revurderingGrunnlag);
    }

    private Behandling lagRevurderingMedOriginalBehandling(UngTestScenario forrigeBehandlingGrunnlag, UngTestScenario revurderingGrunnlag) {
        TestScenarioBuilder builder = TestScenarioBuilder.builderMedSøknad()
            .medBehandlingType(BehandlingType.REVURDERING)
            .medUngTestGrunnlag(forrigeBehandlingGrunnlag);
        var originalBehandling = builder.buildOgLagreMedUng(ungTestRepositories);
        originalBehandling.setBehandlingResultatType(BehandlingResultatType.INNVILGET);
        originalBehandling.avsluttBehandling();

        builder
            .medBehandlingType(BehandlingType.REVURDERING)
            .medUngTestGrunnlag(revurderingGrunnlag)
            .medOriginalBehandling(originalBehandling, null);

        var behandling = builder.buildOgLagreNyUngBehandlingPåEksisterendeSak(ungTestRepositories);


        behandling.setBehandlingResultatType(BehandlingResultatType.INNVILGET);
        behandling.avsluttBehandling();
        return behandling;
    }


    @Override
    protected Behandling lagScenarioForFellesTester() {
        LocalDate sluttdato = LocalDate.of(2025, 8, 15);
        return lagOpphørsbehandling(sluttdato);
    }
}


