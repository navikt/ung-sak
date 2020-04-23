package no.nav.k9.sak.ytelse.omsorgspenger.beregnytelse;

import no.nav.k9.aarskvantum.kontrakter.Aktivitet;
import no.nav.k9.aarskvantum.kontrakter.Arbeidsforhold;
import no.nav.k9.aarskvantum.kontrakter.LukketPeriode;
import no.nav.k9.aarskvantum.kontrakter.Utfall;
import no.nav.k9.aarskvantum.kontrakter.Uttaksperiode;
import no.nav.k9.aarskvantum.kontrakter.Uttaksplan;
import no.nav.k9.aarskvantum.kontrakter.Årsak;
import no.nav.k9.aarskvantum.kontrakter.Årskvantum;
import no.nav.k9.aarskvantum.kontrakter.ÅrskvantumResultat;
import no.nav.k9.kodeverk.uttak.UttakArbeidType;
import no.nav.k9.sak.kontrakt.uttak.*;
import no.nav.k9.sak.ytelse.beregning.regelmodell.UttakResultatPeriode;

import org.junit.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class MapFraÅrskvantumResultatTest {

    private static final BigDecimal _100 = BigDecimal.valueOf(100L);

    @Test
    public void map_fra_årskvantum_resultat() throws Exception {

        var år = new ÅrskvantumResultat(new Årskvantum("2020","1234567890",20,BigDecimal.TEN),lagUttaksplan());


        List<UttakResultatPeriode> perioder = new MapFraÅrskvantumResultat().mapFra(år);
        assertThat(perioder).hasSize(4);

        assertThat(perioder.stream().filter(p -> p.getErOppholdsPeriode())).hasSize(2);
        assertThat(perioder.stream().filter(p -> !p.getErOppholdsPeriode())).hasSize(2);
    }


    private Uttaksplan lagUttaksplan() {
        Uttaksplan uttaksplanOmsorgspenger = new Uttaksplan("123", UUID.randomUUID(), LocalDateTime.now(),lagAktiviteter());

        return uttaksplanOmsorgspenger;
    }

    private List<Aktivitet> lagAktiviteter() {
        List<Aktivitet> aktiviteter = new ArrayList<>();

        var arbeidsforhold1 = new Arbeidsforhold("ARBEIDSTAKER", "12434422323", "12345555", null);

        Aktivitet uttaksPlanOmsorgspengerAktivitet = new Aktivitet(arbeidsforhold1, lagUttaksperioder());

        aktiviteter.add(uttaksPlanOmsorgspengerAktivitet);

        return aktiviteter;
    }

    private List<Uttaksperiode> lagUttaksperioder() {
        var fom = LocalDate.now();
        var tom = fom.plusDays(3);
        var arbeidsforhold1 = new UttakArbeidsforhold("921484240", null, UttakArbeidType.ARBEIDSTAKER, null);
        var arbeidsforhold2 = new UttakArbeidsforhold("90589477", null, UttakArbeidType.ARBEIDSTAKER, null);
        var arbeidsforhold3 = new UttakArbeidsforhold("980484939", null, UttakArbeidType.ARBEIDSTAKER, null);
        return List.of(
            innvilget(fom, tom, _100),
            avslått(fom, tom),
            avslått(tom.plusDays(1), tom.plusDays(10)),
            innvilget(tom.plusDays(1), tom.plusDays(10), _100));
    }

    private Uttaksperiode innvilget(LocalDate fom, LocalDate tom, BigDecimal utbetalingsgrad) {
        return new Uttaksperiode(new LukketPeriode(fom, tom), Duration.ofHours(1), Utfall.AVSLÅTT, Årsak.AVSLÅTT_70ÅR, utbetalingsgrad);
    }

    private Uttaksperiode avslått(LocalDate fom, LocalDate tom) {
        return new Uttaksperiode(new LukketPeriode(fom, tom), Duration.ofHours(1), Utfall.AVSLÅTT, Årsak.AVSLÅTT_70ÅR, BigDecimal.ZERO);
    }
}
