package no.nav.ung.sak.behandlingslager.ytelse;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.mock;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import no.nav.ung.kodeverk.ungdomsytelse.uttak.UngdomsytelseUttakAvslagsårsak;
import no.nav.ung.sak.behandlingslager.ytelse.sats.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.ung.kodeverk.behandling.BehandlingStatus;
import no.nav.ung.kodeverk.behandling.BehandlingType;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.sak.behandlingslager.behandling.BasicBehandlingBuilder;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.fagsak.Fagsak;
import no.nav.ung.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.ung.sak.behandlingslager.ytelse.uttak.UngdomsytelseUttakPeriode;
import no.nav.ung.sak.behandlingslager.ytelse.uttak.UngdomsytelseUttakPerioder;
import no.nav.ung.sak.db.util.JpaExtension;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.typer.AktørId;
import no.nav.ung.sak.typer.Saksnummer;

@ExtendWith(JpaExtension.class)
@ExtendWith(CdiAwareExtension.class)
class UngdomsytelseGrunnlagRepositoryTest {

    @Inject
    private EntityManager entityManager;
    private UngdomsytelseGrunnlagRepository repository;
    private BasicBehandlingBuilder behandlingBuilder;
    private Behandling behandling;

    @BeforeEach
    void setUp() {
        behandlingBuilder = new BasicBehandlingBuilder(entityManager);
        repository = new UngdomsytelseGrunnlagRepository(entityManager);
        Fagsak fagsak = Fagsak.opprettNy(FagsakYtelseType.UNGDOMSYTELSE, AktørId.dummy(), mock(Saksnummer.class), LocalDate.now(), null);
        new FagsakRepository(entityManager).opprettNy(fagsak);
        behandling = behandlingBuilder.opprettNyBehandling(fagsak, BehandlingType.FØRSTEGANGSSØKNAD, BehandlingStatus.UTREDES);
        var behandlingRepository = new BehandlingRepository(entityManager);
        behandlingRepository.lagre(behandling, behandlingRepository.taSkriveLås(behandling));

    }

    @Test
    void skal_kunne_lagre_ned_grunnlag_og_hente_opp_grunnlag() {
        var periode1 = new LocalDateInterval(LocalDate.now(), LocalDate.now());
        var dagsats = BigDecimal.TEN;
        var grunnbeløp = BigDecimal.valueOf(50);
        var grunnbeløpFaktor = BigDecimal.valueOf(2.041);
        lagreBeregning(periode1, dagsats, grunnbeløp, hentSatstypeOgGrunnbeløp(Sats.HØY), 0, 0);

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
        var grunnbeløpFaktor = BigDecimal.valueOf(2.041);
        var antallBarn = 2;
        var barnetilleggDagsats = 100;
        lagreBeregning(periode1, dagsats, grunnbeløp, hentSatstypeOgGrunnbeløp(Sats.HØY), antallBarn, barnetilleggDagsats);

        var uttakperioder1 = new UngdomsytelseUttakPerioder(List.of(new UngdomsytelseUttakPeriode(
                LocalDate.now(), LocalDate.now(), UngdomsytelseUttakAvslagsårsak.IKKE_NOK_DAGER
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
        assertThat(uttakperioder.get(0).getPeriode()).isEqualTo(DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now(), LocalDate.now()));
    }

    private void lagreBeregning(LocalDateInterval periode1, BigDecimal dagsats, BigDecimal grunnbeløp, SatsOgGrunnbeløpfaktor satsOgGrunnbeløpfaktor, int antallBarn, int barnetilleggDagsats) {
        var tidslinje = new LocalDateTimeline<>(List.of(
            lagSegment(periode1, dagsats, grunnbeløp, satsOgGrunnbeløpfaktor, antallBarn, barnetilleggDagsats)
        ));
        repository.lagre(behandling.getId(), new UngdomsytelseSatsResultat(tidslinje, "regelInput", "regelSporing"));
    }

    private static LocalDateSegment lagSegment(LocalDateInterval datoInterval, BigDecimal dagsats, BigDecimal grunnbeløp, SatsOgGrunnbeløpfaktor satsOgGrunnbeløpfaktor, int antallBarn, int barnetilleggDagsats) {
        return new LocalDateSegment(
            datoInterval,
            new UngdomsytelseSatser(
                dagsats,
                grunnbeløp,
                satsOgGrunnbeløpfaktor.grunnbeløpFaktor(), satsOgGrunnbeløpfaktor.satstype(), antallBarn, barnetilleggDagsats));
    }

    private static SatsOgGrunnbeløpfaktor hentSatstypeOgGrunnbeløp(Sats sats) {
        return GrunnbeløpfaktorTidslinje.hentGrunnbeløpfaktorTidslinjeFor(new LocalDateTimeline<>(List.of(
            new LocalDateSegment<>(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31), sats)
        ))).stream().findFirst().orElseThrow().getValue();
    }
}
