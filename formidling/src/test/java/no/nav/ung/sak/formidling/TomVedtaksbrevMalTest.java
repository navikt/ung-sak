package no.nav.ung.sak.formidling;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.ung.kodeverk.behandling.BehandlingStegType;
import no.nav.ung.kodeverk.behandling.BehandlingType;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.ung.sak.db.util.JpaExtension;
import no.nav.ung.sak.formidling.scenarioer.AvslagScenarioer;
import no.nav.ung.sak.formidling.vedtak.VedtaksbrevTjeneste;
import no.nav.ung.sak.test.util.UngTestRepositories;
import no.nav.ung.sak.test.util.behandling.TestScenarioBuilder;
import no.nav.ung.sak.test.util.behandling.UngTestScenario;
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
        UngTestRepositories ungTestRepositories = BrevTestUtils.lagAlleUngTestRepositories(entityManager);
        LocalDate fom = LocalDate.of(2025, 8, 1);
        UngTestScenario ungTestGrunnlag = AvslagScenarioer.avslagAlder(fom);
        TestScenarioBuilder scenarioBuilder = TestScenarioBuilder.builderMedSøknad()
            .medBehandlingType(BehandlingType.FØRSTEGANGSSØKNAD)
            .medUngTestGrunnlag(ungTestGrunnlag);
        scenarioBuilder.leggTilAksjonspunkt(AksjonspunktDefinisjon.FORESLÅ_VEDTAK_MANUELT, BehandlingStegType.FORESLÅ_VEDTAK);
        var behandling = scenarioBuilder
            .buildOgLagreMedUng(ungTestRepositories);


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


