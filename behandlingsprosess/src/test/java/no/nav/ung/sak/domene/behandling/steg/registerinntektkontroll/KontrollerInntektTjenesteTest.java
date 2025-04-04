package no.nav.ung.sak.domene.behandling.steg.registerinntektkontroll;

import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.kodeverk.etterlysning.EtterlysningStatus;
import no.nav.ung.sak.uttalelse.EtterlysningInfo;
import no.nav.ung.sak.ytelse.EtterlysningOgRegisterinntekt;
import no.nav.ung.sak.ytelse.InntektType;
import no.nav.ung.sak.ytelse.RapportertInntekt;
import no.nav.ung.sak.ytelse.RapporterteInntekter;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class KontrollerInntektTjenesteTest {

    @Test
    void skal_sette_på_vent_til_rapporteringsfrist() {
        // Arrange
        final var fom = LocalDate.now().minusDays(10);
        final var tom = LocalDate.now().plusDays(10);
        LocalDateTimeline<Set<BehandlingÅrsakType>> prosessTriggerTidslinje = lagProsesstriggerTidslinjeForInntektRapportering(fom, tom);
        final var gjeldendeRapporterteInntekter = lagRapportertInntektTidslinje(fom, tom);
        LocalDateTimeline<EtterlysningOgRegisterinntekt> ikkeGodkjentUttalelseTidslinje = LocalDateTimeline.empty();

        // Act
        var resultat = KontrollerInntektTjeneste.utførKontroll(prosessTriggerTidslinje, gjeldendeRapporterteInntekter, ikkeGodkjentUttalelseTidslinje);

        // Assert
        assertEquals(new LocalDateTimeline<>(fom, tom, KontrollResultat.SETT_PÅ_VENT_TIL_RAPPORTERINGSFRIST), resultat);
    }

    @Test
    void skal_opprette_aksjonspunkt_dersom_brukes_har_gitt_uttalelse_og_registerinntekt_er_lik() {
        // Arrange
        final var fom = LocalDate.now().minusDays(10);
        final var tom = LocalDate.now().plusDays(10);
        final var bruker = 10;
        final var register = 10_000;
        LocalDateTimeline<Set<BehandlingÅrsakType>> prosessTriggerTidslinje = lagProsesstriggerTidslinjeForInntektRapporteringOgKontroll(fom, tom);
        final var gjeldendeRapporterteInntekter = lagRapportertInntektTidslinjeMedDiffMotRegister(fom, tom, register, bruker);
        LocalDateTimeline<EtterlysningOgRegisterinntekt> ikkeGodkjentUttalelseTidslinje = new LocalDateTimeline<>(fom, tom,
            new EtterlysningOgRegisterinntekt(Set.of(new RapportertInntekt(InntektType.ARBEIDSTAKER_ELLER_FRILANSER, BigDecimal.valueOf(register))), new EtterlysningInfo(EtterlysningStatus.MOTTATT_SVAR, false)));

        // Act
        var resultat = KontrollerInntektTjeneste.utførKontroll(prosessTriggerTidslinje, gjeldendeRapporterteInntekter, ikkeGodkjentUttalelseTidslinje);

        // Assert
        assertEquals(new LocalDateTimeline<>(fom, tom, KontrollResultat.OPPRETT_AKSJONSPUNKT), resultat);
    }

    @Test
    void skal_sette_på_vent_på_nytt_dersom_bruker_har_gitt_uttalelse_og_registerinntekt_er_ulik() {
        // Arrange
        final var fom = LocalDate.now().minusDays(10);
        final var tom = LocalDate.now().plusDays(10);
        final var bruker = 10;
        final var register = 10_000;
        final var registerFraUttalelse = 9999;
        LocalDateTimeline<Set<BehandlingÅrsakType>> prosessTriggerTidslinje = lagProsesstriggerTidslinjeForInntektRapporteringOgKontroll(fom, tom);
        final var gjeldendeRapporterteInntekter = lagRapportertInntektTidslinjeMedDiffMotRegister(fom, tom, register, bruker);
        LocalDateTimeline<EtterlysningOgRegisterinntekt> ikkeGodkjentUttalelseTidslinje = new LocalDateTimeline<>(fom, tom,
            new EtterlysningOgRegisterinntekt(Set.of(new RapportertInntekt(InntektType.ARBEIDSTAKER_ELLER_FRILANSER, BigDecimal.valueOf(registerFraUttalelse))), new EtterlysningInfo(EtterlysningStatus.MOTTATT_SVAR, false)));

        // Act
        var resultat = KontrollerInntektTjeneste.utførKontroll(prosessTriggerTidslinje, gjeldendeRapporterteInntekter, ikkeGodkjentUttalelseTidslinje);

        // Assert
        assertEquals(new LocalDateTimeline<>(fom, tom, KontrollResultat.OPPRETT_OPPGAVE_TIL_BRUKER_MED_NY_FRIST), resultat);
    }

    @Test
    void skal_bruke_brukers_inntekt_dersom_diff_mellom_rapportert_inntekt_fra_register_og_bruker_er_mindre_enn_akseptert_grense() {
        // Arrange
        final var fom = LocalDate.now().minusDays(10);
        final var tom = LocalDate.now().plusDays(10);
        LocalDateTimeline<Set<BehandlingÅrsakType>> prosessTriggerTidslinje = lagProsesstriggerTidslinjeForKontroll(fom, tom);
        final var gjeldendeRapporterteInntekter = lagRapportertInntektTidslinjeMedDiffMotRegister(fom, tom, 10_000, 10_001);
        LocalDateTimeline<EtterlysningOgRegisterinntekt> ikkeGodkjentUttalelseTidslinje = LocalDateTimeline.empty();

        // Act
        var resultat = KontrollerInntektTjeneste.utførKontroll(prosessTriggerTidslinje, gjeldendeRapporterteInntekter, ikkeGodkjentUttalelseTidslinje);

        // Assert
        assertEquals(new LocalDateTimeline<>(fom, tom, KontrollResultat.BRUK_INNTEKT_FRA_BRUKER), resultat);
    }

    @Test
    void skal_bruke_brukers_inntekt_dersom_ingen_rapportert_inntekt_fra_register_eller_bruker() {
        // Arrange
        final var fom = LocalDate.now().minusDays(10);
        final var tom = LocalDate.now().plusDays(10);
        LocalDateTimeline<Set<BehandlingÅrsakType>> prosessTriggerTidslinje = lagProsesstriggerTidslinjeForKontroll(fom, tom);
        final var gjeldendeRapporterteInntekter = ingenRapporterteInntekter(fom, tom);
        LocalDateTimeline<EtterlysningOgRegisterinntekt> ikkeGodkjentUttalelseTidslinje = LocalDateTimeline.empty();

        // Act
        var resultat = KontrollerInntektTjeneste.utførKontroll(prosessTriggerTidslinje, gjeldendeRapporterteInntekter, ikkeGodkjentUttalelseTidslinje);

        // Assert
        assertEquals(new LocalDateTimeline<>(fom, tom, KontrollResultat.BRUK_INNTEKT_FRA_BRUKER), resultat);
    }

    @Test
    void skal_opprette_oppgave_dersom_diff_mellom_rapportert_inntekt_fra_register_og_bruker_er_større_enn_akseptert_grense() {
        // Arrange
        final var fom = LocalDate.now().minusDays(10);
        final var tom = LocalDate.now().plusDays(10);
        LocalDateTimeline<Set<BehandlingÅrsakType>> prosessTriggerTidslinje = lagProsesstriggerTidslinjeForKontroll(fom, tom);
        final var gjeldendeRapporterteInntekter = lagRapportertInntektTidslinjeMedDiffMotRegister(fom, tom, 10_000, 11_001);
        LocalDateTimeline<EtterlysningOgRegisterinntekt> ikkeGodkjentUttalelseTidslinje = LocalDateTimeline.empty();

        // Act
        var resultat = KontrollerInntektTjeneste.utførKontroll(prosessTriggerTidslinje, gjeldendeRapporterteInntekter, ikkeGodkjentUttalelseTidslinje);

        // Assert
        assertEquals(new LocalDateTimeline<>(fom, tom, KontrollResultat.OPPRETT_OPPGAVE_TIL_BRUKER), resultat);
    }


    @Test
    void skal_opprette_oppgave_med_ny_frist_dersom_diff_mellom_rapportert_inntekt_fra_register_og_bruker_er_større_enn_akseptert_grense_og_det_finnes_eksisterende_ikke_bekreftet_oppgave_med_ulik_registerinntekt() {
        // Arrange
        final var fom = LocalDate.now().minusDays(10);
        final var tom = LocalDate.now().plusDays(10);
        LocalDateTimeline<Set<BehandlingÅrsakType>> prosessTriggerTidslinje = lagProsesstriggerTidslinjeForKontroll(fom, tom);
        final var gjeldendeRapporterteInntekter = lagRapportertInntektTidslinjeMedDiffMotRegister(fom, tom, 10_000, 11_001);
        LocalDateTimeline<EtterlysningOgRegisterinntekt> uttalelseTidslinje = new LocalDateTimeline<>(fom, tom,
            new EtterlysningOgRegisterinntekt(Set.of(new RapportertInntekt(InntektType.ARBEIDSTAKER_ELLER_FRILANSER, BigDecimal.valueOf(10_002))), new EtterlysningInfo(EtterlysningStatus.VENTER, null)));

        // Act
        var resultat = KontrollerInntektTjeneste.utførKontroll(prosessTriggerTidslinje, gjeldendeRapporterteInntekter, uttalelseTidslinje);

        // Assert
        assertEquals(new LocalDateTimeline<>(fom, tom, KontrollResultat.OPPRETT_OPPGAVE_TIL_BRUKER_MED_NY_FRIST), resultat);
    }

    @Test
    void skal_opprette_aksjonspunkt_dersom_ingen_inntekt_i_register_og_rapportert_inntekt_fra_bruker() {
        // Arrange
        final var fom = LocalDate.now().minusDays(10);
        final var tom = LocalDate.now().plusDays(10);
        LocalDateTimeline<Set<BehandlingÅrsakType>> prosessTriggerTidslinje = lagProsesstriggerTidslinjeForKontroll(fom, tom);
        final var gjeldendeRapporterteInntekter = lagRapportertInntektTidslinjeMedDiffMotRegister(fom, tom, 0, 11_001);
        LocalDateTimeline<EtterlysningOgRegisterinntekt> ikkeGodkjentUttalelseTidslinje = LocalDateTimeline.empty();

        // Act
        var resultat = KontrollerInntektTjeneste.utførKontroll(prosessTriggerTidslinje, gjeldendeRapporterteInntekter, ikkeGodkjentUttalelseTidslinje);

        // Assert
        assertEquals(new LocalDateTimeline<>(fom, tom, KontrollResultat.OPPRETT_AKSJONSPUNKT), resultat);
    }

    @Test
    void skal_gå_videre_med_brukers_inntekt_dersom_ingen_inntekt_i_register_eller_rapportert_fra_bruker() {
        // Arrange
        final var fom = LocalDate.now().minusDays(10);
        final var tom = LocalDate.now().plusDays(10);
        LocalDateTimeline<Set<BehandlingÅrsakType>> prosessTriggerTidslinje = lagProsesstriggerTidslinjeForKontroll(fom, tom);
        final var gjeldendeRapporterteInntekter = new LocalDateTimeline<RapporterteInntekter>(Set.of());
        LocalDateTimeline<EtterlysningOgRegisterinntekt> ikkeGodkjentUttalelseTidslinje = LocalDateTimeline.empty();

        // Act
        var resultat = KontrollerInntektTjeneste.utførKontroll(prosessTriggerTidslinje, gjeldendeRapporterteInntekter, ikkeGodkjentUttalelseTidslinje);

        // Assert
        assertEquals(new LocalDateTimeline<>(fom, tom, KontrollResultat.BRUK_INNTEKT_FRA_BRUKER), resultat);
    }




    private static LocalDateTimeline<Set<BehandlingÅrsakType>> lagProsesstriggerTidslinjeForInntektRapportering(LocalDate fom, LocalDate tom) {
        return new LocalDateTimeline<>(
            new LocalDateInterval(fom, tom),
            Set.of(BehandlingÅrsakType.RE_RAPPORTERING_INNTEKT)
        );
    }

    private static LocalDateTimeline<Set<BehandlingÅrsakType>> lagProsesstriggerTidslinjeForInntektRapporteringOgKontroll(LocalDate fom, LocalDate tom) {
        return new LocalDateTimeline<>(
            new LocalDateInterval(fom, tom),
            Set.of(BehandlingÅrsakType.RE_RAPPORTERING_INNTEKT, BehandlingÅrsakType.RE_KONTROLL_REGISTER_INNTEKT)
        );
    }

    private static LocalDateTimeline<Set<BehandlingÅrsakType>> lagProsesstriggerTidslinjeForKontroll(LocalDate fom, LocalDate tom) {
        return new LocalDateTimeline<>(
            new LocalDateInterval(fom, tom),
            Set.of(BehandlingÅrsakType.RE_KONTROLL_REGISTER_INNTEKT)
        );
    }

    private static LocalDateTimeline<RapporterteInntekter> lagRapportertInntektTidslinje(LocalDate fom, LocalDate tom) {
        LocalDateTimeline<RapporterteInntekter> gjeldendeRapporterteInntekter = new LocalDateTimeline<>(
            List.of(
                new LocalDateSegment<>(fom, tom, new RapporterteInntekter(Set.of(new RapportertInntekt(InntektType.ARBEIDSTAKER_ELLER_FRILANSER, BigDecimal.TEN)), Set.of()))
            )
        );
        return gjeldendeRapporterteInntekter;
    }

    private static LocalDateTimeline<RapporterteInntekter> lagRapportertInntektTidslinjeMedDiffMotRegister(LocalDate fom, LocalDate tom, int register, int bruker) {
        LocalDateTimeline<RapporterteInntekter> gjeldendeRapporterteInntekter = new LocalDateTimeline<>(
            List.of(
                new LocalDateSegment<>(fom, tom, new RapporterteInntekter(Set.of(
                    new RapportertInntekt(InntektType.ARBEIDSTAKER_ELLER_FRILANSER, BigDecimal.valueOf(bruker))), Set.of(
                    new RapportertInntekt(InntektType.ARBEIDSTAKER_ELLER_FRILANSER, BigDecimal.valueOf(register))
                )))
            )
        );
        return gjeldendeRapporterteInntekter;
    }

    private static LocalDateTimeline<RapporterteInntekter> ingenRapporterteInntekter(LocalDate fom, LocalDate tom) {
        LocalDateTimeline<RapporterteInntekter> gjeldendeRapporterteInntekter = new LocalDateTimeline<>(
            List.of(
                new LocalDateSegment<>(fom, tom, new RapporterteInntekter(Set.of(), Set.of()))
            )
        );
        return gjeldendeRapporterteInntekter;
    }


}
