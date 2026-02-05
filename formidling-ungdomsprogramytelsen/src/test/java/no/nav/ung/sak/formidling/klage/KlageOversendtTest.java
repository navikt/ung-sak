package no.nav.ung.sak.formidling.klage;

import no.nav.ung.kodeverk.formidling.TemplateType;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.formidling.GenerertBrev;
import no.nav.ung.sak.formidling.VedtaksbrevVerifikasjon;
import no.nav.ung.sak.formidling.scenarioer.FørstegangsbehandlingScenarioer;
import no.nav.ung.sak.formidling.scenarioer.KlageScenarioer;
import no.nav.ung.sak.test.util.behandling.ungdomsprogramytelse.TestScenarioBuilder;
import no.nav.ung.sak.test.util.behandling.ungdomsprogramytelse.UngKlageTestScenario;
import org.junit.jupiter.api.Test;

import static no.nav.ung.sak.formidling.HtmlAssert.assertThatHtml;
import static org.assertj.core.api.Assertions.assertThat;

class KlageOversendtTest extends AbstractKlageVedtaksbrevInnholdByggerTest {

    KlageOversendtTest() {
        super(1, "Vi har sendt saken til NAV Klageinstans");
    }


    @Test
    void standardOversendtKlage() {
        TestScenarioBuilder testScenarioBuilder = FørstegangsbehandlingScenarioer.lagAvsluttetStandardBehandling(ungTestRepositories);
        UngKlageTestScenario klageScenario = KlageScenarioer.klageOversendt(testScenarioBuilder);

        var klage = KlageScenarioer.lagKlageBehandling(ungTestRepositories, klageScenario);

        var forventet = VedtaksbrevVerifikasjon.medHeaderOgFooterManuellUtenBeslutter(fnr,
            """
                Vi har sendt saken til NAV Klageinstans \
                Vi har vurdert klagen på vedtaket om ungdomsprogramytelse, og kommet fram til at vedtaket ikke \
                endres. NAV Klageinstans skal derfor vurdere saken din på nytt. Du får melding fra NAV Klageinstans når \
                de har mottatt saken. Du finner oversikt over saksbehandlingstidene på nav.no/saksbehandlingstider. \
                Dette har vi lagt vekt på i vurderingen vår \
                FRITEKST I BREV \
                Har du nye opplysninger eller ønsker å uttale deg, kan du sende dette via klage.nav.no/nb/ettersendelse/klage/SYKDOM_I_FAMILIEN. Velg NAV-enhet: NAV Klageinstans Sør. \
                """);


        GenerertBrev generertBrev = genererVedtaksbrev(klage);
        assertThat(generertBrev.templateType()).isEqualTo(TemplateType.KLAGE_OVERSENDT);

        var brevtekst = generertBrev.dokument().html();

        assertThatHtml(brevtekst)
            .asPlainTextIsEqualTo(forventet)
            .containsHtmlSubSequenceOnce(
                "<h1>Vi har sendt saken til NAV Klageinstans</h1>"
            );

    }


    @Override
    protected Behandling lagScenarioForFellesTester() {
        TestScenarioBuilder påklagdBehandlingScenario = FørstegangsbehandlingScenarioer.lagAvsluttetStandardBehandling(ungTestRepositories);
        UngKlageTestScenario klageScenario = KlageScenarioer.klageOversendt(påklagdBehandlingScenario);
        return KlageScenarioer.lagKlageBehandling(ungTestRepositories, klageScenario);
    }
}


