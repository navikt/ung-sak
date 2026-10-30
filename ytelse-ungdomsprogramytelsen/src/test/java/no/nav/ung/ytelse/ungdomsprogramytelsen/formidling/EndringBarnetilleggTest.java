package no.nav.ung.ytelse.ungdomsprogramytelsen.formidling;

import jakarta.enterprise.inject.Any;
import jakarta.inject.Inject;
import no.nav.ung.kodeverk.behandling.BehandlingResultatType;
import no.nav.ung.kodeverk.behandling.BehandlingType;
import no.nav.ung.kodeverk.dokument.DokumentMalType;
import no.nav.ung.kodeverk.formidling.TemplateType;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.ytelse.UngdomsytelseGrunnlagRepository;
import no.nav.ung.sak.formidling.GenerertBrev;
import no.nav.ung.sak.formidling.vedtak.VedtaksbrevGenerererInput;
import no.nav.ung.sak.formidling.vedtak.VedtaksbrevGenerererTjeneste;
import no.nav.ung.sak.formidling.vedtak.regler.Vedtaksbrev;
import no.nav.ung.sak.formidling.vedtak.regler.VedtaksbrevEgenskaper;
import no.nav.ung.sak.formidling.vedtak.resultat.DefaultDetaljertResultatTidslinjeUtleder;
import no.nav.ung.ytelse.ungdomsprogramytelsen.formidling.innhold.EndringBarnetilleggInnholdBygger;
import no.nav.ung.ytelse.ungdomsprogramytelsen.formidling.scenarioer.EndringBarnetilleggScenarioer;
import no.nav.ung.sak.test.util.behandling.ungdomsprogramytelse.TestScenarioBuilder;
import no.nav.ung.sak.test.util.behandling.ungdomsprogramytelse.UngTestScenario;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static no.nav.ung.ytelse.ungdomsprogramytelsen.formidling.HtmlAssert.assertThatHtml;
import static org.assertj.core.api.Assertions.assertThat;

class EndringBarnetilleggTest extends AbstractUngdomsytelseVedtaksbrevInnholdByggerTest {

    @Inject
    @Any
    private DefaultDetaljertResultatTidslinjeUtleder detaljertResultatTidslinjeUtleder;

    @Inject
    private VedtaksbrevGenerererTjeneste vedtaksbrevGenerererTjeneste;

    EndringBarnetilleggTest() {
        super(1, "Du får mer i ungdomsprogramytelse fordi du har fått barn");
    }

    @Test
    void standardEndringBarnetillegg() {
        LocalDate startdato = LocalDate.of(2025, 5, 3);
        LocalDate barnFødselsdato = LocalDate.of(2025, 5, 27);
        UngTestScenario ungTestGrunnlag = EndringBarnetilleggScenarioer.endringBarnetillegg(startdato, barnFødselsdato);

        var forventet = VedtaksbrevVerifikasjon.medHeaderOgFooter(fnr,
            """
                Du får mer i ungdomsprogramytelse fordi du har fått barn \
                Du får 37 kroner i barnetillegg per dag fra og med 27. mai 2025, utenom lørdag og søndag. \
                Det er fordi du fikk barn denne datoen. \
                Når du har barn, får du et barnetillegg på 37 kroner per dag for hvert barn du har. \
                Vedtaket er gjort etter arbeidsmarkedsloven §§ 12 tredje ledd og 13 fjerde ledd og forskrift om forsøk med ungdomsprogram og ungdomsprogramytelse § 8 jf. § 3 og § 9 tredje ledd. \
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
        UngTestScenario ungTestGrunnlag = EndringBarnetilleggScenarioer.endringBarnetilleggFlereBarn(startdato, barnFødselsdato);

        var forventet = VedtaksbrevVerifikasjon.medHeaderOgFooter(fnr,
            """
                Du får mer i ungdomsprogramytelse fordi du har fått barn \
                Du får 111 kroner i barnetillegg per dag fra og med 27. mai 2025, utenom lørdag og søndag. \
                Det er fordi du fikk barn denne datoen. \
                Når du har barn, får du et barnetillegg på 37 kroner per dag for hvert barn du har. \
                Vedtaket er gjort etter arbeidsmarkedsloven §§ 12 tredje ledd og 13 fjerde ledd og forskrift om forsøk med ungdomsprogram og ungdomsprogramytelse § 8 jf. § 3 og § 9 tredje ledd. \
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
    void barnFødtFørStartdato_girStartdatoOgEgenBegrunnelseMedToggle() {
        LocalDate startdato = LocalDate.of(2025, 5, 3);
        LocalDate barnFødselsdato = LocalDate.of(2025, 4, 10);
        var behandling = lagStandardScenario(EndringBarnetilleggScenarioer.endringBarnetilleggBarnFødtFørStartdato(startdato, barnFødselsdato));

        var forventet = VedtaksbrevVerifikasjon.medHeaderOgFooter(fnr,
            """
                Du får mer i ungdomsprogramytelse fordi du har fått barn \
                Du får 37 kroner i barnetillegg per dag fra og med 3. mai 2025, utenom lørdag og søndag. \
                Det er fordi du fikk barn før du startet i ungdomsprogrammet. \
                Når du har barn, får du et barnetillegg på 37 kroner per dag for hvert barn du har. \
                Vedtaket er gjort etter arbeidsmarkedsloven §§ 12 tredje ledd og 13 fjerde ledd og forskrift om forsøk med ungdomsprogram og ungdomsprogramytelse § 8 jf. § 3 og § 9 tredje ledd. \
                """
        );

        var generertBrev = genererVedtaksbrevMedToggleForFødselFørStartdato(behandling);

        assertThatHtml(generertBrev.dokument().html()).asPlainTextIsEqualTo(forventet);
    }

    @Test
    void barnFødtEtterStartdato_girFødselsdatoOgUendretBegrunnelseMedToggle() {
        LocalDate startdato = LocalDate.of(2025, 5, 3);
        LocalDate barnFødselsdato = LocalDate.of(2025, 5, 27);
        var behandling = lagStandardScenario(EndringBarnetilleggScenarioer.endringBarnetillegg(startdato, barnFødselsdato));

        var forventet = VedtaksbrevVerifikasjon.medHeaderOgFooter(fnr,
            """
                Du får mer i ungdomsprogramytelse fordi du har fått barn \
                Du får 37 kroner i barnetillegg per dag fra og med 27. mai 2025, utenom lørdag og søndag. \
                Det er fordi du fikk barn denne datoen. \
                Når du har barn, får du et barnetillegg på 37 kroner per dag for hvert barn du har. \
                Vedtaket er gjort etter arbeidsmarkedsloven §§ 12 tredje ledd og 13 fjerde ledd og forskrift om forsøk med ungdomsprogram og ungdomsprogramytelse § 8 jf. § 3 og § 9 tredje ledd. \
                """
        );

        var generertBrev = genererVedtaksbrevMedToggleForFødselFørStartdato(behandling);

        assertThatHtml(generertBrev.dokument().html()).asPlainTextIsEqualTo(forventet);
    }

    private GenerertBrev genererVedtaksbrevMedToggleForFødselFørStartdato(Behandling behandling) {
        var bygger = new EndringBarnetilleggInnholdBygger(new UngdomsytelseGrunnlagRepository(entityManager), true);
        var vedtaksbrev = new Vedtaksbrev(DokumentMalType.ENDRING_BARNETILLEGG, bygger, VedtaksbrevEgenskaper.kanRedigere(false), "test");
        var tidslinje = detaljertResultatTidslinjeUtleder.utledDetaljertResultat(behandling);

        var generertBrev = vedtaksbrevGenerererTjeneste.genererAutomatiskVedtaksbrev(
            new VedtaksbrevGenerererInput(behandling.getId(), vedtaksbrev, tidslinje, true));

        assertThat(generertBrev.templateType()).isEqualTo(TemplateType.ENDRING_BARNETILLEGG);
        return generertBrev;
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
    protected Behandling lagScenarioForFellesTester() {
        LocalDate startdato = LocalDate.of(2025, 5, 3);
        LocalDate barnFødselsdato = LocalDate.of(2025, 5, 27);
        return lagStandardScenario(EndringBarnetilleggScenarioer.endringBarnetillegg(startdato, barnFødselsdato));
    }



}


