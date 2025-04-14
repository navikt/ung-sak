package no.nav.ung.sak.formidling;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.ung.kodeverk.behandling.BehandlingResultatType;
import no.nav.ung.kodeverk.behandling.BehandlingStegType;
import no.nav.ung.kodeverk.behandling.BehandlingType;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.ung.sak.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.ung.sak.behandlingslager.behandling.aksjonspunkt.AksjonspunktTestSupport;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.ung.sak.db.util.JpaExtension;
import no.nav.ung.sak.formidling.innhold.EndringRapportertInntektInnholdBygger;
import no.nav.ung.sak.formidling.innhold.VedtaksbrevInnholdBygger;
import no.nav.ung.sak.formidling.vedtak.DetaljertResultatUtlederImpl;
import no.nav.ung.sak.perioder.ProsessTriggerPeriodeUtleder;
import no.nav.ung.sak.perioder.UngdomsytelseSøknadsperiodeTjeneste;
import no.nav.ung.sak.test.util.UngTestRepositories;
import no.nav.ung.sak.test.util.UnitTestLookupInstanceImpl;
import no.nav.ung.sak.test.util.behandling.TestScenarioBuilder;
import no.nav.ung.sak.ungdomsprogram.UngdomsprogramPeriodeTjeneste;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.LocalDate;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.mock;

@ExtendWith(CdiAwareExtension.class)
@ExtendWith(JpaExtension.class)
class VedtaksbrevReglerTest {

    private VedtaksbrevRegler vedtaksbrevRegler;

    @Inject
    private EntityManager entityManager;
    private UngTestRepositories ungTestRepositories;


    @BeforeEach
    void setup() {
        ungTestRepositories = UngTestRepositories.lagAlleUngTestRepositories(entityManager);
        vedtaksbrevRegler = lagVedtaksbrevRegler();
    }


    @Test
    void skal_ikke_kunne_redigere_brev_uten_aksjonspunkt() {
        LocalDate fom = LocalDate.of(2024, 12, 1);
        var ungTestGrunnlag = BrevScenarioer.endringMedInntektPå10k_19år(fom);
        TestScenarioBuilder scenarioBuilder = TestScenarioBuilder.builderMedSøknad()
            .medBehandlingType(BehandlingType.REVURDERING)
            .medUngTestGrunnlag(ungTestGrunnlag);

        var behandling = scenarioBuilder.buildOgLagreMedUng(ungTestRepositories);
        behandling.setBehandlingResultatType(BehandlingResultatType.INNVILGET);

        VedtaksbrevRegelResulat regelResulat = vedtaksbrevRegler.kjør(behandling.getId());

        var vedtaksbrevOperasjonerDto = regelResulat.vedtaksbrevOperasjoner();

        assertThat(vedtaksbrevOperasjonerDto.automatiskBrevOperasjoner().enableRediger()).isFalse();
        assertThat(vedtaksbrevOperasjonerDto.automatiskBrevOperasjoner().redigert()).isFalse();
        assertThat(vedtaksbrevOperasjonerDto.enableHindre()).isFalse();
        assertThat(vedtaksbrevOperasjonerDto.fritekstbrev()).isFalse();
        assertThat(vedtaksbrevOperasjonerDto.harBrev()).isTrue();
        assertThat(vedtaksbrevOperasjonerDto.hindret()).isFalse();

    }

    @Test
    void skal_kunne_redigere_brev_ved_aksjonspunkt() {
        LocalDate fom = LocalDate.of(2024, 12, 1);
        var ungTestGrunnlag = BrevScenarioer.endringMedInntektPå10k_19år(fom);
        TestScenarioBuilder scenarioBuilder = TestScenarioBuilder.builderMedSøknad()
            .medBehandlingType(BehandlingType.REVURDERING)
            .medUngTestGrunnlag(ungTestGrunnlag);

        scenarioBuilder.leggTilAksjonspunkt(AksjonspunktDefinisjon.KONTROLLER_INNTEKT, BehandlingStegType.KONTROLLER_REGISTER_INNTEKT);

        var behandling = scenarioBuilder.buildOgLagreMedUng(ungTestRepositories);
        behandling.setBehandlingResultatType(BehandlingResultatType.INNVILGET);

        Aksjonspunkt aksjonspunkt = behandling.getAksjonspunktFor(AksjonspunktDefinisjon.KONTROLLER_INNTEKT);
        new AksjonspunktTestSupport().setTilUtført(aksjonspunkt, "utført");
        var behandlingRepository = ungTestRepositories.repositoryProvider().getBehandlingRepository();
        behandlingRepository.lagre(behandling, behandlingRepository.taSkriveLås(behandling));

        VedtaksbrevRegelResulat regelResulat = vedtaksbrevRegler.kjør(behandling.getId());

        var vedtaksbrevOperasjonerDto = regelResulat.vedtaksbrevOperasjoner();

        assertThat(vedtaksbrevOperasjonerDto.automatiskBrevOperasjoner().enableRediger()).isTrue();
        assertThat(vedtaksbrevOperasjonerDto.automatiskBrevOperasjoner().redigert()).isFalse();
        assertThat(vedtaksbrevOperasjonerDto.enableHindre()).isTrue();
        assertThat(vedtaksbrevOperasjonerDto.fritekstbrev()).isFalse();
        assertThat(vedtaksbrevOperasjonerDto.harBrev()).isTrue();
        assertThat(vedtaksbrevOperasjonerDto.hindret()).isFalse();
        assertThat(vedtaksbrevOperasjonerDto.forklaring()).contains(AksjonspunktDefinisjon.KONTROLLER_INNTEKT.getKode());

    }

    private VedtaksbrevRegler lagVedtaksbrevRegler() {
        BehandlingRepositoryProvider repositoryProvider = ungTestRepositories.repositoryProvider();
        BehandlingRepository behandlingRepository = repositoryProvider.getBehandlingRepository();

        var detaljertResultatUtleder = new DetaljertResultatUtlederImpl(
            new ProsessTriggerPeriodeUtleder(ungTestRepositories.prosessTriggereRepository(),
                new UngdomsytelseSøknadsperiodeTjeneste(ungTestRepositories.ungdomsytelseStartdatoRepository(),
                    new UngdomsprogramPeriodeTjeneste(ungTestRepositories.ungdomsprogramPeriodeRepository()),
                    behandlingRepository)),
            ungTestRepositories.tilkjentYtelseRepository(),
            repositoryProvider.getVilkårResultatRepository());

        Instance<VedtaksbrevInnholdBygger> innholdByggere = new UnitTestLookupInstanceImpl<>(mock(EndringRapportertInntektInnholdBygger.class));

        return new VedtaksbrevRegler(
            behandlingRepository,
            innholdByggere,
            detaljertResultatUtleder
        );

    }

}
