package no.nav.ung.sak.domene.behandling.steg.uttak;

import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.kodeverk.arbeidsforhold.ArbeidType;
import no.nav.ung.sak.domene.iay.modell.InntektArbeidYtelseGrunnlagBuilder;
import no.nav.ung.sak.domene.iay.modell.InntektArbeidYtelseTjeneste;
import no.nav.ung.sak.domene.iay.modell.OppgittOpptjening;
import no.nav.ung.sak.domene.iay.modell.OppgittOpptjeningBuilder;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.typer.JournalpostId;
import no.nav.ung.sak.ytelse.InntektType;
import no.nav.ung.sak.ytelse.RapportertInntekt;
import no.nav.ung.sak.ytelse.RapportertInntektMapper;
import no.nav.ung.sak.ytelse.RapporterteInntekter;
import no.nav.ung.sak.ytelseperioder.MånedsvisTidslinjeUtleder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RapportertInntektMapperTest {

    private final InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste = mock(InntektArbeidYtelseTjeneste.class);
    private final MånedsvisTidslinjeUtleder ytelsesperiodeutleder = mock(MånedsvisTidslinjeUtleder.class);
    private RapportertInntektMapper rapportertInntektMapper;

    @BeforeEach
    void setUp() {
        when(ytelsesperiodeutleder.periodiserMånedsvis(anyLong())).thenReturn(new LocalDateTimeline<>(List.of(
            new LocalDateSegment<>(LocalDate.now(), LocalDate.now().with(TemporalAdjusters.lastDayOfMonth()), YearMonth.now()),
            new LocalDateSegment<>(LocalDate.now().plusMonths(1).withDayOfMonth(1), LocalDate.now().plusMonths(1).with(TemporalAdjusters.lastDayOfMonth()), YearMonth.now().plusMonths(1)))));
        rapportertInntektMapper = new RapportertInntektMapper(inntektArbeidYtelseTjeneste, ytelsesperiodeutleder);
    }

    @Test
    void skal_mappe_en_mottatt_inntekt() {
        // Arrange
        final var periode = DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now(), LocalDate.now().with(TemporalAdjusters.lastDayOfMonth()));
        final var inntekt = BigDecimal.TEN;
        final var oppgittOpptjening = lagMottattATFLInntekt(periode, inntekt, LocalDateTime.now());
        mockIAY(List.of(oppgittOpptjening));

        // Act
        final var tidslinje = rapportertInntektMapper.mapAlleGjeldendeRegisterOgBrukersInntekter(1L);

        // Assert
        final var forventet = new LocalDateTimeline<>(periode.getFomDato(), periode.getTomDato(),
            new RapporterteInntekter(Set.of(new RapportertInntekt(InntektType.ARBEIDSTAKER_ELLER_FRILANSER, inntekt)),
                Set.of()));
        assertThat(tidslinje).isEqualTo(forventet);

    }

    @Test
    void skal_mappe_mottatte_inntekter_for_arbeid() {
        // Arrange
        final var periode = DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now(), LocalDate.now().with(TemporalAdjusters.lastDayOfMonth()));
        final var arbeidsinntekt = BigDecimal.TEN;
        final var oppgittOpptjening = lagMottattATFLInntekt(periode, arbeidsinntekt, LocalDateTime.now());
        mockIAY(List.of(oppgittOpptjening));

        // Act
        final var tidslinje = rapportertInntektMapper.mapAlleGjeldendeRegisterOgBrukersInntekter(1L);

        // Assert
        final var forventet = new LocalDateTimeline<>(periode.getFomDato(), periode.getTomDato(),
            new RapporterteInntekter(Set.of(
                new RapportertInntekt(InntektType.ARBEIDSTAKER_ELLER_FRILANSER, arbeidsinntekt)
            ), Set.of()));
        assertThat(tidslinje).isEqualTo(forventet);
    }

    @Test
    void skal_mappe_to_mottatte_inntekter_for_forskjellige_perioder() {
        // Arrange
        final var fom = LocalDate.now();
        final var tom = LocalDate.now().with(TemporalAdjusters.lastDayOfMonth());
        final var periode = DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom);
        final var inntekt = BigDecimal.TEN;
        final var innsendtTidspunkt = LocalDateTime.now();
        final var oppgittOpptjening = lagMottattATFLInntekt(periode, inntekt, innsendtTidspunkt);
        final var fom2 = fom.plusMonths(1).withDayOfMonth(1);
        final var tom2 = fom2.with(TemporalAdjusters.lastDayOfMonth());
        final var periode2 = DatoIntervallEntitet.fraOgMedTilOgMed(fom2, tom2);
        final var inntekt2 = BigDecimal.valueOf(50);
        final var oppgittOpptjening2 = lagMottattATFLInntekt(periode2, inntekt2, innsendtTidspunkt);

        mockIAY(List.of(oppgittOpptjening, oppgittOpptjening2));

        // Act
        final var tidslinje = rapportertInntektMapper.mapAlleGjeldendeRegisterOgBrukersInntekter(1L);

        // Assert
        final var forventet = new LocalDateTimeline<>(
            List.of(
                new LocalDateSegment<>(periode.getFomDato(), periode.getTomDato(), new RapporterteInntekter(
                    Set.of(new RapportertInntekt(InntektType.ARBEIDSTAKER_ELLER_FRILANSER, inntekt)),
                    Set.of())),
                new LocalDateSegment<>(periode2.getFomDato(), periode2.getTomDato(), new RapporterteInntekter(
                    Set.of(new RapportertInntekt(InntektType.ARBEIDSTAKER_ELLER_FRILANSER, inntekt2)),
                    Set.of()))
            ));
        assertThat(tidslinje).isEqualTo(forventet);
    }

    @Test
    void skal_mappe_to_mottatte_inntekter_for_samme_perioder() {
        // Arrange
        final var fom = LocalDate.now();
        final var tom = LocalDate.now().with(TemporalAdjusters.lastDayOfMonth());
        final var periode = DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom);
        final var inntekt = BigDecimal.TEN;
        final var innsendtTidspunkt = LocalDateTime.now().minusDays(1);
        final var oppgittOpptjening = lagMottattATFLInntekt(periode, inntekt, innsendtTidspunkt);

        final var inntekt2 = BigDecimal.valueOf(50);
        final var innsendtTidspunkt2 = LocalDateTime.now();
        final var oppgittOpptjening2 = lagMottattATFLInntekt(periode, inntekt2, innsendtTidspunkt2);

        mockIAY(List.of(oppgittOpptjening, oppgittOpptjening2));

        // Act
        final var tidslinje = rapportertInntektMapper.mapAlleGjeldendeRegisterOgBrukersInntekter(1L);

        // Assert
        final var forventet = new LocalDateTimeline<>(periode.getFomDato(), periode.getTomDato(),
            new RapporterteInntekter(
                Set.of(new RapportertInntekt(InntektType.ARBEIDSTAKER_ELLER_FRILANSER, inntekt2)),
                Set.of()));
        assertThat(tidslinje).isEqualTo(forventet);
    }


    private static OppgittOpptjening lagMottattATFLInntekt(DatoIntervallEntitet periode, BigDecimal inntekt, LocalDateTime innsendt) {
        final var oppgittArbeidsforhold = lagOppgittArbeidOgFrilansInntekt(periode, inntekt);
        final var oppgittOpptjening = OppgittOpptjeningBuilder.ny()
            .medInnsendingstidspunkt(innsendt)
            .medJournalpostId(new JournalpostId("21412"))
            .leggTilOppgittArbeidsforhold(oppgittArbeidsforhold)
            .build();
        return oppgittOpptjening;
    }

    private void mockIAY(List<OppgittOpptjening> oppgittOpptjeninger) {
        final var iayGrunnlag = InntektArbeidYtelseGrunnlagBuilder.nytt()
            .medOppgitteOpptjeninger(oppgittOpptjeninger).build();
        when(inntektArbeidYtelseTjeneste.finnGrunnlag(anyLong())).thenReturn(Optional.of(iayGrunnlag));
    }

    private static OppgittOpptjeningBuilder.OppgittArbeidsforholdBuilder lagOppgittArbeidOgFrilansInntekt(DatoIntervallEntitet periode, BigDecimal inntekt) {
        return OppgittOpptjeningBuilder.OppgittArbeidsforholdBuilder.ny()
            .medArbeidType(ArbeidType.VANLIG)
            .medPeriode(periode)
            .medInntekt(inntekt);
    }
}
