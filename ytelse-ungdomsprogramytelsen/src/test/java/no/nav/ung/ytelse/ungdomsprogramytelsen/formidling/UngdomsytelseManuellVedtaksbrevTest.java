package no.nav.ung.ytelse.ungdomsprogramytelsen.formidling;

import no.nav.ung.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.ung.kodeverk.dokument.DokumentMalType;
import no.nav.ung.kodeverk.formidling.TemplateType;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.formidling.VedtaksbrevValgEntitet;
import no.nav.ung.sak.behandlingslager.formidling.VedtaksbrevValgRepository;
import no.nav.ung.sak.formidling.GenerertBrev;
import no.nav.ung.sak.test.util.behandling.ungdomsprogramytelse.UngTestScenario;
import no.nav.ung.ytelse.ungdomsprogramytelsen.formidling.scenarioer.AvslagScenarioer;
import no.nav.ung.ytelse.ungdomsprogramytelsen.formidling.scenarioer.BrevScenarioerUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static no.nav.ung.ytelse.ungdomsprogramytelsen.formidling.HtmlAssert.assertThatHtml;
import static org.assertj.core.api.Assertions.assertThat;

class UngdomsytelseManuellVedtaksbrevTest extends AbstractUngdomsytelseVedtaksbrevInnholdByggerTest {

    private VedtaksbrevValgRepository vedtaksbrevValgRepository;

    UngdomsytelseManuellVedtaksbrevTest() {
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
        var behandling = BrevScenarioerUtils.lagAvsluttetBehandlingMedAP(
            ungTestGrunnlag, ungTestRepositories, AksjonspunktDefinisjon.FATTER_VEDTAK
        );

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
        var behandling = BrevScenarioerUtils.lagAvsluttetBehandlingMedAP(
            ungTestGrunnlag, ungTestRepositories, AksjonspunktDefinisjon.FATTER_VEDTAK
        );

        vedtaksbrevValgRepository.lagre(new VedtaksbrevValgEntitet(
            behandling.getId(),
            DokumentMalType.MANUELT_VEDTAK_DOK,
            true,
            false,
            "<h1>Manuell skrevet overskrift</h1>"));
        return behandling;
    }
}


