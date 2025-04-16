package no.nav.ung.sak.formidling;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.ung.kodeverk.behandling.BehandlingResultatType;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.db.util.JpaExtension;
import no.nav.ung.sak.domene.person.pdl.AktørTjeneste;
import no.nav.ung.sak.formidling.innhold.InnvilgelseInnholdBygger;
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
import no.nav.ung.sak.ytelse.beregning.UngdomsytelseTilkjentYtelseUtleder;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.IOException;
import java.time.LocalDate;

import static no.nav.ung.sak.formidling.HtmlAssert.assertThatHtml;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for brevtekster for innvilgelse. Bruker html for å validere.
 * For manuell verifikasjon av pdf kan env variabel LAGRE_PDF brukes.
 */
@ExtendWith(CdiAwareExtension.class)
@ExtendWith(JpaExtension.class)
class BrevGenerererTjenesteNyInnvilgelseTest {

    private BrevGenerererTjeneste brevGenerererTjeneste;

    @Inject
    private EntityManager entityManager;
    private UngTestRepositories ungTestRepositories;

    PdlKlientFake pdlKlient = PdlKlientFake.medTilfeldigFnr();
    String fnr = pdlKlient.fnr();
    private TestInfo testInfo;


    @BeforeEach
    void setup(TestInfo testInfo) {
        this.testInfo = testInfo;
        ungTestRepositories = BrevUtils.lagAlleUngTestRepositories(entityManager);
        brevGenerererTjeneste = lagBrevGenererTjeneste();
    }

    @NotNull
    private BrevGenerererTjeneste lagBrevGenererTjeneste() {
        var repositoryProvider = ungTestRepositories.repositoryProvider();
        var tilkjentYtelseRepository = ungTestRepositories.tilkjentYtelseRepository();

        var ungdomsprogramPeriodeTjeneste = new UngdomsprogramPeriodeTjeneste(ungTestRepositories.ungdomsprogramPeriodeRepository());

        InnvilgelseInnholdBygger innvilgelseInnholdBygger = new InnvilgelseInnholdBygger(
            ungTestRepositories.ungdomsytelseGrunnlagRepository(),
            ungdomsprogramPeriodeTjeneste,
            new UngdomsytelseTilkjentYtelseUtleder(tilkjentYtelseRepository),
            repositoryProvider.getPersonopplysningRepository());

        var ungdomsytelseSøknadsperiodeTjeneste =
            new UngdomsytelseSøknadsperiodeTjeneste(ungTestRepositories.ungdomsytelseStartdatoRepository(), ungdomsprogramPeriodeTjeneste, repositoryProvider.getBehandlingRepository());

        DetaljertResultatUtlederImpl detaljertResultatUtleder = new DetaljertResultatUtlederImpl(
            new ProsessTriggerPeriodeUtleder(ungTestRepositories.prosessTriggereRepository(), ungdomsytelseSøknadsperiodeTjeneste),
            tilkjentYtelseRepository, repositoryProvider.getVilkårResultatRepository());

        VedtaksbrevRegler vedtaksbrevRegler = new VedtaksbrevRegler(
            repositoryProvider.getBehandlingRepository(), new UnitTestLookupInstanceImpl<>(innvilgelseInnholdBygger),
            detaljertResultatUtleder);

        return new BrevGenerererTjenesteImpl(
            repositoryProvider.getBehandlingRepository(),
            new AktørTjeneste(pdlKlient),
            new PdfGenKlient(),
            repositoryProvider.getPersonopplysningRepository(),
            vedtaksbrevRegler);
    }

    @Test()
    @DisplayName("Verifiserer formatering på overskrifter")
    void verifiserOverskrifter() {
        TestScenarioBuilder scenarioBuilder = BrevScenarioer.lagAvsluttetStandardBehandling(ungTestRepositories);

        var behandling = scenarioBuilder.getBehandling();

        GenerertBrev generertBrev = brevGenerererTjeneste.genererVedtaksbrevKunHtml((behandling.getId()));

        var brevtekst = generertBrev.dokument().html();

        VedtaksbrevVerifikasjon.verifiserStandardOverskrifter(brevtekst);


    }


    @DisplayName("Innvilgelse med riktig fom dato, maks antall dager, lav sats, grunnbeløp, hjemmel")
    @Test
    void standardInnvilgelse() {
        LocalDate fom = LocalDate.of(2024, 12, 1);
        var ungTestGrunnlag = BrevScenarioer.innvilget19år(fom);

        var behandling = lagScenario(ungTestGrunnlag);

        GenerertBrev generertBrev = genererVedtaksbrev(behandling.getId());
        assertThat(generertBrev.templateType()).isEqualTo(TemplateType.INNVILGELSE);

        var brevtekst = generertBrev.dokument().html();

        var forventet = VedtaksbrevVerifikasjon.medHeaderOgFooter(fnr,
            """
                Nav har innvilget søknaden din om ungdomsytelse \
                Du har rett til ungdomsytelse fra 1. desember 2024 i 260 dager. \
                Du får utbetalt 636 kroner dagen, før skatt. \
                Nav utbetaler pengene innen den 25. i hver måned. \
                Informasjon om utbetaling finner du under utbetalingsoversikten på "Min side". \
                Du får ungdomsytelse fordi du er med ungdomsprogrammet. \
                Ungdomsprogrammet skal sikre deg inntekt mens du samarbeider med veileder i Nav om tiltak som skal hjelpe deg med arbeid. \
                Utbetalingene fra Nav kan endre seg etterhvert som du får eller mister inntekt. \
                Det er derfor viktig at du melder i fra om endringer i din inntekt på nav.no/ungdomsytelse/endring og informerer veileder. \
                Hvis du ikke gir beskjed om endringer i inntekten, kan Nav kreve penger tilbake, så det er viktig å gi beskjed med en gang det skjer endringer. \
                Nav bruker grunnbeløpet på 124 028 kroner for å regne ut hvor mye du får. \
                Siden du er under 25 år så får du 1.33 ganger grunnbeløpet. \
                Nav regner med 260 virkedager per år utenom helger og ferie. \
                For å regne ut hva du får per dag, deles årsbeløpet på antall dager. \
                Det betyr at du har rett på 1.33 x 124 028 = 165 370 kroner i året. \
                Dette gir en dagsats på 636 kroner. \
                For å regne hva du får utbetalt i måneden ganges dagsatsen med antall virkedager i måneden. \
                Du kan regne ut hva du får for en måned samt se flere eksempler på utregninger på nav.no/ungdomsytelse. \
                Vedtaket er gjort etter arbeidsmarkedsloven § xx og forskrift om xxx § xx. \
                """);

        assertThatHtml(brevtekst)
            .asPlainTextIsEqualTo(forventet)
            .containsHtmlSubSequenceOnce(
                "<h1>Nav har innvilget søknaden din om ungdomsytelse</h1>"
            );

    }

    @Test
    void høySats() {
        LocalDate fom = LocalDate.of(2024, 12, 1);
        var ungTestGrunnlag = BrevScenarioer.innvilget27år(fom);

        var behandling = lagScenario(ungTestGrunnlag);

        GenerertBrev generertBrev = genererVedtaksbrev(behandling.getId());

        var brevtekst = generertBrev.dokument().html();

        var forventet = VedtaksbrevVerifikasjon.medHeaderOgFooter(fnr,
            """
                Nav har innvilget søknaden din om ungdomsytelse \
                Du har rett til ungdomsytelse fra 1. desember 2024 i 260 dager. \
                Du får utbetalt 954 kroner dagen, før skatt. \
                Nav utbetaler pengene innen den 25. i hver måned. \
                Informasjon om utbetaling finner du under utbetalingsoversikten på "Min side". \
                Du får ungdomsytelse fordi du er med ungdomsprogrammet. \
                Ungdomsprogrammet skal sikre deg inntekt mens du samarbeider med veileder i Nav om tiltak som skal hjelpe deg med arbeid. \
                Utbetalingene fra Nav kan endre seg etterhvert som du får eller mister inntekt. \
                Det er derfor viktig at du melder i fra om endringer i din inntekt på nav.no/ungdomsytelse/endring og informerer veileder. \
                Hvis du ikke gir beskjed om endringer i inntekten, kan Nav kreve penger tilbake, så det er viktig å gi beskjed med en gang det skjer endringer. \
                Nav bruker grunnbeløpet på 124 028 kroner for å regne ut hvor mye du får. \
                Siden du er over 25 år så får du 2 ganger grunnbeløpet. \
                Nav regner med 260 virkedager per år utenom helger og ferie. \
                For å regne ut hva du får per dag, deles årsbeløpet på antall dager. \
                Det betyr at du har rett på 2 x 124 028 = 248 056 kroner i året. \
                Dette gir en dagsats på 954 kroner. \
                For å regne hva du får utbetalt i måneden ganges dagsatsen med antall virkedager i måneden. \
                Du kan regne ut hva du får for en måned samt se flere eksempler på utregninger på nav.no/ungdomsytelse. \
                Vedtaket er gjort etter arbeidsmarkedsloven § xx og forskrift om xxx § xx. \
                """);

        assertThatHtml(brevtekst)
            .asPlainTextIsEqualTo(forventet)
            .containsHtmlSubSequenceOnce("<h1>Nav har innvilget søknaden din om ungdomsytelse</h1>");
    }

    @DisplayName("blir 29 i løpet av programmet og får mindre enn maks antall dager")
    @Test
    void høySatsMaksAlder6MndIProgrammet() {
        LocalDate fom = LocalDate.of(2024, 12, 1);
        var fødselsdato = LocalDate.of(1996, 5, 15); //Blir 29 etter 6 mnd/130 dager i programmet
        var ungTestGrunnlag = BrevScenarioer.innvilget29År(fom, fødselsdato);

        var forventet = VedtaksbrevVerifikasjon.medHeaderOgFooter(fnr,
            """
                Nav har innvilget søknaden din om ungdomsytelse \
                Du har rett til ungdomsytelse fra 1. desember 2024 i 130 dager. \
                Du får utbetalt 954 kroner dagen, før skatt. \
                Nav utbetaler pengene innen den 25. i hver måned. \
                Informasjon om utbetaling finner du under utbetalingsoversikten på "Min side". \
                Du får ungdomsytelse fordi du er med ungdomsprogrammet. \
                Ungdomsprogrammet skal sikre deg inntekt mens du samarbeider med veileder i Nav om tiltak som skal hjelpe deg med arbeid. \
                Utbetalingene fra Nav kan endre seg etterhvert som du får eller mister inntekt. \
                Det er derfor viktig at du melder i fra om endringer i din inntekt på nav.no/ungdomsytelse/endring og informerer veileder. \
                Hvis du ikke gir beskjed om endringer i inntekten, kan Nav kreve penger tilbake, så det er viktig å gi beskjed med en gang det skjer endringer. \
                Nav bruker grunnbeløpet på 124 028 kroner for å regne ut hvor mye du får. \
                Siden du er over 25 år så får du 2 ganger grunnbeløpet til måneden du fyller 29 år. \
                Nav regner med 260 virkedager per år utenom helger og ferie. \
                For å regne ut hva du får per dag, deles årsbeløpet på antall dager. \
                Det betyr at du har rett på 2 x 124 028 = 248 056 kroner i året. \
                Dette gir en dagsats på 954 kroner. \
                For å regne hva du får utbetalt i måneden ganges dagsatsen med antall virkedager i måneden. \
                Du kan regne ut hva du får for en måned samt se flere eksempler på utregninger på nav.no/ungdomsytelse. \
                Vedtaket er gjort etter arbeidsmarkedsloven § xx og forskrift om xxx § xx. \
                """);

        var behandling = lagScenario(ungTestGrunnlag);

        GenerertBrev generertBrev = genererVedtaksbrev(behandling.getId());

        var brevtekst = generertBrev.dokument().html();

        assertThatHtml(brevtekst)
            .asPlainTextIsEqualTo(forventet)
            .containsHtmlSubSequenceOnce(
                "<h1>Nav har innvilget søknaden din om ungdomsytelse</h1>"
            );
    }

    //dekker flere dagsatser også
    @Test
    void lavOgHøySats() {
        var fødselsdato = LocalDate.of(2000, 5, 15); //Blir 25 etter 6 mnd/130 dager i programmet
        var ungTestGrunnlag = BrevScenarioer.innvilget24ÅrSøkerPå25årsdagen(fødselsdato);
        var forventet = VedtaksbrevVerifikasjon.medHeaderOgFooter(fnr,
            """
                Nav har innvilget søknaden din om ungdomsytelse \
                Du har rett til ungdomsytelse fra 1. mai 2025 i 260 dager. \
                Fra 1. mai 2025 til 15. mai 2025 får du utbetalt 636 kroner dagen, før skatt. \
                Fra 16. mai 2025 til 31. mai 2025 får du utbetalt 954 kroner dagen, før skatt. \
                Nav utbetaler pengene innen den 25. i hver måned. \
                Informasjon om utbetaling finner du under utbetalingsoversikten på "Min side". \
                Du får ungdomsytelse fordi du er med ungdomsprogrammet. \
                Ungdomsprogrammet skal sikre deg inntekt mens du samarbeider med veileder i Nav om tiltak som skal hjelpe deg med arbeid. \
                Utbetalingene fra Nav kan endre seg etterhvert som du får eller mister inntekt. \
                Det er derfor viktig at du melder i fra om endringer i din inntekt på nav.no/ungdomsytelse/endring og informerer veileder. \
                Hvis du ikke gir beskjed om endringer i inntekten, kan Nav kreve penger tilbake, så det er viktig å gi beskjed med en gang det skjer endringer. \
                Nav bruker grunnbeløpet på 124 028 kroner for å regne ut hvor mye du får. \
                Du får 1.33 ganger grunnbeløpet mens du er under 25 år og 2 ganger grunnbeløpet fra måneden etter du fyller 25 år. \
                Nav regner med 260 virkedager per år utenom helger og ferie. \
                For å regne ut hva du får per dag, deles årsbeløpet på antall dager. \
                Fra 1. mai 2025 til 15. mai 2025 har du rett på 1.33 x 124 028 = 165 370 kroner i årsbeløp. \
                Dette gir en dagsats på 636 kroner i perioden. \
                Fra 16. mai 2025 til 31. mai 2025 har du rett på 2 x 124 028 = 248 056 kroner i årsbeløp. \
                Dette gir en dagsats på 954 kroner i perioden. \
                For å regne hva du får utbetalt i måneden ganges dagsatsen med antall virkedager i måneden. \
                Du kan regne ut hva du får for en måned samt se flere eksempler på utregninger på nav.no/ungdomsytelse. \
                Vedtaket er gjort etter arbeidsmarkedsloven § xx og forskrift om xxx § xx. \
                """);


        var behandling = lagScenario(ungTestGrunnlag);

        GenerertBrev generertBrev = genererVedtaksbrev(behandling.getId());

        var brevtekst = generertBrev.dokument().html();

        assertThatHtml(brevtekst)
            .asPlainTextIsEqualTo(forventet)
            .containsHtmlSubSequenceOnce(
                "<h1>Nav har innvilget søknaden din om ungdomsytelse</h1>"
            );
    }

    @Test
    void pdfStrukturTest() throws IOException {
        TestScenarioBuilder scenarioBuilder = BrevScenarioer
            .lagAvsluttetStandardBehandling(ungTestRepositories);

        var behandling = scenarioBuilder.getBehandling();

        GenerertBrev generertBrev = brevGenerererTjeneste.genererVedtaksbrev(behandling.getId());

        var pdf = generertBrev.dokument().pdf();

        try (PDDocument pdDocument = Loader.loadPDF(pdf)) {
            assertThat(pdDocument.getNumberOfPages()).isEqualTo(2);
            String pdfTekst = new PDFTextStripper().getText(pdDocument);
            assertThat(pdfTekst).isNotEmpty();
            assertThat(pdfTekst).contains("Nav har innvilget søknaden din om ungdomsytelse");
        }

    }

    private Behandling lagScenario(UngTestScenario ungTestscenario) {
        TestScenarioBuilder scenarioBuilder = TestScenarioBuilder.builderMedSøknad().medUngTestGrunnlag(ungTestscenario);

        var behandling = scenarioBuilder.buildOgLagreMedUng(
            ungTestRepositories);
        behandling.setBehandlingResultatType(BehandlingResultatType.INNVILGET);
        behandling.avsluttBehandling();
        return behandling;
    }


    private GenerertBrev genererVedtaksbrev(Long behandlingId) {
        return BrevUtils.genererBrevOgLagreHvisEnabled(testInfo, behandlingId, brevGenerererTjeneste);
    }


}


