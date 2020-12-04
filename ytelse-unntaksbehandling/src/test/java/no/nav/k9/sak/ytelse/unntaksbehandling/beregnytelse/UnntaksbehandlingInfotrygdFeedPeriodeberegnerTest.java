package no.nav.k9.sak.ytelse.unntaksbehandling.beregnytelse;

import java.time.LocalDate;

import javax.inject.Inject;
import javax.persistence.EntityManager;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import no.nav.foreldrepenger.domene.vedtak.infotrygdfeed.InfotrygdFeedPeriode;
import no.nav.k9.kodeverk.behandling.BehandlingResultatType;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.k9.sak.behandlingslager.behandling.beregning.BeregningsresultatPeriode;
import no.nav.k9.sak.behandlingslager.behandling.beregning.BeregningsresultatRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.k9.sak.db.util.JpaExtension;
import no.nav.k9.sak.test.util.behandling.TestScenarioBuilder;
import no.nav.vedtak.felles.testutilities.cdi.CdiAwareExtension;

@ExtendWith(CdiAwareExtension.class)
@ExtendWith(JpaExtension.class)
class UnntaksbehandlingInfotrygdFeedPeriodeberegnerTest {

    @Inject
    private EntityManager entityManager;

    private BeregningsresultatRepository beregningsresultatRepository;

    private UnntaksbehandlingInfotrygdFeedPeriodeberegner testSubject;
    private TestScenarioBuilder scenario;
    private BehandlingRepository behandlingRepository;

    @BeforeEach
    void setUp() {
        behandlingRepository = new BehandlingRepository(entityManager);
        FagsakRepository fagsakRepository = new FagsakRepository(entityManager);
        beregningsresultatRepository = new BeregningsresultatRepository(entityManager);

        scenario = TestScenarioBuilder
            .builderMedSøknad(FagsakYtelseType.OMSORGSPENGER)
            .medBehandlingsresultat(BehandlingResultatType.INNVILGET);

        testSubject = new UnntaksbehandlingInfotrygdFeedPeriodeberegner(fagsakRepository, behandlingRepository, beregningsresultatRepository);
    }

    @Test
    void infotrygd_feed_periode_skal_tilsvare_første_og_siste_dato_på_beregningsresultat() {
        // Arrange
        LocalDate for20DagerSiden = LocalDate.now().minusDays(20);
        LocalDate for15DagerSiden = LocalDate.now().minusDays(15);

        scenario.medBehandlingVedtak();

        Behandling behandling = scenario.lagre(entityManager);
        avsluttBehandling(behandling);
        lagBeregningsresultatFor(
            behandling,
            for20DagerSiden,
            for15DagerSiden
        );

        // Act
        InfotrygdFeedPeriode infotrygdFeedPeriode = testSubject.finnInnvilgetPeriode(behandling.getFagsak().getSaksnummer());

        // Assert
        Assertions.assertThat(infotrygdFeedPeriode.getFom()).isEqualTo(for20DagerSiden);
        Assertions.assertThat(infotrygdFeedPeriode.getTom()).isEqualTo(for15DagerSiden);
    }

    private void avsluttBehandling(Behandling behandling) {
        behandling.avsluttBehandling();
        behandlingRepository.lagre(behandling, behandlingRepository.taSkriveLås(behandling));
    }

    private void lagBeregningsresultatFor(Behandling behandling, LocalDate fraOgMed, LocalDate tilOgMed) {
        BeregningsresultatEntitet.Builder builder = BeregningsresultatEntitet.builder()
            //TODO Hva brukes regel-greier til?
            .medRegelInput("ikkeInteressant")
            .medRegelSporing("ikkeInteressant");
        BeregningsresultatEntitet beregningsresultat = builder.build();
        BeregningsresultatPeriode brPeriode = buildBeregningsresultatPeriode(beregningsresultat, fraOgMed, tilOgMed);
        beregningsresultatRepository.lagre(behandling, beregningsresultat);
    }

    private BeregningsresultatPeriode buildBeregningsresultatPeriode(BeregningsresultatEntitet beregningsresultat, LocalDate fraOgMed, LocalDate tilOgMed) {
        return BeregningsresultatPeriode.builder()
            .medBeregningsresultatPeriodeFomOgTom(fraOgMed, tilOgMed)
            .build(beregningsresultat);
    }

}
