package no.nav.ung.sak.formidling;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.ung.kodeverk.behandling.BehandlingResultatType;
import no.nav.ung.kodeverk.formidling.TemplateType;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.db.util.JpaExtension;
import no.nav.ung.sak.domene.person.pdl.AktørTjeneste;
import no.nav.ung.sak.formidling.innhold.InnvilgelseInnholdBygger;
import no.nav.ung.sak.formidling.innhold.ManuellVedtaksbrevInnholdBygger;
import no.nav.ung.sak.formidling.pdfgen.PdfGenKlient;
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
            vedtaksbrevRegler,
            ungTestRepositories.vedtaksbrevValgRepository(), new ManuellVedtaksbrevInnholdBygger(ungTestRepositories.vedtaksbrevValgRepository()));
    }

    @Test()
    @DisplayName("Verifiserer formatering på overskrifter")
    void verifiserOverskrifter() {
        TestScenarioBuilder scenarioBuilder = BrevScenarioer.lagAvsluttetStandardBehandling(ungTestRepositories);

        var behandling = scenarioBuilder.getBehandling();

        Long behandlingId = (behandling.getId());
        GenerertBrev generertBrev = brevGenerererTjeneste.genererVedtaksbrevForBehandling(behandlingId, true);

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
                Du får ungdomsprogramytelse \
                Fra 1. desember 2024 får du ungdomsprogramytelse på 636 kroner per dag utenom lørdag og søndag. \
                Pengene får du utbetalt én gang i måneden før den 10. i måneden. \
                Den første utbetalingen får du måneden etter at du begynner i ungdomsprogrammet. \
                Pengene du får, blir det trukket skatt av. \
                Du finner mer informasjon om utbetalingen hvis du logger inn på Min side på nav.no. \
                \
                Hvorfor får du ungdomsprogramytelsen? \
                Du får ytelsen fordi du er med i ungdomsprogrammet. \
                Ytelsen gir deg en inntekt mens du deltar i ungdomsprogrammet. \
                Du får penger gjennom ytelsen så lenge du er i ungdomsprogrammet, men du kan som hovedregel ikke få penger i mer enn ett år. \
                Vedtaket er gjort etter arbeidsmarkedsloven § xx og forskrift om xxx § xx. \
                \
                Hvordan regner vi oss fram til hvor mye penger du har rett på? \
                Når Nav regner ut hvor mye penger du kan ha rett på, bruker vi en bestemt sum som heter grunnbeløpet. \
                Grunnbeløpet er bestemt av Stortinget, og det endrer seg hvert år. \
                Når du er under 25 år, bruker vi grunnbeløpet ganger 1,33. \
                Det vil si 165 370 kroner i året. \
                Denne summen deler vi på 260 dager, fordi du ikke får penger for lørdager og søndager. \
                Det vil si at du har rett på 636 kroner per dag. \
                Slik regner vi ut hvor mye penger du kan får per dag. \
                Se eksempler i Ungdomsportalen på hvordan vi regner ut hvor mye penger du har rett på. \
                \
                Meld fra til oss hvis du har inntekt i tillegg til ungdomsprogramytelsen \
                Hvis du har en annen inntekt i tillegg til ytelsen, er det veldig viktig at du sier fra til oss om det. \
                Du får en SMS den 1. hver måned. \
                Når du har fått SMS-en, logger du inn på Min side på nav.no og gir oss beskjed om hva du har hatt i inntekt måneden før. \
                Når du har en inntekt, får du mindre penger gjennom ungdomsprogramytelsen. \
                Likevel får du til sammen mer penger når du både har en inntekt og får ytelsen, enn hvis du bare får ytelsen. \
                Se hvordan du skal gi beskjed om inntekten din i Ungdomsportalen. \
                """);

        assertThatHtml(brevtekst)
            .asPlainTextIsEqualTo(forventet)
            .containsHtmlSubSequenceOnce(
                "<h1>Du får ungdomsprogramytelse</h1>",
                "<h2>Hvorfor får du ungdomsprogramytelsen?</h2>",
                "<h2>Hvordan regner vi oss fram til hvor mye penger du har rett på?</h2>",
                "<h2>Meld fra til oss hvis du har inntekt i tillegg til ungdomsprogramytelsen</h2>"
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
                Du får ungdomsprogramytelse \
                Fra 1. desember 2024 får du ungdomsprogramytelse på 954 kroner per dag utenom lørdag og søndag. \
                Pengene får du utbetalt én gang i måneden før den 10. i måneden. \
                Den første utbetalingen får du måneden etter at du begynner i ungdomsprogrammet. \
                Pengene du får, blir det trukket skatt av. \
                Du finner mer informasjon om utbetalingen hvis du logger inn på Min side på nav.no. \
                \
                Hvorfor får du ungdomsprogramytelsen? \
                Du får ytelsen fordi du er med i ungdomsprogrammet. \
                Ytelsen gir deg en inntekt mens du deltar i ungdomsprogrammet. \
                Du får penger gjennom ytelsen så lenge du er i ungdomsprogrammet, men du kan som hovedregel ikke få penger i mer enn ett år. \
                Vedtaket er gjort etter arbeidsmarkedsloven § xx og forskrift om xxx § xx. \
                \
                Hvordan regner vi oss fram til hvor mye penger du har rett på? \
                Når Nav regner ut hvor mye penger du kan ha rett på, bruker vi en bestemt sum som heter grunnbeløpet. \
                Grunnbeløpet er bestemt av Stortinget, og det endrer seg hvert år. \
                Når du er over 25 år, bruker vi grunnbeløpet ganger 2,00. \
                Det vil si 248 056 kroner i året. \
                Denne summen deler vi på 260 dager, fordi du ikke får penger for lørdager og søndager. \
                Det vil si at du har rett på 954 kroner per dag. \
                Slik regner vi ut hvor mye penger du kan får per dag. \
                Se eksempler i Ungdomsportalen på hvordan vi regner ut hvor mye penger du har rett på. \
                \
                Meld fra til oss hvis du har inntekt i tillegg til ungdomsprogramytelsen \
                Hvis du har en annen inntekt i tillegg til ytelsen, er det veldig viktig at du sier fra til oss om det. \
                Du får en SMS den 1. hver måned. \
                Når du har fått SMS-en, logger du inn på Min side på nav.no og gir oss beskjed om hva du har hatt i inntekt måneden før. \
                Når du har en inntekt, får du mindre penger gjennom ungdomsprogramytelsen. \
                Likevel får du til sammen mer penger når du både har en inntekt og får ytelsen, enn hvis du bare får ytelsen. \
                Se hvordan du skal gi beskjed om inntekten din i Ungdomsportalen. \
                """);

        assertThatHtml(brevtekst)
            .asPlainTextIsEqualTo(forventet)
            .containsHtmlSubSequenceOnce("<h1>Du får ungdomsprogramytelse</h1>");
    }

    @DisplayName("blir 29 i løpet av programmet og får mindre enn maks antall dager")
    @Test
    void høySatsMaksAlder6MndIProgrammet() {
        LocalDate fom = LocalDate.of(2024, 12, 1);
        var fødselsdato = LocalDate.of(1996, 5, 15); //Blir 29 etter 6 mnd/130 dager i programmet
        var ungTestGrunnlag = BrevScenarioer.innvilget29År(fom, fødselsdato);

        var forventet = VedtaksbrevVerifikasjon.medHeaderOgFooter(fnr,
            """
                Du får ungdomsprogramytelse \
                Fra 1. desember 2024 til 31. mai 2025 får du ungdomsprogramytelse på 954 kroner per dag utenom lørdag og søndag. \
                Pengene får du utbetalt én gang i måneden før den 10. i måneden. \
                Den første utbetalingen får du måneden etter at du begynner i ungdomsprogrammet. \
                Pengene du får, blir det trukket skatt av. \
                Du finner mer informasjon om utbetalingen hvis du logger inn på Min side på nav.no. \
                \
                Hvorfor får du ungdomsprogramytelsen? \
                Du får ytelsen fordi du er med i ungdomsprogrammet. \
                Ytelsen gir deg en inntekt mens du deltar i ungdomsprogrammet. \
                Du får penger gjennom ytelsen så lenge du er i ungdomsprogrammet, men du kan som hovedregel ikke få penger i mer enn ett år. \
                Vedtaket er gjort etter arbeidsmarkedsloven § xx og forskrift om xxx § xx. \
                \
                Hvordan regner vi oss fram til hvor mye penger du har rett på? \
                Når Nav regner ut hvor mye penger du kan ha rett på, bruker vi en bestemt sum som heter grunnbeløpet. \
                Grunnbeløpet er bestemt av Stortinget, og det endrer seg hvert år. \
                Når du er over 25 år, bruker vi grunnbeløpet ganger 2,00. \
                Det vil si 248 056 kroner i året. \
                Denne summen deler vi på 260 dager, fordi du ikke får penger for lørdager og søndager. \
                Det vil si at du har rett på 954 kroner per dag. \
                Slik regner vi ut hvor mye penger du kan får per dag. \
                Se eksempler i Ungdomsportalen på hvordan vi regner ut hvor mye penger du har rett på. \
                \
                Meld fra til oss hvis du har inntekt i tillegg til ungdomsprogramytelsen \
                Hvis du har en annen inntekt i tillegg til ytelsen, er det veldig viktig at du sier fra til oss om det. \
                Du får en SMS den 1. hver måned. \
                Når du har fått SMS-en, logger du inn på Min side på nav.no og gir oss beskjed om hva du har hatt i inntekt måneden før. \
                Når du har en inntekt, får du mindre penger gjennom ungdomsprogramytelsen. \
                Likevel får du til sammen mer penger når du både har en inntekt og får ytelsen, enn hvis du bare får ytelsen. \
                Se hvordan du skal gi beskjed om inntekten din i Ungdomsportalen. \
                """);

        var behandling = lagScenario(ungTestGrunnlag);

        GenerertBrev generertBrev = genererVedtaksbrev(behandling.getId());

        var brevtekst = generertBrev.dokument().html();

        assertThatHtml(brevtekst)
            .asPlainTextIsEqualTo(forventet)
            .containsHtmlSubSequenceOnce(
                "<h1>Du får ungdomsprogramytelse</h1>"
            );
    }

    //dekker flere dagsatser også
    @Test
    void lavOgHøySats() {
        var fødselsdato = LocalDate.of(2000, 5, 15); //Blir 25 etter 6 mnd/130 dager i programmet
        var ungTestGrunnlag = BrevScenarioer.innvilget25årBle25årførsteMåned(fødselsdato);
        var forventet = VedtaksbrevVerifikasjon.medHeaderOgFooter(fnr,
            """
                Du får ungdomsprogramytelse \
                Fra 1. mai 2025 får du ungdomsprogramytelse på 636 kroner per dag utenom lørdag og søndag. \
                Fordi du fylte 25 år 15. mai 2025, får du mer penger fra denne datoen. Da får du 954 kroner per dag, utenom lørdag og søndag. \
                Pengene får du utbetalt én gang i måneden før den 10. i måneden. \
                Den første utbetalingen får du måneden etter at du begynner i ungdomsprogrammet. \
                Pengene du får, blir det trukket skatt av. \
                Du finner mer informasjon om utbetalingen hvis du logger inn på Min side på nav.no. \
                \
                Hvorfor får du ungdomsprogramytelsen? \
                Du får ytelsen fordi du er med i ungdomsprogrammet. \
                Ytelsen gir deg en inntekt mens du deltar i ungdomsprogrammet. \
                Du får penger gjennom ytelsen så lenge du er i ungdomsprogrammet, men du kan som hovedregel ikke få penger i mer enn ett år. \
                Vedtaket er gjort etter arbeidsmarkedsloven § xx og forskrift om xxx § xx. \
                \
                Hvordan regner vi oss fram til hvor mye penger du har rett på? \
                Når Nav regner ut hvor mye penger du kan ha rett på, bruker vi en bestemt sum som heter grunnbeløpet. \
                Grunnbeløpet er bestemt av Stortinget, og det endrer seg hvert år. \
                Når du er under 25 år, bruker vi grunnbeløpet ganger 1,33. \
                Det vil si 165 370 kroner i året. \
                Denne summen deler vi på 260 dager, fordi du ikke får penger for lørdager og søndager. \
                Det vil si at du har rett på 636 kroner per dag. \
                Når du er over 25 år, bruker vi grunnbeløpet ganger 2,00 som blir 248 056 kroner i året. \
                Det vil si at du har rett på 954 kroner per dag. \
                Slik regner vi ut hvor mye penger du kan får per dag. \
                Se eksempler i Ungdomsportalen på hvordan vi regner ut hvor mye penger du har rett på. \
                \
                Meld fra til oss hvis du har inntekt i tillegg til ungdomsprogramytelsen \
                Hvis du har en annen inntekt i tillegg til ytelsen, er det veldig viktig at du sier fra til oss om det. \
                Du får en SMS den 1. hver måned. \
                Når du har fått SMS-en, logger du inn på Min side på nav.no og gir oss beskjed om hva du har hatt i inntekt måneden før. \
                Når du har en inntekt, får du mindre penger gjennom ungdomsprogramytelsen. \
                Likevel får du til sammen mer penger når du både har en inntekt og får ytelsen, enn hvis du bare får ytelsen. \
                Se hvordan du skal gi beskjed om inntekten din i Ungdomsportalen. \
                """);


        var behandling = lagScenario(ungTestGrunnlag);

        GenerertBrev generertBrev = genererVedtaksbrev(behandling.getId());

        var brevtekst = generertBrev.dokument().html();

        assertThatHtml(brevtekst)
            .asPlainTextIsEqualTo(forventet)
            .containsHtmlSubSequenceOnce(
                "<h1>Du får ungdomsprogramytelse</h1>"
            );
    }

    @Test
    void pdfStrukturTest() throws IOException {
        TestScenarioBuilder scenarioBuilder = BrevScenarioer
            .lagAvsluttetStandardBehandling(ungTestRepositories);

        var behandling = scenarioBuilder.getBehandling();

        GenerertBrev generertBrev = brevGenerererTjeneste.genererVedtaksbrevForBehandling(behandling.getId(), false);

        var pdf = generertBrev.dokument().pdf();

        try (PDDocument pdDocument = Loader.loadPDF(pdf)) {
            assertThat(pdDocument.getNumberOfPages()).isEqualTo(2);
            String pdfTekst = new PDFTextStripper().getText(pdDocument);
            assertThat(pdfTekst).isNotEmpty();
            assertThat(pdfTekst).contains("Du får ungdomsprogramytelse");
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


