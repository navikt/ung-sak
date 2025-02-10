package no.nav.ung.sak.domene.behandling.steg.uttak;

import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.kodeverk.ungdomsytelse.uttak.UngdomsytelseUttakAvslagsårsak;

import no.nav.ung.sak.domene.behandling.steg.uttak.regler.RapportertInntekt;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class VurderAvslåttUttakTjenesteTest {

    private final Optional<LocalDate> INGEN_DØDSDATO = Optional.empty();

    @Test
    void skal_returnere_tomt_resultat_dersom_ingen_vilkår_oppfylt() {
        var ungdomsytelseUttakPerioder = VurderAvslåttUttakTjeneste.vurderUttakAvslag(LocalDateTimeline.empty(), LocalDateTimeline.empty(), INGEN_DØDSDATO);

        assertThat(ungdomsytelseUttakPerioder).isEmpty();
    }


    @Test
    void skal_gi__en_dag_avslått_dersom_godkjent_periode_365_dager_og_starter_på_torsdag() {
        var fom = LocalDate.of(2024, 10, 3);
        var tom = fom.plusWeeks(52);

        var ungdomsprogramtidslinje = new LocalDateTimeline<>(List.of(
            new LocalDateSegment<>(fom, tom, true)
        ));

        var ungdomsytelseUttakPerioder = VurderAvslåttUttakTjeneste.vurderUttakAvslag(new LocalDateTimeline<>(fom, tom, Boolean.TRUE),
            ungdomsprogramtidslinje,
            INGEN_DØDSDATO);

        assertThat(ungdomsytelseUttakPerioder).isPresent();

        assertThat(ungdomsytelseUttakPerioder.get().getPerioder().size()).isEqualTo(1);
        var ikkeNokDagerPeriode = ungdomsytelseUttakPerioder.get().getPerioder().getFirst();
        assertThat(ikkeNokDagerPeriode.getPeriode().getFomDato()).isEqualTo(tom);
        assertThat(ikkeNokDagerPeriode.getPeriode().getTomDato()).isEqualTo(tom);
    }

    @Test
    void skal_gi__en_avslått_dersom_godkjent_periode_er_splittet_i_to_med_en_uke_mellomrom_og_261_virkedager_til_sammen() {

        var fom1 = LocalDate.of(2024, 10, 3);
        var tom1 = fom1.plusWeeks(40);
        var fom2 = tom1.plusDays(7);
        var tom2 = fom2.plusWeeks(12).minusDays(1);

        var ungdomsprogramtidslinje = new LocalDateTimeline<>(List.of(
            new LocalDateSegment<>(fom1, tom1, true),
            new LocalDateSegment<>(fom2, tom2, true)
        ));

        var ungdomsytelseUttakPerioder = VurderAvslåttUttakTjeneste.vurderUttakAvslag(new LocalDateTimeline<>(List.of(
            new LocalDateSegment<>(fom1, tom1, true),
            new LocalDateSegment<>(fom2, tom2, true)
        )), ungdomsprogramtidslinje, INGEN_DØDSDATO);

        assertThat(ungdomsytelseUttakPerioder).isPresent();

        assertThat(ungdomsytelseUttakPerioder.get().getPerioder().size()).isEqualTo(1);
        var iterator = ungdomsytelseUttakPerioder.get().getPerioder().iterator();
        var ikkeNokDager = iterator.next();

        assertThat(ikkeNokDager.getPeriode().getFomDato()).isEqualTo(tom2);
        assertThat(ikkeNokDager.getPeriode().getTomDato()).isEqualTo(tom2);
    }


    @Test
    void skal_gi_to_avslått_dersom_godkjent_periode_er_splittet_i_tre_med_en_uke_mellomrom_mellom_hver_og_270_virkedager_til_sammen() {

        var fom1 = LocalDate.of(2024, 10, 3);
        var tom1 = fom1.plusWeeks(40);
        var fom2 = tom1.plusDays(7);
        var tom2 = fom2.plusWeeks(12).minusDays(1);

        var fom3 = tom2.plusDays(7);
        var tom3 = fom3.plusWeeks(1).minusDays(1);

        var ungdomsprogramtidslinje = new LocalDateTimeline<>(List.of(
            new LocalDateSegment<>(fom1, tom1, true),
            new LocalDateSegment<>(fom2, tom2, true),
            new LocalDateSegment<>(fom3, tom3, true)
        ));

        var ungdomsytelseUttakPerioder = VurderAvslåttUttakTjeneste.vurderUttakAvslag(new LocalDateTimeline<>(List.of(
            new LocalDateSegment<>(fom1, tom1, true),
            new LocalDateSegment<>(fom2, tom2, true),
            new LocalDateSegment<>(fom3, tom3, true)

        )), ungdomsprogramtidslinje, INGEN_DØDSDATO);

        assertThat(ungdomsytelseUttakPerioder).isPresent();

        assertThat(ungdomsytelseUttakPerioder.get().getPerioder().size()).isEqualTo(2);
        var iterator = ungdomsytelseUttakPerioder.get().getPerioder().iterator();
        var ikkeNokDager = iterator.next();

        assertThat(ikkeNokDager.getPeriode().getFomDato()).isEqualTo(tom2);
        assertThat(ikkeNokDager.getPeriode().getTomDato()).isEqualTo(tom2);


        var ikkeNokDager2 = iterator.next();

        assertThat(ikkeNokDager2.getPeriode().getFomDato()).isEqualTo(fom3);
        assertThat(ikkeNokDager2.getPeriode().getTomDato()).isEqualTo(tom3);
    }

    @Test
    void skal_gi_avslag_på_oppbrukte_dager_selvom_deltaker_ikke_har_søkt() {
        var fom1 = LocalDate.of(2024, 1, 1);
        var tom1 = fom1.plusWeeks(52).minusWeeks(1).minusDays(1); // 255 dager

        var fom2 = tom1.plusWeeks(2);
        var tom2 = fom2.plusWeeks(1).minusDays(1); // 5 dager

        var fom3 = tom2.plusWeeks(2);
        var tom3 = fom3.plusWeeks(1).minusDays(1); // 5 dager

        // 255 + 5 + 5 = 265 dager
        var ungdomsprogramtidslinje = new LocalDateTimeline<>(List.of(
            new LocalDateSegment<>(fom1, tom1, true), // 255 dager
            new LocalDateSegment<>(fom2, tom2, true), // 5 dager
            new LocalDateSegment<>(fom3, tom3, true) // 5 dager
        ));

        // Deltaker har kun søkt for to av periodene
        var søknadsperioder = new LocalDateTimeline<>(List.of(
            new LocalDateSegment<>(fom1, tom1, true),
            new LocalDateSegment<>(fom3, tom3, true)
        ));

        var ungdomsytelseUttakPerioder = VurderAvslåttUttakTjeneste.vurderUttakAvslag(søknadsperioder, ungdomsprogramtidslinje, INGEN_DØDSDATO);

        assertThat(ungdomsytelseUttakPerioder).isPresent();
        assertThat(ungdomsytelseUttakPerioder.get().getPerioder().size()).isEqualTo(1);

        var andrePeriode = ungdomsytelseUttakPerioder.get().getPerioder().get(0);
        assertThat(andrePeriode.getPeriode().getFomDato()).isEqualTo(fom3);
        assertThat(andrePeriode.getPeriode().getTomDato()).isEqualTo(tom3);
    }


    @Test
    void skal_gi_avslag_etter_brukers_død() {
        var fom = LocalDate.of(2024, 10, 3);
        var tom = fom.plusWeeks(52).minusDays(1);

        var ungdomsprogramtidslinje = new LocalDateTimeline<>(List.of(
            new LocalDateSegment<>(fom, tom, true)
        ));

        var dødsdato = fom.plusWeeks(10);
        var ungdomsytelseUttakPerioder = VurderAvslåttUttakTjeneste.vurderUttakAvslag(new LocalDateTimeline<>(fom, tom, Boolean.TRUE), ungdomsprogramtidslinje, Optional.of(dødsdato));

        assertThat(ungdomsytelseUttakPerioder).isPresent();

        assertThat(ungdomsytelseUttakPerioder.get().getPerioder().size()).isEqualTo(1);


        var periode = ungdomsytelseUttakPerioder.get().getPerioder().get(0);
        assertThat(periode.getPeriode().getFomDato()).isEqualTo(dødsdato.plusDays(1));
        assertThat(periode.getPeriode().getTomDato()).isEqualTo(tom);
        assertThat(periode.getAvslagsårsak()).isEqualTo(UngdomsytelseUttakAvslagsårsak.SØKERS_DØDSFALL);

    }

}
