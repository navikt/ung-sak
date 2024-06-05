package no.nav.k9.sak.ytelse.pleiepengerbarn.beregningsgrunnlag;

import no.nav.k9.kodeverk.vilkår.Utfall;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårBuilder;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.Vilkårene;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriodeBuilder;
import no.nav.k9.sak.domene.behandling.steg.beregningsgrunnlag.FinnPerioderSomSkalFjernesIBeregning;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.typer.Saksnummer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PSBFinnPerioderSomSkalFjernesIBeregningTest {
    BehandlingReferanse behandlingReferanceMock = mock(BehandlingReferanse.class);

    FinnPerioderSomSkalFjernesIBeregning finnPerioderSomSkalFjernesIBeregning = new PSBFinnPerioderSomSkalFjernesIBeregning();

    private static final Saksnummer saksnummer = new Saksnummer("AAAAA");

    @BeforeEach
    void setup() {
        when(behandlingReferanceMock.getSaksnummer()).thenReturn(saksnummer);
    }

    @Test
    void fjern_perioder_med_minst_ett_avslått_vilkår() {
        var fom = LocalDate.of(2024, 2, 1);
        var tom = fom.plusDays(3);

        var avslåttVilkårBuilder = new VilkårBuilder(VilkårType.ALDERSVILKÅR)
            .leggTil( new VilkårPeriodeBuilder()
                .medPeriode(fom, tom)
                .medUtfall(Utfall.IKKE_OPPFYLT)
            );

        var vilkårene = Vilkårene.builder()
            .leggTilIkkeVurderteVilkår(List.of(DatoIntervallEntitet.fra(fom, tom)), List.of(VilkårType.BEREGNINGSGRUNNLAGVILKÅR))
            .leggTil(avslåttVilkårBuilder)
            .build();

        var perioderSomSkalFjernes = finnPerioderSomSkalFjernesIBeregning.finnPerioderSomSkalFjernes(vilkårene, behandlingReferanceMock);
        assertThat(perioderSomSkalFjernes).isNotEmpty();
    }

    @Test
    void fjern_perioder_uten_minst_ett_avslått_vilkår() {
        var fom = LocalDate.of(2024, 2, 1);
        var tom = fom.plusDays(3);

        var avslåttVilkårBuilder = new VilkårBuilder(VilkårType.ALDERSVILKÅR)
            .leggTil( new VilkårPeriodeBuilder()
                .medPeriode(fom, tom)
                .medUtfall(Utfall.OPPFYLT)
            );

        var vilkårene = Vilkårene.builder()
            .leggTilIkkeVurderteVilkår(List.of(DatoIntervallEntitet.fra(fom, tom)), List.of(VilkårType.BEREGNINGSGRUNNLAGVILKÅR))
            .leggTil(avslåttVilkårBuilder)
            .build();

        var perioderSomSkalFjernes = finnPerioderSomSkalFjernesIBeregning.finnPerioderSomSkalFjernes(vilkårene, behandlingReferanceMock);
        assertThat(perioderSomSkalFjernes).isEmpty();
    }
}
