package no.nav.k9.sak.ytelse.pleiepengerbarn.beregningsgrunnlag;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.k9.kodeverk.vilkår.Utfall;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.PåTversAvHelgErKantIKantVurderer;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårBuilder;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.Vilkårene;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriodeBuilder;
import no.nav.k9.sak.domene.behandling.steg.beregningsgrunnlag.FinnPerioderSomSkalFjernesIBeregning;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.perioder.VilkårsPerioderTilVurderingTjeneste;
import no.nav.k9.sak.test.util.UnitTestLookupInstanceImpl;
import no.nav.k9.sak.typer.Saksnummer;

class PleiepengerFinnPerioderSomSkalFjernesIBeregningTest {
    BehandlingReferanse behandlingReferanceMock = mock(BehandlingReferanse.class);

    VilkårsPerioderTilVurderingTjeneste vilkårsPerioderTilVurderingTjeneste = mock(VilkårsPerioderTilVurderingTjeneste.class);

    FinnPerioderSomSkalFjernesIBeregning finnPerioderSomSkalFjernesIBeregning = new PleiepengerFinnPerioderSomSkalFjernesIBeregning(new UnitTestLookupInstanceImpl<>(vilkårsPerioderTilVurderingTjeneste));

    private static final Saksnummer saksnummer = new Saksnummer("AAAAA");

    @BeforeEach
    void setup() {
        when(behandlingReferanceMock.getSaksnummer()).thenReturn(saksnummer);
        when(vilkårsPerioderTilVurderingTjeneste.getKantIKantVurderer()).thenReturn(new PåTversAvHelgErKantIKantVurderer());
    }

    @Test
    void fjern_perioder_med_minst_ett_avslått_vilkår() {
        var fom = LocalDate.of(2024, 2, 1);
        var tom = fom.plusDays(3);

        var avslåttVilkårBuilder = new VilkårBuilder(VilkårType.ALDERSVILKÅR)
            .leggTil(new VilkårPeriodeBuilder()
                .medPeriode(fom, tom)
                .medUtfall(Utfall.IKKE_OPPFYLT)
            );

        var vilkårene = Vilkårene.builder()
            .leggTilIkkeVurderteVilkår(List.of(DatoIntervallEntitet.fra(fom, tom)), List.of(VilkårType.BEREGNINGSGRUNNLAGVILKÅR))
            .leggTil(avslåttVilkårBuilder)
            .build();

        var perioderSomSkalFjernes = finnPerioderSomSkalFjernesIBeregning.finnPerioderSomSkalFjernes(vilkårene, behandlingReferanceMock, Set.of(DatoIntervallEntitet.fra(fom, tom)));
        assertThat(perioderSomSkalFjernes).isNotEmpty();
    }

    @Test
    void ikke_fjern_perioder_uten_minst_ett_avslått_vilkår() {
        var fom = LocalDate.of(2024, 2, 1);
        var tom = fom.plusDays(3);

        var avslåttVilkårBuilder = new VilkårBuilder(VilkårType.ALDERSVILKÅR)
            .leggTil(new VilkårPeriodeBuilder()
                .medPeriode(fom, tom)
                .medUtfall(Utfall.OPPFYLT)
            );

        var vilkårene = Vilkårene.builder()
            .leggTilIkkeVurderteVilkår(List.of(DatoIntervallEntitet.fra(fom, tom)), List.of(VilkårType.BEREGNINGSGRUNNLAGVILKÅR))
            .leggTil(avslåttVilkårBuilder)
            .build();

        var perioderSomSkalFjernes = finnPerioderSomSkalFjernesIBeregning.finnPerioderSomSkalFjernes(vilkårene, behandlingReferanceMock, Set.of(DatoIntervallEntitet.fra(fom, tom)));
        assertThat(perioderSomSkalFjernes).isEmpty();
    }


    @Test
    void skal_fjerne_perioder_fra_innsendt_liste_som_ikke_ligger_på_vilkår() {
        var fom = LocalDate.of(2024, 2, 1);
        var tom = fom.plusDays(3);

        var en_annen_fom = LocalDate.of(2024, 5, 1);
        var en_annen_tom = en_annen_fom.plusDays(3);

        var avslåttVilkårBuilder = new VilkårBuilder(VilkårType.ALDERSVILKÅR)
            .leggTil(new VilkårPeriodeBuilder()
                .medPeriode(fom, tom)
                .medUtfall(Utfall.IKKE_OPPFYLT)
            )
            .leggTil(new VilkårPeriodeBuilder()
                .medPeriode(en_annen_fom, en_annen_tom)
                .medUtfall(Utfall.OPPFYLT)
            );

        var vilkårene = Vilkårene.builder()
            .leggTilIkkeVurderteVilkår(List.of(DatoIntervallEntitet.fra(en_annen_fom, en_annen_tom)), List.of(VilkårType.BEREGNINGSGRUNNLAGVILKÅR))
            .leggTil(avslåttVilkårBuilder)
            .build();

        var perioderSomSkalFjernes = finnPerioderSomSkalFjernesIBeregning.finnPerioderSomSkalFjernes(vilkårene, behandlingReferanceMock,
            Set.of(DatoIntervallEntitet.fra(en_annen_fom, en_annen_tom),
                DatoIntervallEntitet.fra(fom, tom)));
        assertThat(perioderSomSkalFjernes.size()).isEqualTo(1);
        var periodeSomFjernes = perioderSomSkalFjernes.iterator().next();
        assertThat(periodeSomFjernes.getFomDato()).isEqualTo(fom);
        assertThat(periodeSomFjernes.getTomDato()).isEqualTo(tom);
    }


    @Test
    void skal_ikke_fjerne_om_helg_er_avslått() {
        var fredag = LocalDate.of(2024, 6, 7);
        var laurdag = LocalDate.of(2024, 6, 8);
        var søndag = LocalDate.of(2024, 6, 9);
        var mandag = LocalDate.of(2024, 6, 10);

        var avslåttVilkårBuilder = new VilkårBuilder(VilkårType.ALDERSVILKÅR)
            .leggTil(new VilkårPeriodeBuilder()
                .medPeriode(fredag, fredag)
                .medUtfall(Utfall.OPPFYLT)
            )
            .leggTil(new VilkårPeriodeBuilder()
                .medPeriode(laurdag, søndag)
                .medUtfall(Utfall.IKKE_OPPFYLT)
            )
            .leggTil(new VilkårPeriodeBuilder()
                .medPeriode(mandag, mandag)
                .medUtfall(Utfall.OPPFYLT)
            );

        var vilkårene = Vilkårene.builder()
            .leggTilIkkeVurderteVilkår(List.of(DatoIntervallEntitet.fra(fredag, mandag)), List.of(VilkårType.BEREGNINGSGRUNNLAGVILKÅR))
            .leggTil(avslåttVilkårBuilder)
            .build();

        var perioderSomSkalFjernes = finnPerioderSomSkalFjernesIBeregning.finnPerioderSomSkalFjernes(vilkårene, behandlingReferanceMock, Set.of(DatoIntervallEntitet.fra(fredag, mandag)));

        assertThat(perioderSomSkalFjernes).isEmpty();
    }

}
