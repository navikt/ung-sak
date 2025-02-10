package no.nav.ung.sak.perioder;

import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.kodeverk.vilkår.VilkårType;
import no.nav.ung.sak.behandlingslager.perioder.UtledPeriodeTilVurderingFraUngdomsprogram;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.stønadsperioder.Stønadperiodeutleder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

class UngdomsytelseVilkårsperioderTilVurderingTjenesteTest {

    @Mock
    private UngdomsytelseSøknadsperiodeTjeneste fraSøknadsperiode;

    @Mock
    private UtledPeriodeTilVurderingFraUngdomsprogram fraUngdomsprogram;

    @Mock
    private ProsessTriggerPeriodeUtleder fraProsesstriggere;

    @Mock
    private Stønadperiodeutleder stønadperiodeutleder;

    @InjectMocks
    private UngdomsytelseVilkårsperioderTilVurderingTjeneste tjeneste;

    @BeforeEach
    void setUp() {
        openMocks(this);
    }

    @Test
    void testIngenPerioderTilVurderingOgIngenStønadstidslinje() {
        when(fraSøknadsperiode.utledTidslinje(anyLong())).thenReturn(LocalDateTimeline.empty());
        when(fraUngdomsprogram.finnTidslinje(anyLong())).thenReturn(LocalDateTimeline.empty());
        when(fraProsesstriggere.utledTidslinje(anyLong())).thenReturn(LocalDateTimeline.empty());
        when(stønadperiodeutleder.utledStønadstidslinje(anyLong())).thenReturn(LocalDateTimeline.empty());

        final var resultat = tjeneste.utled(1L, VilkårType.UNGDOMSPROGRAMVILKÅRET);

        assertThat(resultat.size()).isEqualTo(0);
    }

    @Test
    void testEndretSøknadsperiodeIMidtenTilMidtenAvNesteMånedOgStønadsperiodeFraStartOgTilSluttAvMåned() {
        LocalDateTimeline<Boolean> søknadsperiodeTidslinje = new LocalDateTimeline<>(
            List.of(
                new LocalDateSegment<>(LocalDate.of(2023, 1, 15), LocalDate.of(2023, 1, 31), true),
                new LocalDateSegment<>(LocalDate.of(2023, 2, 1), LocalDate.of(2023, 2, 28), true)
            )
        );

        LocalDateTimeline<Boolean> stønadstidslinje = new LocalDateTimeline<>(
            List.of(
                new LocalDateSegment<>(LocalDate.of(2023, 1, 1), LocalDate.of(2023, 1, 31), true),
                new LocalDateSegment<>(LocalDate.of(2023, 2, 1), LocalDate.of(2023, 2, 28), true)
            )
        );

        when(fraSøknadsperiode.utledTidslinje(anyLong())).thenReturn(søknadsperiodeTidslinje);
        when(fraUngdomsprogram.finnTidslinje(anyLong())).thenReturn(LocalDateTimeline.empty());
        when(fraProsesstriggere.utledTidslinje(anyLong())).thenReturn(LocalDateTimeline.empty());
        when(stønadperiodeutleder.utledStønadstidslinje(anyLong())).thenReturn(stønadstidslinje);

        final var resultat = tjeneste.utled(1L, VilkårType.UNGDOMSPROGRAMVILKÅRET);

        assertThat(resultat.size()).isEqualTo(2);

        var iterator = resultat.iterator();
        var firstPeriod = iterator.next();
        var secondPeriod = iterator.next();

        assertThat(firstPeriod).isEqualTo(DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.of(2023, 1, 1), LocalDate.of(2023, 1, 31)));
        assertThat(secondPeriod).isEqualTo(DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.of(2023, 2, 1), LocalDate.of(2023, 2, 28)));
    }

}
