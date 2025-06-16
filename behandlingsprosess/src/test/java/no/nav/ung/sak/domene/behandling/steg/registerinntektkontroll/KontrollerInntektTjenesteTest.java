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

    public static final BigDecimal AKSEPTERT_DIFFERANSE = BigDecimal.valueOf(1000);

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
            new EtterlysningOgRegisterinntekt(Set.of(new RapportertInntekt(InntektType.ARBEIDSTAKER_ELLER_FRILANSER, BigDecimal.valueOf(register))), new EtterlysningInfo(EtterlysningStatus.MOTTATT_SVAR, true)));

        // Act
        var resultat = utfør(prosessTriggerTidslinje, gjeldendeRapporterteInntekter, ikkeGodkjentUttalelseTidslinje);

        // Assert
        assertEquals(new LocalDateTimeline<>(fom, tom, KontrollResultatType.OPPRETT_AKSJONSPUNKT), resultat);
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
        var resultat = utfør(prosessTriggerTidslinje, gjeldendeRapporterteInntekter, ikkeGodkjentUttalelseTidslinje);

        // Assert
        assertEquals(new LocalDateTimeline<>(fom, tom, KontrollResultatType.FERDIG_KONTROLLERT), resultat);
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
        var resultat = utfør(prosessTriggerTidslinje, gjeldendeRapporterteInntekter, ikkeGodkjentUttalelseTidslinje);

        // Assert
        assertEquals(new LocalDateTimeline<>(fom, tom, KontrollResultatType.FERDIG_KONTROLLERT), resultat);
    }

    @Test
    void skal_bruke_brukers_inntekt_dersom_det_finnes_ytelse_som_er_lik_akseptert_grense() {
        // Arrange
        final var fom = LocalDate.now().minusDays(10);
        final var tom = LocalDate.now().plusDays(10);
        LocalDateTimeline<Set<BehandlingÅrsakType>> prosessTriggerTidslinje = lagProsesstriggerTidslinjeForKontroll(fom, tom);
        final var gjeldendeRapporterteInntekter = lagRapportertInntektTidslinjeMedDiffMotRegister(fom, tom, 0, 1000, 0);
        LocalDateTimeline<EtterlysningOgRegisterinntekt> ikkeGodkjentUttalelseTidslinje = LocalDateTimeline.empty();

        // Act
        var resultat = utfør(prosessTriggerTidslinje, gjeldendeRapporterteInntekter, ikkeGodkjentUttalelseTidslinje);

        // Assert
        assertEquals(new LocalDateTimeline<>(fom, tom, KontrollResultatType.FERDIG_KONTROLLERT), resultat);
    }

    @Test
    void skal_bruke_brukers_godkjente_inntekt_dersom_bruker_ikke_har_uttalt_seg_om_ytelse_og_inntekt_fra_register() {
        // Arrange
        final var fom = LocalDate.now().minusDays(10);
        final var tom = LocalDate.now().plusDays(10);
        LocalDateTimeline<Set<BehandlingÅrsakType>> prosessTriggerTidslinje = lagProsesstriggerTidslinjeForKontroll(fom, tom);
        final var gjeldendeRapporterteInntekter = lagRapportertInntektTidslinjeMedDiffMotRegister(fom, tom, 0, 1001, 0);
        LocalDateTimeline<EtterlysningOgRegisterinntekt> ikkeGodkjentUttalelseTidslinje = new LocalDateTimeline<>(
            fom, tom,
            new EtterlysningOgRegisterinntekt(Set.of(new RapportertInntekt(InntektType.YTELSE, BigDecimal.valueOf(1001))), new EtterlysningInfo(EtterlysningStatus.MOTTATT_SVAR, false))
        );

        // Act
        var resultat = utfør(prosessTriggerTidslinje, gjeldendeRapporterteInntekter, ikkeGodkjentUttalelseTidslinje);

        // Assert
        assertEquals(new LocalDateTimeline<>(fom, tom, KontrollResultatType.FERDIG_KONTROLLERT), resultat);
    }

    @Test
    void skal_bruke_brukers_godkjente_inntekt_dersom_bruker_tidligere_har_rapportert_inntekt_og_ikke_har_uttalt_seg_om_gjeldende_ytelse_og_inntekt_fra_register() {
        // Arrange
        final var fom = LocalDate.now().minusDays(10);
        final var tom = LocalDate.now().plusDays(10);
        LocalDateTimeline<Set<BehandlingÅrsakType>> prosessTriggerTidslinje = lagProsesstriggerTidslinjeForKontroll(fom, tom);
        final var gjeldendeRapporterteInntekter = lagRapportertInntektTidslinjeMedDiffMotRegister(fom, tom, 2000, 10_000, 500);
        LocalDateTimeline<EtterlysningOgRegisterinntekt> ikkeGodkjentUttalelseTidslinje = new LocalDateTimeline<>(
            fom, tom,
            new EtterlysningOgRegisterinntekt(Set.of(
                new RapportertInntekt(InntektType.ARBEIDSTAKER_ELLER_FRILANSER, BigDecimal.valueOf(2000)),
                new RapportertInntekt(InntektType.YTELSE, BigDecimal.valueOf(10_000))), new EtterlysningInfo(EtterlysningStatus.MOTTATT_SVAR, false))
        );

        // Act
        var resultat = utfør(prosessTriggerTidslinje, gjeldendeRapporterteInntekter, ikkeGodkjentUttalelseTidslinje);

        // Assert
        assertEquals(new LocalDateTimeline<>(fom, tom, KontrollResultatType.FERDIG_KONTROLLERT), resultat);
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
        var resultat = utfør(prosessTriggerTidslinje, gjeldendeRapporterteInntekter, ikkeGodkjentUttalelseTidslinje);

        // Assert
        assertEquals(new LocalDateTimeline<>(fom, tom, KontrollResultatType.OPPRETT_AKSJONSPUNKT), resultat);
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
        var resultat = utfør(prosessTriggerTidslinje, gjeldendeRapporterteInntekter, ikkeGodkjentUttalelseTidslinje);

        // Assert
        assertEquals(new LocalDateTimeline<>(fom, tom, KontrollResultatType.FERDIG_KONTROLLERT), resultat);
    }

    @Test
    void skal_gå_videre_med_register_inntekt_dersom_utløpt_etterlysning() {
        // Arrange
        final var fom = LocalDate.now().minusDays(10);
        final var tom = LocalDate.now().plusDays(10);
        final var bruker = 10;
        final var register = 10_000;
        LocalDateTimeline<Set<BehandlingÅrsakType>> prosessTriggerTidslinje = lagProsesstriggerTidslinjeForInntektRapporteringOgKontroll(fom, tom);
        final var gjeldendeRapporterteInntekter = lagRapportertInntektTidslinjeMedDiffMotRegister(fom, tom, register, bruker);
        LocalDateTimeline<EtterlysningOgRegisterinntekt> etterlysningTidslinje = new LocalDateTimeline<>(fom, tom,
            new EtterlysningOgRegisterinntekt(Set.of(new RapportertInntekt(InntektType.ARBEIDSTAKER_ELLER_FRILANSER, BigDecimal.valueOf(register))), new EtterlysningInfo(EtterlysningStatus.UTLØPT, null)));

        // Act
        var resultat = utfør(prosessTriggerTidslinje, gjeldendeRapporterteInntekter, etterlysningTidslinje);

        // Assert
        assertEquals(new LocalDateTimeline<>(fom, tom, KontrollResultatType.FERDIG_KONTROLLERT), resultat);
    }


    private LocalDateTimeline<KontrollResultatType> utfør(LocalDateTimeline<Set<BehandlingÅrsakType>> prosessTriggerTidslinje, LocalDateTimeline<RapporterteInntekter> gjeldendeRapporterteInntekter, LocalDateTimeline<EtterlysningOgRegisterinntekt> ikkeGodkjentUttalelseTidslinje) {
        return new KontrollerInntektTjeneste(AKSEPTERT_DIFFERANSE).utførKontroll(new KontrollerInntektInput(prosessTriggerTidslinje
                .mapValue(it -> true), gjeldendeRapporterteInntekter, ikkeGodkjentUttalelseTidslinje))
            .mapValue(Kontrollresultat::type);
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


    private static LocalDateTimeline<RapporterteInntekter> lagRapportertInntektTidslinjeMedDiffMotRegister(LocalDate fom, LocalDate tom, int registerATFL, int registerYtelse, int bruker) {
        LocalDateTimeline<RapporterteInntekter> gjeldendeRapporterteInntekter = new LocalDateTimeline<>(
            List.of(
                new LocalDateSegment<>(fom, tom, new RapporterteInntekter(Set.of(
                    new RapportertInntekt(InntektType.ARBEIDSTAKER_ELLER_FRILANSER, BigDecimal.valueOf(bruker))), Set.of(
                    new RapportertInntekt(InntektType.YTELSE, BigDecimal.valueOf(registerYtelse)),
                    new RapportertInntekt(InntektType.ARBEIDSTAKER_ELLER_FRILANSER, BigDecimal.valueOf(registerATFL))
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
