package no.nav.ung.sak.formidling;

import static no.nav.ung.sak.formidling.HtmlAssert.assertThatHtml;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.ExtendWith;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.ung.kodeverk.behandling.BehandlingResultatType;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.personopplysning.PersonopplysningRepository;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.ung.sak.behandlingslager.behandling.startdato.UngdomsytelseStartdatoRepository;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriodeRepository;
import no.nav.ung.sak.behandlingslager.tilkjentytelse.TilkjentYtelseRepository;
import no.nav.ung.sak.behandlingslager.ytelse.UngdomsytelseGrunnlagRepository;
import no.nav.ung.sak.db.util.JpaExtension;
import no.nav.ung.sak.domene.person.pdl.AktørTjeneste;
import no.nav.ung.sak.formidling.innhold.InnvilgelseInnholdBygger;
import no.nav.ung.sak.formidling.pdfgen.PdfGenKlient;
import no.nav.ung.sak.formidling.template.TemplateType;
import no.nav.ung.sak.formidling.vedtak.DetaljertResultatUtlederImpl;
import no.nav.ung.sak.perioder.ProsessTriggerPeriodeUtleder;
import no.nav.ung.sak.perioder.UngdomsytelseSøknadsperiodeTjeneste;
import no.nav.ung.sak.test.util.UngTestRepositories;
import no.nav.ung.sak.test.util.behandling.TestScenarioBuilder;
import no.nav.ung.sak.test.util.behandling.UngTestscenario;
import no.nav.ung.sak.trigger.ProsessTriggereRepository;
import no.nav.ung.sak.ungdomsprogram.UngdomsprogramPeriodeTjeneste;
import no.nav.ung.sak.ytelse.beregning.TilkjentYtelseUtleder;
import no.nav.ung.sak.ytelse.beregning.UngdomsytelseTilkjentYtelseUtleder;

/**
 * Test for brevtekster for innvilgelse. Bruker html for å validere.
 * For manuell verifikasjon av pdf kan env variabel LAGRE_PDF brukes.
 */
@ExtendWith(CdiAwareExtension.class)
@ExtendWith(JpaExtension.class)
class BrevGenerererTjenesteEndringInntektTest {

    private BrevGenerererTjeneste brevGenerererTjeneste;

    @Inject
    private EntityManager entityManager;
    private BehandlingRepositoryProvider repositoryProvider;
    private UngdomsytelseGrunnlagRepository ungdomsytelseGrunnlagRepository;
    private UngdomsprogramPeriodeRepository ungdomsprogramPeriodeRepository;
    private UngdomsytelseStartdatoRepository ungdomsytelseStartdatoRepository;
    private ProsessTriggereRepository prosessTriggereRepository;
    private TilkjentYtelseUtleder tilkjentYtelseUtleder;
    private PersonopplysningRepository personopplysningRepository;

    PdlKlientFake pdlKlient = PdlKlientFake.medTilfeldigFnr();
    String fnr = pdlKlient.fnr();
    private TestInfo testInfo;
    private TilkjentYtelseRepository tilkjentYtelseRepository;


    @BeforeEach
    void setup(TestInfo testInfo) {
        this.testInfo = testInfo;
        repositoryProvider = new BehandlingRepositoryProvider(entityManager);
        ungdomsytelseGrunnlagRepository = new UngdomsytelseGrunnlagRepository(entityManager);
        ungdomsprogramPeriodeRepository = new UngdomsprogramPeriodeRepository(entityManager);
        tilkjentYtelseRepository = new TilkjentYtelseRepository(entityManager);
        tilkjentYtelseUtleder = new UngdomsytelseTilkjentYtelseUtleder(tilkjentYtelseRepository);
        personopplysningRepository = repositoryProvider.getPersonopplysningRepository();
        prosessTriggereRepository = new ProsessTriggereRepository(entityManager);
        ungdomsytelseStartdatoRepository = new UngdomsytelseStartdatoRepository(entityManager);

        brevGenerererTjeneste = lagBrevGenererTjeneste(System.getenv("LAGRE_PDF") == null);
    }

    @NotNull
    private BrevGenerererTjeneste lagBrevGenererTjeneste(boolean ignorePdf) {
        UngdomsprogramPeriodeTjeneste ungdomsprogramPeriodeTjeneste = new UngdomsprogramPeriodeTjeneste(ungdomsprogramPeriodeRepository);;
        return new BrevGenerererTjenesteImpl(
            repositoryProvider.getBehandlingRepository(),
            new AktørTjeneste(pdlKlient),
            new PdfGenKlient(ignorePdf),
            personopplysningRepository,
            new InnvilgelseInnholdBygger(
                ungdomsytelseGrunnlagRepository,
                ungdomsprogramPeriodeTjeneste,
                tilkjentYtelseUtleder,
                personopplysningRepository),
            new DetaljertResultatUtlederImpl(
                new ProsessTriggerPeriodeUtleder(prosessTriggereRepository),
                repositoryProvider.getVilkårResultatRepository(),
                new UngdomsytelseSøknadsperiodeTjeneste(ungdomsytelseStartdatoRepository, ungdomsprogramPeriodeTjeneste, repositoryProvider.getBehandlingRepository()), tilkjentYtelseRepository));
    }

    @Test()
    @DisplayName("Verifiserer faste tekster og mottaker")
    //Vurder å lage gjenbrukbar assertions som sjekker alle standardtekster og mottaker
    void skalHaAlleStandardtekster() {
        TestScenarioBuilder scenarioBuilder = BrevScenarioer
            .lagAvsluttetStandardBehandling(lagUngTestRepositories());

        var ungTestGrunnlag = scenarioBuilder.getUngTestGrunnlag();
        var behandling = scenarioBuilder.getBehandling();

        GenerertBrev generertBrev = genererVedtaksbrevBrev(behandling.getId());

        var brevtekst = generertBrev.dokument().html();

        assertThatHtml(brevtekst).containsTextsOnceInSequence(
            BrevUtils.brevDatoString(LocalDate.now()), //vedtaksdato
            "Til: " + ungTestGrunnlag.navn(),
            "Fødselsnummer: " + fnr,
            "Du har rett til å klage",
            "Du kan klage innen 6 uker fra den datoen du mottok vedtaket. Du finner skjema og informasjon på nav.no/klage",
            "Du har rett til innsyn",
            "Du kan se dokumentene i saken din ved å logge deg inn på nav.no",
            "Trenger du mer informasjon?",
            "Med vennlig hilsen",
            "Nav Arbeid og ytelser"
        ).containsHtmlOnceInSequence(
            "<h2>Du har rett til å klage</h2>",
            "<h2>Du har rett til innsyn</h2>",
            "<h2>Trenger du mer informasjon?</h2>"
        );


    }

    @NotNull
    private UngTestRepositories lagUngTestRepositories() {
        return new UngTestRepositories(repositoryProvider, ungdomsytelseGrunnlagRepository, ungdomsprogramPeriodeRepository, ungdomsytelseStartdatoRepository, tilkjentYtelseRepository, prosessTriggereRepository);
    }


    @DisplayName("Innvilgelse med riktig fom dato, maks antall dager, lav sats, grunnbeløp, hjemmel")
    //Denne testen sjekker også at teksten kommer i riktig rekkefølge
    @Test
    void standardInnvilgelse() {
        LocalDate fom = LocalDate.of(2024, 12, 1);
        var ungTestGrunnlag = BrevScenarioer.endringMedInntektPå10k_19år(fom);

        var behandling = lagScenario(ungTestGrunnlag);

        GenerertBrev generertBrev = genererVedtaksbrevBrev(behandling.getId());
        assertThat(generertBrev.templateType()).isEqualTo(TemplateType.ENDRING_INNTEKT);

        var brevtekst = generertBrev.dokument().html();

        assertThatHtml(brevtekst).containsHtmlOnceInSequence(
            "<h1>Nav har endret din ungdomsytelse</h1>"
        ).containsTextsOnceInSequence(
            "Til: " + ungTestGrunnlag.navn(),
            "Fødselsnummer: " + fnr,
            "Du har meldt inn inntekt på 10 000 kroner for perioden 1. november 2025 til 29. november 2025.",
            "Du får utbetalt 636 kroner dagen, før skatt.",
            "Nav bruker grunnbeløpet på 124 028 kroner for å regne ut hvor mye du får.",
            "Siden du er under 25 år så får du 1.33 ganger grunnbeløpet.",
            "Vedtaket er gjort etter folketrygdloven § X-Y."
        );

    }


    private Behandling lagScenario(UngTestscenario ungTestscenario) {
        TestScenarioBuilder scenarioBuilder = TestScenarioBuilder.builderMedSøknad().medUngTestGrunnlag(ungTestscenario);

        var behandling = scenarioBuilder.buildOgLagreMedUng(
            lagUngTestRepositories());
        behandling.setBehandlingResultatType(BehandlingResultatType.INNVILGET);
        behandling.avsluttBehandling();
        return behandling;
    }

    private GenerertBrev genererVedtaksbrevBrev(Long behandlingId) {
        return genererVedtaksbrevBrev(behandlingId, brevGenerererTjeneste);
    }


    private GenerertBrev genererVedtaksbrevBrev(Long behandlingId, BrevGenerererTjeneste brevGenerererTjeneste1) {
        GenerertBrev generertBrev = brevGenerererTjeneste1.genererVedtaksbrev(behandlingId);
        if (System.getenv("LAGRE_PDF") != null) {
            BrevUtils.lagrePdf(generertBrev, testInfo);
        }
        return generertBrev;
    }


}


