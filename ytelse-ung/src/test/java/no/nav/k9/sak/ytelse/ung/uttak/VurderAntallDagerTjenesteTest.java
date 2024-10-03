package no.nav.k9.sak.ytelse.ung.uttak;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.sak.domene.typer.tid.Virkedager;

class VurderAntallDagerTjenesteTest {

    @Test
    void skal_returnere_tomt_resultat_dersom_ingen_vilkår_oppfylt() {

        var ungdomsytelseUttakPerioder = VurderAntallDagerTjeneste.vurderAntallDagerOgLagUttaksperioder(LocalDateTimeline.empty());

        assertThat(ungdomsytelseUttakPerioder).isEmpty();
    }


    @Test
    void skal_returnere_resultat_med_en_virkedag_godkjent() {

        var fom = LocalDate.of(2024, 10, 3);
        var tom = LocalDate.of(2024, 10, 3);
        var ungdomsytelseUttakPerioder = VurderAntallDagerTjeneste.vurderAntallDagerOgLagUttaksperioder(new LocalDateTimeline<>(fom, tom, Boolean.TRUE));

        assertThat(ungdomsytelseUttakPerioder).isPresent();

        assertThat(ungdomsytelseUttakPerioder.get().getPerioder().size()).isEqualTo(1);
        var periode = ungdomsytelseUttakPerioder.get().getPerioder().getFirst();;
        assertThat(periode.getPeriode().getFomDato()).isEqualTo(fom);
        assertThat(periode.getPeriode().getTomDato()).isEqualTo(tom);
        assertThat(periode.getUtbetalingsgrad().compareTo(BigDecimal.valueOf(100))).isEqualTo(0);
    }


    @Test
    void skal_returnere_en_periode_uten_utbetaling_dersom_kun_helg_oppfylt() {

        var fom = LocalDate.of(2024, 10, 5);
        var tom = LocalDate.of(2024, 10, 6);
        var ungdomsytelseUttakPerioder = VurderAntallDagerTjeneste.vurderAntallDagerOgLagUttaksperioder(new LocalDateTimeline<>(fom, tom, Boolean.TRUE));

        assertThat(ungdomsytelseUttakPerioder).isPresent();

        assertThat(ungdomsytelseUttakPerioder.get().getPerioder().size()).isEqualTo(1);
        var periode = ungdomsytelseUttakPerioder.get().getPerioder().getFirst();;
        assertThat(periode.getPeriode().getFomDato()).isEqualTo(fom);
        assertThat(periode.getPeriode().getTomDato()).isEqualTo(tom);
        assertThat(periode.getUtbetalingsgrad().compareTo(BigDecimal.ZERO)).isEqualTo(0);
    }

    @Test
    void skal_returnere_en_periode_med_utbetaling_dersom_to_uker_oppfylt() {

        var mandag_to_uker_før = LocalDate.of(2024, 9, 22);
        var søndag = LocalDate.of(2024, 10, 6);

        var ungdomsytelseUttakPerioder = VurderAntallDagerTjeneste.vurderAntallDagerOgLagUttaksperioder(new LocalDateTimeline<>(mandag_to_uker_før, søndag, Boolean.TRUE));

        assertThat(ungdomsytelseUttakPerioder).isPresent();

        assertThat(ungdomsytelseUttakPerioder.get().getPerioder().size()).isEqualTo(1);
        var periode = ungdomsytelseUttakPerioder.get().getPerioder().getFirst();;
        assertThat(periode.getPeriode().getFomDato()).isEqualTo(mandag_to_uker_før);
        assertThat(periode.getPeriode().getTomDato()).isEqualTo(søndag);
        assertThat(periode.getUtbetalingsgrad().compareTo(BigDecimal.valueOf(100))).isEqualTo(0);
    }


    @Test
    void skal_returnere_en_periode_med_utbetaling_dersom_to_uker_oppfylt_minus_helg() {

        var mandag_to_uker_før = LocalDate.of(2024, 9, 22);
        var fredag = LocalDate.of(2024, 10, 4);

        var ungdomsytelseUttakPerioder = VurderAntallDagerTjeneste.vurderAntallDagerOgLagUttaksperioder(new LocalDateTimeline<>(mandag_to_uker_før, fredag, Boolean.TRUE));

        assertThat(ungdomsytelseUttakPerioder).isPresent();

        assertThat(ungdomsytelseUttakPerioder.get().getPerioder().size()).isEqualTo(1);
        var periode = ungdomsytelseUttakPerioder.get().getPerioder().getFirst();;
        assertThat(periode.getPeriode().getFomDato()).isEqualTo(mandag_to_uker_før);
        assertThat(periode.getPeriode().getTomDato()).isEqualTo(fredag);
        assertThat(periode.getUtbetalingsgrad().compareTo(BigDecimal.valueOf(100))).isEqualTo(0);
    }



    @Test
    void skal_gi_en_periode_oppfylt_dersom_godkjent_periode_er_364_dager_og_starter_på_torsdag() {

        var fom = LocalDate.of(2024, 10, 3);
        var tom = fom.plusWeeks(52).minusDays(1);

        var ungdomsytelseUttakPerioder = VurderAntallDagerTjeneste.vurderAntallDagerOgLagUttaksperioder(new LocalDateTimeline<>(fom, tom, Boolean.TRUE));

        assertThat(ungdomsytelseUttakPerioder).isPresent();

        assertThat(ungdomsytelseUttakPerioder.get().getPerioder().size()).isEqualTo(1);
        var periode = ungdomsytelseUttakPerioder.get().getPerioder().getFirst();;
        assertThat(periode.getPeriode().getFomDato()).isEqualTo(fom);
        assertThat(periode.getPeriode().getTomDato()).isEqualTo(tom);
        assertThat(periode.getUtbetalingsgrad().compareTo(BigDecimal.valueOf(100))).isEqualTo(0);
    }

    @Test
    void skal_gi_to_perioder_en_oppfylt_og_en_dag_avslått_dersom_godkjent_periode_365_dager_og_starter_på_torsdag() {

        var fom = LocalDate.of(2024, 10, 3);
        var tom = fom.plusWeeks(52);

        var ungdomsytelseUttakPerioder = VurderAntallDagerTjeneste.vurderAntallDagerOgLagUttaksperioder(new LocalDateTimeline<>(fom, tom, Boolean.TRUE));

        assertThat(ungdomsytelseUttakPerioder).isPresent();

        assertThat(ungdomsytelseUttakPerioder.get().getPerioder().size()).isEqualTo(2);
        var nokDagerPeriode = ungdomsytelseUttakPerioder.get().getPerioder().getFirst();;
        assertThat(nokDagerPeriode.getPeriode().getFomDato()).isEqualTo(fom);
        assertThat(nokDagerPeriode.getPeriode().getTomDato()).isEqualTo(tom.minusDays(1));
        assertThat(nokDagerPeriode.getUtbetalingsgrad().compareTo(BigDecimal.valueOf(100))).isEqualTo(0);

        var ikkeNokDagerPeriode = ungdomsytelseUttakPerioder.get().getPerioder().getLast();;
        assertThat(ikkeNokDagerPeriode.getPeriode().getFomDato()).isEqualTo(tom);
        assertThat(ikkeNokDagerPeriode.getPeriode().getTomDato()).isEqualTo(tom);
        assertThat(ikkeNokDagerPeriode.getUtbetalingsgrad().compareTo(BigDecimal.ZERO)).isEqualTo(0);
    }

    @Test
    void skal_gi_en_oppfylt_dersom_godkjent_periode_er_365_dager_og_starter_på_lørdag() {

        var fom = LocalDate.of(2024, 10, 5);
        var tom = fom.plusDays(365);

        var ungdomsytelseUttakPerioder = VurderAntallDagerTjeneste.vurderAntallDagerOgLagUttaksperioder(new LocalDateTimeline<>(fom, tom, Boolean.TRUE));

        assertThat(ungdomsytelseUttakPerioder).isPresent();

        assertThat(ungdomsytelseUttakPerioder.get().getPerioder().size()).isEqualTo(1);
        var nokDagerPeriode = ungdomsytelseUttakPerioder.get().getPerioder().getFirst();;
        assertThat(nokDagerPeriode.getPeriode().getFomDato()).isEqualTo(fom);
        assertThat(nokDagerPeriode.getPeriode().getTomDato()).isEqualTo(tom);
        assertThat(nokDagerPeriode.getUtbetalingsgrad().compareTo(BigDecimal.valueOf(100))).isEqualTo(0);
    }


    @Test
    void skal_gi_to_oppfylt_dersom_godkjent_periode_er_splittet_i_to_med_en_uke_mellomrom_og_260_virkedager_til_sammen() {

        var fom1 = LocalDate.of(2024, 10, 3);
        var tom1 = fom1.plusWeeks(40);
        var fom2 = tom1.plusDays(7);
        var tom2 = fom2.plusWeeks(12).minusDays(2);

        var ungdomsytelseUttakPerioder = VurderAntallDagerTjeneste.vurderAntallDagerOgLagUttaksperioder(new LocalDateTimeline<>(List.of(
            new LocalDateSegment<>(fom1, tom1, true),
            new LocalDateSegment<>(fom2, tom2, true)
        )));

        assertThat(ungdomsytelseUttakPerioder).isPresent();

        assertThat(ungdomsytelseUttakPerioder.get().getPerioder().size()).isEqualTo(2);
        var nokDagerPeriode1 = ungdomsytelseUttakPerioder.get().getPerioder().getFirst();;
        assertThat(nokDagerPeriode1.getPeriode().getFomDato()).isEqualTo(fom1);
        assertThat(nokDagerPeriode1.getPeriode().getTomDato()).isEqualTo(tom1);
        assertThat(nokDagerPeriode1.getUtbetalingsgrad().compareTo(BigDecimal.valueOf(100))).isEqualTo(0);

        var nokDagerPeriode2 = ungdomsytelseUttakPerioder.get().getPerioder().getLast();;
        assertThat(nokDagerPeriode2.getPeriode().getFomDato()).isEqualTo(fom2);
        assertThat(nokDagerPeriode2.getPeriode().getTomDato()).isEqualTo(tom2);
        assertThat(nokDagerPeriode2.getUtbetalingsgrad().compareTo(BigDecimal.valueOf(100))).isEqualTo(0);
    }

    @Test
    void skal_gi_to_oppfylt_og_en_avslått_dersom_godkjent_periode_er_splittet_i_to_med_en_uke_mellomrom_og_261_virkedager_til_sammen() {

        var fom1 = LocalDate.of(2024, 10, 3);
        var tom1 = fom1.plusWeeks(40);
        var fom2 = tom1.plusDays(7);
        var tom2 = fom2.plusWeeks(12).minusDays(1);

        var ungdomsytelseUttakPerioder = VurderAntallDagerTjeneste.vurderAntallDagerOgLagUttaksperioder(new LocalDateTimeline<>(List.of(
            new LocalDateSegment<>(fom1, tom1, true),
            new LocalDateSegment<>(fom2, tom2, true)
        )));

        assertThat(ungdomsytelseUttakPerioder).isPresent();

        assertThat(ungdomsytelseUttakPerioder.get().getPerioder().size()).isEqualTo(3);
        var iterator = ungdomsytelseUttakPerioder.get().getPerioder().iterator();
        var nokDagerPeriode1 = iterator.next();;
        assertThat(nokDagerPeriode1.getPeriode().getFomDato()).isEqualTo(fom1);
        assertThat(nokDagerPeriode1.getPeriode().getTomDato()).isEqualTo(tom1);
        assertThat(nokDagerPeriode1.getUtbetalingsgrad().compareTo(BigDecimal.valueOf(100))).isEqualTo(0);

        var nokDagerPeriode2 = iterator.next();;
        assertThat(nokDagerPeriode2.getPeriode().getFomDato()).isEqualTo(fom2);
        assertThat(nokDagerPeriode2.getPeriode().getTomDato()).isEqualTo(tom2.minusDays(1));
        assertThat(nokDagerPeriode2.getUtbetalingsgrad().compareTo(BigDecimal.valueOf(100))).isEqualTo(0);

        var ikkeNokDager = iterator.next();;
        assertThat(ikkeNokDager.getPeriode().getFomDato()).isEqualTo(tom2);
        assertThat(ikkeNokDager.getPeriode().getTomDato()).isEqualTo(tom2);
        assertThat(ikkeNokDager.getUtbetalingsgrad().compareTo(BigDecimal.ZERO)).isEqualTo(0);
    }


}
