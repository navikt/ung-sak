package no.nav.ung.sak.formidling;

import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.ung.kodeverk.behandling.BehandlingResultatType;
import no.nav.ung.kodeverk.behandling.BehandlingType;
import no.nav.ung.kodeverk.formidling.TemplateType;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.formidling.innhold.EndringProgramPeriodeInnholdBygger;
import no.nav.ung.sak.formidling.innhold.VedtaksbrevInnholdBygger;
import no.nav.ung.sak.test.util.behandling.TestScenarioBuilder;
import no.nav.ung.sak.test.util.behandling.UngTestScenario;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.LocalDate;

import static no.nav.ung.sak.formidling.HtmlAssert.assertThatHtml;
import static org.assertj.core.api.Assertions.assertThat;

class EndringProgramPeriodeTest extends AbstractVedtaksbrevInnholdByggerTest {

   EndringProgramPeriodeTest() {
        super(1,
            "Vi har endret ungdomsprogramytelsen din");
    }


    @ParameterizedTest
    @CsvSource({
        "2025-08-20, 20. august 2025", //fremover
        "2025-08-10, 10. august 2025" //bakover
    })
    void flytteSluttdato(String nyOpphørsdatoStr, String forventetDatoTekst) {
        LocalDate fomDato = LocalDate.of(2024, 12, 1);
        LocalDate opprinneligOpphørsdato = LocalDate.of(2025, 8, 15);
        LocalDate nyOpphørsdato = LocalDate.parse(nyOpphørsdatoStr);

        var opphørGrunnlag = BrevScenarioer.endringOpphør(opprinneligOpphørsdato, new LocalDateInterval(fomDato, fomDato.plusWeeks(52)));
        var endringGrunnlag = BrevScenarioer.endringSluttdato(nyOpphørsdato, opphørGrunnlag.programPerioder().getFirst().getPeriode().toLocalDateInterval());
        var behandling = lagEndringScenario(endringGrunnlag, opphørGrunnlag);

        var forventet = VedtaksbrevVerifikasjon.medHeaderOgFooter(fnr,
            """
                Vi har endret ungdomsprogramytelsen din \
                Fra %1$s får du ikke lenger penger fordi du ikke lenger er med i ungomdsprogrammet. \
                Du fikk tidligere melding om at du skulle få penger til og med 14. august 2025, \
                men den datoen gjelder ikke lenger fordi du sluttet i ungdomsprogrammet %1$s. \
                Vedtaket er gjort etter arbeidsmarkedsloven § xx og forskrift om xxx § xx. \
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
                Vedtaket er gjort etter arbeidsmarkedsloven § xx og forskrift om xxx § xx. \
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
            .medUngTestGrunnlag(forrigeBehandlingScenario)
            ;
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
    protected VedtaksbrevInnholdBygger lagVedtaksbrevInnholdBygger() {
        return new EndringProgramPeriodeInnholdBygger(ungTestRepositories.ungdomsprogramPeriodeRepository());
    }

    @Override
    protected Behandling lagScenarioForFellesTester() {
        LocalDate fomDato = LocalDate.of(2024, 12, 1);
        LocalDate opprinnligOpphørsdato = LocalDate.of(2025, 8, 15);
        LocalDate nyOpphørsdato = LocalDate.of(2025, 8, 10);

        var opphørGrunnlag = BrevScenarioer.endringOpphør(opprinnligOpphørsdato, new LocalDateInterval(fomDato, fomDato.plusWeeks(52)));
        var endringGrunnlag = BrevScenarioer.endringSluttdato(nyOpphørsdato, opphørGrunnlag.programPerioder().getFirst().getPeriode().toLocalDateInterval());
        return lagEndringScenario(endringGrunnlag, opphørGrunnlag);
    }
}


