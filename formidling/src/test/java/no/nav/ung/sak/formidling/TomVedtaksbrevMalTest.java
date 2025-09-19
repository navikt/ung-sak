package no.nav.ung.sak.formidling;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.ung.sak.db.util.JpaExtension;
import no.nav.ung.sak.formidling.scenarioer.EndringInntektScenarioer;
import no.nav.ung.sak.formidling.vedtak.VedtaksbrevTjeneste;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.LocalDate;

import static no.nav.ung.sak.formidling.HtmlAssert.assertThatHtml;

@ExtendWith(CdiAwareExtension.class)
@ExtendWith(JpaExtension.class)
class TomVedtaksbrevMalTest {

    @Inject
    PdlKlientFake pdlKlientFake;

    @Inject
    EntityManager entityManager;

    @Inject
    VedtaksbrevTjeneste vedtaksbrevTjeneste;


    @DisplayName("Tom vedtaksbrev")
    @Test
    void tomBrevMal(TestInfo testInfo) {
        LocalDate fom = LocalDate.of(2024, 12, 1);

        var behandling = EndringInntektScenarioer
            .lagBehandlingMedAksjonspunktKontrollerInntekt(
                EndringInntektScenarioer.endring0KrInntekt_19år(fom),
                BrevTestUtils.lagAlleUngTestRepositories(entityManager));
        behandling.avsluttBehandling();

        var forventetMal = VedtaksbrevVerifikasjon.medHeaderOgFooterManuell(pdlKlientFake.fnr(),
            "<Fyll inn overskrift...> " +
                "<Fyll inn brødtekst...> ");
        var mal = AbstractVedtaksbrevInnholdByggerTest.genererVedtaksbrev(behandling.getId(), testInfo, vedtaksbrevTjeneste).dokument().html();

        assertThatHtml(mal)
            .asPlainTextIsEqualTo(forventetMal)
            .containsHtmlSubSequenceOnce(
                "<h1>&lt;Fyll inn overskrift...&gt;</h1>"
            );
    }

}


