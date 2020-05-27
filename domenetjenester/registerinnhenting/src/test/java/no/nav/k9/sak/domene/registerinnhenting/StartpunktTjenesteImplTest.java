package no.nav.k9.sak.domene.registerinnhenting;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;

import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.persistence.EntityManager;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingslager.behandling.EndringsresultatDiff;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.behandlingslager.diff.DiffResult;
import no.nav.k9.sak.behandlingslager.hendelser.StartpunktType;
import no.nav.k9.sak.db.util.UnittestRepositoryRule;
import no.nav.k9.sak.test.util.behandling.TestScenarioBuilder;
import no.nav.vedtak.felles.testutilities.cdi.CdiRunner;
import no.nav.vedtak.felles.testutilities.db.RepositoryRule;


@RunWith(CdiRunner.class)
public class StartpunktTjenesteImplTest {

    private StartpunktTjeneste tjeneste;

    @Rule
    public final RepositoryRule repoRule = new UnittestRepositoryRule();
    private final EntityManager entityManager = repoRule.getEntityManager();

    private final BehandlingRepositoryProvider repositoryProvider = new BehandlingRepositoryProvider(entityManager);

    @Any
    @Inject
    private Instance<StartpunktUtleder> startpunktUtledere;

    @Before
    public void before() {
        tjeneste = new StartpunktTjenesteImpl(startpunktUtledere, repositoryProvider);
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
