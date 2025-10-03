package no.nav.ung.sak.formidling.vedtak;

import jakarta.enterprise.inject.Any;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.ung.kodeverk.behandling.BehandlingResultatType;
import no.nav.ung.kodeverk.behandling.BehandlingType;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.ung.kodeverk.dokument.DokumentMalType;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.db.util.JpaExtension;
import no.nav.ung.sak.formidling.BrevTestUtils;
import no.nav.ung.sak.formidling.innhold.*;
import no.nav.ung.sak.formidling.scenarioer.*;
import no.nav.ung.sak.formidling.vedtak.regler.BehandlingVedtaksbrevResultat;
import no.nav.ung.sak.formidling.vedtak.regler.IngenBrevÅrsakType;
import no.nav.ung.sak.formidling.vedtak.regler.Vedtaksbrev;
import no.nav.ung.sak.formidling.vedtak.regler.VedtaksbrevReglerUng;
import no.nav.ung.sak.test.util.UngTestRepositories;
import no.nav.ung.sak.test.util.behandling.TestScenarioBuilder;
import no.nav.ung.sak.test.util.behandling.UngTestScenario;
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
class VedtaksbrevReglerTest {

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


    @Test
    void skal_ikke_redigere_brev_uten_aksjonspunkt() {
        var behandling = lagBehandling(EndringInntektScenarioer.endringMedInntektPå10k_19år(LocalDate.of(2024, 12, 1)));

        BehandlingVedtaksbrevResultat totalresultater = vedtaksbrevRegler.kjør(behandling.getId());
        assertThat(totalresultater.harBrev()).isTrue();
        assertThat(totalresultater.vedtaksbrevResultater()).hasSize(1);

        var regelResulat = totalresultater.vedtaksbrevResultater().getFirst();

        assertFullAutomatiskBrev(regelResulat, DokumentMalType.ENDRING_INNTEKT, EndringRapportertInntektInnholdBygger.class);
    }

    @Test
    void skal_gi_ingen_brev_ved_full_ungdomsprogram_med_ingen_rapportert_inntekt_uten_ap() {
        LocalDate fom = LocalDate.of(2024, 12, 1);
        var behandling = lagBehandling(EndringInntektScenarioer.endring0KrInntekt_19år(fom));

        BehandlingVedtaksbrevResultat totalresultater = vedtaksbrevRegler.kjør(behandling.getId());
        assertThat(totalresultater.harBrev()).isFalse();
        assertThat(totalresultater.ingenBrevResultater()).hasSize(1);

        var regelResulat = totalresultater.ingenBrevResultater().getFirst();
        assertThat(regelResulat.ingenBrevÅrsakType()).isEqualTo(IngenBrevÅrsakType.IKKE_RELEVANT);

        assertThat(regelResulat.forklaring()).containsIgnoringCase("ingen brev");

    }

    @Test
    void skal_gi_ingen_brev_ved_dødsfall_av_barn_hendelse() {
        LocalDate fom = LocalDate.of(2024, 12, 1);
        var behandling = lagBehandling(EndringBarnetilleggScenarioer.endringDødsfall(fom, fom.plusDays(4)));

        BehandlingVedtaksbrevResultat totalresultater = vedtaksbrevRegler.kjør(behandling.getId());
        assertThat(totalresultater.harBrev()).isFalse();

        assertThat(totalresultater.ingenBrevResultater()).hasSize(1);

        var regelResulat = totalresultater.ingenBrevResultater().getFirst();
        assertThat(regelResulat.ingenBrevÅrsakType()).isEqualTo(IngenBrevÅrsakType.IKKE_IMPLEMENTERT);

        assertThat(regelResulat.forklaring()).containsIgnoringCase("ingen brev");

    }

    @Test
    void skal_gi_ingen_brev_ved_dødsfall_av_barn_under_førstegangsbehandling() {
        LocalDate fom = LocalDate.of(2024, 12, 1);
        var behandling = lagBehandling(FørstegangsbehandlingScenarioer.innvilget19årMedDødsfallBarn15DagerEtterStartdato(fom));

        BehandlingVedtaksbrevResultat totalresultater = vedtaksbrevRegler.kjør(behandling.getId());
        assertThat(totalresultater.harBrev()).isFalse();

        assertThat(totalresultater.ingenBrevResultater()).hasSize(1);

        var regelResulat = totalresultater.ingenBrevResultater().getFirst();
        assertThat(regelResulat.ingenBrevÅrsakType()).isEqualTo(IngenBrevÅrsakType.IKKE_IMPLEMENTERT);

        assertThat(regelResulat.forklaring()).containsIgnoringCase("ingen brev");

    }

    @Test
    void skal_gi_ingen_brev_ved_avslag_aldersvilkår() {
        LocalDate fom = LocalDate.of(2025, 8, 1);
        UngTestScenario ungTestGrunnlag = AvslagScenarioer.avslagAlder(fom);
        var behandling = TestScenarioBuilder.builderMedSøknad()
            .medBehandlingType(BehandlingType.FØRSTEGANGSSØKNAD)
            .medUngTestGrunnlag(ungTestGrunnlag)
            .buildOgLagreMedUng(ungTestRepositories);

        BehandlingVedtaksbrevResultat totalresultater = vedtaksbrevRegler.kjør(behandling.getId());
        assertThat(totalresultater.harBrev()).isFalse();

        assertThat(totalresultater.ingenBrevResultater()).hasSize(1);

        var regelResulat = totalresultater.ingenBrevResultater().getFirst();
        assertThat(regelResulat.ingenBrevÅrsakType()).isEqualTo(IngenBrevÅrsakType.IKKE_IMPLEMENTERT);

        assertThat(regelResulat.forklaring()).containsIgnoringCase("ingen brev");

    }

    @Test
    void skal_ikke_kunne_redigere_eller_hindre_automatisk_brev_for_andre_totrinn_ap_enn_kontroller_inntekt() {
        LocalDate fom = LocalDate.of(2024, 12, 1);
        // Bruker aksjonspunkt med totrinn for å trigge redigering av brev
        var behandling = BrevScenarioerUtils.lagBehandlingMedAP(
          EndringHøySatsScenarioer.endring25År(fom.minusYears(25)), ungTestRepositories,
            AksjonspunktDefinisjon.VURDER_TILBAKETREKK
        );

        behandling.avsluttBehandling();

        BehandlingVedtaksbrevResultat totalresultater = vedtaksbrevRegler.kjør(behandling.getId());
        assertThat(totalresultater.harBrev()).isTrue();
        assertThat(totalresultater.vedtaksbrevResultater()).hasSize(1);

        var regelResulat = totalresultater.vedtaksbrevResultater().getFirst();

        assertFullAutomatiskBrev(regelResulat, DokumentMalType.ENDRING_HØY_SATS, EndringHøySatsInnholdBygger.class);
        assertThat(regelResulat.forklaring()).contains("høy sats");

    }

    /**
     * RE_ENDRET_SATS brukes for å manuelt trigge førstegansbehandling etter en feilretting.
     */
    @Test
    void skal_gi_førstegangsinnvilgelse_ved_manuell_re_endret_sats() {
        UngTestScenario grunnlag = FørstegangsbehandlingScenarioer.endret_sats(LocalDate.of(2024, 12, 1));

        var behandling = TestScenarioBuilder.builderMedSøknad()
            .medBehandlingType(BehandlingType.REVURDERING)
            .medManuellOpprettet()
            .medUngTestGrunnlag(grunnlag).buildOgLagreMedUng(ungTestRepositories);
        behandling.setBehandlingResultatType(BehandlingResultatType.INNVILGET);
        behandling.avsluttBehandling();

        BehandlingVedtaksbrevResultat totalresultater = vedtaksbrevRegler.kjør(behandling.getId());
        assertThat(totalresultater.harBrev()).isTrue();
        assertThat(totalresultater.vedtaksbrevResultater()).hasSize(1);

        var regelResulat = totalresultater.vedtaksbrevResultater().getFirst();

        assertFullAutomatiskBrev(regelResulat, DokumentMalType.INNVILGELSE_DOK, FørstegangsInnvilgelseInnholdBygger.class);
    }


    @Test
    void skal_lage_to_brev_ved_kontroller_inntekt_og_andre_hendelser() {
        LocalDate fom = LocalDate.of(2025, 8, 1);
        var behandling = EndringInntektScenarioer.lagBehandlingMedAksjonspunktKontrollerInntekt(KombinasjonScenarioer.kombinasjon_endringMedInntektOgFødselAvBarn(fom), ungTestRepositories);
        behandling.avsluttBehandling();

        BehandlingVedtaksbrevResultat totalresultater = vedtaksbrevRegler.kjør(behandling.getId());
        assertThat(totalresultater.harBrev()).isTrue();
        assertThat(totalresultater.vedtaksbrevResultater()).hasSize(2);

        var inntektResultat = totalresultater.vedtaksbrevResultater().stream()
            .filter(resultat -> resultat.dokumentMalType().equals(DokumentMalType.ENDRING_INNTEKT))
            .findFirst()
            .orElseThrow();

        assertRedigerbarBrev(inntektResultat, DokumentMalType.ENDRING_INNTEKT, EndringRapportertInntektInnholdBygger.class);

        assertThat(inntektResultat.forklaring()).contains(AksjonspunktDefinisjon.KONTROLLER_INNTEKT.getKode());

        var barnetilleggResultat = totalresultater.vedtaksbrevResultater().stream()
            .filter(resultat -> resultat.dokumentMalType().equals(DokumentMalType.ENDRING_BARNETILLEGG))
            .findFirst()
            .orElseThrow();

        assertFullAutomatiskBrev(barnetilleggResultat, DokumentMalType.ENDRING_BARNETILLEGG, EndringBarnetilleggInnholdBygger.class);

        assertThat(barnetilleggResultat.forklaring()).doesNotContain(AksjonspunktDefinisjon.KONTROLLER_INNTEKT.getKode());
        assertThat(barnetilleggResultat.forklaring()).contains("barn");
    }

    private static void assertRedigerbarBrev(Vedtaksbrev vedtaksbrev, DokumentMalType dokumentMalType, Class<? extends VedtaksbrevInnholdBygger> type) {
        var egenskaper = vedtaksbrev.vedtaksbrevEgenskaper();
        assertThat(vedtaksbrev.vedtaksbrevBygger()).isInstanceOf(type);
        assertThat(vedtaksbrev.dokumentMalType()).isEqualTo(dokumentMalType);
        assertThat(egenskaper.kanHindre()).isFalse();
        assertThat(egenskaper.kanOverstyreHindre()).isFalse();
        assertThat(egenskaper.kanRedigere()).isTrue();
        assertThat(egenskaper.kanOverstyreRediger()).isTrue();
    }

    private static void assertFullAutomatiskBrev(Vedtaksbrev vedtaksbrev, DokumentMalType dokumentMalType, Class<? extends VedtaksbrevInnholdBygger> type) {
        var egenskaper = vedtaksbrev.vedtaksbrevEgenskaper();
        assertThat(vedtaksbrev.vedtaksbrevBygger()).isInstanceOf(type);
        assertThat(vedtaksbrev.dokumentMalType()).isEqualTo(dokumentMalType);
        assertThat(egenskaper.kanHindre()).isFalse();
        assertThat(egenskaper.kanRedigere()).isFalse();
        assertThat(egenskaper.kanOverstyreRediger()).isFalse();
    }


    private Behandling lagBehandling(UngTestScenario ungTestGrunnlag) {
        TestScenarioBuilder scenarioBuilder = TestScenarioBuilder.builderMedSøknad()
            .medBehandlingType(BehandlingType.REVURDERING)
            .medUngTestGrunnlag(ungTestGrunnlag);

        var behandling = scenarioBuilder.buildOgLagreMedUng(ungTestRepositories);
        behandling.setBehandlingResultatType(BehandlingResultatType.INNVILGET);

        behandling.avsluttBehandling();

        return behandling;
    }

}
