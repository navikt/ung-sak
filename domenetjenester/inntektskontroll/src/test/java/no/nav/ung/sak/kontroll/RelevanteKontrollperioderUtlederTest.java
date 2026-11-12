package no.nav.ung.sak.kontroll;

import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.felles.testutilities.cdi.UnitTestLookupInstanceImpl;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class RelevanteKontrollperioderUtlederTest {

    private final InntektskontrollFritakUtleder fritakUtleder = mock(InntektskontrollFritakUtleder.class);
    private final RelevanteKontrollperioderUtleder utleder = new RelevanteKontrollperioderUtleder(
        null, null, null, new UnitTestLookupInstanceImpl<>(fritakUtleder));
    private static final YearMonth NOVEMBER = YearMonth.of(2026, 11);

    @BeforeEach
    void setUp() {
        when(fritakUtleder.finnPerioderFritattForKontroll(any())).thenReturn(LocalDateTimeline.empty());
    }

    @Test
    void skal_kontrollere_en_enkelt_delvis_måned_uten_fritak() {
        var perioder = new LocalDateTimeline<>(NOVEMBER.atDay(10), NOVEMBER.atDay(20), NOVEMBER);

        var resultat = utleder.utledPerioderRelevantForKontrollAvInntekt(perioder, FagsakYtelseType.AKTIVITETSPENGER);

        assertThat(resultat).isEqualTo(new LocalDateTimeline<>(NOVEMBER.atDay(1), NOVEMBER.atEndOfMonth(),
            new RelevanteKontrollperioderUtleder.InfoOmRådata(false)));
    }

    @Test
    void skal_kontrollere_første_og_siste_måned_og_ny_periode_uten_fritak() {
        var desember = NOVEMBER.plusMonths(1);
        var februar = NOVEMBER.plusMonths(3);
        var perioder = new LocalDateTimeline<>(List.of(
            new LocalDateSegment<>(NOVEMBER.atDay(15), NOVEMBER.atEndOfMonth(), NOVEMBER),
            new LocalDateSegment<>(desember.atDay(1), desember.atDay(10), desember),
            new LocalDateSegment<>(februar.atDay(1), februar.atEndOfMonth(), februar)));

        var resultat = utleder.utledPerioderRelevantForKontrollAvInntekt(perioder, FagsakYtelseType.AKTIVITETSPENGER);

        assertThat(resultat.getMinLocalDate()).isEqualTo(NOVEMBER.atDay(1));
        assertThat(resultat.getMaxLocalDate()).isEqualTo(februar.atEndOfMonth());
        assertThat(resultat.intersection(new LocalDateInterval(
            LocalDate.of(2027, 1, 1), LocalDate.of(2027, 1, 31))).isEmpty()).isTrue();
        assertThat(resultat.intersection(new LocalDateInterval(
            februar.atDay(1), februar.atEndOfMonth())).isEmpty()).isFalse();
    }

    @Test
    void skal_samle_flere_ytelsesperioder_i_samme_kontrollmåned() {
        var perioder = new LocalDateTimeline<>(List.of(
            new LocalDateSegment<>(NOVEMBER.atDay(1), NOVEMBER.atDay(10), NOVEMBER),
            new LocalDateSegment<>(NOVEMBER.atDay(20), NOVEMBER.atEndOfMonth(), NOVEMBER)));

        var resultat = utleder.utledPerioderRelevantForKontrollAvInntekt(perioder, FagsakYtelseType.AKTIVITETSPENGER);

        assertThat(resultat).isEqualTo(new LocalDateTimeline<>(NOVEMBER.atDay(1), NOVEMBER.atEndOfMonth(),
            new RelevanteKontrollperioderUtleder.InfoOmRådata(false)));
    }

    @Test
    void skal_trekke_fra_perioder_fra_ytelsesspesifikk_fritaksutleder() {
        var desember = NOVEMBER.plusMonths(1);
        var perioder = new LocalDateTimeline<>(List.of(
            new LocalDateSegment<>(NOVEMBER.atDay(1), NOVEMBER.atEndOfMonth(), NOVEMBER),
            new LocalDateSegment<>(desember.atDay(1), desember.atDay(10), desember)));
        when(fritakUtleder.finnPerioderFritattForKontroll(any())).thenReturn(
            new LocalDateTimeline<>(NOVEMBER.atDay(1), NOVEMBER.atEndOfMonth(), true));

        var resultat = utleder.utledPerioderRelevantForKontrollAvInntekt(perioder, FagsakYtelseType.UNGDOMSYTELSE);

        assertThat(resultat).isEqualTo(new LocalDateTimeline<>(desember.atDay(1), desember.atEndOfMonth(),
            new RelevanteKontrollperioderUtleder.InfoOmRådata(false)));
        assertThat(utleder.utledPerioderRelevantForKontrollAvInntekt(
            new LocalDateTimeline<>(NOVEMBER.atDay(1), NOVEMBER.atEndOfMonth(), NOVEMBER), FagsakYtelseType.UNGDOMSYTELSE).isEmpty()).isTrue();
    }

    @ParameterizedTest
    @EnumSource(value = FagsakYtelseType.class, names = {"AKTIVITETSPENGER", "UNGDOMSYTELSE"})
    void tom_ytelsestidslinje_skal_ikke_gi_kontroll(FagsakYtelseType ytelseType) {
        assertThat(utleder.utledPerioderRelevantForKontrollAvInntekt(LocalDateTimeline.empty(), ytelseType).isEmpty()).isTrue();
    }

    @Test
    void hel_ytelsesmåned_skal_markeres_som_hel() {
        var resultat = utleder.utledPerioderRelevantForKontrollAvInntekt(
            new LocalDateTimeline<>(NOVEMBER.atDay(1), NOVEMBER.atEndOfMonth(), NOVEMBER), FagsakYtelseType.AKTIVITETSPENGER);

        assertThat(resultat).isEqualTo(new LocalDateTimeline<>(NOVEMBER.atDay(1), NOVEMBER.atEndOfMonth(),
            new RelevanteKontrollperioderUtleder.InfoOmRådata(true)));
    }
}
