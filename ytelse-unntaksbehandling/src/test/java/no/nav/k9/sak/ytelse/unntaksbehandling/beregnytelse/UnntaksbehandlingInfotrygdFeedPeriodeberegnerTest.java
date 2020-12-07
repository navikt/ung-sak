package no.nav.k9.sak.ytelse.unntaksbehandling.beregnytelse;

import static java.util.stream.Stream.of;
import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.OMSORGSPENGER;
import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.PLEIEPENGER_SYKT_BARN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.time.LocalDate;

import javax.inject.Inject;
import javax.persistence.EntityManager;

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
    private BehandlingRepository behandlingRepository;

    @BeforeEach
    void setUp() {
        behandlingRepository = new BehandlingRepository(entityManager);
        FagsakRepository fagsakRepository = new FagsakRepository(entityManager);
        beregningsresultatRepository = new BeregningsresultatRepository(entityManager);

        testSubject = new UnntaksbehandlingInfotrygdFeedPeriodeberegner(fagsakRepository, behandlingRepository, beregningsresultatRepository);
    }

    @Test
    void infotrygd_feed_periode_skal_tilsvare_første_og_siste_dato_på_beregningsresultat() {
        // Arrange
        LocalDate for20DagerSiden = LocalDate.now().minusDays(20);
        LocalDate for15DagerSiden = LocalDate.now().minusDays(15);

        TestScenarioBuilder builder = TestScenarioBuilder
            .builderUtenSøknad()
            .medBehandlingsresultat(BehandlingResultatType.INNVILGET);

        builder.medBehandlingVedtak();

        Behandling behandling = builder.lagre(entityManager);
        avsluttBehandling(behandling);
        lagBeregningsresultatFor(behandling, for20DagerSiden, for15DagerSiden);

        // Act
        InfotrygdFeedPeriode infotrygdFeedPeriode = testSubject.finnInnvilgetPeriode(behandling.getFagsak().getSaksnummer());

        // Assert
        assertThat(infotrygdFeedPeriode.getFom()).isEqualTo(for20DagerSiden);
        assertThat(infotrygdFeedPeriode.getTom()).isEqualTo(for15DagerSiden);
    }

    @Test
    void skal_utlede_ytelseskode_fra_ytelsetype_for_pleiepenger_sykt_barn() {
        // Arrange
        TestScenarioBuilder builder = TestScenarioBuilder
            .builderUtenSøknad(PLEIEPENGER_SYKT_BARN)
            .medBehandlingsresultat(BehandlingResultatType.INNVILGET);
        Behandling behandling = builder.lagre(entityManager);

        // Act
        String infotrygdYtelseKode = testSubject.getInfotrygdYtelseKode(behandling.getFagsak().getSaksnummer());

        // Assert
        assertThat(infotrygdYtelseKode).isEqualTo("PN");


    }

    @Test
    void skal_utlede_ytelseskode_fra_ytelsetype_for_omsorgspenger() {
        // Arrange
        TestScenarioBuilder builder = TestScenarioBuilder
            .builderUtenSøknad(OMSORGSPENGER)
            .medBehandlingsresultat(BehandlingResultatType.INNVILGET);
        Behandling behandling = builder.lagre(entityManager);

        // Act
        String infotrygdYtelseKode = testSubject.getInfotrygdYtelseKode(behandling.getFagsak().getSaksnummer());

        // Assert
        assertThat(infotrygdYtelseKode).isEqualTo("OM");
    }

    @Test
    void skal_ikke_støtte_utlede_ytelseskode_fra_ytelsetype_for_andre_ytelsetyper() {

        of(FagsakYtelseType.values())
            .filter(ytelse -> of(OMSORGSPENGER, PLEIEPENGER_SYKT_BARN).noneMatch(it -> it.equals(ytelse)))
            .forEach(ikkeStøttetYtelseType ->
                assertThatCode(() ->
                    {
                        // Arrange
                        TestScenarioBuilder builder = TestScenarioBuilder
                            .builderUtenSøknad(ikkeStøttetYtelseType)
                            .medBehandlingsresultat(BehandlingResultatType.INNVILGET);
                        Behandling behandling = builder.lagre(entityManager);

                        // Act
                        String infotrygdYtelseKode = testSubject.getInfotrygdYtelseKode(behandling.getFagsak().getSaksnummer());
                    }
                )
                    // Assert
                    .isInstanceOf(UnsupportedOperationException.class)
                    .hasMessageContaining("Kan ikke utlede infotrygdytelsekode for ytelsetype")
                    .hasMessageContaining(ikkeStøttetYtelseType.toString())
                    .hasMessageContaining("mapping for dette mangler")
            );
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
