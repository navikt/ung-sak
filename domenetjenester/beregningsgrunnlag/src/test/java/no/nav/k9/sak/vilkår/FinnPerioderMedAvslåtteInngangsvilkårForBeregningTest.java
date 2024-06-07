package no.nav.k9.sak.vilkår;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.k9.felles.testutilities.cdi.UnitTestLookupInstanceImpl;
import no.nav.k9.kodeverk.vilkår.Utfall;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.PåTversAvHelgErKantIKantVurderer;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårBuilder;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.Vilkårene;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriodeBuilder;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.perioder.VilkårsPerioderTilVurderingTjeneste;
import no.nav.k9.sak.typer.Saksnummer;

class FinnPerioderMedAvslåtteInngangsvilkårForBeregningTest {

    @Test
    void skal_ikke_fjerne_om_helg_er_avslått() {
        var fredag = LocalDate.of(2024, 6, 7);
        var laurdag = LocalDate.of(2024, 6, 8);
        var søndag = LocalDate.of(2024, 6, 9);
        var mandag = LocalDate.of(2024, 6, 10);

        var avslåttVilkårBuilder = new VilkårBuilder(VilkårType.ALDERSVILKÅR)
            .leggTil( new VilkårPeriodeBuilder()
                .medPeriode(fredag, fredag)
                .medUtfall(Utfall.OPPFYLT)
            )
            .leggTil( new VilkårPeriodeBuilder()
                .medPeriode(laurdag, søndag)
                .medUtfall(Utfall.IKKE_OPPFYLT)
            )
            .leggTil( new VilkårPeriodeBuilder()
                .medPeriode(mandag, mandag)
                .medUtfall(Utfall.OPPFYLT)
            );

        var vilkårene = Vilkårene.builder()
            .leggTilIkkeVurderteVilkår(List.of(DatoIntervallEntitet.fra(fredag, mandag)), List.of(VilkårType.BEREGNINGSGRUNNLAGVILKÅR))
            .leggTil(avslåttVilkårBuilder)
            .build();

        var perioderSomSkalFjernes = FinnPerioderMedAvslåtteInngangsvilkårForBeregning.finnPerioderMedAvslåtteInngangsvilkår(vilkårene, new PåTversAvHelgErKantIKantVurderer());

        assertThat(perioderSomSkalFjernes).isEmpty();
    }

}
