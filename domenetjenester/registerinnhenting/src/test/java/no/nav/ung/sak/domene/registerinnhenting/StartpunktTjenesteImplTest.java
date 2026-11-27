package no.nav.ung.sak.domene.registerinnhenting;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

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
import no.nav.ung.sak.behandlingslager.behandling.vilkår.VilkårsResultat;
import no.nav.ung.sak.behandlingslager.tilkjentytelse.KontrollertInntektPerioder;
import no.nav.ung.sak.behandlingslager.ytelse.UngdomsytelseGrunnlag;
import no.nav.ung.sak.diff.DiffResult;
import no.nav.ung.sak.behandlingslager.hendelser.StartpunktType;
import no.nav.ung.sak.db.util.JpaExtension;
import no.nav.ung.sak.test.util.behandling.ungdomsprogramytelse.TestScenarioBuilder;
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
        var behRef = BehandlingReferanse.fra(behandling);

        // Act/Assert
        assertThat(tjeneste.utledStartpunktForDiffBehandlingsgrunnlag(behRef, endringsresultat)).isEqualTo(StartpunktType.INIT_PERIODER);
    }

    @Test
    public void skal_flytte_til_vurder_kompletthet_ved_endring_i_aggregater_fra_synk() {
        var behandling = TestScenarioBuilder.builderMedSøknad().lagre(repositoryProvider);
        var behRef = BehandlingReferanse.fra(behandling);

        for (var aggregat : List.of(VilkårsResultat.class, KontrollertInntektPerioder.class, UngdomsytelseGrunnlag.class)) {
            assertThat(tjeneste.utledStartpunktForDiffBehandlingsgrunnlag(behRef, opprettEndringsresultat(aggregat, 1L, 2L)))
                .as(aggregat.getSimpleName())
                .isEqualTo(StartpunktType.VURDER_KOMPLETTHET);
        }
    }

    private EndringsresultatDiff opprettEndringsresultat(Long grunnlagId1, Long grunnlagId2) {
        return opprettEndringsresultat(GrunnlagAggregatMock.class, grunnlagId1, grunnlagId2);
    }

    private EndringsresultatDiff opprettEndringsresultat(Class<?> aggregat, Long grunnlagId1, Long grunnlagId2) {

        EndringsresultatDiff endringsresultat = EndringsresultatDiff.opprett();
        DiffResult diffResult = mock(DiffResult.class);
        when(diffResult.isEmpty()).thenReturn(false); // Indikerer at det finnes diff
        endringsresultat.leggTilSporetEndring(EndringsresultatDiff.medDiff(aggregat, grunnlagId1, grunnlagId2), () -> diffResult);

        return endringsresultat;
    }
}
