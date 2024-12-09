package no.nav.ung.sak.ytelse.ung.beregning;

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
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.ung.sak.db.util.JpaExtension;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.test.util.behandling.TestScenarioBuilder;
import no.nav.ung.sak.ytelse.ung.uttak.UngdomsytelseUttakPeriode;
import no.nav.ung.sak.ytelse.ung.uttak.UngdomsytelseUttakPerioder;

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
        lagreBeregning(periode1, dagsats, grunnbeløp, Sats.HØY, 0, BigDecimal.ZERO);

        var ungdomsytelseGrunnlag = repository.hentGrunnlag(behandling.getId());
        assertThat(ungdomsytelseGrunnlag.isPresent()).isTrue();
        var perioder = ungdomsytelseGrunnlag.get().getSatsPerioder().getPerioder();
        assertThat(perioder.size()).isEqualTo(1);
        assertThat(perioder.get(0).getDagsats().compareTo(dagsats)).isEqualTo(0);
        assertThat(perioder.get(0).getPeriode()).isEqualTo(DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now(), LocalDate.now()));
        assertThat(perioder.get(0).getGrunnbeløp().compareTo(grunnbeløp)).isEqualTo(0);
        assertThat(perioder.get(0).getGrunnbeløpFaktor().compareTo(grunnbeløpFaktor)).isEqualTo(0);

    }

    @Test
    void skal_kunne_lagre_ned_uttak_og_hente_opp_grunnlag() {

        var periode1 = new LocalDateInterval(LocalDate.now(), LocalDate.now());
        var dagsats = BigDecimal.TEN;
        var grunnbeløp = BigDecimal.valueOf(50);
        var grunnbeløpFaktor = BigDecimal.valueOf(2);
        var antallBarn = 2;
        var barnetilleggDagsats = BigDecimal.valueOf(100);
        lagreBeregning(periode1, dagsats, grunnbeløp, Sats.HØY, antallBarn, barnetilleggDagsats);

        var utbetalingsgrad = BigDecimal.TEN;
        var uttakperioder1 = new UngdomsytelseUttakPerioder(List.of(new UngdomsytelseUttakPeriode(
            utbetalingsgrad, DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now(), LocalDate.now())
        )));
        uttakperioder1.setRegelInput("En input");
        uttakperioder1.setRegelSporing("En sporing");
        repository.lagre(behandling.getId(), uttakperioder1);

        var ungdomsytelseGrunnlag = repository.hentGrunnlag(behandling.getId());
        assertThat(ungdomsytelseGrunnlag.isPresent()).isTrue();
        var perioder = ungdomsytelseGrunnlag.get().getSatsPerioder().getPerioder();
        assertThat(perioder.size()).isEqualTo(1);
        assertThat(perioder.get(0).getDagsats().compareTo(dagsats)).isEqualTo(0);
        assertThat(perioder.get(0).getPeriode()).isEqualTo(DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now(), LocalDate.now()));
        assertThat(perioder.get(0).getGrunnbeløp().compareTo(grunnbeløp)).isEqualTo(0);
        assertThat(perioder.get(0).getGrunnbeløpFaktor().compareTo(grunnbeløpFaktor)).isEqualTo(0);


        var uttakperioder = ungdomsytelseGrunnlag.get().getUttakPerioder().getPerioder();
        assertThat(uttakperioder.size()).isEqualTo(1);
        assertThat(uttakperioder.get(0).getUtbetalingsgrad().compareTo(utbetalingsgrad)).isEqualTo(0);
        assertThat(uttakperioder.get(0).getPeriode()).isEqualTo(DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now(), LocalDate.now()));
    }

    private void lagreBeregning(LocalDateInterval periode1, BigDecimal dagsats, BigDecimal grunnbeløp, Sats sats, int antallBarn, BigDecimal barnetilleggDagsats) {
        var tidslinje = new LocalDateTimeline<>(List.of(
            lagSegment(periode1, dagsats, grunnbeløp, sats, antallBarn, barnetilleggDagsats)
        ));
        repository.lagre(behandling.getId(), new UngdomsytelseSatsResultat(tidslinje, "regelInput", "regelSporing"));
    }

    private static LocalDateSegment lagSegment(LocalDateInterval datoInterval, BigDecimal dagsats, BigDecimal grunnbeløp, Sats sats, int antallBarn, BigDecimal barnetilleggDagsats) {
        return new LocalDateSegment(
            datoInterval,
            new UngdomsytelseSatser(
                dagsats,
                grunnbeløp,
                sats.getGrunnbeløpFaktor(), sats.getSatsType(), antallBarn, barnetilleggDagsats));
    }
}
