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

class KlageMedholdTest extends AbstractKlageVedtaksbrevInnholdByggerTest {

    KlageMedholdTest() {
        super(1, "NAV har omgjort vedtaket ditt om ");
    }


    @Test
    void standardMedholdKlage() {
        TestScenarioBuilder testScenarioBuilder = FørstegangsbehandlingScenarioer.lagAvsluttetStandardBehandling(ungTestRepositories);
        UngKlageTestScenario klageScenario = KlageScenarioer.klageMedhold(testScenarioBuilder);

        var klage = KlageScenarioer.lagKlageBehandling(ungTestRepositories, klageScenario);

        var forventet = VedtaksbrevVerifikasjon.medHeaderOgFooterManuellUtenBeslutter(fnr,
            """
                NAV har omgjort vedtaket ditt om ungdomsprogramytelse \
                Etter at du klaget har vi vurdert saken på nytt. Vi har kommet fram til at vedtaket gjøres om. \
                Dette har vi lagt vekt på i vurderingen vår \
                FRITEKST I BREV \
                Du må melde fra om endringer \
                Dersom det skjer endringer som kan ha betydning for vedtaket, må du straks melde fra til NAV. Du finner \
                mer informasjon på nav.no/rettogplikt. \
                """);


        GenerertBrev generertBrev = genererVedtaksbrev(klage);
        assertThat(generertBrev.templateType()).isEqualTo(TemplateType.KLAGE_MEDHOLD);

        var brevtekst = generertBrev.dokument().html();

        assertThatHtml(brevtekst)
            .asPlainTextIsEqualTo(forventet)
            .containsHtmlSubSequenceOnce(
                "<h1>NAV har omgjort vedtaket ditt om ungdomsprogramytelse</h1>"
            );

    }


    @Override
    protected Behandling lagScenarioForFellesTester() {
        TestScenarioBuilder påklagdBehandlingScenario = FørstegangsbehandlingScenarioer.lagAvsluttetStandardBehandling(ungTestRepositories);
        UngKlageTestScenario klageScenario = KlageScenarioer.klageMedhold(påklagdBehandlingScenario);
        return KlageScenarioer.lagKlageBehandling(ungTestRepositories, klageScenario);
    }
}


