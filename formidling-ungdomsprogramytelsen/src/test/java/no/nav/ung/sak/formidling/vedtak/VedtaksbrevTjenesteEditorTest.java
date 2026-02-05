package no.nav.ung.sak.formidling.vedtak;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.ung.kodeverk.dokument.DokumentMalType;
import no.nav.ung.sak.db.util.JpaExtension;
import no.nav.ung.sak.formidling.BrevTestUtils;
import no.nav.ung.sak.formidling.scenarioer.BrevScenarioerUtils;
import no.nav.ung.sak.formidling.scenarioer.EndringInntektScenarioer;
import no.nav.ung.sak.kontrakt.formidling.vedtaksbrev.editor.VedtaksbrevEditorResponse;
import no.nav.ung.sak.kontrakt.formidling.vedtaksbrev.editor.VedtaksbrevSeksjon;
import no.nav.ung.sak.kontrakt.formidling.vedtaksbrev.editor.VedtaksbrevSeksjonType;
import no.nav.ung.sak.test.util.behandling.ungdomsprogramytelse.UngTestRepositories;
import no.nav.ung.sak.test.util.behandling.ungdomsprogramytelse.UngTestScenario;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(CdiAwareExtension.class)
@ExtendWith(JpaExtension.class)
class VedtaksbrevTjenesteEditorTest {

    @Inject
    private VedtaksbrevTjeneste vedtaksbrevTjeneste;
    @Inject
    private EntityManager entityManager;

    private UngTestRepositories ungTestRepositories;

    @BeforeEach
    void setup() {
        ungTestRepositories = BrevTestUtils.lagAlleUngTestRepositories(entityManager);
    }

    @Test
    void skal_lage_editor_response() {
        UngTestScenario ungTestscenario = EndringInntektScenarioer.endringMedInntektPå10k_19år(LocalDate.of(2024, 12, 1));

        var behandling = EndringInntektScenarioer.lagBehandlingMedAksjonspunktKontrollerInntekt(ungTestscenario, ungTestRepositories);

        //Initielle valg - kun automatisk brev
        VedtaksbrevEditorResponse response = vedtaksbrevTjeneste.editor(behandling.getId(), DokumentMalType.ENDRING_INNTEKT);
        List<VedtaksbrevSeksjon> seksjoner = response.original();
        assertThat(seksjoner).hasSize(4);

        // Rekkefølge er viktig
        VedtaksbrevSeksjon stiler = seksjoner.getFirst();
        VedtaksbrevSeksjon statiskHeader = seksjoner.get(1);
        VedtaksbrevSeksjon redigerbar = seksjoner.get(2);
        VedtaksbrevSeksjon statiskFooter = seksjoner.get(3);

        assertThat(stiler.type()).isEqualTo(VedtaksbrevSeksjonType.STYLE);
        assertThat(statiskHeader.type()).isEqualTo(VedtaksbrevSeksjonType.STATISK);
        assertThat(redigerbar.type()).isEqualTo(VedtaksbrevSeksjonType.REDIGERBAR);
        assertThat(statiskFooter.type()).isEqualTo(VedtaksbrevSeksjonType.STATISK);


        String stilInnhold = stiler.innhold();
        verifiserWellformedFragment(stilInnhold, "style");

        String statiskHeaderInnhold = statiskHeader.innhold();
        verifiserWellformedFragment(statiskHeaderInnhold,
            "header", "div", "p");
        assertThat(statiskHeader.innhold()).contains(BrevScenarioerUtils.DEFAULT_NAVN);

        String redigerbarInnhold = redigerbar.innhold();
        verifiserWellformedFragment(redigerbarInnhold,
            "h1", "p");
        assertThat(redigerbarInnhold).contains("Vi har endret ungdomsprogramytelsen din");


        String statiskFooterInnhold = statiskFooter.innhold();
        verifiserWellformedFragment(statiskFooterInnhold, "h2", "p", "div", "a", "td", "tbody", "table", "tr");
        assertThat(statiskFooterInnhold).contains("Med vennlig hilsen");

    }

    private static void verifiserWellformedFragment(String html, String... forventedeTagger) {
        //Disse legges til av jsoup ved parsing så sjekker at de ikke er med i orginalen.
        assertThat(html).doesNotContainIgnoringCase("<html>", "<body>", "<head>", "#root");

        Parser parser = Parser.htmlParser();
        parser.setTrackErrors(50);
        Document elements = Jsoup.parse(html, "", parser);
        assertThat(parser.getErrors()).isEmpty();
        Set<String> allTags = elements.getAllElements().stream()
            .map(Element::tagName)
            .filter(t -> !t.equals("body"))
            .filter(t -> !t.equals("head"))
            .filter(t -> !t.equals("html"))
            .filter(t -> !t.equals("#root"))
            .collect(Collectors.toSet());

        assertThat(allTags).containsExactlyInAnyOrder(forventedeTagger);
    }

}
