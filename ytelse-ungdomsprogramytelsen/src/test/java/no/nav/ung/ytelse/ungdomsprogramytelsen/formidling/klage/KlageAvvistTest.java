package no.nav.ung.ytelse.ungdomsprogramytelsen.formidling.klage;

import no.nav.ung.kodeverk.formidling.TemplateType;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.formidling.GenerertBrev;
import no.nav.ung.ytelse.ungdomsprogramytelsen.formidling.VedtaksbrevVerifikasjon;
import no.nav.ung.ytelse.ungdomsprogramytelsen.formidling.scenarioer.FørstegangsbehandlingScenarioer;
import no.nav.ung.ytelse.ungdomsprogramytelsen.formidling.scenarioer.KlageScenarioer;
import no.nav.ung.sak.test.util.behandling.ungdomsprogramytelse.TestScenarioBuilder;
import no.nav.ung.sak.test.util.behandling.ungdomsprogramytelse.UngKlageTestScenario;
import org.junit.jupiter.api.Test;

import static no.nav.ung.ytelse.ungdomsprogramytelsen.formidling.HtmlAssert.assertThatHtml;
import static org.assertj.core.api.Assertions.assertThat;

class KlageAvvistTest extends AbstractKlageVedtaksbrevInnholdByggerTest {

    KlageAvvistTest() {
        super(1, "NAV har avvist klagen din på vedtaket om ");
    }


    @Test
    void standardAvvistKlage() {
        TestScenarioBuilder testScenarioBuilder = FørstegangsbehandlingScenarioer.lagAvsluttetStandardBehandling(ungTestRepositories);
        UngKlageTestScenario klageScenario = KlageScenarioer.klageAvvist(testScenarioBuilder);

        var klage = KlageScenarioer.lagKlageBehandling(ungTestRepositories, klageScenario);

        var forventet = VedtaksbrevVerifikasjon.medHeaderOgFooterManuellUtenBeslutter(fnr,
            """
                NAV har avvist klagen din på vedtaket om ungdomsprogramytelse \
                Vi har avvist klagen fordi du har klaget for sent. \
                Du har ikke oppgitt en grunn til at du klaget for sent som gjør at vi kan behandle klagen. \
                Vedtaket er gjort etter arbeidsmarkedsloven § 17 og forvaltningsloven §§ 31 og 33. \
                """);


        GenerertBrev generertBrev = genererVedtaksbrev(klage);
        assertThat(generertBrev.templateType()).isEqualTo(TemplateType.KLAGE_AVVIST);

        var brevtekst = generertBrev.dokument().html();

        assertThatHtml(brevtekst)
            .asPlainTextIsEqualTo(forventet)
            .containsHtmlSubSequenceOnce(
                "<h1>NAV har avvist klagen din på vedtaket om ungdomsprogramytelse</h1>"
            );

    }


    @Override
    protected Behandling lagScenarioForFellesTester() {
        TestScenarioBuilder påklagdBehandlingScenario = FørstegangsbehandlingScenarioer.lagAvsluttetStandardBehandling(ungTestRepositories);
        UngKlageTestScenario klageScenario = KlageScenarioer.klageAvvist(påklagdBehandlingScenario);
        return KlageScenarioer.lagKlageBehandling(ungTestRepositories, klageScenario);
    }
}


