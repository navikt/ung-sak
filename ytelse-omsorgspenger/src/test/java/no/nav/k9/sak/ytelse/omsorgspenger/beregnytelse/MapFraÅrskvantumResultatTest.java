package no.nav.k9.sak.ytelse.omsorgspenger.beregnytelse;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import no.nav.k9.aarskvantum.kontrakter.*;
import org.junit.jupiter.api.Test;

import no.nav.k9.sak.ytelse.beregning.regelmodell.UttakResultatPeriode;

public class MapFraÅrskvantumResultatTest {

    private static final BigDecimal _100 = BigDecimal.valueOf(100L);
    private final List<Hjemmel> hjemler = Arrays.asList(Hjemmel.FTRL_9_3__1, Hjemmel.COVID19_4_1__2);

    @Test
    public void map_fra_årskvantum_resultat() throws Exception {

        var år = new ÅrskvantumForbrukteDager(
            BigDecimal.TEN,
            BigDecimal.TEN,
            BigDecimal.TEN,
            BigDecimal.TEN,
            BigDecimal.TEN,
            Duration.ZERO,
            BigDecimal.TEN,
            Duration.ZERO,
            Duration.ZERO,
            lagUttaksplan(),
            new LinkedList<>(),
            new LinkedList<>()
        );


        List<UttakResultatPeriode> perioder = new MapFraÅrskvantumResultat().mapFra(år.getSisteUttaksplan().getAktiviteter());
        assertThat(perioder).hasSize(4);

        assertThat(perioder.stream().filter(p -> p.getErOppholdsPeriode())).hasSize(2);
        assertThat(perioder.stream().filter(p -> !p.getErOppholdsPeriode())).hasSize(2);
    }

    @Test
    void map_frilanser_arbeidsforhold() {
        var arbeidsforhold = new Arbeidsforhold("FL", null, null, null);
        var resultat = MapFraÅrskvantumResultat.mapArbeidsforhold(arbeidsforhold);
        assertThat(resultat.erFrilanser()).isTrue();
        assertThat(resultat.getIdentifikator()).isNull();
        assertThat(resultat.getArbeidsforholdId()).isNull();
    }

    private Uttaksplan lagUttaksplan() {
        Uttaksplan uttaksplanOmsorgspenger = new Uttaksplan("123", UUID.randomUUID(), lagAktiviteter(), false, true, Bekreftet.SYSTEMBEKREFTET, null, emptyList());

        return uttaksplanOmsorgspenger;
    }

    private List<Aktivitet> lagAktiviteter() {
        List<Aktivitet> aktiviteter = new ArrayList<>();

        var arbeidsforhold1 = new Arbeidsforhold("ARBEIDSTAKER", "12434422323", "12345555", null);
        var arbeidsforhold2 = new Arbeidsforhold("ARBEIDSTAKER", "12434422324", "12345554", null);

        Aktivitet uttaksPlanOmsorgspengerAktivitet = new Aktivitet(arbeidsforhold1, lagUttaksperioder());
        Aktivitet uttaksPlanOmsorgspengerAktivitet2 = new Aktivitet(arbeidsforhold2, lagUttaksperioder());

        aktiviteter.add(uttaksPlanOmsorgspengerAktivitet);
        aktiviteter.add(uttaksPlanOmsorgspengerAktivitet2);

        return aktiviteter;
    }

    private List<Uttaksperiode> lagUttaksperioder() {
        var fom = LocalDate.now();
        var tom = fom.plusDays(3);
        return List.of(
            innvilget(fom, tom, _100),
            avslått(fom, tom),
            avslått(tom.plusDays(1), tom.plusDays(10)),
            innvilget(tom.plusDays(1), tom.plusDays(10), _100));
    }

    private Uttaksperiode innvilget(LocalDate fom, LocalDate tom, BigDecimal utbetalingsgrad) {
        Map<Vilkår, Utfall> emptyVurderteVilkår = new HashMap<>();
        return new Uttaksperiode(new LukketPeriode(fom, tom), Duration.ofHours(1), Utfall.INNVILGET, new VurderteVilkår(emptyVurderteVilkår), hjemler, utbetalingsgrad, Periodetype.NY, tom.atStartOfDay(), null, Bekreftet.SYSTEMBEKREFTET, FraværÅrsak.ORDINÆRT_FRAVÆR, SøknadÅrsak.UDEFINERT, null, true, AvvikImSøknad.UDEFINERT);

    }

    private Uttaksperiode avslått(LocalDate fom, LocalDate tom) {
        Map<Vilkår, Utfall> emptyVurderteVilkår = new HashMap<>();
        return new Uttaksperiode(new LukketPeriode(fom, tom), Duration.ofHours(1), Utfall.AVSLÅTT, new VurderteVilkår(emptyVurderteVilkår), hjemler, BigDecimal.ZERO, Periodetype.NY, tom.atStartOfDay(), null, Bekreftet.SYSTEMBEKREFTET, FraværÅrsak.ORDINÆRT_FRAVÆR, SøknadÅrsak.UDEFINERT, null, true, AvvikImSøknad.UDEFINERT);

    }
}
