package no.nav.k9.sak.domene.uttak.repo;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.LocalDate;
import java.util.Set;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.uttak.UttakArbeidType;
import no.nav.k9.sak.domene.uttak.BasicBehandlingBuilder;
import no.nav.k9.sak.domene.uttak.repo.OppgittTilsynsordning.OppgittTilsynSvar;
import no.nav.vedtak.felles.testutilities.cdi.CdiRunner;

@RunWith(CdiRunner.class)
public class UttakRepositoryTest {

    @Rule
    public final UnittestRepositoryRule repoRule = new UnittestRepositoryRule();

    @Inject
    private UttakRepository uttakRepository;

    private Behandling behandling;

    @Before
    public void before() throws Exception {
        behandling = new BasicBehandlingBuilder(repoRule.getEntityManager()).opprettOgLagreFørstegangssøknad(FagsakYtelseType.PLEIEPENGER_SYKT_BARN);
    }

    @Test
    public void skal_lagre_nytt_grunnlag_med_oppgitt_uttak() throws Exception {
        Long behandlingId = behandling.getId();
        var fom = LocalDate.now();
        var tom = LocalDate.now().plusDays(10);
        var p1 = new UttakAktivitetPeriode(fom, tom, UttakArbeidType.ARBEIDSTAKER);
        var p2 = new UttakAktivitetPeriode(tom.plusDays(1), tom.plusDays(10), UttakArbeidType.FRILANSER);

        var perioder = Set.of(p1, p2);
        var data = new UttakAktivitet(perioder);

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
        var p1 = new UttakAktivitetPeriode(fom, tom, UttakArbeidType.ARBEIDSTAKER);
        var p2 = new UttakAktivitetPeriode(tom.plusDays(1), tom.plusDays(10), UttakArbeidType.FRILANSER);

        var perioder = Set.of(p1, p2);
        var data = new UttakAktivitet(perioder);

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
        var p1 = new Søknadsperiode(fom, tom);
        var p2 = new Søknadsperiode(tom.plusDays(1), tom.plusDays(10));

        var perioder = Set.of(p1, p2);
        var data = new Søknadsperioder(perioder);

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
        var p1 = new FeriePeriode(fom, tom);
        var p2 = new FeriePeriode(tom.plusDays(1), tom.plusDays(10));

        var perioder = Set.of(p1, p2);
        var data = new Ferie(perioder);

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
        var p1 = new TilsynsordningPeriode(fom, tom, Duration.parse("P1DT3H"));
        var p2 = new TilsynsordningPeriode(tom.plusDays(1), tom.plusDays(10), Duration.ofHours(3));

        var perioder = Set.of(p1, p2);
        var data = new OppgittTilsynsordning(perioder, OppgittTilsynSvar.JA);

        uttakRepository.lagreOgFlushOppgittTilsynsordning(behandlingId, data);

        var data2 = uttakRepository.hentOppgittTilsynsordning(behandlingId);
        assertThat(data2).isNotNull();
        assertThat(data2.getOppgittTilsynSvar()).isEqualTo(OppgittTilsynSvar.JA);
        assertThat(data2.getPerioder()).hasSameSizeAs(data.getPerioder());
    }
}
