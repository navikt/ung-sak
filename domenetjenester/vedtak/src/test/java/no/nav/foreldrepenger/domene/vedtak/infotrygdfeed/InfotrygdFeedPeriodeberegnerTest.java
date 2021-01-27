package no.nav.foreldrepenger.domene.vedtak.infotrygdfeed;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import javax.inject.Inject;
import javax.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import no.nav.k9.kodeverk.behandling.BehandlingResultatType;
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
class InfotrygdFeedPeriodeberegnerTest {
    @Inject
    private EntityManager entityManager;
    private BeregningsresultatRepository beregningsresultatRepository;
    private InfotrygdFeedPeriodeberegner testSubject;
    private BehandlingRepository behandlingRepository;

    @BeforeEach
    void setUp() {
        behandlingRepository = new BehandlingRepository(entityManager);
        FagsakRepository fagsakRepository = new FagsakRepository(entityManager);
        beregningsresultatRepository = new BeregningsresultatRepository(entityManager);

        testSubject = new InfotrygdFeedPeriodeberegner(fagsakRepository, behandlingRepository, beregningsresultatRepository);
    }

    @Test
    void infotrygd_feed_periode_skal_tilsvare_første_og_siste_dato_på_beregningsresultat() {
        // Arrange
        Behandling behandling = byggBehandlingMedVedtak();

        LocalDate for20DagerSiden = LocalDate.now().minusDays(20);
        LocalDate for15DagerSiden = LocalDate.now().minusDays(15);
        lagBeregningsresultatFor(behandling, for20DagerSiden, for15DagerSiden);

        // Act
        InfotrygdFeedPeriode infotrygdFeedPeriode = testSubject.finnInnvilgetPeriode(behandling.getFagsak().getSaksnummer());

        // Assert
        assertThat(infotrygdFeedPeriode.getFom()).isEqualTo(for20DagerSiden);
        assertThat(infotrygdFeedPeriode.getTom()).isEqualTo(for15DagerSiden);
    }

    @Test
    public void infotrygd_feed_periode_skal_gi_annullert_dersom_ingen_perioder() {
        // Arrange
        Behandling behandling = byggBehandlingMedVedtak();

        // Act
        InfotrygdFeedPeriode infotrygdFeedPeriode = testSubject.finnInnvilgetPeriode(behandling.getFagsak().getSaksnummer());

        // Assert
        assertThat(infotrygdFeedPeriode).isEqualTo(InfotrygdFeedPeriode.annullert());
    }

    private Behandling byggBehandlingMedVedtak() {
        TestScenarioBuilder builder = TestScenarioBuilder
            .builderUtenSøknad()
            .medBehandlingsresultat(BehandlingResultatType.INNVILGET);

        builder.medBehandlingVedtak();

        Behandling behandling = builder.lagre(entityManager);
        behandling.avsluttBehandling();
        behandlingRepository.lagre(behandling, behandlingRepository.taSkriveLås(behandling));

        return behandling;
    }

    private void lagBeregningsresultatFor(Behandling behandling, LocalDate fraOgMed, LocalDate tilOgMed) {
        BeregningsresultatEntitet.Builder builder = BeregningsresultatEntitet.builder()
            .medRegelInput("ikkeInteressant")
            .medRegelSporing("ikkeInteressant");
        BeregningsresultatEntitet beregningsresultat = builder.build();
        BeregningsresultatPeriode.builder()
            .medBeregningsresultatPeriodeFomOgTom(fraOgMed, tilOgMed)
            .build(beregningsresultat);
        beregningsresultatRepository.lagre(behandling, beregningsresultat);
    }
}
