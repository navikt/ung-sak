package no.nav.ung.sak.formidling;

import no.nav.ung.kodeverk.behandling.BehandlingResultatType;
import no.nav.ung.kodeverk.behandling.BehandlingType;
import no.nav.ung.kodeverk.formidling.TemplateType;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.formidling.innhold.OpphørInnholdBygger;
import no.nav.ung.sak.formidling.innhold.VedtaksbrevInnholdBygger;
import no.nav.ung.sak.test.util.behandling.TestScenarioBuilder;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static no.nav.ung.sak.formidling.HtmlAssert.assertThatHtml;
import static org.assertj.core.api.Assertions.assertThat;


class EndringOpphørTest extends AbstractVedtaksbrevInnholdByggerTest {

    private final LocalDate DAGENS_DATO = LocalDate.of(2025, 8, 15);


    EndringOpphørTest() {
        super(1, "Du får ikke lenger ungdomsprogramytelse");
    }


    @Test
    void standardOpphørsbrev() {
        LocalDate sluttdato = LocalDate.of(2025, 8, 15);
        var forventet = VedtaksbrevVerifikasjon.medHeaderOgFooter(fnr,
            """
                Du får ikke lenger ungdomsprogramytelse \
                Fra 16. august 2025 får du ikke lenger penger gjennom ungdomsytelsen. \
                Det er fordi du ikke lenger er med i ungdomsprogrammet. \
                Den siste utbetalingen får du før den 10. september 2025. \
                Vedtaket er gjort etter arbeidsmarkedsloven § xx og forskrift om xxx § xx. \
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
                Fra 16. juni 2025 får du ikke lenger penger gjennom ungdomsytelsen. \
                Det er fordi du ikke lenger er med i ungdomsprogrammet. \
                Vedtaket er gjort etter arbeidsmarkedsloven § xx og forskrift om xxx § xx. \
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

    private Behandling lagOpphørsbehandling(LocalDate sluttdato) {
        var forrigeBehandlingGrunnlag = BrevScenarioer.innvilget19år(LocalDate.of(2025, 1, 1));
        var ungTestGrunnlag = BrevScenarioer.endringOpphør(forrigeBehandlingGrunnlag.programPerioder().getFirst().getPeriode().toLocalDateInterval(), sluttdato);

        TestScenarioBuilder builder = TestScenarioBuilder.builderMedSøknad()
            .medBehandlingType(BehandlingType.REVURDERING)
            .medUngTestGrunnlag(forrigeBehandlingGrunnlag);
        var originalBehandling = builder.buildOgLagreMedUng(ungTestRepositories);
        originalBehandling.setBehandlingResultatType(BehandlingResultatType.INNVILGET);
        originalBehandling.avsluttBehandling();

        builder
            .medBehandlingType(BehandlingType.REVURDERING)
            .medUngTestGrunnlag(ungTestGrunnlag)
            .medOriginalBehandling(originalBehandling, null);

        var behandling = builder.buildOgLagreNyUngBehandlingPåEksisterendeSak(ungTestRepositories);


        behandling.setBehandlingResultatType(BehandlingResultatType.INNVILGET);
        behandling.avsluttBehandling();


        return behandling;
    }


    @Override
    protected VedtaksbrevInnholdBygger lagVedtaksbrevInnholdBygger() {
        return new OpphørInnholdBygger(DAGENS_DATO);
    }

    @Override
    protected Behandling lagScenarioForFellesTester() {
        LocalDate sluttdato = LocalDate.of(2025, 8, 15);
        return lagOpphørsbehandling(sluttdato);
    }
}


