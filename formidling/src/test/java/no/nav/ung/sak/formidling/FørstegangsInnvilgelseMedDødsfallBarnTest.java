package no.nav.ung.sak.formidling;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.ung.kodeverk.behandling.BehandlingResultatType;
import no.nav.ung.kodeverk.formidling.TemplateType;
import no.nav.ung.sak.db.util.JpaExtension;
import no.nav.ung.sak.formidling.scenarioer.FørstegangsbehandlingScenarioer;
import no.nav.ung.sak.formidling.vedtak.VedtaksbrevTjeneste;
import no.nav.ung.sak.test.util.behandling.TestScenarioBuilder;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.LocalDate;

import static no.nav.ung.sak.formidling.HtmlAssert.assertThatHtml;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(CdiAwareExtension.class)
@ExtendWith(JpaExtension.class)
public class FørstegangsInnvilgelseMedDødsfallBarnTest {

    private static final LocalDate DAGENS_DATO = LocalDate.of(2025, 8, 15);


    @Inject
    PdlKlientFake pdlKlientFake;

    @Inject
    EntityManager entityManager;

    @Inject
    VedtaksbrevTjeneste vedtaksbrevTjeneste;

    @BeforeAll
    static void beforeAll() {
        System.setProperty("ENABLE_AUTO_BREV_BARN_DØDSFALL", "true");
        System.setProperty("BREV_DAGENS_DATO_TEST", DAGENS_DATO.toString());
    }

    @AfterAll
    static void afterAll() {
        System.clearProperty("ENABLE_AUTO_BREV_BARN_DØDSFALL");
        System.clearProperty("BREV_DAGENS_DATO_TEST");

    }

    @DisplayName("Innvilgelsesbrev med barnedødsfall av barn")
    @Test
    void medDødsfallAvBarn(TestInfo testInfo) {
        //Må toggles på for at brevet skal genereres
        LocalDate fom = LocalDate.of(2025, 8, 1);
        var ungTestGrunnlag = FørstegangsbehandlingScenarioer.innvilget19årMedDødsfallBarn15DagerEtterStartdato(fom);

        TestScenarioBuilder scenarioBuilder = TestScenarioBuilder.builderMedSøknad().medUngTestGrunnlag(ungTestGrunnlag);

        var behandling = scenarioBuilder.buildOgLagreMedUng(BrevTestUtils.lagAlleUngTestRepositories(entityManager));
        behandling.setBehandlingResultatType(BehandlingResultatType.INNVILGET);
        behandling.avsluttBehandling();

        GenerertBrev generertBrev = AbstractVedtaksbrevInnholdByggerTest.genererVedtaksbrev(behandling.getId(), testInfo, vedtaksbrevTjeneste);
        assertThat(generertBrev.templateType()).isEqualTo(TemplateType.INNVILGELSE);

        var brevtekst = generertBrev.dokument().html();

        var forventet = VedtaksbrevVerifikasjon.medHeaderOgFooter(pdlKlientFake.fnr(),
            """
                Du får ungdomsprogramytelse \
                Fra 1. august 2025 får du ungdomsprogramytelse på 718 kroner per dag, utenom lørdag og søndag. \
                Fordi du mistet barn 16. august 2025, får du ikke barnetillegg på 37 kroner fra denne datoen. Da får du 681 kroner per dag, utenom lørdag og søndag. \
                Pengene får du utbetalt én gang i måneden før den 12. i måneden. \
                Den første utbetalingen får du måneden etter at du begynner i ungdomsprogrammet. \
                Pengene du får, blir det trukket skatt av. Hvis du har frikort, blir det ikke trukket skatt. \
                Du finner mer informasjon om utbetalingen hvis du logger inn på Min side på nav.no. \
                """ + FørstegangsInnvilgelseTest.hvorforFårDuPleiepengerAvsnitt() + """
                Hvordan har vi regnet ut hvor mye penger du får? \
                Når vi har regnet ut hvor mye penger du får, har vi brukt en sum som heter grunnbeløpet. \
                Grunnbeløpet er bestemt av Stortinget, og det øker hvert år. \
                Nå er grunnbeløpet på 130 160 kroner. \
                Fordi du er under 25 år, bruker vi grunnbeløpet ganger 2/3 av 2,041. \
                Det blir 177 104 kroner i året. \
                Denne summen har vi delt vi på 260 dager, fordi du ikke får penger for lørdager og søndager. \
                Det vil si at du har rett på 681 kroner per dag. \
                """ + FørstegangsInnvilgelseTest.meldFraTilOssHvisDuHarEndringerAvsnitt()
        );

        assertThatHtml(brevtekst)
            .asPlainTextIsEqualTo(forventet)
            .containsHtmlSubSequenceOnce(
                "<h1>Du får ungdomsprogramytelse</h1>"
            );

    }

}
