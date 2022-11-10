package no.nav.k9.sak.domene.behandling.steg.inngangsvilkår.alder;


import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Set;
import java.util.TreeSet;

import org.junit.jupiter.api.Test;

import no.nav.k9.kodeverk.vilkår.Avslagsårsak;
import no.nav.k9.kodeverk.vilkår.Utfall;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårBuilder;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;

class VurderAldersVilkårTjenesteTest {

    private final VurderAldersVilkårTjeneste tjeneste = new VurderAldersVilkårTjeneste();

    @Test
    void skal_vurdere_aldersvilkåret() {
        var fødselsdato = LocalDate.now().minusYears(70).plusDays(7);
        var vilkårBuilder = new VilkårBuilder(VilkårType.ALDERSVILKÅR);

        tjeneste.vurderPerioder(vilkårBuilder, new TreeSet<>(Set.of(DatoIntervallEntitet.fraOgMedTilOgMed(fødselsdato.plusYears(70).minusDays(7), fødselsdato.plusYears(70).plusDays(7)))), fødselsdato);

        var vilkår = vilkårBuilder.build();

        assertThat(vilkår).isNotNull();
        assertThat(vilkår.getPerioder()).hasSize(2);
        assertThat(vilkår.getPerioder().stream().filter(it -> Utfall.IKKE_OPPFYLT.equals(it.getUtfall()))).hasSize(1);
        assertThat(vilkår.getPerioder().stream().filter(it -> Utfall.OPPFYLT.equals(it.getUtfall()))).hasSize(1);
        assertThat(vilkår.getPerioder().stream().filter(it -> Avslagsårsak.SØKER_OVER_HØYESTE_ALDER.equals(it.getAvslagsårsak()))).hasSize(1);
    }

}
