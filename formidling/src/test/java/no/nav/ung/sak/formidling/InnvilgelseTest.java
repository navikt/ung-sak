package no.nav.ung.sak.formidling;

import static no.nav.ung.sak.formidling.HtmlAssert.assertThatHtml;

import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.ung.sak.behandlingslager.behandling.personopplysning.PersonopplysningRepository;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriodeRepository;
import no.nav.ung.sak.behandlingslager.ytelse.UngdomsytelseGrunnlagRepository;
import no.nav.ung.sak.db.util.JpaExtension;
import no.nav.ung.sak.domene.person.pdl.AktørTjeneste;
import no.nav.ung.sak.formidling.domene.GenerertBrev;
import no.nav.ung.sak.formidling.pdfgen.PdfGenKlient;
import no.nav.ung.sak.test.util.behandling.TestScenarioBuilder;
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

    String navn = "Halvorsen Halvor";
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
            ungdomsprogramPeriodeRepository,
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
    void standardInnvilgelse() {

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


