package no.nav.ung.sak.formidling;

import no.nav.ung.kodeverk.behandling.BehandlingResultatType;
import no.nav.ung.kodeverk.behandling.BehandlingType;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.ung.kodeverk.formidling.TemplateType;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.ung.sak.behandlingslager.behandling.aksjonspunkt.AksjonspunktTestSupport;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.formidling.VedtaksbrevValgEntitet;
import no.nav.ung.sak.behandlingslager.formidling.VedtaksbrevValgRepository;
import no.nav.ung.sak.formidling.innhold.ManueltVedtaksbrevInnholdBygger;
import no.nav.ung.sak.formidling.innhold.VedtaksbrevInnholdBygger;
import no.nav.ung.sak.test.util.behandling.TestScenarioBuilder;
import no.nav.ung.sak.test.util.behandling.UngTestScenario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static no.nav.ung.sak.formidling.HtmlAssert.assertThatHtml;
import static org.assertj.core.api.Assertions.assertThat;

// TODO test som først får automatisk brev som redigeres.

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
        LocalDate fom = LocalDate.of(2024, 12, 1);

        var behandling = lagScenario(
            BrevScenarioer.endring0KrInntekt_19år(fom), AksjonspunktDefinisjon.FORESLÅ_VEDTAK_MANUELT);

        vedtaksbrevValgRepository.lagre(new VedtaksbrevValgEntitet(
            behandling.getId(),
            true,
            false,
            "<h1>Du får ungdomsprogramytelse</h1>" +
                "<p>Du skal få penger, men du har tjent for mye og vi vil derfor kreve tilbake et beløp.</p>" +
                "<p>Du får mer informasjon om dette i nærmeste fremtid.</p>"
        ));

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

    private Behandling lagScenario(UngTestScenario ungTestscenario, AksjonspunktDefinisjon aksjonspunktDefinisjon) {
        TestScenarioBuilder scenarioBuilder = TestScenarioBuilder.builderMedSøknad()
            .medBehandlingType(BehandlingType.REVURDERING)
            .medUngTestGrunnlag(ungTestscenario);

        scenarioBuilder.leggTilAksjonspunkt(aksjonspunktDefinisjon, null);


        var behandling = scenarioBuilder.buildOgLagreMedUng(ungTestRepositories);
        behandling.setBehandlingResultatType(BehandlingResultatType.INNVILGET);

        Aksjonspunkt aksjonspunkt = behandling.getAksjonspunktFor(aksjonspunktDefinisjon);
        new AksjonspunktTestSupport().setTilUtført(aksjonspunkt, "utført");
        BehandlingRepository behandlingRepository = ungTestRepositories.repositoryProvider().getBehandlingRepository();
        behandlingRepository.lagre(behandling, behandlingRepository.taSkriveLås(behandling));

        behandling.avsluttBehandling();

        return behandling;
    }


    @Override
    protected VedtaksbrevInnholdBygger lagVedtaksbrevInnholdBygger() {
        return new ManueltVedtaksbrevInnholdBygger(vedtaksbrevValgRepository);
    }

    @Override
    protected Behandling lagScenarioForFellesTester() {
        LocalDate fom = LocalDate.of(2024, 12, 1);
        Behandling behandling = lagScenario(BrevScenarioer.endring0KrInntekt_19år(fom), AksjonspunktDefinisjon.FORESLÅ_VEDTAK_MANUELT);

        vedtaksbrevValgRepository.lagre(new VedtaksbrevValgEntitet(
            behandling.getId(),
            true,
            false,
            "<h1>Manuell skrevet overskrift</h1>"
        ));
        return behandling;
    }
}


