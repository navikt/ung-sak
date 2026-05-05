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

    @DisplayName("Førstegangsinnvilgelse med lav og høy sats (24 år blir 25), besteberegning (siste år) høyest under 25")
    @Test
    void lavOgHøySatsMedBesteberegning() {
        var fom = LocalDate.of(2025, 8, 1);
        var scenario = AktivitetspengerFørstegangsbehandlingScenarioer.innvilget24ÅrBle25ÅrLavSatsMedBesteberegning(fom);

        var behandling = lagScenario(scenario);

        GenerertBrev generertBrev = genererVedtaksbrev(behandling.getId());
        assertThat(generertBrev.templateType()).isEqualTo(TemplateType.AKTIVITETSPENGER_INNVILGELSE);

        var brevtekst = generertBrev.dokument().html();

        var forventet = VedtaksbrevVerifikasjon.medHeaderOgFooter(fnr,
            """
                Du får aktivitetspenger \
                Fra 1. august 2025 får du aktivitetspenger på 762 kroner utenom lørdag og søndag. \
                Fordi du fylte 25 år 16. august 2025, får du mer penger fra denne datoen. Da får du 1 022 kroner per dag, utenom lørdag og søndag. \
                Pengene blir utbetalt én gang i måneden. Den første utbetalingen får du innen 12. september, og deretter får du pengene innen den 12. hver måned. \
                Pengene du får, blir det trukket skatt av. Hvis du har frikort, blir det ikke trukket skatt. \
                Du finner mer informasjon om utbetalingen hvis du logger inn på Min side på nav.no. \
                """ + hvorforFårDuPengerAvsnitt() + """
                Hvordan har vi regnet ut hvor mye penger du får? \
                Fordi du får én dagsats når du er under 25 år og en annen dagsats når du er over 25 år, har vi gjort to ulike beregninger. \
                Slik har vi regnet ut satsen som du får før du fylte 25 år. \
                Når vi har regnet ut hvor mye du får i aktivitetspenger før du fylte 25 år, har vi brukt årsinntekten din for 2024 på 300 000 kroner. \
                Det er fordi denne inntekten er høyere enn minstesatsen for deg som er under 25 år, og høyere enn gjennomsnittet av årsinntekten din for de tre siste ferdiglignede årene. Vi bruker den høyeste summen i beregningen vår slik at du får mer i aktivitetspenger. \
                For å finne dagsatsen din, har vi regnet ut hva 66 prosent av 300 000 kroner er. Denne summen deler vi på antall dager i et år, men fordi du ikke får penger for lørdager og søndager, deler vi summen på 260 dager. \
                Det vil si at du har rett på 762 kroner per dag. \
                Slik har vi regnet ut satsen du får etter at du fylte 25 år. \
                Når vi har regnet ut hvor mye du får i aktivitetspenger etter at du fylte 25, har vi sett på inntekten din de tre siste årene. Fordi minstesatsen for deg som er over 25 år, er høyere enn inntekten din de tre siste årene, får du minstesatsen. \
                Minstesatsen er 2,041 ganger grunnbeløpet på 130 160 kroner. Det vil si at du kan få opptil 265 657 kroner i året. Denne summen har vi delt på 260 dager, fordi du ikke får penger for lørdager og søndager. \
                Det vil si at du har rett på 1 022 kroner per dag. \
                """ + meldFraOmArbeidsinntektAvsnitt()
        );

        assertThatHtml(brevtekst)
            .asPlainTextIsEqualTo(forventet)
            .containsHtmlSubSequenceOnce(
                "<h1>Du får aktivitetspenger</h1>",
                "<h3>Slik har vi regnet ut satsen som du får før du fylte 25 år.</h3>",
                "<h3>Slik har vi regnet ut satsen du får etter at du fylte 25 år.</h3>",
                "<h2>Meld fra til oss hvis du har arbeidsinntekt i tillegg til aktivitetspengene</h2>"
            );
    }

    @DisplayName("Førstegangsinnvilgelse kun lav sats (under 25 år), minstesats")
    @Test
    void kunLavSats() {
        var fom = LocalDate.of(2025, 8, 1);
        var scenario = AktivitetspengerFørstegangsbehandlingScenarioer.innvilgetKunLavSats(fom);

        var behandling = lagScenario(scenario);

        GenerertBrev generertBrev = genererVedtaksbrev(behandling.getId());
        assertThat(generertBrev.templateType()).isEqualTo(TemplateType.AKTIVITETSPENGER_INNVILGELSE);

        var brevtekst = generertBrev.dokument().html();

        var forventet = VedtaksbrevVerifikasjon.medHeaderOgFooter(fnr,
            """
                Du får aktivitetspenger \
                Fra 1. august 2025 får du aktivitetspenger på 681 kroner utenom lørdag og søndag. \
                Pengene blir utbetalt én gang i måneden. Den første utbetalingen får du innen 12. september, og deretter får du pengene innen den 12. hver måned. \
                Pengene du får, blir det trukket skatt av. Hvis du har frikort, blir det ikke trukket skatt. \
                Du finner mer informasjon om utbetalingen hvis du logger inn på Min side på nav.no. \
                """ + hvorforFårDuPengerAvsnitt() + """
                Hvordan har vi regnet ut hvor mye penger du får? \
                Når vi har regnet ut hvor mye du får i aktivitetspenger, har vi sett på inntekten din de tre siste årene. \
                Fordi minstesatsen for deg som er under 25 år, er høyere enn inntekten din de tre siste årene, får du minstesatsen. \
                Minstesatsen er 2/3 av 2,041 ganger grunnbeløpet som er på 130 160 kroner. \
                Det vil si at du kan få opptil 177 104 kroner i året. Denne summen har vi delt på 260 dager, fordi du ikke får penger for lørdager og søndager. \
                Det vil si at du har rett på 681 kroner per dag. \
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


    @DisplayName("Førstegangsinnvilgelse kun høy sats (over 25 år), minstesats")
    @Test
    void kunHøySats() {
        var fom = LocalDate.of(2025, 8, 1);
        var scenario = AktivitetspengerFørstegangsbehandlingScenarioer.innvilgetKunHøySats(fom);

        var behandling = lagScenario(scenario);

        GenerertBrev generertBrev = genererVedtaksbrev(behandling.getId());
        assertThat(generertBrev.templateType()).isEqualTo(TemplateType.AKTIVITETSPENGER_INNVILGELSE);

        var brevtekst = generertBrev.dokument().html();

        var forventet = VedtaksbrevVerifikasjon.medHeaderOgFooter(fnr,
            """
                Du får aktivitetspenger \
                Fra 1. august 2025 får du aktivitetspenger på 1 022 kroner utenom lørdag og søndag. \
                Pengene blir utbetalt én gang i måneden. Den første utbetalingen får du innen 12. september, og deretter får du pengene innen den 12. hver måned. \
                Pengene du får, blir det trukket skatt av. Hvis du har frikort, blir det ikke trukket skatt. \
                Du finner mer informasjon om utbetalingen hvis du logger inn på Min side på nav.no. \
                """ + hvorforFårDuPengerAvsnitt() + """
                Hvordan har vi regnet ut hvor mye penger du får? \
                Når vi har regnet ut hvor mye du får i aktivitetspenger, har vi sett på inntekten din de tre siste årene. \
                Fordi minstesatsen for deg som er over 25 år, er høyere enn inntekten din de tre siste årene, får du minstesatsen. \
                Minstesatsen er 2,041 ganger grunnbeløpet på 130 160 kroner. Det vil si at du kan få opptil 265 657 kroner i året. \
                Denne summen har vi delt på 260 dager, fordi du ikke får penger for lørdager og søndager. \
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
                Fra 1. august 2025 får du aktivitetspenger på 681 kroner utenom lørdag og søndag. \
                Fordi du fylte 25 år 16. august 2025, får du mer penger fra denne datoen. Da får du 1 022 kroner per dag, utenom lørdag og søndag. \
                Pengene blir utbetalt én gang i måneden. Den første utbetalingen får du innen 12. september, og deretter får du pengene innen den 12. hver måned. \
                Pengene du får, blir det trukket skatt av. Hvis du har frikort, blir det ikke trukket skatt. \
                Du finner mer informasjon om utbetalingen hvis du logger inn på Min side på nav.no. \
                """ + hvorforFårDuPengerAvsnitt() + """
                Hvordan har vi regnet ut hvor mye penger du får? \
                Når vi har regnet ut hvor mye du får i aktivitetspenger, har vi sett på inntekten din de tre siste årene. \
                Fordi minstesatsen er høyere enn inntekten din de tre siste årene, får du minstesatsen både når du er under og når du er over 25 år. \
                Minstesatsen når du er under 25 år, er grunnbeløpet ganger 2/3 av 2,041. \
                Det blir 177 104 kroner i året. \
                Denne summen har vi delt på 260 dager, fordi du ikke får penger for lørdager og søndager. \
                Det vil si at du har rett på 681 kroner per dag. \
                Minstesatsen når du er over 25 år, er grunnbeløpet ganger 2,041 som blir 265 657 kroner i året. \
                Denne summen har vi delt på 260 dager, fordi du ikke får penger for lørdager og søndager. \
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
            Hvis du har en arbeidsinntekt i tillegg til aktivitetspengene, er det viktig at du sier fra til oss om det. \
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
