package no.nav.ung.sak.formidling;

import no.nav.ung.kodeverk.behandling.BehandlingResultatType;
import no.nav.ung.kodeverk.behandling.BehandlingType;
import no.nav.ung.kodeverk.formidling.TemplateType;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.formidling.innhold.EndringBarnetilleggInnholdBygger;
import no.nav.ung.sak.formidling.innhold.VedtaksbrevInnholdBygger;
import no.nav.ung.sak.test.util.behandling.TestScenarioBuilder;
import no.nav.ung.sak.test.util.behandling.UngTestScenario;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static no.nav.ung.sak.formidling.HtmlAssert.assertThatHtml;
import static org.assertj.core.api.Assertions.assertThat;

class BrevGenerererTjenesteEndringBarnetilleggTest extends BaseVedtaksbrevInnholdByggerTest {


    @Test
    void standardEndringBarnetillegg() {
        LocalDate startdato = LocalDate.of(2025, 5, 3);
        LocalDate barnFødselsdato = LocalDate.of(2025, 5, 27);
        UngTestScenario ungTestGrunnlag = BrevScenarioer.endringBarnetillegg(startdato, barnFødselsdato);

        var forventet = VedtaksbrevVerifikasjon.medHeaderOgFooter(fnr,
            """
                Du får mer i ungdomsprogramytelse fordi du har fått barn \
                Du får 37 kroner i barnetillegg per dag fra og med 27. mai 2025, utenom lørdag og søndag. \
                Det er fordi du fikk barn denne datoen. \
                Når du har barn, får du et barnetillegg på 37 kroner per dag for hvert barn du har. \
                Vedtaket er gjort etter arbeidsmarkedsloven § xx og forskrift om xxx § xx. \
                """
        );

        var behandling = lagStandardScenario(ungTestGrunnlag);

        GenerertBrev generertBrev = genererVedtaksbrev(behandling.getId());
        assertThat(generertBrev.templateType()).isEqualTo(TemplateType.ENDRING_BARNETILLEGG);

        var brevtekst = generertBrev.dokument().html();

        assertThatHtml(brevtekst)
            .asPlainTextIsEqualTo(forventet)
            .containsHtmlSubSequenceOnce(
                "<h1>Du får mer i ungdomsprogramytelse fordi du har fått barn</h1>"
            );

    }

    @Test
    void flereBarn() {
        LocalDate startdato = LocalDate.of(2025, 5, 3);
        LocalDate barnFødselsdato = LocalDate.of(2025, 5, 27);
        UngTestScenario ungTestGrunnlag = BrevScenarioer.endringBarnetilleggFlereBarn(startdato, barnFødselsdato);

        var forventet = VedtaksbrevVerifikasjon.medHeaderOgFooter(fnr,
            """
                Du får mer i ungdomsprogramytelse fordi du har fått barn \
                Du får 111 kroner i barnetillegg per dag fra og med 27. mai 2025, utenom lørdag og søndag. \
                Det er fordi du fikk barn denne datoen. \
                Når du har barn, får du et barnetillegg på 37 kroner per dag for hvert barn du har. \
                Vedtaket er gjort etter arbeidsmarkedsloven § xx og forskrift om xxx § xx. \
                """
        );

        var behandling = lagStandardScenario(ungTestGrunnlag);

        GenerertBrev generertBrev = genererVedtaksbrev(behandling.getId());

        assertThat(generertBrev.templateType()).isEqualTo(TemplateType.ENDRING_BARNETILLEGG);

        var brevtekst = generertBrev.dokument().html();

        assertThatHtml(brevtekst)
            .asPlainTextIsEqualTo(forventet)
            .containsHtmlSubSequenceOnce(
                "<h1>Du får mer i ungdomsprogramytelse fordi du har fått barn</h1>"
            );

    }

    private Behandling lagStandardScenario(UngTestScenario ungTestscenario) {
        TestScenarioBuilder scenarioBuilder = TestScenarioBuilder.builderMedSøknad()
            .medBehandlingType(BehandlingType.REVURDERING)
            .medUngTestGrunnlag(ungTestscenario);

        var behandling = scenarioBuilder.buildOgLagreMedUng(ungTestRepositories);


        behandling.setBehandlingResultatType(BehandlingResultatType.INNVILGET);
        behandling.avsluttBehandling();


        return behandling;
    }


    @Override
    protected VedtaksbrevInnholdBygger lagVedtaksbrevInnholdBygger() {
        return new EndringBarnetilleggInnholdBygger(ungTestRepositories.ungdomsytelseGrunnlagRepository());
    }

    @Override
    protected String standardOverskrift() {
        return "Du får mer i ungdomsprogramytelse fordi du har fått barn";
    }

    @Override
    protected Behandling lagBehandlingMedStandardScenario() {
        LocalDate startdato = LocalDate.of(2025, 5, 3);
        LocalDate barnFødselsdato = LocalDate.of(2025, 5, 27);
        return lagStandardScenario(BrevScenarioer.endringBarnetillegg(startdato, barnFødselsdato));
    }



}


