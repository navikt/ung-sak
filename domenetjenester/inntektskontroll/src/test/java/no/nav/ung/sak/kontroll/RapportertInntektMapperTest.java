package no.nav.ung.sak.kontroll;

import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.kodeverk.arbeidsforhold.ArbeidType;
import no.nav.ung.kodeverk.arbeidsforhold.InntektsKilde;
import no.nav.ung.kodeverk.arbeidsforhold.InntektspostType;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.fagsak.Fagsak;
import no.nav.ung.sak.domene.iay.modell.InntektArbeidYtelseAggregatBuilder;
import no.nav.ung.sak.domene.iay.modell.InntektArbeidYtelseGrunnlagBuilder;
import no.nav.ung.sak.domene.iay.modell.InntektArbeidYtelseTjeneste;
import no.nav.ung.sak.domene.iay.modell.InntektBuilder;
import no.nav.ung.sak.domene.iay.modell.InntektspostBuilder;
import no.nav.ung.sak.domene.iay.modell.OppgittOpptjening;
import no.nav.ung.sak.domene.iay.modell.OppgittOpptjeningBuilder;
import no.nav.ung.sak.domene.iay.modell.VersjonType;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.typer.AktørId;
import no.nav.ung.sak.typer.Arbeidsgiver;
import no.nav.ung.sak.typer.JournalpostId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RapportertInntektMapperTest {

    public static final LocalDateTimeline<Boolean> RELEVANT_TIDSLINJE = new LocalDateTimeline<>(List.of(
        new LocalDateSegment<>(LocalDate.now(), LocalDate.now().with(TemporalAdjusters.lastDayOfMonth()), true),
        new LocalDateSegment<>(LocalDate.now().plusMonths(1).withDayOfMonth(1), LocalDate.now().plusMonths(1).with(TemporalAdjusters.lastDayOfMonth()), true)));

    private final InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste = mock(InntektArbeidYtelseTjeneste.class);
    private final BehandlingRepository behandlingRepository = mock(BehandlingRepository.class);
    private RapportertInntektMapper rapportertInntektMapper;

    @BeforeEach
    void setUp() {
        mockBehandling(FagsakYtelseType.UNGDOMSYTELSE);
        rapportertInntektMapper = new RapportertInntektMapper(inntektArbeidYtelseTjeneste, behandlingRepository);
    }

    @Test
    void skal_mappe_en_mottatt_inntekt() {
        // Arrange
        final var periode = DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now(), LocalDate.now().with(TemporalAdjusters.lastDayOfMonth()));
        final var inntekt = BigDecimal.TEN;
        final var oppgittOpptjening = lagMottattATFLInntekt(periode, inntekt, LocalDateTime.now());
        mockIAY(List.of(oppgittOpptjening));

        // Act
        final var tidslinje = rapportertInntektMapper.mapAlleGjeldendeRegisterOgBrukersInntekter(1L, RELEVANT_TIDSLINJE);

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
        final var tidslinje = rapportertInntektMapper.mapAlleGjeldendeRegisterOgBrukersInntekter(1L, RELEVANT_TIDSLINJE);

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
        final var tidslinje = rapportertInntektMapper.mapAlleGjeldendeRegisterOgBrukersInntekter(1L, RELEVANT_TIDSLINJE);

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
        final var tidslinje = rapportertInntektMapper.mapAlleGjeldendeRegisterOgBrukersInntekter(1L, RELEVANT_TIDSLINJE);

        // Assert
        final var forventet = new LocalDateTimeline<>(periode.getFomDato(), periode.getTomDato(),
            new RapporterteInntekter(
                Set.of(new RapportertInntekt(InntektType.ARBEIDSTAKER_ELLER_FRILANSER, inntekt2)),
                Set.of()));
        assertThat(tidslinje).isEqualTo(forventet);
    }


    @Test
    void skal_beholde_negativ_registerinntekt_uendret() {
        // Arrange
        final var periode = DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30));
        mockIAYMedRegisterinntekter(List.of(), periode, List.of(BigDecimal.valueOf(-5830.54)));

        // Act
        final var tidslinje = rapportertInntektMapper.mapAlleGjeldendeRegisterOgBrukersInntekter(1L, new LocalDateTimeline<>(periode.toLocalDateInterval(), true));

        // Assert
        final var registerinntekter = tidslinje.getSegment(periode.toLocalDateInterval()).getValue().registerRapporterteInntekter();
        assertThat(registerinntekter).hasSize(1);
        final var registerinntekt = registerinntekter.iterator().next();
        assertThat(registerinntekt.inntektType()).isEqualTo(InntektType.ARBEIDSTAKER_ELLER_FRILANSER);
        assertThat(registerinntekt.beløp()).isEqualByComparingTo(BigDecimal.valueOf(-5830.54));
    }

    @Test
    void skal_motregne_negativ_korrigering_mot_positiv_registerinntekt_i_samme_måned() {
        // Arrange
        final var periode = DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30));
        mockIAYMedRegisterinntekter(List.of(), periode, List.of(BigDecimal.valueOf(10000), BigDecimal.valueOf(-4000)));

        // Act
        final var tidslinje = rapportertInntektMapper.mapAlleGjeldendeRegisterOgBrukersInntekter(1L, new LocalDateTimeline<>(periode.toLocalDateInterval(), true));

        // Assert
        final var registerinntekter = tidslinje.getSegment(periode.toLocalDateInterval()).getValue().registerRapporterteInntekter();
        assertThat(registerinntekter).hasSize(1);
        assertThat(registerinntekter.iterator().next().beløp()).isEqualByComparingTo(BigDecimal.valueOf(6000));
    }

    @Test
    void skal_bruke_registerinntekt_fra_aktivitetspengerfilter_for_aktivitetspenger() {
        // Arrange
        mockBehandling(FagsakYtelseType.AKTIVITETSPENGER);
        final var periode = DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30));
        mockIAYMedRegisterinntekter(List.of(), periode, List.of(BigDecimal.valueOf(10000)), InntektsKilde.INNTEKT_AKTIVITETSPENGER);

        // Act
        final var tidslinje = rapportertInntektMapper.mapAlleGjeldendeRegisterOgBrukersInntekter(1L, new LocalDateTimeline<>(periode.toLocalDateInterval(), true));

        // Assert
        final var registerinntekter = tidslinje.getSegment(periode.toLocalDateInterval()).getValue().registerRapporterteInntekter();
        assertThat(registerinntekter).hasSize(1);
        assertThat(registerinntekter.iterator().next().beløp()).isEqualByComparingTo(BigDecimal.valueOf(10000));
    }

    private void mockBehandling(FagsakYtelseType ytelseType) {
        final var behandling = Behandling.forFørstegangssøknad(Fagsak.opprettNy(ytelseType, AktørId.dummy())).build();
        when(behandlingRepository.hentBehandling(anyLong())).thenReturn(behandling);
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

    private void mockIAYMedRegisterinntekter(List<OppgittOpptjening> oppgittOpptjeninger, DatoIntervallEntitet periode, List<BigDecimal> beløp) {
        mockIAYMedRegisterinntekter(oppgittOpptjeninger, periode, beløp, InntektsKilde.INNTEKT_UNGDOMSYTELSE);
    }

    private void mockIAYMedRegisterinntekter(List<OppgittOpptjening> oppgittOpptjeninger, DatoIntervallEntitet periode, List<BigDecimal> beløp, InntektsKilde inntektsKilde) {
        final var registerBuilder = InntektArbeidYtelseAggregatBuilder.oppdatere(Optional.empty(), VersjonType.REGISTER);
        final var inntekterBuilder = registerBuilder.getInntekterBuilder();
        for (int i = 0; i < beløp.size(); i++) {
            final var inntektBuilder = InntektBuilder.oppdatere(Optional.empty())
                .medInntektsKilde(inntektsKilde)
                .medArbeidsgiver(Arbeidsgiver.virksomhet("97300000" + i))
                .leggTilInntektspost(InntektspostBuilder.ny()
                    .medPeriode(periode.getFomDato(), periode.getTomDato())
                    .medInntektspostType(InntektspostType.LØNN)
                    .medBeløp(beløp.get(i)));
            inntekterBuilder.leggTilInntekt(inntektBuilder);
        }
        registerBuilder.leggTilInntekter(inntekterBuilder);
        final var iayGrunnlag = InntektArbeidYtelseGrunnlagBuilder.nytt()
            .medData(registerBuilder)
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
