package no.nav.ung.sak.perioder;

import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.kodeverk.vilkår.VilkårType;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.VilkårBuilder;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.Vilkårene;
import no.nav.ung.sak.behandlingslager.perioder.UtledPeriodeTilVurderingFraUngdomsprogram;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.ytelseperioder.YtelseperiodeUtleder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

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
    private VilkårResultatRepository vilkårResultatRepository;

    @Mock
    private ProsessTriggerPeriodeUtleder fraProsesstriggere;

    @Mock
    private YtelseperiodeUtleder ytelseperiodeUtleder;

    @InjectMocks
    private UngdomsytelseVilkårsperioderTilVurderingTjeneste tjeneste;

    @BeforeEach
    void setUp() {
        openMocks(this);
    }

    @Test
    void skal_returnere_periodisering_til_vilkår_dersom_eksisterer() {
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

        mockVilkårPeriode(LocalDate.of(2023, 1, 1), LocalDate.of(2023, 2, 28));
        when(fraSøknadsperiode.utledTidslinje(anyLong())).thenReturn(søknadsperiodeTidslinje);
        when(fraUngdomsprogram.finnTidslinje(anyLong())).thenReturn(LocalDateTimeline.empty());
        when(fraProsesstriggere.utledTidslinje(anyLong())).thenReturn(LocalDateTimeline.empty());
        when(ytelseperiodeUtleder.utledYtelsestidslinje(anyLong())).thenReturn(stønadstidslinje);

        final var resultat = tjeneste.utled(1L, VilkårType.UNGDOMSPROGRAMVILKÅRET);

        assertThat(resultat.size()).isEqualTo(1);

        var iterator = resultat.iterator();
        var firstPeriod = iterator.next();

        assertThat(firstPeriod).isEqualTo(DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.of(2023, 1, 1), LocalDate.of(2023, 2, 28)));
    }


    @Test
    void skal_returnere_hele_ytelsesperioden_dersom_vilkår_er_oppstykket() {
        LocalDateTimeline<Boolean> søknadsperiodeTidslinje = new LocalDateTimeline<>(
                List.of(
                        new LocalDateSegment<>(LocalDate.of(2023, 2, 14), LocalDate.of(2023, 2, 28), true)
                )
        );

        LocalDateTimeline<Boolean> stønadstidslinje = new LocalDateTimeline<>(
                List.of(
                        new LocalDateSegment<>(LocalDate.of(2023, 2, 1), LocalDate.of(2023, 2, 28), true)
                )
        );

        mockVilkårPerioder(List.of(
                DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.of(2023, 2, 10), LocalDate.of(2023, 2, 10)),
                DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.of(2023, 2, 14), LocalDate.of(2023, 2, 28))
                ));
        when(fraSøknadsperiode.utledTidslinje(anyLong())).thenReturn(søknadsperiodeTidslinje);
        when(fraUngdomsprogram.finnTidslinje(anyLong())).thenReturn(LocalDateTimeline.empty());
        when(fraProsesstriggere.utledTidslinje(anyLong())).thenReturn(LocalDateTimeline.empty());
        when(ytelseperiodeUtleder.utledYtelsestidslinje(anyLong())).thenReturn(stønadstidslinje);

        final var resultat = tjeneste.utled(1L, VilkårType.UNGDOMSPROGRAMVILKÅRET);

        assertThat(resultat.size()).isEqualTo(2);

        var iterator = resultat.iterator();
        var firstPeriod = iterator.next();
        var secondPeriod = iterator.next();

        assertThat(firstPeriod).isEqualTo(DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.of(2023, 2, 10), LocalDate.of(2023, 2, 10)));
        assertThat(secondPeriod).isEqualTo(DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.of(2023, 2, 14), LocalDate.of(2023, 2, 28)));

    }

    private void mockVilkårPeriode(LocalDate fom, LocalDate tom) {
        final var builder = Vilkårene.builder();
        final var vilkårBuilder = builder.hentBuilderFor(VilkårType.UNGDOMSPROGRAMVILKÅRET);
        final var vilkårPeriodeBuilder = vilkårBuilder.hentBuilderFor(fom, tom);
        vilkårBuilder.leggTil(vilkårPeriodeBuilder);
        builder.leggTil(vilkårBuilder);
        when(vilkårResultatRepository.hentHvisEksisterer(anyLong())).thenReturn(Optional.of(builder.build()));
    }

    private void mockVilkårPerioder(List<DatoIntervallEntitet> perioder) {
        final var builder = Vilkårene.builder();
        final var vilkårBuilder = builder.hentBuilderFor(VilkårType.UNGDOMSPROGRAMVILKÅRET);
        perioder.forEach(p -> {
            final var periodeBuilder = vilkårBuilder.hentBuilderFor(p.getFomDato(), p.getTomDato());
            vilkårBuilder.leggTil(periodeBuilder);
        });
        builder.leggTil(vilkårBuilder);
        when(vilkårResultatRepository.hentHvisEksisterer(anyLong())).thenReturn(Optional.of(builder.build()));
    }

}
