package no.nav.ung.ytelse.ungdomsprogramytelsen.revurdering.varselopphorvedmaksdato;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriode;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriodeRepository;
import no.nav.ung.sak.db.util.JpaExtension;
import no.nav.ung.sak.test.util.behandling.ungdomsprogramytelse.TestScenarioBuilder;
import no.nav.ung.ytelse.ungdomsprogramytelsen.ungdomsprogrammet.MaksdatoOpphørVarslingPeriode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(JpaExtension.class)
@ExtendWith(CdiAwareExtension.class)
class MaksdatoOpphorVarslingPeriodeRepositoryTest {

    private static final LocalDate FOM = LocalDate.now().minusMonths(6);

    @Inject
    private EntityManager entityManager;

    @Inject
    private UngdomsprogramPeriodeRepository ungdomsprogramPeriodeRepository;

    private AktuelleFagsakerForMaksdatoVarselRepository repository;

    @BeforeEach
    void setUp() {
        repository = new AktuelleFagsakerForMaksdatoVarselRepository(entityManager);
    }

    @Test
    void should_give_same_result_as_repository_method_when_maksdato_is_within_window() {
        assertSameResultForScenario(LocalDate.now().plusWeeks(2), LocalDate.now().plusWeeks(2));
    }

    @Test
    void should_give_same_result_as_repository_method_when_maksdato_is_outside_window() {
        assertSameResultForScenario(LocalDate.now().plusWeeks(4), LocalDate.now().plusWeeks(4));
    }

    @Test
    void should_give_same_result_as_repository_method_when_opphor_is_before_maksdato() {
        var maksdato = LocalDate.now().plusWeeks(2);
        assertSameResultForScenario(maksdato.minusDays(1), maksdato);
    }

    @Test
    void should_give_same_result_as_repository_method_for_other_ytelse_type() {
        var maksdato = LocalDate.now().plusWeeks(2);
        var behandling = TestScenarioBuilder.builderMedSøknad(FagsakYtelseType.AKTIVITETSPENGER).lagre(entityManager);
        lagreUngdomsprogramGrunnlag(behandling, maksdato, maksdato);

        var relevantForVarsling = MaksdatoOpphørVarslingPeriode.erRelevantForVarsling(maksdato, maksdato);
        var expected = relevantForVarsling && behandling.getFagsak().getYtelseType() == FagsakYtelseType.UNGDOMSYTELSE;
        var actual = isFagsakRelevantForVarselet(behandling);

        assertThat(actual).isEqualTo(expected);
    }

    private void assertSameResultForScenario(LocalDate tom, LocalDate maksdato) {
        var behandling = TestScenarioBuilder.builderMedSøknad().lagre(entityManager);
        lagreUngdomsprogramGrunnlag(behandling, tom, maksdato);

        var expected = MaksdatoOpphørVarslingPeriode.erRelevantForVarsling(tom, maksdato);
        var actual = isFagsakRelevantForVarselet(behandling);

        assertThat(actual).isEqualTo(expected);
    }

    private boolean isFagsakRelevantForVarselet(Behandling behandling) {
        return repository.hentFagsakerRelevantForMaksdatoVarsel().stream()
            .anyMatch(f -> f.getId().equals(behandling.getFagsak().getId()));
    }

    private void lagreUngdomsprogramGrunnlag(Behandling behandling, LocalDate tom, LocalDate maksdato) {
        ungdomsprogramPeriodeRepository.lagre(
            behandling.getId(),
            List.of(new UngdomsprogramPeriode(FOM, tom)),
            false,
            maksdato
        );
    }
}

