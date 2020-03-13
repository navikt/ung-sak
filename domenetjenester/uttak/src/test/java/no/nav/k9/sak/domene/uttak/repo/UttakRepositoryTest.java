package no.nav.k9.sak.domene.uttak.repo;

import static org.assertj.core.api.Assertions.assertThat;

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
        var uttak = new UttakAktivitet(perioder);

        uttakRepository.lagreOgFlushOppgittUttak(behandlingId, uttak);

        var uttak2 = uttakRepository.hentOppgittUttak(behandlingId);
        assertThat(uttak2).isNotNull();
        assertThat(uttak2.getPerioder()).hasSameSizeAs(uttak.getPerioder());
    }

    @Test
    public void skal_lagre_nytt_grunnlag_med_fastsatt_uttak() throws Exception {
        Long behandlingId = behandling.getId();
        var fom = LocalDate.now();
        var tom = LocalDate.now().plusDays(10);
        var p1 = new UttakAktivitetPeriode(fom, tom, UttakArbeidType.ARBEIDSTAKER);
        var p2 = new UttakAktivitetPeriode(tom.plusDays(1), tom.plusDays(10), UttakArbeidType.FRILANSER);

        var perioder = Set.of(p1, p2);
        var uttak = new UttakAktivitet(perioder);

        uttakRepository.lagreOgFlushFastsattUttak(behandlingId, uttak);

        var uttak2 = uttakRepository.hentFastsattUttak(behandlingId);
        assertThat(uttak2).isNotNull();
        assertThat(uttak2.getPerioder()).hasSameSizeAs(uttak.getPerioder());
    }

    @Test
    public void skal_lagre_søknadsperioder() throws Exception {
        Long behandlingId = behandling.getId();
        var fom = LocalDate.now();
        var tom = LocalDate.now().plusDays(10);
        var p1 = new Søknadsperiode(fom, tom);
        var p2 = new Søknadsperiode(tom.plusDays(1), tom.plusDays(10));

        var perioder = Set.of(p1, p2);
        var søknadsperioder = new Søknadsperioder(perioder);

        uttakRepository.lagreOgFlushSøknadsperioder(behandlingId, søknadsperioder);

        var søknasperioder2 = uttakRepository.hentOppgittSøknadsperioder(behandlingId);
        assertThat(søknasperioder2).isNotNull();
        assertThat(søknasperioder2.getPerioder()).hasSameSizeAs(søknadsperioder.getPerioder());
    }

    @Test
    public void skal_lagre_ferie() throws Exception {
        Long behandlingId = behandling.getId();
        var fom = LocalDate.now();
        var tom = LocalDate.now().plusDays(10);
        var p1 = new FeriePeriode(fom, tom);
        var p2 = new FeriePeriode(tom.plusDays(1), tom.plusDays(10));

        var perioder = Set.of(p1, p2);
        var ferie = new Ferie(perioder);

        uttakRepository.lagreOgFlushOppgittFerie(behandlingId, ferie);

        var feire2 = uttakRepository.hentOppgittFerie(behandlingId);
        assertThat(feire2).isNotNull();
        assertThat(feire2.getPerioder()).hasSameSizeAs(ferie.getPerioder());
    }
}
