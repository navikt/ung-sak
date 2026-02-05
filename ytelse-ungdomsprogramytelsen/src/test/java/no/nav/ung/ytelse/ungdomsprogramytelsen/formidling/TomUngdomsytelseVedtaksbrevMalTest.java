package no.nav.ung.ytelse.ungdomsprogramytelsen.formidling;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.ung.sak.db.util.JpaExtension;
import no.nav.ung.sak.formidling.vedtak.VedtaksbrevTjeneste;
import no.nav.ung.sak.test.util.behandling.ungdomsprogramytelse.UngTestRepositories;
import no.nav.ung.sak.test.util.behandling.ungdomsprogramytelse.UngTestScenario;
import no.nav.ung.ytelse.ungdomsprogramytelsen.formidling.scenarioer.AvslagScenarioer;
import no.nav.ung.ytelse.ungdomsprogramytelsen.formidling.scenarioer.BrevScenarioerUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.LocalDate;

import static no.nav.ung.ytelse.ungdomsprogramytelsen.formidling.HtmlAssert.assertThatHtml;

@ExtendWith(CdiAwareExtension.class)
@ExtendWith(JpaExtension.class)
class TomUngdomsytelseVedtaksbrevMalTest {

    @Inject
    PdlKlientFake pdlKlientFake;

    @Inject
    EntityManager entityManager;

    @Inject
    VedtaksbrevTjeneste vedtaksbrevTjeneste;


    @DisplayName("Tom vedtaksbrev")
    @Test
    void tomBrevMal(TestInfo testInfo) {
        UngTestRepositories ungTestRepositories = BrevTestUtils.lagAlleUngTestRepositories(entityManager);
        LocalDate fom = LocalDate.of(2025, 8, 1);
        UngTestScenario ungTestGrunnlag = AvslagScenarioer.avslagAlder(fom);
        var behandling = BrevScenarioerUtils
            .lagÅpenBehandlingMedAP(ungTestGrunnlag, ungTestRepositories, AksjonspunktDefinisjon.FORESLÅ_VEDTAK);


        var forventetMal = VedtaksbrevVerifikasjon.medHeaderOgFooterManuellUtenBeslutter(pdlKlientFake.fnr(),
            "<Fyll inn overskrift...> " +
                "<Fyll inn brødtekst...> ");
        var mal = AbstractUngdomsytelseVedtaksbrevInnholdByggerTest.genererVedtaksbrev(behandling.getId(), testInfo, vedtaksbrevTjeneste).dokument().html();

        assertThatHtml(mal)
            .asPlainTextIsEqualTo(forventetMal)
            .containsHtmlSubSequenceOnce(
                "<h1>&lt;Fyll inn overskrift...&gt;</h1>"
            );
    }

}


