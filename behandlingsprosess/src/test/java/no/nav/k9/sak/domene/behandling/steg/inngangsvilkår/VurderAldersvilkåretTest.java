package no.nav.k9.sak.domene.behandling.steg.inngangsvilkår;


import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Set;
import java.util.TreeSet;

import org.junit.jupiter.api.Test;

import no.nav.k9.kodeverk.vilkår.Utfall;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårBuilder;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;

public class VurderAldersvilkåretTest {

    private VurderAldersvilkåretSteg steg = new VurderAldersvilkåretSteg();

    @Test
    void skal_vurdere_aldersvilkåret() {
        var fødselsdato = LocalDate.now().minusYears(70).plusDays(7);
        var vilkårBuilder = new VilkårBuilder(VilkårType.ALDERSVILKÅR);

        steg.vurderPerioder(vilkårBuilder, new TreeSet<>(Set.of(DatoIntervallEntitet.fraOgMedTilOgMed(fødselsdato.plusYears(70).minusDays(7), fødselsdato.plusYears(70).plusDays(7)))), fødselsdato);

        var vilkår = vilkårBuilder.build();

        assertThat(vilkår).isNotNull();
        assertThat(vilkår.getPerioder()).hasSize(2);
        assertThat(vilkår.getPerioder().stream().filter(it -> Utfall.IKKE_OPPFYLT.equals(it.getUtfall()))).hasSize(1);
        assertThat(vilkår.getPerioder().stream().filter(it -> Utfall.OPPFYLT.equals(it.getUtfall()))).hasSize(1);
    }
}
