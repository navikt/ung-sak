package no.nav.ung.sak.formidling.klage;

import no.nav.ung.kodeverk.behandling.BehandlingResultatType;
import no.nav.ung.kodeverk.behandling.BehandlingType;
import no.nav.ung.kodeverk.formidling.TemplateType;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.formidling.GenerertBrev;
import no.nav.ung.sak.formidling.VedtaksbrevVerifikasjon;
import no.nav.ung.sak.formidling.scenarioer.FørstegangsbehandlingScenarioer;
import no.nav.ung.sak.formidling.scenarioer.KlageScenarioer;
import org.junit.jupiter.api.Test;

import static no.nav.ung.sak.formidling.HtmlAssert.assertThatHtml;
import static org.assertj.core.api.Assertions.assertThat;

class KlageAvvistTest extends AbstractKlageVedtaksbrevInnholdByggerTest {

    KlageAvvistTest() {
        super(1, "NAV har avvist klagen din på vedtaket om ");
    }


    @Test
    void standardAvvistKlage() {
        var klage = lagAvvistScenario();

        var forventet = VedtaksbrevVerifikasjon.medHeaderOgFooter(fnr,
            """
                NAV har avvist klagen din på vedtaket om ungdomsprogramytelse \
                Vi har avvist klagen fordi du har klaget for sent. \
                Du har ikke oppgitt en grunn til at du klaget for sent som gjør at vi kan behandle klagen. \
                FTRL/FL \
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

    private Behandling lagAvvistScenario() {
        var builder = FørstegangsbehandlingScenarioer.lagAvsluttetStandardBehandling(ungTestRepositories);

        Behandling påklagdBehandling = builder.getBehandling();
        var klageGrunnlag = KlageScenarioer.klageAvvist(påklagdBehandling, "begrunnelse");
        builder
            .medBehandlingType(BehandlingType.KLAGE)
            .medBehandlingsresultat(BehandlingResultatType.INNVILGET)
            .medKlageGrunnlag(klageGrunnlag);

        return builder.buildOgLagreKlage(ungTestRepositories);
    }


    @Override
    protected Behandling lagScenarioForFellesTester() {
        return lagAvvistScenario();
    }
}


