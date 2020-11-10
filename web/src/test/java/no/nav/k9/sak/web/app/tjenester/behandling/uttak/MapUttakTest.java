package no.nav.k9.sak.web.app.tjenester.behandling.uttak;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import no.nav.k9.kodeverk.uttak.UttakArbeidType;
import no.nav.k9.sak.domene.uttak.repo.Ferie;
import no.nav.k9.sak.domene.uttak.repo.FeriePeriode;
import no.nav.k9.sak.domene.uttak.repo.OppgittTilsynsordning;
import no.nav.k9.sak.domene.uttak.repo.OppgittTilsynsordning.OppgittTilsynSvar;
import no.nav.k9.sak.domene.uttak.repo.Søknadsperiode;
import no.nav.k9.sak.domene.uttak.repo.Søknadsperioder;
import no.nav.k9.sak.domene.uttak.repo.TilsynsordningPeriode;
import no.nav.k9.sak.domene.uttak.repo.UttakAktivitet;
import no.nav.k9.sak.domene.uttak.repo.UttakAktivitetPeriode;
import no.nav.k9.sak.domene.uttak.repo.UttakGrunnlag;

public class MapUttakTest {
    private static final Duration KORT_UKE = Duration.ofHours(10);
    private static final BigDecimal FULLTID_STILLING = BigDecimal.valueOf(100L);

    private final MapUttak mapper = new MapUttak();
    private final LocalDate fom = LocalDate.now(), tom = fom.plusDays(10);
    private final UUID behandlingUuid = UUID.randomUUID();

    @Test
    public void test_map_oppgitt_uttak() throws Exception {

        var grunnlag = opprettGrunnlag(fom, tom);
        var res = mapper.mapOppgittUttak(behandlingUuid, grunnlag);
        assertThat(res).isNotNull();
    }

    @Test
    public void test_map_fastsatt_uttak() throws Exception {
        var grunnlag = opprettGrunnlag(fom, tom);
        var res = mapper.mapFastsattUttak(behandlingUuid, grunnlag);

        assertThat(res).isNotNull();

    }

    private UttakGrunnlag opprettGrunnlag(LocalDate fom, LocalDate tom) {

        LocalDate fom2 = tom.plusDays(1);
        LocalDate tom2 = fom2.plusDays(10);

        var søknadsperioder = new Søknadsperioder(new Søknadsperiode(fom, tom), new Søknadsperiode(fom2, tom2));
        var ferie = new Ferie(new FeriePeriode(fom, tom), new FeriePeriode(fom2, tom2));

        var oppgittTilsynsordning = new OppgittTilsynsordning(Set.of(
            new TilsynsordningPeriode(fom, tom, Duration.parse("P1DT3H")),
            new TilsynsordningPeriode(fom2, tom2, Duration.ofHours(3))),
            OppgittTilsynSvar.JA);

        var oppgittUttak = new UttakAktivitet(
            new UttakAktivitetPeriode(fom, tom, UttakArbeidType.ARBEIDSTAKER, KORT_UKE, FULLTID_STILLING),
            new UttakAktivitetPeriode(fom2, tom2, UttakArbeidType.FRILANSER, KORT_UKE, FULLTID_STILLING));
        var fastsattUttak = new UttakAktivitet(
            new UttakAktivitetPeriode(fom, tom, UttakArbeidType.ARBEIDSTAKER, KORT_UKE, FULLTID_STILLING),
            new UttakAktivitetPeriode(fom2, tom2, UttakArbeidType.FRILANSER, KORT_UKE, FULLTID_STILLING));

        Long behandlingId = 1L;
        var grunnlag = new UttakGrunnlag(behandlingId, oppgittUttak, fastsattUttak, søknadsperioder, ferie, oppgittTilsynsordning);
        return grunnlag;
    }
}
