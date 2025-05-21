package no.nav.ung.sak.inngangsvilkår.alder;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Set;
import java.util.TreeSet;

import org.junit.jupiter.api.Test;

import no.nav.ung.kodeverk.vilkår.Avslagsårsak;
import no.nav.ung.kodeverk.vilkår.Utfall;
import no.nav.ung.kodeverk.vilkår.VilkårType;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.VilkårBuilder;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;

class VurderAldersVilkårTjenesteTest {

    private final VurderAldersVilkårTjeneste tjeneste = new VurderAldersVilkårTjeneste();

    @Test
    void skal_gi_avslag_under_laveste_alder() {
        var fødselsdato = LocalDate.now().minusYears(25);
        var vilkårBuilder = new VilkårBuilder(VilkårType.ALDERSVILKÅR);

        LocalDate førsteSøknadsdato = fødselsdato.plusYears(18).minusDays(1);
        LocalDate sisteSøknadsdato = fødselsdato.plusYears(30);
        tjeneste.vurderPerioder(vilkårBuilder, new TreeSet<>(Set.of(DatoIntervallEntitet.fraOgMedTilOgMed(førsteSøknadsdato, sisteSøknadsdato))), fødselsdato);

        var vilkår = vilkårBuilder.build();


        assertThat(vilkår).isNotNull();
        var sortertePerioder = new ArrayList<>(vilkår.getPerioder().stream().sorted().toList());

        assertThat(sortertePerioder).hasSize(1);
        assertThat(sortertePerioder.get(0).getPeriode().getFomDato()).isEqualTo(førsteSøknadsdato);
        assertThat(sortertePerioder.get(0).getPeriode().getTomDato()).isEqualTo(sisteSøknadsdato);
        assertThat(sortertePerioder.get(0).getUtfall()).isEqualTo(Utfall.IKKE_OPPFYLT);
        assertThat(sortertePerioder.get(0).getAvslagsårsak()).isEqualTo(Avslagsårsak.SØKER_UNDER_MINSTE_ALDER);
    }

    @Test
    void skal_gi_avslag_over_høyeste_alder() {
        var fødselsdato = LocalDate.now().minusYears(25);
        var vilkårBuilder = new VilkårBuilder(VilkårType.ALDERSVILKÅR);

        LocalDate førsteSøknadsdato = fødselsdato.plusYears(29);
        LocalDate sisteSøknadsdato = fødselsdato.plusYears(30);
        tjeneste.vurderPerioder(vilkårBuilder, new TreeSet<>(Set.of(DatoIntervallEntitet.fraOgMedTilOgMed(førsteSøknadsdato, sisteSøknadsdato))), fødselsdato);

        var vilkår = vilkårBuilder.build();


        assertThat(vilkår).isNotNull();
        var sortertePerioder = new ArrayList<>(vilkår.getPerioder().stream().sorted().toList());

        assertThat(sortertePerioder).hasSize(1);
        assertThat(sortertePerioder.get(0).getPeriode().getFomDato()).isEqualTo(førsteSøknadsdato);
        assertThat(sortertePerioder.get(0).getPeriode().getTomDato()).isEqualTo(sisteSøknadsdato);
        assertThat(sortertePerioder.get(0).getUtfall()).isEqualTo(Utfall.IKKE_OPPFYLT);
        assertThat(sortertePerioder.get(0).getAvslagsårsak()).isEqualTo(Avslagsårsak.SØKER_OVER_HØYESTE_ALDER);
    }

    @Test
    void skal_gi_innvilget_i_nedre_grensetilfelle() {
        var fødselsdato = LocalDate.now().minusYears(25);
        var vilkårBuilder = new VilkårBuilder(VilkårType.ALDERSVILKÅR);

        LocalDate førsteSøknadsdato = fødselsdato.plusYears(18);
        LocalDate sisteSøknadsdato = fødselsdato.plusYears(19);
        tjeneste.vurderPerioder(vilkårBuilder, new TreeSet<>(Set.of(DatoIntervallEntitet.fraOgMedTilOgMed(førsteSøknadsdato, sisteSøknadsdato))), fødselsdato);

        var vilkår = vilkårBuilder.build();


        assertThat(vilkår).isNotNull();
        var sortertePerioder = new ArrayList<>(vilkår.getPerioder().stream().sorted().toList());

        assertThat(sortertePerioder).hasSize(1);
        assertThat(sortertePerioder.get(0).getPeriode().getFomDato()).isEqualTo(førsteSøknadsdato);
        assertThat(sortertePerioder.get(0).getPeriode().getTomDato()).isEqualTo(sisteSøknadsdato);
        assertThat(sortertePerioder.get(0).getUtfall()).isEqualTo(Utfall.OPPFYLT);
        assertThat(sortertePerioder.get(0).getAvslagsårsak()).isNull();
    }


    @Test
    void skal_gi_innvilget_i_øvre_grensetilfelle() {
        var fødselsdato = LocalDate.now().minusYears(25);
        var vilkårBuilder = new VilkårBuilder(VilkårType.ALDERSVILKÅR);

        LocalDate førsteSøknadsdato = fødselsdato.plusYears(29).minusDays(1);
        LocalDate sisteSøknadsdato = fødselsdato.plusYears(30);
        tjeneste.vurderPerioder(vilkårBuilder, new TreeSet<>(Set.of(DatoIntervallEntitet.fraOgMedTilOgMed(førsteSøknadsdato, sisteSøknadsdato))), fødselsdato);

        var vilkår = vilkårBuilder.build();


        assertThat(vilkår).isNotNull();
        var sortertePerioder = new ArrayList<>(vilkår.getPerioder().stream().sorted().toList());

        assertThat(sortertePerioder).hasSize(1);
        assertThat(sortertePerioder.get(0).getPeriode().getFomDato()).isEqualTo(førsteSøknadsdato);
        assertThat(sortertePerioder.get(0).getPeriode().getTomDato()).isEqualTo(sisteSøknadsdato);
        assertThat(sortertePerioder.get(0).getUtfall()).isEqualTo(Utfall.OPPFYLT);
        assertThat(sortertePerioder.get(0).getAvslagsårsak()).isNull();
    }

}
