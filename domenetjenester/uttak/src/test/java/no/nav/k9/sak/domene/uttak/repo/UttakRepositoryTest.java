package no.nav.k9.sak.domene.uttak.repo;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.util.Set;

import javax.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.Rule;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;

import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.uttak.UttakArbeidType;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.db.util.UnittestRepositoryRule;
import no.nav.k9.sak.domene.uttak.BasicBehandlingBuilder;
import no.nav.k9.sak.domene.uttak.repo.OppgittTilsynsordning.OppgittTilsynSvar;
import no.nav.vedtak.felles.testutilities.cdi.CdiRunner;

@RunWith(CdiRunner.class)
public class UttakRepositoryTest {

    private static final Duration KORT_UKE = Duration.ofHours(10);

    private static final BigDecimal FULLTID_STILLING = BigDecimal.valueOf(100L);

    @Rule
    public final UnittestRepositoryRule repoRule = new UnittestRepositoryRule();

    @Inject
    private UttakRepository uttakRepository;

    private Behandling behandling;

    @BeforeEach
    public void before() throws Exception {
        behandling = new BasicBehandlingBuilder(repoRule.getEntityManager()).opprettOgLagreFørstegangssøknad(FagsakYtelseType.PLEIEPENGER_SYKT_BARN);
    }

    @Test
    public void skal_lagre_nytt_grunnlag_med_oppgitt_uttak() throws Exception {
        Long behandlingId = behandling.getId();
        var fom = LocalDate.now();
        var tom = LocalDate.now().plusDays(10);
        var data = new UttakAktivitet(Set.of(
            new UttakAktivitetPeriode(fom, tom, UttakArbeidType.ARBEIDSTAKER, KORT_UKE, FULLTID_STILLING),
            new UttakAktivitetPeriode(tom.plusDays(1), tom.plusDays(10), UttakArbeidType.FRILANSER, KORT_UKE, FULLTID_STILLING)));

        uttakRepository.lagreOgFlushOppgittUttak(behandlingId, data);

        var data2 = uttakRepository.hentOppgittUttak(behandlingId);
        assertThat(data2).isNotNull();
        assertThat(data2.getPerioder()).hasSameSizeAs(data.getPerioder());
    }

    @Test
    public void skal_lagre_nytt_grunnlag_med_fastsatt_uttak() throws Exception {
        Long behandlingId = behandling.getId();
        var fom = LocalDate.now();
        var tom = LocalDate.now().plusDays(10);
        var data = new UttakAktivitet(Set.of(
            new UttakAktivitetPeriode(fom, tom, UttakArbeidType.ARBEIDSTAKER, KORT_UKE, FULLTID_STILLING),
            new UttakAktivitetPeriode(tom.plusDays(1), tom.plusDays(10), UttakArbeidType.FRILANSER, KORT_UKE, FULLTID_STILLING)));

        uttakRepository.lagreOgFlushFastsattUttak(behandlingId, data);

        var data2 = uttakRepository.hentFastsattUttak(behandlingId);
        assertThat(data2).isNotNull();
        assertThat(data2.getPerioder()).hasSameSizeAs(data.getPerioder());
    }

    @Test
    public void skal_lagre_søknadsperioder() throws Exception {
        Long behandlingId = behandling.getId();
        var fom = LocalDate.now();
        var tom = LocalDate.now().plusDays(10);
        var data = new Søknadsperioder(Set.of(new Søknadsperiode(fom, tom), new Søknadsperiode(tom.plusDays(1), tom.plusDays(10))));

        uttakRepository.lagreOgFlushSøknadsperioder(behandlingId, data);

        var data2 = uttakRepository.hentOppgittSøknadsperioder(behandlingId);
        assertThat(data2).isNotNull();
        assertThat(data2.getPerioder()).hasSameSizeAs(data.getPerioder());
    }

    @Test
    public void skal_lagre_ferie() throws Exception {
        Long behandlingId = behandling.getId();
        var fom = LocalDate.now();
        var tom = LocalDate.now().plusDays(10);
        var data = new Ferie(Set.of(new FeriePeriode(fom, tom), new FeriePeriode(tom.plusDays(1), tom.plusDays(10))));

        uttakRepository.lagreOgFlushOppgittFerie(behandlingId, data);

        var data2 = uttakRepository.hentOppgittFerie(behandlingId);
        assertThat(data2).isNotNull();
        assertThat(data2.getPerioder()).hasSameSizeAs(data.getPerioder());
    }

    @Test
    public void skal_lagre_tilsynsordning() throws Exception {
        Long behandlingId = behandling.getId();
        var fom = LocalDate.now();
        var tom = LocalDate.now().plusDays(10);
        var data = new OppgittTilsynsordning(Set.of(
            new TilsynsordningPeriode(fom, tom, Duration.parse("P1DT3H")),
            new TilsynsordningPeriode(tom.plusDays(1), tom.plusDays(10), Duration.ofHours(3))),
            OppgittTilsynSvar.JA);

        uttakRepository.lagreOgFlushOppgittTilsynsordning(behandlingId, data);

        var data2 = uttakRepository.hentOppgittTilsynsordning(behandlingId);
        assertThat(data2).isNotNull();
        assertThat(data2.getOppgittTilsynSvar()).isEqualTo(OppgittTilsynSvar.JA);
        assertThat(data2.getPerioder()).hasSameSizeAs(data.getPerioder());
    }
}
