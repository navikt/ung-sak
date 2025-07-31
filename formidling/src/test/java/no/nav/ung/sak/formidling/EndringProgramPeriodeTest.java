package no.nav.ung.sak.formidling;

import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.ung.kodeverk.behandling.BehandlingResultatType;
import no.nav.ung.kodeverk.behandling.BehandlingType;
import no.nav.ung.kodeverk.formidling.TemplateType;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.formidling.innhold.EndringProgramPeriodeInnholdBygger;
import no.nav.ung.sak.formidling.innhold.OpphørInnholdBygger;
import no.nav.ung.sak.formidling.scenarioer.BrevScenarioer;
import no.nav.ung.sak.formidling.vedtak.regler.EndringSluttdatoStrategy;
import no.nav.ung.sak.formidling.vedtak.regler.EndringStartdatoStrategy;
import no.nav.ung.sak.formidling.vedtak.regler.VedtaksbrevInnholdbyggerStrategy;
import no.nav.ung.sak.test.util.behandling.TestScenarioBuilder;
import no.nav.ung.sak.test.util.behandling.UngTestScenario;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.LocalDate;
import java.util.List;

import static no.nav.ung.sak.formidling.HtmlAssert.assertThatHtml;
import static org.assertj.core.api.Assertions.assertThat;

class EndringProgramPeriodeTest extends AbstractVedtaksbrevInnholdByggerTest {

    private final LocalDate DAGENS_DATO = LocalDate.of(2025, 8, 15);


    EndringProgramPeriodeTest() {
        super(1,
            "Vi har endret ungdomsprogramytelsen din");
    }


    @Test
    void flytteSluttdato_fremover() {
        var nySluttDato = LocalDate.of(2025, 8, 22);
        var opprinneligSluttdato = LocalDate.of(2025, 8, 15);
        var behandling = lagScenarioForSluttdato(opprinneligSluttdato, nySluttDato);

        var forventet = VedtaksbrevVerifikasjon.medHeaderOgFooter(fnr,
            """
                Vi har endret ungdomsprogramytelsen din \
                Fra 23. august 2025 får du ikke lenger penger fordi du ikke lenger er med i ungdomsprogrammet. \
                Du fikk tidligere melding om at du skulle få penger til og med 15. august 2025, \
                men den datoen gjelder ikke lenger fordi du sluttet i ungdomsprogrammet 22. august 2025. \
                Den siste utbetalingen får du før den 10. september 2025. \
                Vedtaket er gjort etter arbeidsmarkedsloven §§ 12 tredje ledd og 13 fjerde ledd og forskrift om forsøk med ungdomsprogram og ungdomsprogramytelse § 8 jf. § 6. \
                """);

        GenerertBrev generertBrev = genererVedtaksbrev(behandling.getId());
        assertThat(generertBrev.templateType()).isEqualTo(TemplateType.ENDRING_PROGRAMPERIODE);

        var brevtekst = generertBrev.dokument().html();

        assertThatHtml(brevtekst)
            .asPlainTextIsEqualTo(forventet)
            .containsHtmlSubSequenceOnce(
                "<h1>Vi har endret ungdomsprogramytelsen din</h1>"
            );
    }

    @Test
    @DisplayName("Flytter sluttdato bakover til forrige måned")
    void flytteSluttdato_bakover() {
        var nySluttdato = LocalDate.of(2025, 7, 31);
        var opprinneligSluttdato = LocalDate.of(2025, 8, 15);
        var behandling = lagScenarioForSluttdato(opprinneligSluttdato, nySluttdato);

        var forventet = VedtaksbrevVerifikasjon.medHeaderOgFooter(fnr,
            """
                Vi har endret ungdomsprogramytelsen din \
                Fra 1. august 2025 får du ikke lenger penger fordi du ikke lenger er med i ungdomsprogrammet. \
                Du fikk tidligere melding om at du skulle få penger til og med 15. august 2025, \
                men den datoen gjelder ikke lenger fordi du sluttet i ungdomsprogrammet 31. juli 2025. \
                Vedtaket er gjort etter arbeidsmarkedsloven §§ 12 tredje ledd og 13 fjerde ledd og forskrift om forsøk med ungdomsprogram og ungdomsprogramytelse § 8 jf. § 6. \
                """);

        GenerertBrev generertBrev = genererVedtaksbrev(behandling.getId());
        assertThat(generertBrev.templateType()).isEqualTo(TemplateType.ENDRING_PROGRAMPERIODE);

        var brevtekst = generertBrev.dokument().html();

        assertThatHtml(brevtekst)
            .asPlainTextIsEqualTo(forventet)
            .containsHtmlSubSequenceOnce(
                "<h1>Vi har endret ungdomsprogramytelsen din</h1>"
            );
    }

    private Behandling lagScenarioForSluttdato(LocalDate opprinneligSluttdato, LocalDate nySluttdato) {
        LocalDate fomDato = LocalDate.of(2024, 12, 1);

        LocalDateInterval opprinneligProgramPeriode = new LocalDateInterval(fomDato, fomDato.plusWeeks(52));
        var opphørGrunnlag = BrevScenarioer.endringOpphør(opprinneligProgramPeriode, opprinneligSluttdato);
        var endringGrunnlag = BrevScenarioer.endringSluttdato(nySluttdato, opphørGrunnlag.programPerioder().getFirst().getPeriode().toLocalDateInterval());
        return lagEndringScenario(endringGrunnlag, opphørGrunnlag);
    }

    @ParameterizedTest
    @CsvSource({
        "2025-08-20, 20. august 2025", //fremover
        "2025-08-10, 10. august 2025" //bakover
    })
    void flytteStartdato(String nyStartdatoStr, String forventetDatoTekst) {
        LocalDate opprinneligStartdato = LocalDate.of(2025, 8, 15);
        LocalDate nyStartdato = LocalDate.parse(nyStartdatoStr);

        var førstegangsbehandling = BrevScenarioer.innvilget19år(opprinneligStartdato);
        var endringGrunnlag = BrevScenarioer.endringStartdato(nyStartdato, førstegangsbehandling.programPerioder().getFirst().getPeriode().toLocalDateInterval());
        var behandling = lagEndringScenario(endringGrunnlag, førstegangsbehandling);

        var forventet = VedtaksbrevVerifikasjon.medHeaderOgFooter(fnr,
            """
                Vi har endret ungdomsprogramytelsen din \
                Fra %1$s får du penger fordi du er med i ungdomsprogrammet. \
                Du fikk tidligere melding om at du skulle få penger fra og med 15. august 2025, \
                men den datoen gjelder ikke lenger fordi du startet i ungdomsprogrammet %1$s. \
                Vedtaket er gjort etter arbeidsmarkedsloven §§ 12 tredje ledd og 13 fjerde ledd og forskrift om forsøk med ungdomsprogram og ungdomsprogramytelse § 8 jf. § 6. \
                """.formatted(forventetDatoTekst));

        GenerertBrev generertBrev = genererVedtaksbrev(behandling.getId());
        assertThat(generertBrev.templateType()).isEqualTo(TemplateType.ENDRING_PROGRAMPERIODE);

        var brevtekst = generertBrev.dokument().html();

        assertThatHtml(brevtekst)
            .asPlainTextIsEqualTo(forventet)
            .containsHtmlSubSequenceOnce(
                "<h1>Vi har endret ungdomsprogramytelsen din</h1>"
            );

    }

    private Behandling lagEndringScenario(UngTestScenario ungTestscenario, UngTestScenario forrigeBehandlingScenario) {
        TestScenarioBuilder builder = TestScenarioBuilder.builderMedSøknad()
            .medBehandlingType(BehandlingType.REVURDERING)
            .medUngTestGrunnlag(forrigeBehandlingScenario);
        var originalBehandling = builder.buildOgLagreMedUng(ungTestRepositories);
        originalBehandling.setBehandlingResultatType(BehandlingResultatType.INNVILGET);
        originalBehandling.avsluttBehandling();

        builder
            .medBehandlingType(BehandlingType.REVURDERING)
            .medUngTestGrunnlag(ungTestscenario)
            .medOriginalBehandling(originalBehandling, null);

        var behandling = builder.buildOgLagreNyUngBehandlingPåEksisterendeSak(ungTestRepositories);


        behandling.setBehandlingResultatType(BehandlingResultatType.INNVILGET);
        behandling.avsluttBehandling();


        return behandling;
    }


    @Override
    protected List<VedtaksbrevInnholdbyggerStrategy> lagVedtaksbrevByggerStrategier() {
        var ungdomsprogramPeriodeRepository = ungTestRepositories.ungdomsprogramPeriodeRepository();
        var endringProgramPeriodeInnholdBygger = new EndringProgramPeriodeInnholdBygger(ungdomsprogramPeriodeRepository, DAGENS_DATO);

        return List.of(
            new EndringSluttdatoStrategy(
                ungdomsprogramPeriodeRepository,
                new OpphørInnholdBygger(DAGENS_DATO),
                endringProgramPeriodeInnholdBygger
            ),
            new EndringStartdatoStrategy(endringProgramPeriodeInnholdBygger));
    }

    @Override
    protected Behandling lagScenarioForFellesTester() {
        LocalDate fomDato = LocalDate.of(2024, 12, 1);
        LocalDate opprinnligOpphørsdato = LocalDate.of(2025, 8, 15);
        LocalDate nyOpphørsdato = LocalDate.of(2025, 8, 10);

        var opphørGrunnlag = BrevScenarioer.endringOpphør(new LocalDateInterval(fomDato, fomDato.plusWeeks(52)), opprinnligOpphørsdato);
        var endringGrunnlag = BrevScenarioer.endringSluttdato(nyOpphørsdato, opphørGrunnlag.programPerioder().getFirst().getPeriode().toLocalDateInterval());
        return lagEndringScenario(endringGrunnlag, opphørGrunnlag);
    }
}


