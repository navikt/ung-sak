package no.nav.ung.ytelse.aktivitetspenger.formidling;

import no.nav.ung.kodeverk.behandling.BehandlingResultatType;
import no.nav.ung.kodeverk.formidling.TemplateType;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.formidling.GenerertBrev;
import no.nav.ung.ytelse.aktivitetspenger.formidling.scenarioer.AktivitetspengerFørstegangsbehandlingScenarioer;
import no.nav.ung.ytelse.aktivitetspenger.testdata.AktivitetspengerTestScenario;
import no.nav.ung.ytelse.aktivitetspenger.testdata.AktivitetspengerTestScenarioBuilder;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static no.nav.ung.ytelse.aktivitetspenger.formidling.HtmlAssert.assertThatHtml;
import static org.assertj.core.api.Assertions.assertThat;

class FørstegangsInnvilgelseTest extends AbstractAktivitetspengerVedtaksbrevInnholdByggerTest {

    private static final LocalDate DAGENS_DATO = LocalDate.of(2025, 8, 15);

    FørstegangsInnvilgelseTest() {
        super(2, "Du får aktivitetspenger");
    }

    @BeforeAll
    static void beforeAll() {
        System.setProperty("BREV_DAGENS_DATO_TEST", DAGENS_DATO.toString());
    }

    @AfterAll
    static void afterAll() {
        System.clearProperty("BREV_DAGENS_DATO_TEST");
    }

    @DisplayName("Førstegangsinnvilgelse med lav og høy sats (24 år blir 25)")
    @Test
    void lavOgHøySats() {
        var fom = LocalDate.of(2025, 8, 1);
        var scenario = AktivitetspengerFørstegangsbehandlingScenarioer.innvilget24årBle25årførsteMåned(fom);

        var behandling = lagScenario(scenario);

        GenerertBrev generertBrev = genererVedtaksbrev(behandling.getId());
        assertThat(generertBrev.templateType()).isEqualTo(TemplateType.AKTIVITETSPENGER_INNVILGELSE);

        var brevtekst = generertBrev.dokument().html();

        var forventet = VedtaksbrevVerifikasjon.medHeaderOgFooter(fnr,
            """
                Du får aktivitetspenger \
                Fra 1. september 2025 får du aktivitetspenger på 681 kroner utenom lørdag og søndag. \
                Fordi du fylte 25 år 16. august 2025, får du mer penger fra denne datoen. Da får du 1 022 kroner per dag, utenom lørdag og søndag. \
                Pengene blir utbetalt én gang i måneden. Den første utbetalingen får du innen 12. september, og deretter får du pengene innen den 12. hver måned. \
                Pengene du får, blir det trukket skatt av. Hvis du har frikort, blir det ikke trukket skatt. \
                Du finner mer informasjon om utbetalingen hvis du logger inn på Min side på nav.no. \
                """ + hvorforFårDuPengerAvsnitt() + """
                Hvordan har vi regnet ut hvor mye penger du får? \
                Når du er under 25 år, bruker vi grunnbeløpet ganger 2,041. \
                Det blir 177 104 kroner i året. \
                Denne summen har vi delt på 260 dager, fordi du ikke får penger for lørdager og søndager. \
                Det vil si at du har rett på 681 kroner per dag. \
                Når du er over 25 år, bruker vi grunnbeløpet ganger 2,041 som blir 265 657 kroner i året. \
                Det vil si at du har rett på 1 022 kroner per dag. \
                """ + meldFraOmArbeidsinntektAvsnitt()
        );

        assertThatHtml(brevtekst)
            .asPlainTextIsEqualTo(forventet)
            .containsHtmlSubSequenceOnce(
                "<h1>Du får aktivitetspenger</h1>",
                "<h2>Hvorfor får du penger?</h2>",
                "<h2>Hvordan har vi regnet ut hvor mye penger du får?</h2>",
                "<h2>Meld fra til oss hvis du har arbeidsinntekt i tillegg til aktivitetspengene</h2>"
            );
    }

    static String hvorforFårDuPengerAvsnitt() {
        return """
            Hvorfor får du penger? \
            Du får penger fordi du har behov for hjelp til å komme i jobb eller utdanning. \
            Pengene får du så lenge du får oppfølging fra Nav, og i inntil ett år. \
            """;
    }

    static String meldFraOmArbeidsinntektAvsnitt() {
        return """
            Meld fra til oss hvis du har arbeidsinntekt i tillegg til aktivitetspengene \
            Hvis du har en arbeidstinntekt i tillegg til aktivitetspengene, er det viktig at du sier fra til oss om det. \
            Du får en SMS den 1. hver måned. Når du har fått SMS-en, logger du inn på Min side på nav.no og gir oss beskjed om all arbeidsinntekt du har hatt måneden før. \
            Når du jobber og har en inntekt, får du mindre aktivitetspenger. \
            Likevel får du til sammen mer penger når du både har en inntekt og får aktivitetspenger enn hvis du bare får aktivitetspenger. \
            """;
    }

    private Behandling lagScenario(AktivitetspengerTestScenario testScenario) {
        AktivitetspengerTestScenarioBuilder scenarioBuilder = AktivitetspengerTestScenarioBuilder.builderMedSøknad()
            .medAktivitetspengerTestGrunnlag(testScenario);

        var behandling = scenarioBuilder.buildOgLagreMedAktivitspenger(repositories);
        behandling.setBehandlingResultatType(BehandlingResultatType.INNVILGET);
        behandling.avsluttBehandling();
        return behandling;
    }

    @Override
    protected Behandling lagScenarioForFellesTester() {
        var fom = LocalDate.of(2025, 8, 1);
        var scenario = AktivitetspengerFørstegangsbehandlingScenarioer.innvilget24årBle25årførsteMåned(fom);
        return lagScenario(scenario);
    }
}
