package no.nav.ung.sak.kontroll;

import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.prosesstask.impl.cron.CronExpression;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class RyddingAvInntektsrapporteringUtlederTest {

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }


    @Test
    void skal_returnere_periode_for_rydding_når_periode_er_bortfalt_i_rapporteringsvinduet() {
        // Arrange
        LocalDate desemberFom = LocalDate.of(2025, 12, 1);
        LocalDate desemberTom = LocalDate.of(2025, 12, 31);

        // Initielle perioder inkluderer desember 2025
        LocalDateTimeline<Boolean> initielleRelevantePerioder = new LocalDateTimeline<>(
            List.of(
                new LocalDateSegment<>(desemberFom, desemberTom, true)
            )
        );

        // Gjeldende relevante perioder er tom (perioden er bortfalt)
        LocalDateTimeline<Boolean> relevantePerioder = new LocalDateTimeline<>(List.of());

        // Statisk cron: rapporteringsfrist 8. januar, rapporteringsstart 1. januar
        CronExpression inntektskontrollCron = new CronExpression("0 0 7 8 * *");
        CronExpression inntektsrapporteringCron = new CronExpression("0 0 7 1 * *");

        // Simuler at vi er 5. januar (mellom 1. og 8.)
        ZonedDateTime testTidspunkt = ZonedDateTime.of(2026, 1, 5, 12, 0, 0, 0, ZoneId.systemDefault());

        // Act
        Optional<DatoIntervallEntitet> resultat = RyddingAvInntektsrapporteringUtleder.finnBortfaltRapporteringsperiode(
            initielleRelevantePerioder,
            relevantePerioder,
            testTidspunkt,
            inntektskontrollCron,
            inntektsrapporteringCron
        );

        // Assert
        assertThat(resultat).isPresent();
        assertThat(resultat.get().getFomDato()).isEqualTo(desemberFom);
        assertThat(resultat.get().getTomDato()).isEqualTo(desemberTom);
    }

    @Test
    void skal_ikke_returnere_periode_for_rydding_når_ingen_perioder_er_bortfalt() {
        // Arrange
        LocalDate desemberFom = LocalDate.of(2025, 12, 1);
        LocalDate desemberTom = LocalDate.of(2025, 12, 31);

        // Samme perioder både initielt og gjeldende
        LocalDateTimeline<Boolean> relevantePerioder = new LocalDateTimeline<>(
            List.of(
                new LocalDateSegment<>(desemberFom, desemberTom, true)
            )
        );

        CronExpression inntektskontrollCron = new CronExpression("0 0 7 8 * *");
        CronExpression inntektsrapporteringCron = new CronExpression("0 0 7 1 * *");
        ZonedDateTime testTidspunkt = ZonedDateTime.of(2026, 1, 5, 12, 0, 0, 0, ZoneId.systemDefault());

        // Act
        Optional<DatoIntervallEntitet> resultat = RyddingAvInntektsrapporteringUtleder.finnBortfaltRapporteringsperiode(
            relevantePerioder,
            relevantePerioder,
            testTidspunkt,
            inntektskontrollCron,
            inntektsrapporteringCron
        );

        // Assert
        assertThat(resultat).isEmpty();
    }

    @Test
    void skal_ikke_returnere_periode_for_rydding_når_vi_er_utenfor_rapporteringsvinduet() {
        // Arrange
        LocalDate novemberFom = LocalDate.of(2025, 11, 1);
        LocalDate novemberTom = LocalDate.of(2025, 11, 30);

        // Initielle perioder inkluderer november
        LocalDateTimeline<Boolean> initielleRelevantePerioder = new LocalDateTimeline<>(
            List.of(
                new LocalDateSegment<>(novemberFom, novemberTom, true)
            )
        );

        // Gjeldende perioder er tom (november er bortfalt)
        LocalDateTimeline<Boolean> relevantePerioder = new LocalDateTimeline<>(List.of());

        CronExpression inntektskontrollCron = new CronExpression("0 0 7 8 * *");
        CronExpression inntektsrapporteringCron = new CronExpression("0 0 7 1 * *");

        // Simuler at vi er 10. januar (etter rapporteringsfristen 8. januar)
        ZonedDateTime testTidspunkt = ZonedDateTime.of(2026, 1, 10, 12, 0, 0, 0, ZoneId.systemDefault());

        // Act
        Optional<DatoIntervallEntitet> resultat = RyddingAvInntektsrapporteringUtleder.finnBortfaltRapporteringsperiode(
            initielleRelevantePerioder,
            relevantePerioder,
            testTidspunkt,
            inntektskontrollCron,
            inntektsrapporteringCron
        );

        // Assert
        assertThat(resultat).isEmpty();
    }

    @Test
    void skal_kun_returnere_periode_for_inneværende_måned_minus_en() {
        // Arrange
        LocalDate novemberFom = LocalDate.of(2025, 11, 1);
        LocalDate novemberTom = LocalDate.of(2025, 11, 30);
        LocalDate desemberFom = LocalDate.of(2025, 12, 1);
        LocalDate desemberTom = LocalDate.of(2025, 12, 31);

        // Initielle relevante perioder inkluderer både november og desember
        LocalDateTimeline<Boolean> initielleRelevantePerioder = new LocalDateTimeline<>(
            List.of(
                new LocalDateSegment<>(novemberFom, novemberTom, true),
                new LocalDateSegment<>(desemberFom, desemberTom, true)
            )
        );

        // Gjeldende relevante perioder er tom (begge er bortfalt)
        LocalDateTimeline<Boolean> relevantePerioder = new LocalDateTimeline<>(List.of());

        CronExpression inntektskontrollCron = new CronExpression("0 0 7 8 * *");
        CronExpression inntektsrapporteringCron = new CronExpression("0 0 7 1 * *");

        // Simuler at vi er 5. januar (mellom 1. og 8.)
        ZonedDateTime testTidspunkt = ZonedDateTime.of(2026, 1, 5, 12, 0, 0, 0, ZoneId.systemDefault());

        // Act
        Optional<DatoIntervallEntitet> resultat = RyddingAvInntektsrapporteringUtleder.finnBortfaltRapporteringsperiode(
            initielleRelevantePerioder,
            relevantePerioder,
            testTidspunkt,
            inntektskontrollCron,
            inntektsrapporteringCron
        );

        // Assert
        assertThat(resultat).isPresent();
        // Skal kun returnere desember, ikke november
        assertThat(resultat.get().getFomDato()).isEqualTo(desemberFom);
        assertThat(resultat.get().getTomDato()).isEqualTo(desemberTom);
    }
}

