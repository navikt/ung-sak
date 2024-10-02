package no.nav.k9.sak.ytelse.ung.beregning;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.db.util.JpaExtension;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.test.util.behandling.TestScenarioBuilder;

@ExtendWith(JpaExtension.class)
@ExtendWith(CdiAwareExtension.class)
class UngdomsytelseGrunnlagRepositoryTest {

    @Inject
    private EntityManager entityManager;
    private UngdomsytelseGrunnlagRepository repository;
    private Behandling behandling;

    @BeforeEach
    void setUp() {
        repository = new UngdomsytelseGrunnlagRepository(entityManager);
        var repositoryProvider = new BehandlingRepositoryProvider(entityManager);
        TestScenarioBuilder førstegangScenario = TestScenarioBuilder.builderMedSøknad();
        behandling = førstegangScenario.lagre(repositoryProvider);

    }

    @Test
    void skal_kunne_lagre_ned_grunnlag_og_hente_opp_grunnlag() {

        var periode1 = new LocalDateInterval(LocalDate.now(), LocalDate.now());
        var dagsats = BigDecimal.TEN;
        var grunnbeløp = BigDecimal.valueOf(50);
        var grunnbeløpFaktor = BigDecimal.valueOf(2);
        repository.lagre(behandling.getId(), new LocalDateTimeline<>(List.of(
            lagSegment(periode1, dagsats, grunnbeløp, grunnbeløpFaktor)
        )));

        var ungdomsytelseGrunnlag = repository.hentGrunnlag(behandling.getId());
        assertThat(ungdomsytelseGrunnlag.isPresent()).isTrue();
        var perioder = ungdomsytelseGrunnlag.get().getSatsPerioder().getPerioder();
        assertThat(perioder.size()).isEqualTo(1);
        assertThat(perioder.get(0).getDagsats().compareTo(dagsats)).isEqualTo(0);
        assertThat(perioder.get(0).getPeriode()).isEqualTo(DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now(), LocalDate.now()));
        assertThat(perioder.get(0).getGrunnbeløp().compareTo(grunnbeløp)).isEqualTo(0);
        assertThat(perioder.get(0).getGrunnbeløpFaktor().compareTo(grunnbeløpFaktor)).isEqualTo(0);

    }

    private static LocalDateSegment lagSegment(LocalDateInterval datoInterval, BigDecimal dagsats, BigDecimal grunnbeløp, BigDecimal grunnbeløpFaktor) {
        return new LocalDateSegment(
            datoInterval,
            new UngdomsytelseSatser(
                dagsats,
                grunnbeløp,
                grunnbeløpFaktor));
    }
}
