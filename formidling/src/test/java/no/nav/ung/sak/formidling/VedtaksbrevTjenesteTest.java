package no.nav.ung.sak.formidling;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.ung.kodeverk.behandling.BehandlingResultatType;
import no.nav.ung.kodeverk.behandling.BehandlingStegType;
import no.nav.ung.kodeverk.behandling.BehandlingType;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.ung.kodeverk.dokument.DokumentMalType;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.ung.sak.behandlingslager.behandling.aksjonspunkt.AksjonspunktTestSupport;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.db.util.JpaExtension;
import no.nav.ung.sak.formidling.scenarioer.EndringInntektScenarioer;
import no.nav.ung.sak.formidling.scenarioer.KombinasjonScenarioer;
import no.nav.ung.sak.formidling.vedtak.VedtaksbrevTjeneste;
import no.nav.ung.sak.kontrakt.formidling.vedtaksbrev.VedtaksbrevForhåndsvisRequest;
import no.nav.ung.sak.kontrakt.formidling.vedtaksbrev.VedtaksbrevValg;
import no.nav.ung.sak.kontrakt.formidling.vedtaksbrev.VedtaksbrevValgRequest;
import no.nav.ung.sak.kontrakt.formidling.vedtaksbrev.VedtaksbrevValgResponse;
import no.nav.ung.sak.test.util.UngTestRepositories;
import no.nav.ung.sak.test.util.behandling.TestScenarioBuilder;
import no.nav.ung.sak.test.util.behandling.UngTestScenario;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tester at ulike flyter fra klient funker.
 */
@ExtendWith(CdiAwareExtension.class)
@ExtendWith(JpaExtension.class)
class VedtaksbrevTjenesteTest {

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
    void skal_bruke_automatisk_brev_hvis_ikke_overstyrt() {
        UngTestScenario ungTestscenario = EndringInntektScenarioer.endringMedInntektPå10k_19år(LocalDate.of(2024, 12, 1));

        var behandling = lagBehandlingMedAksjonspunkt(ungTestscenario);

        //Initielle valg - kun automatisk brev
        VedtaksbrevValgResponse response = vedtaksbrevTjeneste.vedtaksbrevValg(behandling.getId());
        assertThat(response.vedtaksbrevValg()).hasSize(1);
        var valg = response.vedtaksbrevValg().getFirst();
        assertThat(response.harBrev()).isTrue();
        assertThat(valg.enableRediger()).isTrue();
        assertThat(valg.redigert()).isFalse();
        assertThat(valg.kanOverstyreRediger()).isTrue();
        assertThat(valg.redigertBrevHtml()).isNull();

        //Forhåndsviser automatisk brev
        String automatiskBrevHtmlSnippet = "<h1>";
        assertThat(forhåndsvis(behandling, false)).contains(automatiskBrevHtmlSnippet);
        //Brevet behandlingen kommer til å bruke skal være automatisk brev
        assertThat(forhåndsvis(behandling, null)).contains(automatiskBrevHtmlSnippet);

        //Forhåndsviser redigert brev - skal feile da det ikke finnes noe lagret
        assertThatThrownBy(() -> forhåndsvis(behandling, true))
            .isInstanceOf(IllegalStateException.class);

    }

    @Test
    void skal_bruke_manuell_brev_hvis_redigert() {
        UngTestScenario ungTestscenario = EndringInntektScenarioer.endringMedInntektPå10k_19år(LocalDate.of(2024, 12, 1));

        var behandling = lagBehandlingMedAksjonspunkt(ungTestscenario);
        String automatiskBrevHtmlSnippet = "<h1>";

        //Lager redigert tekst
        String redigertHtml = "<h2>Manuell skrevet brev</h2>";
        vedtaksbrevTjeneste.lagreVedtaksbrev(
            new VedtaksbrevValgRequest(
                behandling.getId(),
                false,
                true,
                redigertHtml,
                DokumentMalType.ENDRING_INNTEKT)
        );

        VedtaksbrevValgResponse response = vedtaksbrevTjeneste.vedtaksbrevValg(behandling.getId());
        assertThat(response.vedtaksbrevValg()).hasSize(1);
        var valgEtterRedigering1 = response.vedtaksbrevValg().getFirst();
        assertThat(response.harBrev()).isTrue();
        assertThat(valgEtterRedigering1.enableRediger()).isTrue();
        assertThat(valgEtterRedigering1.redigert()).isTrue();
        assertThat(valgEtterRedigering1.kanOverstyreRediger()).isTrue();
        assertThat(valgEtterRedigering1.redigertBrevHtml()).isEqualTo(redigertHtml);

        //Forhåndsviser automatisk brev - skal fortsått gå bra
        assertThat(forhåndsvis(behandling, false)).contains(automatiskBrevHtmlSnippet);

        //Forhåndsviser manuell brev - skal nå gå bra
        assertThat(forhåndsvis(behandling, true)).contains(redigertHtml);

        //Brevet behandlingen kommer til å bruke skal være manuell brev
        assertThat(forhåndsvis(behandling, null)).contains(redigertHtml);
    }


    @Test
    void endrer_fra_rediger_tilbake_til_automatisk() {
        UngTestScenario ungTestscenario = EndringInntektScenarioer.endringMedInntektPå10k_19år(LocalDate.of(2024, 12, 1));

        var behandling = lagBehandlingMedAksjonspunkt(ungTestscenario);
        String automatiskBrevHtmlSnippet = "<h1>";

        //Lager redigert tekst
        String redigertHtml = "<h2>Manuell skrevet brev</h2>";
        vedtaksbrevTjeneste.lagreVedtaksbrev(
            new VedtaksbrevValgRequest(
                behandling.getId(),
                false,
                true,
                redigertHtml,
                DokumentMalType.ENDRING_INNTEKT)
        );

        //Brevet behandlingen kommer til å bruke skal være manuell brev
        assertThat(forhåndsvis(behandling, null)).contains(redigertHtml);

        //Tilbakestiller manuell brev
        vedtaksbrevTjeneste.lagreVedtaksbrev(
            new VedtaksbrevValgRequest(
                behandling.getId(),
                false,
                false,
                null,
                DokumentMalType.ENDRING_INNTEKT)
        );

        VedtaksbrevValgResponse response = vedtaksbrevTjeneste.vedtaksbrevValg(behandling.getId());
        assertThat(response.vedtaksbrevValg()).hasSize(1);
        var valgEtterRedigering2 = response.vedtaksbrevValg().getFirst();
        assertThat(response.harBrev()).isTrue();
        assertThat(valgEtterRedigering2.enableRediger()).isTrue();
        assertThat(valgEtterRedigering2.redigert()).isFalse();
        assertThat(valgEtterRedigering2.kanOverstyreRediger()).isTrue();
        assertThat(valgEtterRedigering2.redigertBrevHtml()).isNull();


        //Forhåndsviser automatisk brev
        assertThat(forhåndsvis(behandling, false)).contains(automatiskBrevHtmlSnippet);

        //Forhåndsviser redigert brev - skal feile da det ikke finnes noe lagret lenger
        assertThatThrownBy(() -> forhåndsvis(behandling, true))
            .isInstanceOf(IllegalStateException.class);

        //Brevet behandlingen kommer til å bruke skal være automatisk brev
        assertThat(forhåndsvis(behandling, null)).contains(automatiskBrevHtmlSnippet);

    }

    @Test
    void skal_beholde_redigert_tekst_ved_tilbakehopp() {
        UngTestScenario ungTestscenario = EndringInntektScenarioer.endringMedInntektPå10k_19år(LocalDate.of(2024, 12, 1));

        var behandling = lagBehandlingMedAksjonspunkt(ungTestscenario);
        String automatiskBrevHtmlSnippet = "<h1>";
        String redigertHtml = "<h2>Manuell skrevet brev</h2>";

        vedtaksbrevTjeneste.lagreVedtaksbrev(
            new VedtaksbrevValgRequest(
                behandling.getId(),
                false,
                true,
                redigertHtml,
                DokumentMalType.ENDRING_INNTEKT)
        );

        //Tilbakestiller
        vedtaksbrevTjeneste.ryddVedTilbakeHopp(behandling.getId());
        VedtaksbrevValgResponse response = vedtaksbrevTjeneste.vedtaksbrevValg(behandling.getId());
        assertThat(response.vedtaksbrevValg()).hasSize(1);
        var valgEtterRedigering = response.vedtaksbrevValg().getFirst();
        assertThat(response.harBrev()).isTrue();
        assertThat(valgEtterRedigering.enableRediger()).isTrue();
        assertThat(valgEtterRedigering.redigert()).isFalse();
        assertThat(valgEtterRedigering.kanOverstyreRediger()).isTrue();
        //Beholder teksten
        assertThat(valgEtterRedigering.redigertBrevHtml()).isEqualTo(redigertHtml);

        //Brevet behandlingen kommer til å bruke skal være automatisk brev
        assertThat(forhåndsvis(behandling, null)).contains(automatiskBrevHtmlSnippet);

        //Forhåndsviser redigert brev - skal bruke den gamle teksten
        assertThat(forhåndsvis(behandling, true)).contains(redigertHtml);

    }

    @Test
    void skal_ikke_lage_brev_hvis_hindret() {
        UngTestScenario ungTestscenario = EndringInntektScenarioer.endringMedInntektPå10k_19år(LocalDate.of(2024, 12, 1));

        var behandling = lagBehandlingMedAksjonspunkt(ungTestscenario);
        String automatiskBrevHtmlSnippet = "<h1>";
        String redigertHtml = "<h2>Manuell skrevet brev</h2>";

        //Lagerer hindret valget
        vedtaksbrevTjeneste.lagreVedtaksbrev(
            new VedtaksbrevValgRequest(
                behandling.getId(),
                true,
                true,
                redigertHtml,
                DokumentMalType.ENDRING_INNTEKT)
        );

        VedtaksbrevValgResponse response = vedtaksbrevTjeneste.vedtaksbrevValg(behandling.getId());
        assertThat(response.vedtaksbrevValg()).hasSize(1);
        var valgEtterRedigering1 = response.vedtaksbrevValg().getFirst();
        assertThat(response.harBrev()).isTrue();
        assertThat(valgEtterRedigering1.enableHindre()).isTrue();
        assertThat(valgEtterRedigering1.hindret()).isTrue();
        assertThat(valgEtterRedigering1.kanOverstyreHindre()).isTrue();

        //Forhåndsviser automatisk brev - skal fortsått gå bra
        assertThat(forhåndsvis(behandling, false)).contains(automatiskBrevHtmlSnippet);

        //Forhåndsviser manuell brev - skal fortsatt gå bra
        assertThat(forhåndsvis(behandling, true)).contains(redigertHtml);

        //Ingen brev som brukes av behandling
        assertThatThrownBy(() -> forhåndsvis(behandling, null))
            .isInstanceOf(IllegalStateException.class);
    }

    @Test //Flere brev test
    void redigere_flere_brev() {
        UngTestScenario ungTestscenario = KombinasjonScenarioer.kombinasjon_endringMedInntektOgFødselAvBarn((LocalDate.of(2025, 8, 1)));
        var behandling = lagBehandlingMedAksjonspunkt(ungTestscenario);
        String automatiskInntekstbrevSnippet = "inntekt";
        String automatiskBarnetilleggSnippet = "du har fått barn";
        String redigertHtml = "<h2>Manuell skrevet brev</h2>";

        VedtaksbrevValgResponse valg1 = vedtaksbrevTjeneste.vedtaksbrevValg(behandling.getId());
        assertThat(valg1.vedtaksbrevValg()).hasSize(2);
        assertThat(valg1.vedtaksbrevValg()).extracting(VedtaksbrevValg::dokumentMalType)
            .containsExactlyInAnyOrder(DokumentMalType.ENDRING_INNTEKT, DokumentMalType.ENDRING_BARNETILLEGG);
        assertThat(valg1.vedtaksbrevValg()).extracting(VedtaksbrevValg::kanOverstyreRediger)
            .containsExactly(true, true);

        //Redigerer ett av brevene
        vedtaksbrevTjeneste.lagreVedtaksbrev(
            new VedtaksbrevValgRequest(
                behandling.getId(),
                false,
                true,
                redigertHtml,
                DokumentMalType.ENDRING_BARNETILLEGG)
        );

        VedtaksbrevValgResponse valg2 = vedtaksbrevTjeneste.vedtaksbrevValg(behandling.getId());
        assertThat(valg2.vedtaksbrevValg()).hasSize(2);

        var barnetilleggValg = valg2.vedtaksbrevValg().stream()
            .filter(v -> v.dokumentMalType().equals(DokumentMalType.ENDRING_BARNETILLEGG))
            .findFirst()
            .orElseThrow();
        assertThat(barnetilleggValg.redigert()).isTrue();
        assertThat(barnetilleggValg.redigertBrevHtml()).isEqualTo(redigertHtml);

        var inntektValg = valg2.vedtaksbrevValg().stream()
            .filter(v -> v.dokumentMalType().equals(DokumentMalType.ENDRING_INNTEKT))
            .findFirst()
            .orElseThrow();
        assertThat(inntektValg.redigert()).isFalse();
        assertThat(inntektValg.redigertBrevHtml()).isNull();

        //Forhåndsviser automatisk brev
        assertThat(forhåndsvis(behandling, DokumentMalType.ENDRING_BARNETILLEGG, false))
            .contains(automatiskBarnetilleggSnippet);
        assertThat(forhåndsvis(behandling, DokumentMalType.ENDRING_INNTEKT, false))
            .contains(automatiskInntekstbrevSnippet);

        //Forhåndsviser manuell brev
        assertThat(forhåndsvis(behandling, DokumentMalType.ENDRING_BARNETILLEGG, true))
            .contains(redigertHtml);
        assertThatThrownBy(() -> forhåndsvis(behandling, DokumentMalType.ENDRING_INNTEKT, true))
            .isInstanceOf(IllegalStateException.class);


        //Brevet behandlingen kommer til å bruke skal være manuell brev
        assertThat(forhåndsvis(behandling, DokumentMalType.ENDRING_BARNETILLEGG, null)).contains(redigertHtml);
        assertThat(forhåndsvis(behandling, DokumentMalType.ENDRING_INNTEKT, null)).contains(automatiskInntekstbrevSnippet);
    }


    @NotNull
    private Behandling lagBehandlingMedAksjonspunkt(UngTestScenario ungTestscenario) {
        TestScenarioBuilder scenarioBuilder = TestScenarioBuilder.builderMedSøknad()
            .medBehandlingType(BehandlingType.REVURDERING)
            .medUngTestGrunnlag(ungTestscenario);

        scenarioBuilder.leggTilAksjonspunkt(AksjonspunktDefinisjon.KONTROLLER_INNTEKT, BehandlingStegType.KONTROLLER_REGISTER_INNTEKT);

        var behandling = scenarioBuilder.buildOgLagreMedUng(ungTestRepositories);
        behandling.setBehandlingResultatType(BehandlingResultatType.INNVILGET);
        Aksjonspunkt aksjonspunkt = behandling.getAksjonspunktFor(AksjonspunktDefinisjon.KONTROLLER_INNTEKT);
        new AksjonspunktTestSupport().setTilUtført(aksjonspunkt, "utført");
        BehandlingRepository behandlingRepository = ungTestRepositories.repositoryProvider().getBehandlingRepository();
        behandlingRepository.lagre(behandling, behandlingRepository.taSkriveLås(behandling));
        return behandling;
    }

    private String forhåndsvis(Behandling behandling, Boolean redigertVersjon) {
        var generertBrev = vedtaksbrevTjeneste.forhåndsvis(
            new VedtaksbrevForhåndsvisRequest(behandling.getId(), redigertVersjon, true, null)
        );
        assertThat(generertBrev).as("Forventet kun ett brev ved kall til denne metoden").hasSize(1);
        return generertBrev.stream().findFirst().map(it -> it.dokument().html()).orElseThrow();
    }

    private String forhåndsvis(Behandling behandling, DokumentMalType dokumentMalType, Boolean redigertVersjon) {
        var generertBrev = vedtaksbrevTjeneste.forhåndsvis(
            new VedtaksbrevForhåndsvisRequest(behandling.getId(), redigertVersjon, true, dokumentMalType)
        );
        assertThat(generertBrev).as("Forventet kun ett brev").hasSize(1);
        return generertBrev.stream().findFirst().map(it -> it.dokument().html()).orElseThrow();
    }

}
