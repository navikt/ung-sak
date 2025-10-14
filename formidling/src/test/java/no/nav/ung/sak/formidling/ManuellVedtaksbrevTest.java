package no.nav.ung.sak.formidling;

import no.nav.ung.kodeverk.behandling.BehandlingType;
import no.nav.ung.kodeverk.dokument.DokumentMalType;
import no.nav.ung.kodeverk.formidling.TemplateType;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.formidling.VedtaksbrevValgEntitet;
import no.nav.ung.sak.behandlingslager.formidling.VedtaksbrevValgRepository;
import no.nav.ung.sak.formidling.scenarioer.AvslagScenarioer;
import no.nav.ung.sak.test.util.behandling.TestScenarioBuilder;
import no.nav.ung.sak.test.util.behandling.UngTestScenario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static no.nav.ung.sak.formidling.HtmlAssert.assertThatHtml;
import static org.assertj.core.api.Assertions.assertThat;

class ManuellVedtaksbrevTest extends AbstractVedtaksbrevInnholdByggerTest {

    private VedtaksbrevValgRepository vedtaksbrevValgRepository;

    ManuellVedtaksbrevTest() {
        super(1, "Manuell skrevet overskrift");
    }


    @BeforeEach
    void setup() {
        vedtaksbrevValgRepository = new VedtaksbrevValgRepository(entityManager);
    }

    @DisplayName("Standard manuell brev")
    @Test
    void standardManuellBrev() {

        LocalDate fom = LocalDate.of(2025, 8, 1);
        UngTestScenario ungTestGrunnlag = AvslagScenarioer.avslagAlder(fom);
        var behandling = TestScenarioBuilder.builderMedSøknad()
            .medBehandlingType(BehandlingType.FØRSTEGANGSSØKNAD)
            .medUngTestGrunnlag(ungTestGrunnlag)
            .buildOgLagreMedUng(ungTestRepositories);

        vedtaksbrevValgRepository.lagre(new VedtaksbrevValgEntitet(
            behandling.getId(),
            DokumentMalType.MANUELT_VEDTAK_DOK,
            true,
            false,
            "<h1>Du får ungdomsprogramytelse</h1>" +
                "<p>Du skal få penger, men du har tjent for mye og vi vil derfor kreve tilbake et beløp.</p>" +
                "<p>Du får mer informasjon om dette i nærmeste fremtid.</p>"));

        var forventet = VedtaksbrevVerifikasjon.medHeaderOgFooterManuell(fnr,
            "Du får ungdomsprogramytelse " +
                "Du skal få penger, men du har tjent for mye og vi vil derfor kreve tilbake et beløp. " +
                "Du får mer informasjon om dette i nærmeste fremtid. "
        );


        GenerertBrev generertBrev = genererVedtaksbrev(behandling.getId());
        assertThat(generertBrev.templateType()).isEqualTo(TemplateType.MANUELT_VEDTAKSBREV);

        var brevtekst = generertBrev.dokument().html();

        assertThatHtml(brevtekst)
            .asPlainTextIsEqualTo(forventet)
            .containsHtmlSubSequenceOnce(
                "<h1>Du får ungdomsprogramytelse</h1>"
            );

    }


    @Override
    protected Behandling lagScenarioForFellesTester() {
        LocalDate fom = LocalDate.of(2025, 8, 1);
        UngTestScenario ungTestGrunnlag = AvslagScenarioer.avslagAlder(fom);
        var behandling = TestScenarioBuilder.builderMedSøknad()
            .medBehandlingType(BehandlingType.FØRSTEGANGSSØKNAD)
            .medUngTestGrunnlag(ungTestGrunnlag)
            .buildOgLagreMedUng(ungTestRepositories);


        behandling.avsluttBehandling();

        vedtaksbrevValgRepository.lagre(new VedtaksbrevValgEntitet(
            behandling.getId(),
            DokumentMalType.MANUELT_VEDTAK_DOK,
            true,
            false,
            "<h1>Manuell skrevet overskrift</h1>"));
        return behandling;
    }
}


