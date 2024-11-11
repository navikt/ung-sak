package no.nav.ung.sak.domene.registerinnhenting;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;

import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import no.nav.ung.sak.behandling.BehandlingReferanse;
import no.nav.ung.sak.behandlingslager.behandling.EndringsresultatDiff;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.ung.sak.behandlingslager.diff.DiffResult;
import no.nav.ung.sak.behandlingslager.hendelser.StartpunktType;
import no.nav.ung.sak.db.util.JpaExtension;
import no.nav.ung.sak.test.util.behandling.TestScenarioBuilder;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;

@ExtendWith(CdiAwareExtension.class)
@ExtendWith(JpaExtension.class)
public class StartpunktTjenesteImplTest {

    private EndringStartpunktTjeneste tjeneste;

    @Inject
    private EntityManager entityManager;

    private BehandlingRepositoryProvider repositoryProvider;

    @Any
    @Inject
    private Instance<EndringStartpunktUtleder> startpunktUtledere;

    @BeforeEach
    public void before() {
        repositoryProvider = new BehandlingRepositoryProvider(entityManager);
        tjeneste = new EndringStartpunktTjeneste(startpunktUtledere);
    }

    @Test
    public void skal_returnere_startpunkt_for_endret_aggregat() {
        // Arrange
        var scenario = TestScenarioBuilder.builderMedSøknad();
        var behandling = scenario.lagre(repositoryProvider);

        // To forskjellige id-er indikerer endring på grunnlag
        long grunnlagId1 = 1L, grunnlagId2 = 2L;
        var endringsresultat = opprettEndringsresultat(grunnlagId1, grunnlagId2);
        var behRef = BehandlingReferanse.fra(behandling, LocalDate.now());

        // Act/Assert
        assertThat(tjeneste.utledStartpunktForDiffBehandlingsgrunnlag(behRef, endringsresultat)).isEqualTo(StartpunktType.KONTROLLER_FAKTA);
    }

    private EndringsresultatDiff opprettEndringsresultat(Long grunnlagId1, Long grunnlagId2) {

        EndringsresultatDiff endringsresultat = EndringsresultatDiff.opprett();
        DiffResult diffResult = mock(DiffResult.class);
        when(diffResult.isEmpty()).thenReturn(false); // Indikerer at det finnes diff
        endringsresultat.leggTilSporetEndring(EndringsresultatDiff.medDiff(GrunnlagAggregatMock.class, grunnlagId1, grunnlagId2), () -> diffResult);

        return endringsresultat;
    }
}
