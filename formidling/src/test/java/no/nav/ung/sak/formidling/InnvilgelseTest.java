package no.nav.ung.sak.formidling;

import static no.nav.ung.sak.formidling.HtmlAssert.assertThatHtml;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import domene.ungdomsprogram.UngdomsprogramPeriodeTjeneste;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.ung.kodeverk.behandling.BehandlingResultatType;
import no.nav.ung.sak.behandlingslager.behandling.personopplysning.PersonopplysningRepository;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriodeRepository;
import no.nav.ung.sak.behandlingslager.ytelse.UngdomsytelseGrunnlagRepository;
import no.nav.ung.sak.db.util.JpaExtension;
import no.nav.ung.sak.domene.person.pdl.AktørTjeneste;
import no.nav.ung.sak.formidling.domene.GenerertBrev;
import no.nav.ung.sak.formidling.pdfgen.PdfGenKlient;
import no.nav.ung.sak.formidling.template.TemplateType;
import no.nav.ung.sak.test.util.behandling.TestScenarioBuilder;
import no.nav.ung.sak.test.util.behandling.UngTestGrunnlag;
import no.nav.ung.sak.ytelse.beregning.TilkjentYtelseUtleder;
import no.nav.ung.sak.ytelse.beregning.UngdomsytelseTilkjentYtelseUtleder;

/**
 * Test for brevtekster for innvilgelse. Lager ikke pdf, men bruker html for å validere.
 * For manuell verifikasjon av pdf kan env variabel LAGRE_PDF brukes.
 */
@ExtendWith(CdiAwareExtension.class)
@ExtendWith(JpaExtension.class)
class InnvilgelseTest {

    private BrevGenerererTjeneste brevGenerererTjeneste;


    @Inject
    private EntityManager entityManager;
    private BehandlingRepositoryProvider repositoryProvider;
    private UngdomsytelseGrunnlagRepository ungdomsytelseGrunnlagRepository;
    private UngdomsprogramPeriodeRepository ungdomsprogramPeriodeRepository;
    private TilkjentYtelseUtleder tilkjentYtelseUtleder;
    private PersonopplysningRepository personopplysningRepository;

    PdlKlientFake pdlKlient = PdlKlientFake.medTilfeldigFnr();
    String fnr = pdlKlient.fnr();


    @BeforeEach
    void setup() {
        repositoryProvider = new BehandlingRepositoryProvider(entityManager);
        ungdomsytelseGrunnlagRepository = new UngdomsytelseGrunnlagRepository(entityManager);
        ungdomsprogramPeriodeRepository = new UngdomsprogramPeriodeRepository(entityManager);
        tilkjentYtelseUtleder = new UngdomsytelseTilkjentYtelseUtleder(ungdomsytelseGrunnlagRepository);
        personopplysningRepository = repositoryProvider.getPersonopplysningRepository();

        brevGenerererTjeneste = new BrevGenerererTjeneste(
            repositoryProvider.getBehandlingRepository(),
            new AktørTjeneste(pdlKlient),
            new PdfGenKlient(System.getenv("LAGRE_PDF") == null),
            ungdomsytelseGrunnlagRepository,
            new UngdomsprogramPeriodeTjeneste(ungdomsprogramPeriodeRepository),
            tilkjentYtelseUtleder,
            personopplysningRepository);
    }

    @Test()
    @DisplayName("Verifiserer faste tekster og mottaker")
    //Vurder å lage gjenbrukbar assertions som sjekker alle standardtekster og mottaker
    void skalHaAlleStandardtekster() {
        TestScenarioBuilder scenarioBuilder = BrevScenarioer
            .lagAvsluttetStandardBehandling(repositoryProvider, ungdomsytelseGrunnlagRepository, ungdomsprogramPeriodeRepository);

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


    @DisplayName("Innvilgelse med riktig fom dato, maks antall dager, lav sats, grunnbeløp, hjemmel")
    //Denne testen sjekker også at teksten kommer i riktig rekkefølge
    @Test
    void standardInnvilgelse() {
        LocalDate fom = LocalDate.of(2024, 12, 1);
        var ungTestGrunnlag = UngTestGrunnlag.standardInnvilget(fom);

        TestScenarioBuilder scenarioBuilder = TestScenarioBuilder.builderMedSøknad().medUngTestGrunnlag(ungTestGrunnlag);

        var behandling = scenarioBuilder.buildOgLagreMedUng(repositoryProvider, ungdomsytelseGrunnlagRepository, ungdomsprogramPeriodeRepository);
        behandling.setBehandlingResultatType(BehandlingResultatType.INNVILGET);
        behandling.avsluttBehandling();

        GenerertBrev generertBrev = genererVedtaksbrevBrev(behandling.getId());
        assertThat(generertBrev.templateType()).isEqualTo(TemplateType.INNVILGELSE);

        var brevtekst = generertBrev.dokument().html();

        assertThatHtml(brevtekst).containsHtmlOnceInSequence(
            "<h1>Nav har innvilget søknaden din om ungdomsytelse</h1>"
        ).containsTextsOnceInSequence(
            "Til: " + ungTestGrunnlag.navn(),
            "Fødselsnummer: " + fnr,
            "Du har rett til ungdomsytelse fra 1. desember 2024 i 260 dager.",
            "Du får utbetalt 636 kroner dagen, før skatt.",
            "Nav bruker grunnbeløpet på 124 028 kroner for å regne ut hvor mye du får.",
            "Siden du er under 25 år så får du 1.33 ganger grunnbeløpet.",
            "Vedtaket er gjort etter folketrygdloven § X-Y."
        );

    }

    void høySats() {

    }

    void høySatsMaksAlder() {

    }

    //dekker flere dagsatser også
    void lavOgHøySats() {

    }

    void antallDagerUnder260() {

    }

    void periodisertBarneTillegg() {

    }

    @DisplayName("Pdf'en skal ha logo og tekst og riktig antall sider")
    void pdfStrukturTest() {

    }


    private GenerertBrev genererVedtaksbrevBrev(Long behandlingId) {
        GenerertBrev generertBrev = brevGenerererTjeneste.genererVedtaksbrev(behandlingId);
        if (System.getenv("LAGRE_PDF") != null) {
            BrevUtils.lagrePdf(generertBrev.dokument().pdf(), generertBrev.malType().name());
        }
        return generertBrev;
    }


}


