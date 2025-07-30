package no.nav.ung.sak.formidling.vedtak;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.ung.kodeverk.behandling.BehandlingResultatType;
import no.nav.ung.kodeverk.behandling.BehandlingStegType;
import no.nav.ung.kodeverk.behandling.BehandlingType;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.ung.sak.behandlingslager.behandling.aksjonspunkt.AksjonspunktTestSupport;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.ung.sak.db.util.JpaExtension;
import no.nav.ung.sak.formidling.BrevScenarioer;
import no.nav.ung.sak.formidling.BrevTestUtils;
import no.nav.ung.sak.formidling.innhold.EndringRapportertInntektInnholdBygger;
import no.nav.ung.sak.formidling.innhold.ManueltVedtaksbrevInnholdBygger;
import no.nav.ung.sak.formidling.vedtak.regler.*;
import no.nav.ung.sak.perioder.ProsessTriggerPeriodeUtleder;
import no.nav.ung.sak.perioder.UngdomsytelseSøknadsperiodeTjeneste;
import no.nav.ung.sak.test.util.UngTestRepositories;
import no.nav.ung.sak.test.util.UnitTestMultiLookupInstanceImpl;
import no.nav.ung.sak.test.util.behandling.TestScenarioBuilder;
import no.nav.ung.sak.test.util.behandling.UngTestScenario;
import no.nav.ung.sak.ungdomsprogram.UngdomsprogramPeriodeTjeneste;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.LocalDate;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Test av regler for hvilke vedtaksbrev skal tilbys og egenskaper for redigering og hindring.
 * <p>
 * For rene automatiske brev så er testene i BrevGenrererTjeneste<Bygger>Test dekkende. Denne testen sjekker unntak fra de testene
 */
@ExtendWith(CdiAwareExtension.class)
@ExtendWith(JpaExtension.class)
class VedtaksbrevReglerTest {

    @Inject
    private EntityManager entityManager;
    private UngTestRepositories ungTestRepositories;


    @BeforeEach
    void setup() {
        ungTestRepositories = BrevTestUtils.lagAlleUngTestRepositories(entityManager);
    }


    @Test
    void skal_ikke_redigere_brev_uten_aksjonspunkt() {
        var behandling = lagBehandling(BrevScenarioer.endringMedInntektPå10k_19år(LocalDate.of(2024, 12, 1)));

        var vedtaksbrevRegler = lagVedtaksbrevRegler();
        VedtaksbrevRegelResulat regelResulat = vedtaksbrevRegler.kjør(behandling.getId());

        var vedtaksbrevEgenskaper = regelResulat.vedtaksbrevEgenskaper();

        assertThat(regelResulat.automatiskVedtaksbrevBygger()).isInstanceOf(EndringRapportertInntektInnholdBygger.class);
        assertThat(vedtaksbrevEgenskaper.kanHindre()).isFalse();
        assertThat(vedtaksbrevEgenskaper.kanRedigere()).isFalse();
        assertThat(vedtaksbrevEgenskaper.harBrev()).isTrue();
        assertThat(vedtaksbrevEgenskaper.kanOverstyreHindre()).isFalse();
        assertThat(vedtaksbrevEgenskaper.kanOverstyreRediger()).isFalse();
    }

    @Test
    void skal_kunne_redigere_automatisk_brev_ved_aksjonspunkt() {
        LocalDate fom = LocalDate.of(2024, 12, 1);
        var behandling = lagBehandling(BrevScenarioer.endringMedInntektPå10k_19år(fom), BehandlingStegType.KONTROLLER_REGISTER_INNTEKT, AksjonspunktDefinisjon.KONTROLLER_INNTEKT);

        var vedtaksbrevRegler = lagVedtaksbrevRegler();
        VedtaksbrevRegelResulat regelResulat = vedtaksbrevRegler.kjør(behandling.getId());

        var vedtaksbrevEgenskaper = regelResulat.vedtaksbrevEgenskaper();

        assertThat(regelResulat.automatiskVedtaksbrevBygger()).isInstanceOf(EndringRapportertInntektInnholdBygger.class);
        assertThat(vedtaksbrevEgenskaper.kanHindre()).isTrue();
        assertThat(vedtaksbrevEgenskaper.kanRedigere()).isTrue();
        assertThat(vedtaksbrevEgenskaper.harBrev()).isTrue();
        assertThat(vedtaksbrevEgenskaper.kanOverstyreHindre()).isTrue();
        assertThat(vedtaksbrevEgenskaper.kanOverstyreRediger()).isTrue();

        assertThat(regelResulat.forklaring()).contains(AksjonspunktDefinisjon.KONTROLLER_INNTEKT.getKode());

    }

    @Test
    void skal_gi_ingen_brev_ved_full_ungdomsprogram_med_ingen_rapportert_inntekt_uten_ap() {
        LocalDate fom = LocalDate.of(2024, 12, 1);
        var behandling = lagBehandling(BrevScenarioer.endring0KrInntekt_19år(fom));

        var vedtaksbrevRegler = lagVedtaksbrevRegler();
        VedtaksbrevRegelResulat regelResulat = vedtaksbrevRegler.kjør(behandling.getId());

        var vedtaksbrevEgenskaper = regelResulat.vedtaksbrevEgenskaper();

        assertThat(regelResulat.automatiskVedtaksbrevBygger()).isNull();
        assertThat(vedtaksbrevEgenskaper.kanHindre()).isFalse();
        assertThat(vedtaksbrevEgenskaper.kanRedigere()).isFalse();
        assertThat(vedtaksbrevEgenskaper.harBrev()).isFalse();
        assertThat(vedtaksbrevEgenskaper.ingenBrevÅrsakType()).isEqualTo(IngenBrevÅrsakType.IKKE_RELEVANT);
        assertThat(vedtaksbrevEgenskaper.kanOverstyreHindre()).isFalse();
        assertThat(vedtaksbrevEgenskaper.kanOverstyreRediger()).isFalse();

        assertThat(regelResulat.forklaring()).containsIgnoringCase("ingen brev");

    }

    @Test
    void skal_gi_ingen_brev_ved_dødsfall_av_barn_hendelse() {
        LocalDate fom = LocalDate.of(2024, 12, 1);
        var behandling = lagBehandling(BrevScenarioer.endringDødsfall(fom, fom.plusDays(4)));

        var vedtaksbrevRegler = lagVedtaksbrevRegler();
        VedtaksbrevRegelResulat regelResulat = vedtaksbrevRegler.kjør(behandling.getId());

        var vedtaksbrevEgenskaper = regelResulat.vedtaksbrevEgenskaper();

        assertThat(regelResulat.automatiskVedtaksbrevBygger()).isNull();
        assertThat(vedtaksbrevEgenskaper.harBrev()).isFalse();
        assertThat(vedtaksbrevEgenskaper.ingenBrevÅrsakType()).isEqualTo(IngenBrevÅrsakType.IKKE_IMPLEMENTERT);

        assertThat(regelResulat.forklaring()).containsIgnoringCase("ingen brev");

    }

    @Test
    void skal_gi_ingen_brev_ved_dødsfall_av_barn_under_førstegangsbehandling() {
        LocalDate fom = LocalDate.of(2024, 12, 1);
        var behandling = lagBehandling(BrevScenarioer.innvilget19årMedDødsfallBarn15DagerEtterStartdato(fom));

        var vedtaksbrevRegler = lagVedtaksbrevRegler();
        VedtaksbrevRegelResulat regelResulat = vedtaksbrevRegler.kjør(behandling.getId());

        var vedtaksbrevEgenskaper = regelResulat.vedtaksbrevEgenskaper();

        assertThat(regelResulat.automatiskVedtaksbrevBygger()).isNull();
        assertThat(vedtaksbrevEgenskaper.harBrev()).isFalse();
        assertThat(vedtaksbrevEgenskaper.ingenBrevÅrsakType()).isEqualTo(IngenBrevÅrsakType.IKKE_IMPLEMENTERT);

        assertThat(regelResulat.forklaring()).containsIgnoringCase("ingen brev");

    }

    @Test
    void skal_gi_manuell_vedtaksbrev_som_må_redigeres_ved_aksjonspunkt_uten_automatisk_brev() {
        LocalDate fom = LocalDate.of(2024, 12, 1);
        var behandling = lagBehandling(BrevScenarioer.endring0KrInntekt_19år(fom), null, AksjonspunktDefinisjon.KONTROLLER_INNTEKT); // Bruker aksjonspunkt med totrinn for å trigge redigering av brev

        var vedtaksbrevRegler = lagVedtaksbrevRegler();
        VedtaksbrevRegelResulat regelResulat = vedtaksbrevRegler.kjør(behandling.getId());

        var vedtaksbrevEgenskaper = regelResulat.vedtaksbrevEgenskaper();

        assertThat(regelResulat.automatiskVedtaksbrevBygger()).isInstanceOf(ManueltVedtaksbrevInnholdBygger.class);
        assertThat(vedtaksbrevEgenskaper.kanHindre()).isTrue();
        assertThat(vedtaksbrevEgenskaper.kanRedigere()).isTrue();
        assertThat(vedtaksbrevEgenskaper.harBrev()).isTrue();
        assertThat(vedtaksbrevEgenskaper.kanOverstyreHindre()).isTrue();
        assertThat(vedtaksbrevEgenskaper.kanOverstyreRediger()).isFalse();

        assertThat(regelResulat.forklaring()).contains(AksjonspunktDefinisjon.KONTROLLER_INNTEKT.getKode());

    }

    private VedtaksbrevRegler lagVedtaksbrevRegler() {
        BehandlingRepositoryProvider repositoryProvider = ungTestRepositories.repositoryProvider();
        BehandlingRepository behandlingRepository = repositoryProvider.getBehandlingRepository();

        var detaljertResultatUtleder = new DetaljertResultatUtlederImpl(
            new ProsessTriggerPeriodeUtleder(ungTestRepositories.prosessTriggereRepository(),
                new UngdomsytelseSøknadsperiodeTjeneste(ungTestRepositories.ungdomsytelseStartdatoRepository(),
                    new UngdomsprogramPeriodeTjeneste(ungTestRepositories.ungdomsprogramPeriodeRepository(), ungTestRepositories.ungdomsytelseStartdatoRepository()),
                    behandlingRepository)),
            ungTestRepositories.tilkjentYtelseRepository(),
            repositoryProvider.getVilkårResultatRepository());

        return new VedtaksbrevRegler(
            behandlingRepository,
            detaljertResultatUtleder,
            new UnitTestMultiLookupInstanceImpl<>(
                new FørstegangsInnvilgelseStrategy(mock()),
                new EndringInntektReduksjonStrategy(mock()),
                new EndringSluttdatoStrategy(mock(), mock(), mock()),
                new EndringStartdatoStrategy(mock()),
                new EndringInntektFullUtbetalingStrategy(),
                new EndringBarnDødsfallStrategy(ungTestRepositories.ungdomsytelseGrunnlagRepository(), false)
            ),
            new ManueltVedtaksbrevInnholdBygger(ungTestRepositories.vedtaksbrevValgRepository()));

    }

    private Behandling lagBehandling(UngTestScenario ungTestGrunnlag) {
        return this.lagBehandling(ungTestGrunnlag, null, null);
    }

    private Behandling lagBehandling(UngTestScenario ungTestGrunnlag, BehandlingStegType behandlingStegType, AksjonspunktDefinisjon aksjonspunktDefinisjon) {
        TestScenarioBuilder scenarioBuilder = TestScenarioBuilder.builderMedSøknad()
            .medBehandlingType(BehandlingType.REVURDERING)
            .medUngTestGrunnlag(ungTestGrunnlag);

        if (aksjonspunktDefinisjon != null) {
            scenarioBuilder.leggTilAksjonspunkt(aksjonspunktDefinisjon, behandlingStegType);
        }

        var behandling = scenarioBuilder.buildOgLagreMedUng(ungTestRepositories);
        behandling.setBehandlingResultatType(BehandlingResultatType.INNVILGET);

        if (aksjonspunktDefinisjon != null) {
            Aksjonspunkt aksjonspunkt = behandling.getAksjonspunktFor(aksjonspunktDefinisjon);
            new AksjonspunktTestSupport().setTilUtført(aksjonspunkt, "utført");
            BehandlingRepository behandlingRepository = ungTestRepositories.repositoryProvider().getBehandlingRepository();
            behandlingRepository.lagre(behandling, behandlingRepository.taSkriveLås(behandling));
        }


        behandling.avsluttBehandling();

        return behandling;
    }

}
