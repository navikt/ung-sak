package no.nav.ung.sak.formidling;

import no.nav.ung.kodeverk.behandling.BehandlingResultatType;
import no.nav.ung.kodeverk.behandling.BehandlingType;
import no.nav.ung.kodeverk.formidling.TemplateType;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.formidling.innhold.OpphørInnholdBygger;
import no.nav.ung.sak.formidling.innhold.VedtaksbrevInnholdBygger;
import no.nav.ung.sak.test.util.behandling.TestScenarioBuilder;
import no.nav.ung.sak.test.util.behandling.UngTestScenario;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static no.nav.ung.sak.formidling.HtmlAssert.assertThatHtml;
import static org.assertj.core.api.Assertions.assertThat;


class EndringOpphørTest extends AbstractVedtaksbrevInnholdByggerTest {

   EndringOpphørTest() {
        super(1, "Du får ikke lenger ungdomsprogramytelse");
    }


    @DisplayName("Opphørsbrev")
    @Test
    void standardOpphørsbrev() {
        LocalDate sluttdato = LocalDate.of(2025, 8, 15);
        var forrigeBehandlingGrunnlag = BrevScenarioer.innvilget19år(LocalDate.of(2024, 12, 1));
        var ungTestGrunnlag = BrevScenarioer.endringOpphør(forrigeBehandlingGrunnlag.programPerioder().getFirst().getPeriode().toLocalDateInterval(), sluttdato);
        var forventet = VedtaksbrevVerifikasjon.medHeaderOgFooter(fnr,
            """
                Du får ikke lenger ungdomsprogramytelse \
                Fra 16. august 2025 får du ikke lenger penger gjennom ungdomsytelsen. \
                Det er fordi du ikke lenger er med i ungdomsprogrammet. \
                Den siste utbetalingen får du i september 2025, før den 10. i måneden. \
                Vedtaket er gjort etter arbeidsmarkedsloven § xx og forskrift om xxx § xx. \
                """);


        var behandling = lagOpphørscenario(ungTestGrunnlag, forrigeBehandlingGrunnlag);

        GenerertBrev generertBrev = genererVedtaksbrev(behandling.getId());
        assertThat(generertBrev.templateType()).isEqualTo(TemplateType.OPPHØR);

        var brevtekst = generertBrev.dokument().html();

        assertThatHtml(brevtekst)
            .asPlainTextIsEqualTo(forventet)
            .containsHtmlSubSequenceOnce(
                "<h1>Du får ikke lenger ungdomsprogramytelse</h1>"
            );

    }

    private Behandling lagOpphørscenario(UngTestScenario ungTestscenario, UngTestScenario forrigeBehandlingScenario) {
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
        return new OpphørInnholdBygger();
    }

    @Override
    protected Behandling lagScenarioForFellesTester() {
        LocalDate sluttdato = LocalDate.of(2025, 8, 15);
        var forrigeBehandlingGrunnlag = BrevScenarioer.innvilget19år(LocalDate.of(2024, 12, 1));
        var ungTestGrunnlag = BrevScenarioer.endringOpphør(forrigeBehandlingGrunnlag.programPerioder().getFirst().getPeriode().toLocalDateInterval(), sluttdato);
        return lagOpphørscenario(ungTestGrunnlag, forrigeBehandlingGrunnlag);
    }
}


