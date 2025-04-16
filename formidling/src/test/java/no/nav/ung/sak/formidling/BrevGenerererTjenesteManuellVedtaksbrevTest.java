package no.nav.ung.sak.formidling;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.ung.kodeverk.behandling.BehandlingResultatType;
import no.nav.ung.kodeverk.behandling.BehandlingType;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.ung.sak.behandlingslager.behandling.aksjonspunkt.AksjonspunktTestSupport;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.formidling.VedtaksbrevValgEntitet;
import no.nav.ung.sak.behandlingslager.formidling.VedtaksbrevValgRepository;
import no.nav.ung.sak.db.util.JpaExtension;
import no.nav.ung.sak.domene.person.pdl.AktørTjeneste;
import no.nav.ung.sak.formidling.innhold.ManuellVedtaksbrevInnholdBygger;
import no.nav.ung.sak.formidling.innhold.VedtaksbrevInnholdBygger;
import no.nav.ung.sak.formidling.pdfgen.PdfGenKlient;
import no.nav.ung.sak.formidling.template.TemplateType;
import no.nav.ung.sak.formidling.vedtak.DetaljertResultatUtlederImpl;
import no.nav.ung.sak.perioder.ProsessTriggerPeriodeUtleder;
import no.nav.ung.sak.perioder.UngdomsytelseSøknadsperiodeTjeneste;
import no.nav.ung.sak.test.util.UngTestRepositories;
import no.nav.ung.sak.test.util.UnitTestLookupInstanceImpl;
import no.nav.ung.sak.test.util.behandling.TestScenarioBuilder;
import no.nav.ung.sak.test.util.behandling.UngTestScenario;
import no.nav.ung.sak.ungdomsprogram.UngdomsprogramPeriodeTjeneste;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.IOException;
import java.time.LocalDate;

import static no.nav.ung.sak.formidling.HtmlAssert.assertThatHtml;
import static org.assertj.core.api.Assertions.assertThat;

// TODO test som først får automatisk brev som redigeres.

@ExtendWith(CdiAwareExtension.class)
@ExtendWith(JpaExtension.class)
class BrevGenerererTjenesteManuellVedtaksbrevTest {

    private BrevGenerererTjeneste brevGenerererTjeneste;

    @Inject
    private EntityManager entityManager;
    private UngTestRepositories ungTestRepositories;

    PdlKlientFake pdlKlient = PdlKlientFake.medTilfeldigFnr();
    String fnr = pdlKlient.fnr();
    private TestInfo testInfo;
    private VedtaksbrevValgRepository vedtaksbrevValgRepository;


    @BeforeEach
    void setup(TestInfo testInfo) {
        this.testInfo = testInfo;
        vedtaksbrevValgRepository = new VedtaksbrevValgRepository(entityManager);
        ungTestRepositories = BrevUtils.lagAlleUngTestRepositories(entityManager);
        brevGenerererTjeneste = lagBrevGenererTjeneste();
    }

    private BrevGenerererTjeneste lagBrevGenererTjeneste() {
        var repositoryProvider = ungTestRepositories.repositoryProvider();

        UngdomsprogramPeriodeTjeneste ungdomsprogramPeriodeTjeneste = new UngdomsprogramPeriodeTjeneste(ungTestRepositories.ungdomsprogramPeriodeRepository());

        BehandlingRepository behandlingRepository = repositoryProvider.getBehandlingRepository();

        var detaljertResultatUtleder = new DetaljertResultatUtlederImpl(
            new ProsessTriggerPeriodeUtleder(ungTestRepositories.prosessTriggereRepository(), new UngdomsytelseSøknadsperiodeTjeneste(ungTestRepositories.ungdomsytelseStartdatoRepository(), ungdomsprogramPeriodeTjeneste, behandlingRepository)),
            ungTestRepositories.tilkjentYtelseRepository(), repositoryProvider.getVilkårResultatRepository());

        Instance<VedtaksbrevInnholdBygger> innholdByggere = new UnitTestLookupInstanceImpl<>(new ManuellVedtaksbrevInnholdBygger(
            vedtaksbrevValgRepository
        ));

        return new BrevGenerererTjenesteImpl(
            behandlingRepository,
            new AktørTjeneste(pdlKlient),
            new PdfGenKlient(),
            repositoryProvider.getPersonopplysningRepository(),
            new VedtaksbrevRegler(
                behandlingRepository, innholdByggere, detaljertResultatUtleder),
            ungTestRepositories.vedtaksbrevValgRepository(),
            new ManuellVedtaksbrevInnholdBygger(ungTestRepositories.vedtaksbrevValgRepository()));
    }

    @Test()
    @DisplayName("Verifiserer formatering på overskrifter")
    void verifiserOverskrifter() {

        var behandling = lagScenarioMedAksjonspunktSomGirKunManuellBrev();

        vedtaksbrevValgRepository.lagre(new VedtaksbrevValgEntitet(
            behandling.getId(),
            true,
            false,
            "<h1>Manuell skrevet overskrift</h1>"
        ));

        Long behandlingId = (behandling.getId());
        GenerertBrev generertBrev = brevGenerererTjeneste.genererVedtaksbrev(behandlingId, true);

        var brevtekst = generertBrev.dokument().html();

        VedtaksbrevVerifikasjon.verifiserStandardOverskrifter(brevtekst);

    }

    @DisplayName("Standard manuell brev")
    @Test
    void standardManuellBrev() {
        LocalDate fom = LocalDate.of(2024, 12, 1);

        var behandling = lagScenario(
            BrevScenarioer.endring0KrInntekt_19år(fom), AksjonspunktDefinisjon.MANUELL_TILKJENT_YTELSE);

        vedtaksbrevValgRepository.lagre(new VedtaksbrevValgEntitet(
            behandling.getId(),
            true,
            false,
            "<h1>Nav har innvilget søknaden din om ungdomsytelse</h1>" +
                "<p>Du skal få penger, men du har tjent for mye og vi vil derfor kreve tilbake et beløp.</p>" +
                "<p>Du får mer informasjon om dette i nærmeste fremtid.</p>"
        ));

        var forventet = VedtaksbrevVerifikasjon.medHeaderOgFooter(fnr,
            "Nav har innvilget søknaden din om ungdomsytelse " +
                "Du skal få penger, men du har tjent for mye og vi vil derfor kreve tilbake et beløp. " +
                "Du får mer informasjon om dette i nærmeste fremtid. "
        );



        GenerertBrev generertBrev = genererVedtaksbrev(behandling.getId());
        assertThat(generertBrev.templateType()).isEqualTo(TemplateType.MANUELL_VEDTAKSBREV);

        var brevtekst = generertBrev.dokument().html();

        assertThatHtml(brevtekst)
            .asPlainTextIsEqualTo(forventet)
            .containsHtmlSubSequenceOnce(
                "<h1>Nav har innvilget søknaden din om ungdomsytelse</h1>"
            );

    }

    @Test
    void pdfStrukturTest() throws IOException {
        var behandling = lagScenarioMedAksjonspunktSomGirKunManuellBrev();

        vedtaksbrevValgRepository.lagre(new VedtaksbrevValgEntitet(
            behandling.getId(),
            true,
            false,
            "<h1>Manuell skrevet overskrift</h1>"
        ));

        GenerertBrev generertBrev = brevGenerererTjeneste.genererVedtaksbrev(behandling.getId(), true);

        var pdf = generertBrev.dokument().pdf();

        try (PDDocument pdDocument = Loader.loadPDF(pdf)) {
            assertThat(pdDocument.getNumberOfPages()).isEqualTo(1);
            String pdfTekst = new PDFTextStripper().getText(pdDocument);
            assertThat(pdfTekst).isNotEmpty();
            assertThat(pdfTekst).contains("Manuell skrevet overskrift");
        }

    }

    private Behandling lagScenarioMedAksjonspunktSomGirKunManuellBrev() {
        LocalDate fom = LocalDate.of(2024, 12, 1);

        return lagScenario(
            BrevScenarioer.endring0KrInntekt_19år(fom),
            AksjonspunktDefinisjon.MANUELL_TILKJENT_YTELSE
        );
    }

    private Behandling lagScenario(UngTestScenario ungTestscenario, AksjonspunktDefinisjon aksjonspunktDefinisjon) {
        TestScenarioBuilder scenarioBuilder = TestScenarioBuilder.builderMedSøknad()
            .medBehandlingType(BehandlingType.REVURDERING)
            .medUngTestGrunnlag(ungTestscenario);

        if (aksjonspunktDefinisjon != null) {
            scenarioBuilder.leggTilAksjonspunkt(aksjonspunktDefinisjon, null);
        }


        var behandling = scenarioBuilder.buildOgLagreMedUng(ungTestRepositories);
        behandling.setBehandlingResultatType(BehandlingResultatType.INNVILGET);


        if (aksjonspunktDefinisjon != null) {
            Aksjonspunkt aksjonspunkt = behandling.getAksjonspunktFor(aksjonspunktDefinisjon);
            new AksjonspunktTestSupport().setTilUtført(aksjonspunkt, "utført");
            BehandlingRepository behandlingRepository = ungTestRepositories.repositoryProvider().getBehandlingRepository();
            behandlingRepository.lagre(behandling, behandlingRepository.taSkriveLås(behandling));
        }

        behandling.avsluttBehandling();

        return behandling;
    }


    private GenerertBrev genererVedtaksbrev(Long behandlingId) {
        return BrevUtils.genererBrevOgLagreHvisEnabled(testInfo, behandlingId, brevGenerererTjeneste);
    }


}


