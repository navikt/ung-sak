package no.nav.ung.ytelse.ungdomsprogramytelsen.formidling;

import no.nav.ung.kodeverk.behandling.BehandlingResultatType;
import no.nav.ung.kodeverk.behandling.BehandlingType;
import no.nav.ung.kodeverk.formidling.TemplateType;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.formidling.GenerertBrev;
import no.nav.ung.ytelse.ungdomsprogramytelsen.formidling.scenarioer.EndringUtvidetKvoteScenarioer;
import no.nav.ung.ytelse.ungdomsprogramytelsen.formidling.scenarioer.FørstegangsbehandlingScenarioer;
import no.nav.ung.sak.test.util.behandling.ungdomsprogramytelse.TestScenarioBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static no.nav.ung.ytelse.ungdomsprogramytelsen.formidling.HtmlAssert.assertThatHtml;
import static org.assertj.core.api.Assertions.assertThat;

class EndringUtvidetKvoteTest extends AbstractUngdomsytelseVedtaksbrevInnholdByggerTest {

    private static final LocalDate FOM = LocalDate.of(2025, 1, 1);
    private static final LocalDate OPPRINNELIG_SLUTTDATO = LocalDate.of(2025, 12, 31);
    private static final LocalDate NY_SLUTTDATO = LocalDate.of(2026, 1, 28);

    EndringUtvidetKvoteTest() {
        super(1, "Kvoten din i ungdomsprogrammet er utvidet");
    }

    @DisplayName("Utvidet kvote gir korrekt brev")
    @Test
    void utvidetKvote() {
        var behandling = lagUtvidetKvoteBehandling(OPPRINNELIG_SLUTTDATO, NY_SLUTTDATO);

        GenerertBrev generertBrev = genererVedtaksbrev(behandling.getId());
        assertThat(generertBrev.templateType()).isEqualTo(TemplateType.FORLENGET_PERIODE);

        var forventet = VedtaksbrevVerifikasjon.medHeaderOgFooter(fnr,
            """
                Kvoten din i ungdomsprogrammet er utvidet \
                Du var planlagt å delta i ungdomsprogrammet til og med 30. desember 2025. \
                Kvoten din er nå utvidet, og du kan fortsette til og med 28. januar 2026. \
                Vedtaket er gjort etter arbeidsmarkedsloven §§ 12 tredje ledd og 13 fjerde ledd og forskrift om forsøk med ungdomsprogram og ungdomsprogramytelse § 6. \
                """);

        assertThatHtml(generertBrev.dokument().html())
            .asPlainTextIsEqualTo(forventet)
            .containsHtmlSubSequenceOnce(
                "<h1>Kvoten din i ungdomsprogrammet er utvidet</h1>"
            );
    }

    private Behandling lagUtvidetKvoteBehandling(LocalDate opprinneligSluttdato, LocalDate nySluttdato) {
        var forrigeBehandlingGrunnlag = FørstegangsbehandlingScenarioer.innvilget19år(FOM);
        var utvidetKvoteGrunnlag = EndringUtvidetKvoteScenarioer.utvidetKvote(FOM, opprinneligSluttdato, nySluttdato);

        TestScenarioBuilder builder = TestScenarioBuilder.builderMedSøknad()
            .medBehandlingType(BehandlingType.REVURDERING)
            .medUngTestGrunnlag(forrigeBehandlingGrunnlag);
        var originalBehandling = builder.buildOgLagreMedUng(ungTestRepositories);
        originalBehandling.setBehandlingResultatType(BehandlingResultatType.INNVILGET);
        originalBehandling.avsluttBehandling();

        builder
            .medBehandlingType(BehandlingType.REVURDERING)
            .medUngTestGrunnlag(utvidetKvoteGrunnlag)
            .medOriginalBehandling(originalBehandling, null);

        var behandling = builder.buildOgLagreNyUngBehandlingPåEksisterendeSak(ungTestRepositories);
        behandling.setBehandlingResultatType(BehandlingResultatType.INNVILGET);
        behandling.avsluttBehandling();

        return behandling;
    }

    @Override
    protected Behandling lagScenarioForFellesTester() {
        return lagUtvidetKvoteBehandling(OPPRINNELIG_SLUTTDATO, NY_SLUTTDATO);
    }
}
