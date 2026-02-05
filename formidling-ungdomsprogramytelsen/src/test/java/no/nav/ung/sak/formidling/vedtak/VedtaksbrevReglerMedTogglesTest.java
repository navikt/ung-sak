package no.nav.ung.sak.formidling.vedtak;

import jakarta.enterprise.inject.Any;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.ung.kodeverk.dokument.DokumentMalType;
import no.nav.ung.sak.db.util.JpaExtension;
import no.nav.ung.sak.formidling.BrevTestUtils;
import no.nav.ung.sak.formidling.innhold.EndringInntektUtenReduksjonInnholdBygger;
import no.nav.ung.sak.formidling.scenarioer.EndringInntektScenarioer;
import no.nav.ung.sak.formidling.vedtak.regler.BehandlingVedtaksbrevResultat;
import no.nav.ung.sak.formidling.vedtak.regler.VedtaksbrevReglerUng;
import no.nav.ung.sak.test.util.UngTestRepositories;
import no.nav.ung.sak.test.util.behandling.UngTestScenario;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;


/**
 * Test av regler for hvilke vedtaksbrev skal tilbys og egenskaper for redigering og hindring.
 * <p>
 * For rene automatiske brev så er testene som sjekker brevinnhold dekkende. Denne testen sjekker unntak fra de testene
 */
@ExtendWith(CdiAwareExtension.class)
@ExtendWith(JpaExtension.class)
class VedtaksbrevReglerMedTogglesTest {

    @Inject
    private EntityManager entityManager;
    private UngTestRepositories ungTestRepositories;

    @Inject
    @Any
    private VedtaksbrevReglerUng vedtaksbrevRegler;


    @BeforeEach
    void setup() {
        ungTestRepositories = BrevTestUtils.lagAlleUngTestRepositories(entityManager);
    }

    @BeforeAll
    static void beforeAll() {
        System.setProperty("ENABLE_ENDRING_UTEN_REDUKSJON_SJEKK", "true");
    }

    @AfterAll
    static void afterAll() {
        System.clearProperty("ENABLE_ENDRING_UTEN_REDUKSJON_SJEKK");
    }


    @Test //Denne testen er ikke avklart funksjonelt. Avhengig av toggle
    void skal_gi_brev_ved_avkortning_men_fastsatt_til_0_kr_og_register_er_over_0kr() {
        UngTestScenario ungTestGrunnlag = EndringInntektScenarioer.endring0KrRapportert10000KrRegister0krFastsatt(LocalDate.of(2024, 12, 1));
        var behandling = EndringInntektScenarioer.lagBehandlingMedAksjonspunktKontrollerInntekt(ungTestGrunnlag, ungTestRepositories);

        BehandlingVedtaksbrevResultat totalresultater = vedtaksbrevRegler.kjør(behandling.getId());
        assertThat(totalresultater.harBrev()).isTrue();
        assertThat(totalresultater.vedtaksbrevResultater()).hasSize(1);

        var regelResulat = totalresultater.vedtaksbrevResultater().getFirst();

        VedtaksbrevReglerTest.assertRedigerbarBrev(regelResulat, DokumentMalType.ENDRING_INNTEKT_UTEN_REDUKSJON, EndringInntektUtenReduksjonInnholdBygger.class);

    }


}
