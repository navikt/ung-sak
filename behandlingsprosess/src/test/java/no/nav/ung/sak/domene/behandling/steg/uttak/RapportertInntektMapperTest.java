package no.nav.ung.sak.domene.behandling.steg.uttak;

import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.kodeverk.arbeidsforhold.ArbeidType;
import no.nav.ung.sak.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.ung.sak.domene.behandling.steg.uttak.regler.InntektType;
import no.nav.ung.sak.domene.behandling.steg.uttak.regler.RapportertInntekt;
import no.nav.ung.sak.domene.iay.modell.*;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.typer.JournalpostId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RapportertInntektMapperTest {

    private InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste = mock(InntektArbeidYtelseTjeneste.class);
    private RapportertInntektMapper rapportertInntektMapper;

    @BeforeEach
    void setUp() {
        rapportertInntektMapper = new RapportertInntektMapper(inntektArbeidYtelseTjeneste);
    }

    @Test
    void skal_mappe_en_mottatt_inntekt() {
        // Arrange
        final var periode = DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now(), LocalDate.now().plusDays(1));
        final var inntekt = BigDecimal.TEN;
        final var oppgittOpptjening = lagMottattATFLInntekt(periode, inntekt, LocalDateTime.now());
        mockIAY(List.of(oppgittOpptjening));

        // Act
        final var tidslinje = rapportertInntektMapper.map(1L);

        // Assert
        final var forventet = new LocalDateTimeline<>(periode.getFomDato(), periode.getTomDato(),
            Set.of(new RapportertInntekt(InntektType.ARBEIDSTAKER_ELLER_FRILANSER, inntekt)));
        assertThat(tidslinje).isEqualTo(forventet);

    }

    @Test
    void skal_mappe_mottatte_inntekter_for_næring_og_arbeid() {
        // Arrange
        final var periode = DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now(), LocalDate.now().plusDays(1));
        final var arbeidsinntekt = BigDecimal.TEN;
        final var næringsinntekt = BigDecimal.ONE;
        final var oppgittOpptjening = lagMottattATFLOgNæringInntekt(periode, arbeidsinntekt, næringsinntekt, LocalDateTime.now());
        mockIAY(List.of(oppgittOpptjening));

        // Act
        final var tidslinje = rapportertInntektMapper.map(1L);

        // Assert
        final var forventet = new LocalDateTimeline<>(periode.getFomDato(), periode.getTomDato(),
            Set.of(
                new RapportertInntekt(InntektType.ARBEIDSTAKER_ELLER_FRILANSER, arbeidsinntekt),
                new RapportertInntekt(InntektType.SELVSTENDIG_NÆRINGSDRIVENDE, næringsinntekt)
                ));
        assertThat(tidslinje).isEqualTo(forventet);
    }

    @Test
    void skal_mappe_to_mottatte_inntekter_for_forskjellige_perioder() {
        // Arrange
        final var fom = LocalDate.now();
        final var tom = LocalDate.now().plusDays(1);
        final var periode = DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom);
        final var inntekt = BigDecimal.TEN;
        final var innsendtTidspunkt = LocalDateTime.now();
        final var oppgittOpptjening = lagMottattATFLInntekt(periode, inntekt, innsendtTidspunkt);
        final var fom2 = tom.plusDays(10);
        final var tom2 = fom2.plusDays(10);
        final var periode2 = DatoIntervallEntitet.fraOgMedTilOgMed(fom2, tom2);
        final var inntekt2 = BigDecimal.valueOf(50);
        final var oppgittOpptjening2 = lagMottattATFLInntekt(periode2, inntekt2, innsendtTidspunkt);

        mockIAY(List.of(oppgittOpptjening, oppgittOpptjening2));

        // Act
        final var tidslinje = rapportertInntektMapper.map(1L);

        // Assert
        final var forventet = new LocalDateTimeline<>(
            List.of(
                new LocalDateSegment<>(periode.getFomDato(), periode.getTomDato(), Set.of(new RapportertInntekt(InntektType.ARBEIDSTAKER_ELLER_FRILANSER, inntekt))),
                new LocalDateSegment<>(periode2.getFomDato(), periode2.getTomDato(), Set.of(new RapportertInntekt(InntektType.ARBEIDSTAKER_ELLER_FRILANSER, inntekt2)))
                ));
        assertThat(tidslinje).isEqualTo(forventet);
    }

    @Test
    void skal_mappe_to_mottatte_inntekter_for_samme_perioder() {
        // Arrange
        final var fom = LocalDate.now();
        final var tom = LocalDate.now().plusDays(1);
        final var periode = DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom);
        final var inntekt = BigDecimal.TEN;
        final var innsendtTidspunkt = LocalDateTime.now().minusDays(1);
        final var oppgittOpptjening = lagMottattATFLInntekt(periode, inntekt, innsendtTidspunkt);

        final var inntekt2 = BigDecimal.valueOf(50);
        final var innsendtTidspunkt2 = LocalDateTime.now();
        final var oppgittOpptjening2 = lagMottattATFLInntekt(periode, inntekt2, innsendtTidspunkt2);

        mockIAY(List.of(oppgittOpptjening, oppgittOpptjening2));

        // Act
        final var tidslinje = rapportertInntektMapper.map(1L);

        // Assert
        final var forventet = new LocalDateTimeline<>(periode.getFomDato(), periode.getTomDato(),
            Set.of(new RapportertInntekt(InntektType.ARBEIDSTAKER_ELLER_FRILANSER, inntekt2)));
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

    private static OppgittOpptjening lagMottattATFLOgNæringInntekt(DatoIntervallEntitet periode, BigDecimal atflInntekt, BigDecimal næringsinntekt, LocalDateTime innsendt) {
        final var oppgittArbeidsforhold = lagOppgittArbeidOgFrilansInntekt(periode, atflInntekt);
        final var oppgittNæring = lagOppgittNæringsinntekt(periode, næringsinntekt);
        final var oppgittOpptjening = OppgittOpptjeningBuilder.ny()
            .medInnsendingstidspunkt(innsendt)
            .medJournalpostId(new JournalpostId("21412"))
            .leggTilOppgittArbeidsforhold(oppgittArbeidsforhold)
            .leggTilEgneNæringer(List.of(oppgittNæring))
            .build();
        return oppgittOpptjening;
    }

    private void mockIAY(List<OppgittOpptjening> oppgittOpptjeninger) {
        final var iayGrunnlag = InntektArbeidYtelseGrunnlagBuilder.nytt()
            .medOppgitteOpptjeninger(oppgittOpptjeninger).build();
        when(inntektArbeidYtelseTjeneste.hentGrunnlag(anyLong())).thenReturn(iayGrunnlag);
    }

    private static OppgittOpptjeningBuilder.OppgittArbeidsforholdBuilder lagOppgittArbeidOgFrilansInntekt(DatoIntervallEntitet periode, BigDecimal inntekt) {
        return OppgittOpptjeningBuilder.OppgittArbeidsforholdBuilder.ny()
            .medArbeidType(ArbeidType.VANLIG)
            .medPeriode(periode)
            .medInntekt(inntekt);
    }

    private static OppgittOpptjeningBuilder.EgenNæringBuilder lagOppgittNæringsinntekt(DatoIntervallEntitet periode, BigDecimal inntekt) {
        return OppgittOpptjeningBuilder.EgenNæringBuilder.ny()
            .medPeriode(periode)
            .medBruttoInntekt(inntekt);
    }

}
