package no.nav.k9.sak.behandlingslager.behandling.vilkår;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.Test;

import no.nav.k9.kodeverk.vilkår.Utfall;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriode;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;

public class VilkårBuilderTest {

    @Test
    public void skal_opprette_perioder_for_resultat() {
        final var vilkårBuilder = new VilkårBuilder().medType(VilkårType.MEDLEMSKAPSVILKÅRET);

        final var førsteSkjæringstidspunkt = LocalDate.now();
        final var sluttFørstePeriode = LocalDate.now().plusMonths(3);
        final var førstePeriode = vilkårBuilder.hentBuilderFor(førsteSkjæringstidspunkt, sluttFørstePeriode)
            .medUtfall(Utfall.IKKE_VURDERT);
        final var andreSkjæringstidspunkt = LocalDate.now().plusMonths(4);
        final var andrePeriode = vilkårBuilder.hentBuilderFor(andreSkjæringstidspunkt, LocalDate.now().plusMonths(7))
            .medUtfall(Utfall.IKKE_VURDERT);

        vilkårBuilder.leggTil(førstePeriode)
            .leggTil(andrePeriode);

        final var vilkår = vilkårBuilder.build();

        assertThat(vilkår).isNotNull();
        assertThat(vilkår.getPerioder()).hasSize(2);
        assertThat(vilkår.getPerioder().stream().map(VilkårPeriode::getPeriode).map(DatoIntervallEntitet::getFomDato)).containsExactly(førsteSkjæringstidspunkt, andreSkjæringstidspunkt);
        assertThat(vilkår.getPerioder().stream().map(VilkårPeriode::getUtfall)).containsExactly(Utfall.IKKE_VURDERT, Utfall.IKKE_VURDERT);

        final var oppdatertVilkårBuilder = new VilkårBuilder(vilkår);
        final var oppdatertFørstePeriodeBuilder = oppdatertVilkårBuilder.hentBuilderFor(førsteSkjæringstidspunkt, sluttFørstePeriode)
            .medUtfall(Utfall.IKKE_OPPFYLT)
            .medUtfallOverstyrt(Utfall.OPPFYLT);
        oppdatertVilkårBuilder.leggTil(oppdatertFørstePeriodeBuilder);

        final var oppdatertVilkår = oppdatertVilkårBuilder.build();

        assertThat(oppdatertVilkår).isNotNull();
        assertThat(oppdatertVilkår.getPerioder()).hasSize(2);

        assertThat(oppdatertVilkår.getPerioder().stream().map(VilkårPeriode::getPeriode).map(DatoIntervallEntitet::getFomDato)).containsExactly(førsteSkjæringstidspunkt, andreSkjæringstidspunkt);
        assertThat(oppdatertVilkår.getPerioder().stream().map(VilkårPeriode::getUtfall)).containsExactly(Utfall.IKKE_OPPFYLT, Utfall.IKKE_VURDERT);
        assertThat(oppdatertVilkår.getPerioder().stream().map(VilkårPeriode::getGjeldendeUtfall)).containsExactly(Utfall.OPPFYLT, Utfall.IKKE_VURDERT);

        final var oppdatertVilkårBuilder2 = new VilkårBuilder(oppdatertVilkår);
        final var oppdatertFørstePeriodeBuilder2 = oppdatertVilkårBuilder2.hentBuilderFor(førsteSkjæringstidspunkt, sluttFørstePeriode)
            .medUtfall(Utfall.OPPFYLT);
        oppdatertVilkårBuilder2.leggTil(oppdatertFørstePeriodeBuilder2);

        final var oppdatertVilkår2 = oppdatertVilkårBuilder2.build();

        assertThat(oppdatertVilkår2).isNotNull();
        assertThat(oppdatertVilkår2.getPerioder()).hasSize(2);

        assertThat(oppdatertVilkår2.getPerioder().stream().map(VilkårPeriode::getPeriode).map(DatoIntervallEntitet::getFomDato)).containsExactly(førsteSkjæringstidspunkt, andreSkjæringstidspunkt);
        assertThat(oppdatertVilkår2.getPerioder().stream().map(VilkårPeriode::getUtfall)).containsExactly(Utfall.OPPFYLT, Utfall.IKKE_VURDERT);
        assertThat(oppdatertVilkår2.getPerioder().stream().map(VilkårPeriode::getGjeldendeUtfall)).containsExactly(Utfall.OPPFYLT, Utfall.IKKE_VURDERT);
    }

    @Test
    public void skal_teste_mellomliggende_perioder() {
        final var vilkårBuilder = new VilkårBuilder()
            .medType(VilkårType.MEDLEMSKAPSVILKÅRET)
            .medMaksMellomliggendePeriodeAvstand(7);

        final var førsteSkjæringstidspunkt = LocalDate.now();
        final var sluttFørstePeriode = LocalDate.now().plusMonths(3);
        final var førstePeriode = vilkårBuilder.hentBuilderFor(førsteSkjæringstidspunkt, sluttFørstePeriode)
            .medUtfall(Utfall.IKKE_VURDERT);
        final var andreSkjæringstidspunkt = LocalDate.now().plusMonths(3).plusDays(6);
        final var andrePeriode = vilkårBuilder.hentBuilderFor(andreSkjæringstidspunkt, LocalDate.now().plusMonths(5))
            .medUtfall(Utfall.IKKE_VURDERT);

        vilkårBuilder.leggTil(førstePeriode)
            .leggTil(andrePeriode);

        final var vilkår = vilkårBuilder.build();
        assertThat(vilkår).isNotNull();
        assertThat(vilkår.getPerioder()).hasSize(1);
        assertThat(vilkår.getPerioder().stream().map(VilkårPeriode::getPeriode).map(DatoIntervallEntitet::getFomDato)).containsExactly(førsteSkjæringstidspunkt);
        assertThat(vilkår.getPerioder().stream().map(VilkårPeriode::getPeriode).map(DatoIntervallEntitet::getTomDato)).containsExactly(LocalDate.now().plusMonths(5));
    }

    @Test
    public void skal_teste_mellomliggende_perioder_forskjellig_begrunnelse() {
        final var vilkårBuilder = new VilkårBuilder()
            .medType(VilkårType.MEDLEMSKAPSVILKÅRET)
            .medMaksMellomliggendePeriodeAvstand(7);

        final var førsteSkjæringstidspunkt = LocalDate.now();
        final var sluttFørstePeriode = LocalDate.now().plusMonths(3);
        final var førstePeriode = vilkårBuilder.hentBuilderFor(førsteSkjæringstidspunkt, sluttFørstePeriode)
            .medUtfall(Utfall.OPPFYLT)
            .medBegrunnelse("JADDA!");
        final var andreSkjæringstidspunkt = LocalDate.now().plusMonths(3).plusDays(6);
        final var andrePeriode = vilkårBuilder.hentBuilderFor(andreSkjæringstidspunkt, LocalDate.now().plusMonths(5))
            .medUtfall(Utfall.OPPFYLT)
            .medBegrunnelse("JESSS");

        vilkårBuilder.leggTil(førstePeriode)
            .leggTil(andrePeriode);

        final var vilkår = vilkårBuilder.build();
        assertThat(vilkår).isNotNull();
        assertThat(vilkår.getPerioder()).hasSize(2);
        assertThat(vilkår.getPerioder().stream().map(VilkårPeriode::getPeriode).map(DatoIntervallEntitet::getFomDato)).containsExactly(førsteSkjæringstidspunkt, andreSkjæringstidspunkt);
        assertThat(vilkår.getPerioder().stream().map(VilkårPeriode::getPeriode).map(DatoIntervallEntitet::getTomDato)).containsExactly(andreSkjæringstidspunkt.minusDays(1), LocalDate.now().plusMonths(5));
    }

    @Test
    public void skal_få_to_perioder_hvis_avstand_er_mer_enn_6_dager() {
        final var vilkårBuilder = new VilkårBuilder()
            .medType(VilkårType.MEDLEMSKAPSVILKÅRET)
            .medMaksMellomliggendePeriodeAvstand(7);

        final var førsteSkjæringstidspunkt = LocalDate.now();
        final var sluttFørstePeriode = LocalDate.now().plusMonths(3);
        final var førstePeriode = vilkårBuilder.hentBuilderFor(førsteSkjæringstidspunkt, sluttFørstePeriode)
            .medUtfall(Utfall.OPPFYLT);
        final var andreSkjæringstidspunkt = LocalDate.now().plusMonths(3).plusDays(7);
        final var andrePeriode = vilkårBuilder.hentBuilderFor(andreSkjæringstidspunkt, LocalDate.now().plusMonths(5))
            .medUtfall(Utfall.OPPFYLT);

        vilkårBuilder.leggTil(førstePeriode)
            .leggTil(andrePeriode);

        final var vilkår = vilkårBuilder.build();
        assertThat(vilkår).isNotNull();
        assertThat(vilkår.getPerioder()).hasSize(2);
        assertThat(vilkår.getPerioder().stream().map(VilkårPeriode::getPeriode).map(DatoIntervallEntitet::getFomDato)).containsExactly(førsteSkjæringstidspunkt, andreSkjæringstidspunkt);
        assertThat(vilkår.getPerioder().stream().map(VilkårPeriode::getPeriode).map(DatoIntervallEntitet::getTomDato)).containsExactly(andreSkjæringstidspunkt.minusDays(7), LocalDate.now().plusMonths(5));
    }
}
