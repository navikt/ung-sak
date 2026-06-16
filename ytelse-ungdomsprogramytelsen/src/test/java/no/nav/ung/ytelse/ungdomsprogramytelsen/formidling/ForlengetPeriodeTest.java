package no.nav.ung.ytelse.ungdomsprogramytelsen.formidling;

import no.nav.ung.kodeverk.behandling.BehandlingResultatType;
import no.nav.ung.kodeverk.behandling.BehandlingType;
import no.nav.ung.kodeverk.formidling.TemplateType;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.formidling.GenerertBrev;
import no.nav.ung.sak.test.util.behandling.ungdomsprogramytelse.TestScenarioBuilder;
import no.nav.ung.ytelse.ungdomsprogramytelsen.formidling.scenarioer.EndringProgramPeriodeScenarioer;
import no.nav.ung.ytelse.ungdomsprogramytelsen.formidling.scenarioer.FørstegangsbehandlingScenarioer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static no.nav.ung.ytelse.ungdomsprogramytelsen.formidling.HtmlAssert.assertThatHtml;
import static org.assertj.core.api.Assertions.assertThat;

class ForlengetPeriodeTest extends AbstractUngdomsytelseVedtaksbrevInnholdByggerTest {

    private static final LocalDate FOM = LocalDate.of(2025, 1, 1);
    private static final LocalDate OPPRINNELIG_SLUTTDATO = LocalDate.of(2025, 12, 31);
    private static final LocalDate NY_SLUTTDATO = LocalDate.of(2026, 1, 28);

    ForlengetPeriodeTest() {
        super(1, "Du får forlenget perioden med ungdomsprogramytelsen");
    }

    @DisplayName("Forlenget periode gir korrekt brev")
    @Test
    void forlengetPeriode() {
        var behandling = lagForlengetPeriodeBehandling(OPPRINNELIG_SLUTTDATO, NY_SLUTTDATO);

        GenerertBrev generertBrev = genererVedtaksbrev(behandling.getId());
        assertThat(generertBrev.templateType()).isEqualTo(TemplateType.FORLENGET_PERIODE);

        var forventet = VedtaksbrevVerifikasjon.medHeaderOgFooter(fnr,
            """
                Du får forlenget perioden med ungdomsprogramytelsen \
                Fra 1. januar 2026 får du forlenget perioden din med ungdomsprogramytelsen med inntil 8 nye uker. \
                Du får ytelsen så lenge du deltar i ungdomsprogrammet, men ikke lenger enn til 28. januar 2026. \
                Vedtaket er gjort etter arbeidsmarkedsloven §§ 12 tredje ledd og 13 fjerde ledd og forskrift om forsøk med ungdomsprogram og ungdomsprogramytelsen § 6. \
                Meld fra til oss hvis du har arbeidsinntekt i tillegg til ungdomsprogramytelsen \
                Hvis du har en arbeidsinntekt i tillegg til ungdomsprogramytelsen, er det viktig at du sier fra til oss om det. \
                Du får en SMS den 1. hver måned. Når du har fått SMS-en, logger du inn på Min side på nav.no og gir oss beskjed om all arbeidsinntekt du har hatt måneden før. \
                """);

        assertThatHtml(generertBrev.dokument().html())
            .asPlainTextIsEqualTo(forventet)
            .containsHtmlSubSequenceOnce(
                "<h1>Du får forlenget perioden med ungdomsprogramytelsen</h1>"
            );
    }

    private Behandling lagForlengetPeriodeBehandling(LocalDate opprinneligSluttdato, LocalDate nySluttdato) {
        var forrigeBehandlingGrunnlag = FørstegangsbehandlingScenarioer.innvilget19årMedMaksDato(FOM, opprinneligSluttdato);
        var forlengetPeriodeGrunnlag = EndringProgramPeriodeScenarioer.forlengetPeriode(FOM, opprinneligSluttdato, nySluttdato);

        TestScenarioBuilder builder = TestScenarioBuilder.builderMedSøknad()
            .medBehandlingType(BehandlingType.REVURDERING)
            .medUngTestGrunnlag(forrigeBehandlingGrunnlag);
        var originalBehandling = builder.buildOgLagreMedUng(ungTestRepositories);
        originalBehandling.setBehandlingResultatType(BehandlingResultatType.INNVILGET);
        originalBehandling.avsluttBehandling();

        builder
            .medBehandlingType(BehandlingType.REVURDERING)
            .medUngTestGrunnlag(forlengetPeriodeGrunnlag)
            .medOriginalBehandling(originalBehandling, null);

        var behandling = builder.buildOgLagreNyUngBehandlingPåEksisterendeSak(ungTestRepositories);
        behandling.setBehandlingResultatType(BehandlingResultatType.INNVILGET);
        behandling.avsluttBehandling();

        return behandling;
    }

    @Override
    protected Behandling lagScenarioForFellesTester() {
        return lagForlengetPeriodeBehandling(OPPRINNELIG_SLUTTDATO, NY_SLUTTDATO);
    }
}
