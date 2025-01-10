package no.nav.ung.sak.formidling;

import static no.nav.ung.sak.formidling.HtmlAssert.assertThatHtml;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.k9.søknad.JsonUtils;
import no.nav.ung.kodeverk.dokument.DokumentMalType;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriodeRepository;
import no.nav.ung.sak.behandlingslager.ytelse.UngdomsytelseGrunnlagRepository;
import no.nav.ung.sak.db.util.JpaExtension;
import no.nav.ung.sak.domene.person.pdl.AktørTjeneste;
import no.nav.ung.sak.domene.person.pdl.PersonBasisTjeneste;
import no.nav.ung.sak.formidling.domene.GenerertBrev;
import no.nav.ung.sak.formidling.dto.Brevbestilling;
import no.nav.ung.sak.formidling.pdfgen.PdfGenKlient;
import no.nav.ung.sak.test.util.behandling.TestScenarioBuilder;
import no.nav.ung.sak.typer.AktørId;
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
    private final ObjectMapper objectMapper = JsonUtils.getObjectMapper();


    @Inject
    private EntityManager entityManager;
    private BehandlingRepositoryProvider repositoryProvider;
    private UngdomsytelseGrunnlagRepository ungdomsytelseGrunnlagRepository;
    private UngdomsprogramPeriodeRepository ungdomsprogramPeriodeRepository;
    private TilkjentYtelseUtleder tilkjentYtelseUtleder;

    String navn = "Halvorsen Halvor";
    String fnr = PdlKlientFake.gyldigFnr();

    @BeforeEach
    void setup() {
        repositoryProvider = new BehandlingRepositoryProvider(entityManager);
        ungdomsytelseGrunnlagRepository = new UngdomsytelseGrunnlagRepository(entityManager);
        ungdomsprogramPeriodeRepository = new UngdomsprogramPeriodeRepository(entityManager);
        tilkjentYtelseUtleder = new UngdomsytelseTilkjentYtelseUtleder(ungdomsytelseGrunnlagRepository);

        var pdlKlient = new PdlKlientFake("Halvor", "Halvorsen", fnr);

        brevGenerererTjeneste = new BrevGenerererTjeneste(
            repositoryProvider.getBehandlingRepository(),
            new PersonBasisTjeneste(pdlKlient),
            new AktørTjeneste(pdlKlient),
            new PdfGenKlient(System.getenv("LAGRE_PDF") == null),
            ungdomsytelseGrunnlagRepository,
            ungdomsprogramPeriodeRepository,
            tilkjentYtelseUtleder);
    }

    @Test
    void standard_innvilgelse() {
        var ungdom = AktørId.dummy();
        TestScenarioBuilder scenarioBuilder = TestScenarioBuilder.builderMedSøknad(ungdom);
        var behandling = scenarioBuilder.lagre(repositoryProvider);

        var bestillBrevDto = lagBestilling(behandling);
        GenerertBrev generertBrev = genererBrev(bestillBrevDto);

        var brevtekst = generertBrev.dokument().html();
        assertThatHtml(brevtekst).contains("<h1>NAV har innvilget søknaden din om ungdomsytelse</h1>");
        assertThatHtml(brevtekst).contains("Til: " + navn);
        assertThatHtml(brevtekst).contains("Fødselsnummer: " + fnr);

    }

    private GenerertBrev genererBrev(Brevbestilling bestillBrevDto) {
        GenerertBrev generertBrev = brevGenerererTjeneste.generer(bestillBrevDto);
        if (System.getenv("LAGRE_PDF") != null) {
            PdfUtils.lagrePdf(generertBrev.dokument().pdf(), generertBrev.malType().name());
        }
        return generertBrev;
    }

    private Brevbestilling lagBestilling(Behandling behandling) {
        return new Brevbestilling(
            behandling.getId(),
            DokumentMalType.INNVILGELSE_DOK,
            behandling.getFagsak().getSaksnummer().getVerdi(),
            null,
            objectMapper.createObjectNode()
        );
    }


}


