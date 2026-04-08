package no.nav.ung.sak.kontroll;

import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.kodeverk.kontroll.KontrollertInntektKilde;
import no.nav.ung.sak.behandlingslager.tilkjentytelse.KontrollertInntektPeriode;
import no.nav.ung.sak.behandlingslager.tilkjentytelse.TilkjentYtelseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class KontrollerteInntektperioderTjenesteTest {

    private static final LocalDate FOM = LocalDate.of(2025, 1, 1);
    private static final LocalDate TOM = LocalDate.of(2025, 1, 31);

    private TilkjentYtelseRepository tilkjentYtelseRepository;
    private RelevanteKontrollperioderUtleder relevanteKontrollperioderUtleder;
    private KontrollerteInntektperioderTjeneste tjeneste;

    @BeforeEach
    void setUp() {
        tilkjentYtelseRepository = mock(TilkjentYtelseRepository.class);
        relevanteKontrollperioderUtleder = mock(RelevanteKontrollperioderUtleder.class);
        when(tilkjentYtelseRepository.hentKontrollertInntektPerioder(anyLong())).thenReturn(Optional.empty());
        tjeneste = new KontrollerteInntektperioderTjeneste(tilkjentYtelseRepository, relevanteKontrollperioderUtleder);
    }

    @Test
    void skal_separere_atfl_og_ytelse_korrekt() {
        var atflBeløp = BigDecimal.valueOf(5000);
        var ytelseBeløp = BigDecimal.valueOf(3000);
        var inntektsresultat = new Inntektsresultat(
            Set.of(
                new RapportertInntekt(InntektType.ARBEIDSTAKER_ELLER_FRILANSER, atflBeløp),
                new RapportertInntekt(InntektType.YTELSE, ytelseBeløp)
            ),
            KontrollertInntektKilde.BRUKER
        );
        var inntektTidslinje = new LocalDateTimeline<>(List.of(new LocalDateSegment<>(FOM, TOM, inntektsresultat)));
        var rapporterteInntekterTidslinje = LocalDateTimeline.<RapporterteInntekter>empty();

        tjeneste.opprettKontrollerteInntekterPerioderFraBruker(1L, inntektTidslinje, rapporterteInntekterTidslinje, "input", "sporing");

        ArgumentCaptor<List<KontrollertInntektPeriode>> captor = ArgumentCaptor.captor();
        verify(tilkjentYtelseRepository).lagreKontrollertePerioder(eq(1L), captor.capture(), anyString(), anyString());
        var perioder = captor.getValue();
        assertThat(perioder).hasSize(1);
        var periode = perioder.get(0);
        assertThat(periode.getInntekt()).isEqualByComparingTo(atflBeløp);
        assertThat(periode.getYtelse()).isEqualByComparingTo(ytelseBeløp);
        assertThat(periode.getKilde()).isEqualTo(KontrollertInntektKilde.BRUKER);
    }

    @Test
    void skal_sette_ytelse_til_null_naar_bare_atfl() {
        var atflBeløp = BigDecimal.valueOf(4000);
        var inntektsresultat = new Inntektsresultat(
            Set.of(new RapportertInntekt(InntektType.ARBEIDSTAKER_ELLER_FRILANSER, atflBeløp)),
            KontrollertInntektKilde.BRUKER
        );
        var inntektTidslinje = new LocalDateTimeline<>(List.of(new LocalDateSegment<>(FOM, TOM, inntektsresultat)));
        var rapporterteInntekterTidslinje = LocalDateTimeline.<RapporterteInntekter>empty();

        tjeneste.opprettKontrollerteInntekterPerioderFraBruker(1L, inntektTidslinje, rapporterteInntekterTidslinje, "input", "sporing");

        ArgumentCaptor<List<KontrollertInntektPeriode>> captor = ArgumentCaptor.captor();
        verify(tilkjentYtelseRepository).lagreKontrollertePerioder(eq(1L), captor.capture(), anyString(), anyString());
        var periode = captor.getValue().get(0);
        assertThat(periode.getInntekt()).isEqualByComparingTo(atflBeløp);
        assertThat(periode.getYtelse()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void skal_sette_inntekt_til_null_naar_bare_ytelse() {
        var ytelseBeløp = BigDecimal.valueOf(2000);
        var inntektsresultat = new Inntektsresultat(
            Set.of(new RapportertInntekt(InntektType.YTELSE, ytelseBeløp)),
            KontrollertInntektKilde.BRUKER
        );
        var inntektTidslinje = new LocalDateTimeline<>(List.of(new LocalDateSegment<>(FOM, TOM, inntektsresultat)));
        var rapporterteInntekterTidslinje = LocalDateTimeline.<RapporterteInntekter>empty();

        tjeneste.opprettKontrollerteInntekterPerioderFraBruker(1L, inntektTidslinje, rapporterteInntekterTidslinje, "input", "sporing");

        ArgumentCaptor<List<KontrollertInntektPeriode>> captor = ArgumentCaptor.captor();
        verify(tilkjentYtelseRepository).lagreKontrollertePerioder(eq(1L), captor.capture(), anyString(), anyString());
        var periode = captor.getValue().get(0);
        assertThat(periode.getInntekt()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(periode.getYtelse()).isEqualByComparingTo(ytelseBeløp);
    }

    @Test
    void skal_sette_begge_til_null_naar_ingen_inntekter() {
        var inntektsresultat = new Inntektsresultat(Set.of(), KontrollertInntektKilde.BRUKER);
        var inntektTidslinje = new LocalDateTimeline<>(List.of(new LocalDateSegment<>(FOM, TOM, inntektsresultat)));
        var rapporterteInntekterTidslinje = LocalDateTimeline.<RapporterteInntekter>empty();

        tjeneste.opprettKontrollerteInntekterPerioderFraBruker(1L, inntektTidslinje, rapporterteInntekterTidslinje, "input", "sporing");

        ArgumentCaptor<List<KontrollertInntektPeriode>> captor = ArgumentCaptor.captor();
        verify(tilkjentYtelseRepository).lagreKontrollertePerioder(eq(1L), captor.capture(), anyString(), anyString());
        var periode = captor.getValue().get(0);
        assertThat(periode.getInntekt()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(periode.getYtelse()).isEqualByComparingTo(BigDecimal.ZERO);
    }
}
