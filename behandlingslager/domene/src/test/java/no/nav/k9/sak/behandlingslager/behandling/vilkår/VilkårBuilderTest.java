package no.nav.k9.sak.behandlingslager.behandling.vilkår;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.LocalDate;
import java.util.stream.Collectors;

import org.junit.Test;

import no.nav.k9.kodeverk.vilkår.Utfall;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriode;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriodeBuilder;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;

public class VilkårBuilderTest {

    @Test
    public void skal_opprette_perioder_for_resultat() {
        var vilkårBuilder = new VilkårBuilder(VilkårType.OPPTJENINGSVILKÅRET)
            .medKantIKantVurderer(new DefaultKantIKantVurderer());

        var førsteSkjæringstidspunkt = LocalDate.now();
        var sluttFørstePeriode = LocalDate.now().plusMonths(3);
        var førstePeriode = vilkårBuilder.hentBuilderFor(førsteSkjæringstidspunkt, sluttFørstePeriode)
            .medUtfall(Utfall.IKKE_VURDERT);
        var andreSkjæringstidspunkt = LocalDate.now().plusMonths(4);
        var andrePeriode = vilkårBuilder.hentBuilderFor(andreSkjæringstidspunkt, LocalDate.now().plusMonths(7))
            .medUtfall(Utfall.IKKE_VURDERT);

        vilkårBuilder.leggTil(førstePeriode)
            .leggTil(andrePeriode);

        var vilkår = vilkårBuilder.build();

        assertThat(vilkår).isNotNull();
        assertThat(vilkår.getPerioder()).hasSize(2);
        assertThat(vilkår.getPerioder().stream().map(VilkårPeriode::getPeriode).map(DatoIntervallEntitet::getFomDato)).containsExactly(førsteSkjæringstidspunkt, andreSkjæringstidspunkt);
        assertThat(vilkår.getPerioder().stream().map(VilkårPeriode::getUtfall)).containsExactly(Utfall.IKKE_VURDERT, Utfall.IKKE_VURDERT);

        var oppdatertVilkårBuilder = new VilkårBuilder(vilkår).medKantIKantVurderer(new DefaultKantIKantVurderer());
        var oppdatertFørstePeriodeBuilder = oppdatertVilkårBuilder.hentBuilderFor(førsteSkjæringstidspunkt, sluttFørstePeriode)
            .medUtfall(Utfall.IKKE_OPPFYLT)
            .medUtfallOverstyrt(Utfall.OPPFYLT);
        oppdatertVilkårBuilder.leggTil(oppdatertFørstePeriodeBuilder);

        var oppdatertVilkår = oppdatertVilkårBuilder.build();

        assertThat(oppdatertVilkår).isNotNull();
        assertThat(oppdatertVilkår.getPerioder()).hasSize(2);

        assertThat(oppdatertVilkår.getPerioder().stream().map(VilkårPeriode::getPeriode).map(DatoIntervallEntitet::getFomDato)).containsExactly(førsteSkjæringstidspunkt, andreSkjæringstidspunkt);
        assertThat(oppdatertVilkår.getPerioder().stream().map(VilkårPeriode::getUtfall)).containsExactly(Utfall.IKKE_OPPFYLT, Utfall.IKKE_VURDERT);
        assertThat(oppdatertVilkår.getPerioder().stream().map(VilkårPeriode::getGjeldendeUtfall)).containsExactly(Utfall.OPPFYLT, Utfall.IKKE_VURDERT);

        var oppdatertVilkårBuilder2 = new VilkårBuilder(oppdatertVilkår).medKantIKantVurderer(new DefaultKantIKantVurderer());
        var oppdatertFørstePeriodeBuilder2 = oppdatertVilkårBuilder2.hentBuilderFor(førsteSkjæringstidspunkt, sluttFørstePeriode)
            .medUtfall(Utfall.OPPFYLT);
        oppdatertVilkårBuilder2.leggTil(oppdatertFørstePeriodeBuilder2);

        var oppdatertVilkår2 = oppdatertVilkårBuilder2.build();

        assertThat(oppdatertVilkår2).isNotNull();
        assertThat(oppdatertVilkår2.getPerioder()).hasSize(2);

        assertThat(oppdatertVilkår2.getPerioder().stream().map(VilkårPeriode::getPeriode).map(DatoIntervallEntitet::getFomDato)).containsExactly(førsteSkjæringstidspunkt, andreSkjæringstidspunkt);
        assertThat(oppdatertVilkår2.getPerioder().stream().map(VilkårPeriode::getUtfall)).containsExactly(Utfall.OPPFYLT, Utfall.IKKE_VURDERT);
        assertThat(oppdatertVilkår2.getPerioder().stream().map(VilkårPeriode::getGjeldendeUtfall)).containsExactly(Utfall.OPPFYLT, Utfall.IKKE_VURDERT);
    }

    @Test
    public void skal_teste_mellomliggende_perioder() {
        var vilkårBuilder = new VilkårBuilder(VilkårType.OPPTJENINGSVILKÅRET)
            .medKantIKantVurderer(new DefaultKantIKantVurderer())
            .medMaksMellomliggendePeriodeAvstand(7);

        var førsteSkjæringstidspunkt = LocalDate.now();
        var sluttFørstePeriode = LocalDate.now().plusMonths(3);
        var førstePeriode = vilkårBuilder.hentBuilderFor(førsteSkjæringstidspunkt, sluttFørstePeriode)
            .medUtfall(Utfall.IKKE_VURDERT);
        var andreSkjæringstidspunkt = LocalDate.now().plusMonths(3).plusDays(6);
        var andrePeriode = vilkårBuilder.hentBuilderFor(andreSkjæringstidspunkt, LocalDate.now().plusMonths(5))
            .medUtfall(Utfall.IKKE_VURDERT);

        vilkårBuilder.leggTil(førstePeriode)
            .leggTil(andrePeriode);

        var vilkår = vilkårBuilder.build();
        assertThat(vilkår).isNotNull();
        assertThat(vilkår.getPerioder()).hasSize(1);
        assertThat(vilkår.getPerioder().stream().map(VilkårPeriode::getPeriode).map(DatoIntervallEntitet::getFomDato)).containsExactly(førsteSkjæringstidspunkt);
        assertThat(vilkår.getPerioder().stream().map(VilkårPeriode::getPeriode).map(DatoIntervallEntitet::getTomDato)).containsExactly(LocalDate.now().plusMonths(5));
    }

    @Test
    public void skal_revertere_mellomliggende_perioder() {
        var vilkårBuilder = new VilkårBuilder(VilkårType.OPPTJENINGSVILKÅRET)
            .medKantIKantVurderer(new DefaultKantIKantVurderer())
            .medMaksMellomliggendePeriodeAvstand(7);

        var førsteSkjæringstidspunkt = LocalDate.now();
        var sluttFørstePeriode = LocalDate.now().plusMonths(3);
        var førstePeriode = vilkårBuilder.hentBuilderFor(førsteSkjæringstidspunkt, sluttFørstePeriode)
            .medUtfall(Utfall.IKKE_VURDERT);
        var andreSkjæringstidspunkt = LocalDate.now().plusMonths(3).plusDays(6);
        var sluttAndrePeriode = LocalDate.now().plusMonths(5);
        var andrePeriode = vilkårBuilder.hentBuilderFor(andreSkjæringstidspunkt, sluttAndrePeriode)
            .medUtfall(Utfall.IKKE_VURDERT);

        vilkårBuilder.leggTil(førstePeriode)
            .leggTil(andrePeriode);

        var vilkår = vilkårBuilder.build();
        assertThat(vilkår).isNotNull();
        assertThat(vilkår.getPerioder()).hasSize(1);
        assertThat(vilkår.getPerioder().stream().map(VilkårPeriode::getPeriode).map(DatoIntervallEntitet::getFomDato)).containsExactly(førsteSkjæringstidspunkt);
        assertThat(vilkår.getPerioder().stream().map(VilkårPeriode::getPeriode).map(DatoIntervallEntitet::getTomDato)).containsExactly(sluttAndrePeriode);

        var fullstendigTidslinje = new VilkårBuilder(VilkårType.OPPTJENINGSVILKÅRET)
            .somDummy()
            .medKantIKantVurderer(new DefaultKantIKantVurderer())
            .medMaksMellomliggendePeriodeAvstand(7);
        fullstendigTidslinje.leggTil(fullstendigTidslinje.hentBuilderFor(andreSkjæringstidspunkt, sluttAndrePeriode));

        var tilbakestill = new VilkårBuilder(vilkår)
            .medKantIKantVurderer(new DefaultKantIKantVurderer())
            .medMaksMellomliggendePeriodeAvstand(7)
            .medFullstendigTidslinje(fullstendigTidslinje.getTidslinje())
            .tilbakestill(DatoIntervallEntitet.fraOgMedTilOgMed(førsteSkjæringstidspunkt, sluttFørstePeriode));

        var tilbakestiltVilkår = tilbakestill.build();
        assertThat(tilbakestiltVilkår).isNotNull();
        assertThat(tilbakestiltVilkår.getPerioder()).hasSize(1);
        assertThat(tilbakestiltVilkår.getPerioder().stream().map(VilkårPeriode::getPeriode).map(DatoIntervallEntitet::getFomDato)).containsExactly(andreSkjæringstidspunkt);
        assertThat(tilbakestiltVilkår.getPerioder().stream().map(VilkårPeriode::getPeriode).map(DatoIntervallEntitet::getTomDato)).containsExactly(sluttAndrePeriode);
    }

    @Test
    public void skal_teste_mellomliggende_perioder_forskjellig_begrunnelse() {
        var vilkårBuilder = new VilkårBuilder(VilkårType.OPPTJENINGSVILKÅRET)
            .medKantIKantVurderer(new DefaultKantIKantVurderer())
            .medMaksMellomliggendePeriodeAvstand(7);

        var førsteSkjæringstidspunkt = LocalDate.now();
        var sluttFørstePeriode = LocalDate.now().plusMonths(3);
        var førstePeriode = vilkårBuilder.hentBuilderFor(førsteSkjæringstidspunkt, sluttFørstePeriode)
            .medUtfall(Utfall.OPPFYLT)
            .medBegrunnelse("JADDA!");
        var andreSkjæringstidspunkt = LocalDate.now().plusMonths(3).plusDays(6);
        var andrePeriode = vilkårBuilder.hentBuilderFor(andreSkjæringstidspunkt, LocalDate.now().plusMonths(5))
            .medUtfall(Utfall.OPPFYLT)
            .medBegrunnelse("JESSS");

        vilkårBuilder.leggTil(førstePeriode)
            .leggTil(andrePeriode);

        var vilkår = vilkårBuilder.build();
        assertThat(vilkår).isNotNull();
        assertThat(vilkår.getPerioder()).hasSize(2);
        assertThat(vilkår.getPerioder().stream().map(VilkårPeriode::getPeriode).map(DatoIntervallEntitet::getFomDato)).containsExactly(førsteSkjæringstidspunkt, andreSkjæringstidspunkt);
        assertThat(vilkår.getPerioder().stream().map(VilkårPeriode::getPeriode).map(DatoIntervallEntitet::getTomDato)).containsExactly(andreSkjæringstidspunkt.minusDays(1),
            LocalDate.now().plusMonths(5));
    }

    @Test
    public void skal_få_to_perioder_hvis_avstand_er_mer_enn_6_dager() {
        var vilkårBuilder = new VilkårBuilder(VilkårType.OPPTJENINGSVILKÅRET)
            .medKantIKantVurderer(new DefaultKantIKantVurderer())
            .medMaksMellomliggendePeriodeAvstand(7);

        var førsteSkjæringstidspunkt = LocalDate.now();
        var sluttFørstePeriode = LocalDate.now().plusMonths(3);
        var førstePeriode = vilkårBuilder.hentBuilderFor(førsteSkjæringstidspunkt, sluttFørstePeriode)
            .medUtfall(Utfall.OPPFYLT);
        var andreSkjæringstidspunkt = LocalDate.now().plusMonths(3).plusDays(7);
        var andrePeriode = vilkårBuilder.hentBuilderFor(andreSkjæringstidspunkt, LocalDate.now().plusMonths(5))
            .medUtfall(Utfall.OPPFYLT);

        vilkårBuilder.leggTil(førstePeriode)
            .leggTil(andrePeriode);

        var vilkår = vilkårBuilder.build();
        assertThat(vilkår).isNotNull();
        assertThat(vilkår.getPerioder()).hasSize(2);
        assertThat(vilkår.getPerioder().stream().map(VilkårPeriode::getPeriode).map(DatoIntervallEntitet::getFomDato)).containsExactly(førsteSkjæringstidspunkt, andreSkjæringstidspunkt);
        assertThat(vilkår.getPerioder().stream().map(VilkårPeriode::getPeriode).map(DatoIntervallEntitet::getTomDato)).containsExactly(andreSkjæringstidspunkt.minusDays(7),
            LocalDate.now().plusMonths(5));
    }

    @Test
    public void skal_håndtere_tilbakestilling_av_periode() {
        var vilkårBuilder = new VilkårBuilder(VilkårType.OPPTJENINGSVILKÅRET)
            .medKantIKantVurderer(new DefaultKantIKantVurderer())
            .medMaksMellomliggendePeriodeAvstand(7);

        var førsteSkjæringstidspunkt = LocalDate.now();
        var sluttFørstePeriode = LocalDate.now().plusMonths(3);
        var førstePeriode = vilkårBuilder.hentBuilderFor(førsteSkjæringstidspunkt, sluttFørstePeriode)
            .medUtfall(Utfall.OPPFYLT);
        var andreSkjæringstidspunkt = LocalDate.now().plusMonths(3).plusDays(7);
        var andrePeriode = vilkårBuilder.hentBuilderFor(andreSkjæringstidspunkt, LocalDate.now().plusMonths(5))
            .medUtfall(Utfall.OPPFYLT);

        vilkårBuilder.leggTil(førstePeriode)
            .leggTil(andrePeriode);

        var vilkår = vilkårBuilder.build();
        assertThat(vilkår).isNotNull();
        assertThat(vilkår.getPerioder()).hasSize(2);
        assertThat(vilkår.getPerioder().stream().map(VilkårPeriode::getPeriode).map(DatoIntervallEntitet::getFomDato)).containsExactly(førsteSkjæringstidspunkt, andreSkjæringstidspunkt);
        assertThat(vilkår.getPerioder().stream().map(VilkårPeriode::getPeriode).map(DatoIntervallEntitet::getTomDato)).containsExactly(andreSkjæringstidspunkt.minusDays(7),
            LocalDate.now().plusMonths(5));

        var vilkårTilbakestilt = new VilkårBuilder(vilkår)
            .medKantIKantVurderer(new DefaultKantIKantVurderer())
            .tilbakestill(DatoIntervallEntitet.fraOgMedTilOgMed(førsteSkjæringstidspunkt, førsteSkjæringstidspunkt.plusMonths(1)))
            .leggTil(new VilkårPeriodeBuilder()
                .medPeriode(førsteSkjæringstidspunkt.plusDays(10), førsteSkjæringstidspunkt.plusDays(14))
                .medUtfall(Utfall.IKKE_VURDERT))
            .build();

        assertThat(vilkårTilbakestilt).isNotNull();
        assertThat(vilkårTilbakestilt.getPerioder()).hasSize(3);
    }

    @Test
    public void skal_få_periode_selv_om_denne_er_en_del_etterspurt_periode() {
        var vilkårBuilder = new VilkårBuilder(VilkårType.OPPTJENINGSVILKÅRET)
            .medKantIKantVurderer(new DefaultKantIKantVurderer())
            .medMaksMellomliggendePeriodeAvstand(7);

        var førsteSkjæringstidspunkt = LocalDate.now();
        var sluttFørstePeriode = LocalDate.now().plusMonths(3);
        var førstePeriode = vilkårBuilder.hentBuilderFor(førsteSkjæringstidspunkt, sluttFørstePeriode)
            .medUtfall(Utfall.OPPFYLT);
        var andreSkjæringstidspunkt = LocalDate.now().plusMonths(3).plusDays(7);
        var andrePeriode = vilkårBuilder.hentBuilderFor(andreSkjæringstidspunkt, LocalDate.now().plusMonths(5))
            .medUtfall(Utfall.OPPFYLT);

        vilkårBuilder.leggTil(førstePeriode)
            .leggTil(andrePeriode);

        var vilkår = vilkårBuilder.build();
        assertThat(vilkår).isNotNull();
        assertThat(vilkår.getPerioder()).hasSize(2);
        assertThat(vilkår.getPerioder().stream().map(VilkårPeriode::getPeriode).map(DatoIntervallEntitet::getFomDato)).containsExactly(førsteSkjæringstidspunkt, andreSkjæringstidspunkt);
        assertThat(vilkår.getPerioder().stream().map(VilkårPeriode::getPeriode).map(DatoIntervallEntitet::getTomDato)).containsExactly(andreSkjæringstidspunkt.minusDays(7),
            LocalDate.now().plusMonths(5));

        var oppdateringBuilder = new VilkårBuilder(vilkår).medKantIKantVurderer(new DefaultKantIKantVurderer());
        var oppdatertFørsteSkjæringstidspunkt = førsteSkjæringstidspunkt.minusDays(10);
        oppdateringBuilder.leggTil(oppdateringBuilder.hentBuilderFor(oppdatertFørsteSkjæringstidspunkt, sluttFørstePeriode)
            .medUtfall(Utfall.IKKE_VURDERT));

        var oppdatertVilkår = oppdateringBuilder.build();
        assertThat(oppdatertVilkår).isNotNull();
        assertThat(oppdatertVilkår.getPerioder()).hasSize(2);
        assertThat(oppdatertVilkår.getPerioder().stream().map(VilkårPeriode::getPeriode).map(DatoIntervallEntitet::getFomDato)).containsExactly(oppdatertFørsteSkjæringstidspunkt,
            andreSkjæringstidspunkt);
        assertThat(oppdatertVilkår.getPerioder().stream().map(VilkårPeriode::getPeriode).map(DatoIntervallEntitet::getTomDato)).containsExactly(andreSkjæringstidspunkt.minusDays(7),
            LocalDate.now().plusMonths(5));
    }

    @Test
    public void skal_utvide_godkjent_periode_ved_ny_dag_til_vurdering() {
        var vilkårBuilder = new VilkårBuilder(VilkårType.OPPTJENINGSVILKÅRET)
            .medKantIKantVurderer(new DefaultKantIKantVurderer())
            .medMaksMellomliggendePeriodeAvstand(7);

        var førsteSkjæringstidspunkt = LocalDate.now();
        var sluttFørstePeriode = LocalDate.now().plusMonths(3);
        var førstePeriode = vilkårBuilder.hentBuilderFor(førsteSkjæringstidspunkt, sluttFørstePeriode)
            .medUtfall(Utfall.OPPFYLT);
        var andreSkjæringstidspunkt = LocalDate.now().plusMonths(3).plusDays(7);
        var andrePeriode = vilkårBuilder.hentBuilderFor(andreSkjæringstidspunkt, LocalDate.now().plusMonths(5))
            .medUtfall(Utfall.OPPFYLT);

        vilkårBuilder.leggTil(førstePeriode)
            .leggTil(andrePeriode);

        var vilkår = vilkårBuilder.build();
        assertThat(vilkår).isNotNull();
        assertThat(vilkår.getPerioder()).hasSize(2);
        assertThat(vilkår.getPerioder().stream().map(VilkårPeriode::getPeriode).map(DatoIntervallEntitet::getFomDato)).containsExactly(førsteSkjæringstidspunkt, andreSkjæringstidspunkt);
        assertThat(vilkår.getPerioder().stream().map(VilkårPeriode::getPeriode).map(DatoIntervallEntitet::getTomDato)).containsExactly(andreSkjæringstidspunkt.minusDays(7),
            LocalDate.now().plusMonths(5));

        var oppdateringBuilder = new VilkårBuilder(vilkår).medKantIKantVurderer(new DefaultKantIKantVurderer());
        var nyPeriode = LocalDate.now().plusMonths(5).plusDays(1);
        oppdateringBuilder.leggTil(oppdateringBuilder.hentBuilderFor(nyPeriode, nyPeriode.plusDays(3))
            .medUtfall(Utfall.IKKE_VURDERT));

        var oppdatertVilkår = oppdateringBuilder.build();
        assertThat(oppdatertVilkår).isNotNull();
        assertThat(oppdatertVilkår.getPerioder()).hasSize(2);
    }

    @Test
    public void skal_utvide_godkjent_periode_ved_ny_dag_til_vurdering_selv_ved_overstyring() {
        var vilkårBuilder = new VilkårBuilder(VilkårType.OPPTJENINGSVILKÅRET)
            .medKantIKantVurderer(new DefaultKantIKantVurderer())
            .medMaksMellomliggendePeriodeAvstand(7);

        var førsteSkjæringstidspunkt = LocalDate.now();
        var sluttFørstePeriode = LocalDate.now().plusMonths(3);
        var førstePeriode = vilkårBuilder.hentBuilderFor(førsteSkjæringstidspunkt, sluttFørstePeriode)
            .medUtfall(Utfall.IKKE_OPPFYLT)
            .medUtfallOverstyrt(Utfall.OPPFYLT);
        var andreSkjæringstidspunkt = LocalDate.now().plusMonths(3).plusDays(7);
        var andrePeriode = vilkårBuilder.hentBuilderFor(andreSkjæringstidspunkt, LocalDate.now().plusMonths(5))
            .medUtfall(Utfall.OPPFYLT);

        vilkårBuilder.leggTil(førstePeriode)
            .leggTil(andrePeriode);

        var vilkår = vilkårBuilder.build();
        assertThat(vilkår).isNotNull();
        assertThat(vilkår.getPerioder()).hasSize(2);
        assertThat(vilkår.getPerioder().stream().map(VilkårPeriode::getPeriode).map(DatoIntervallEntitet::getFomDato)).containsExactly(førsteSkjæringstidspunkt, andreSkjæringstidspunkt);
        assertThat(vilkår.getPerioder().stream().map(VilkårPeriode::getPeriode).map(DatoIntervallEntitet::getTomDato)).containsExactly(andreSkjæringstidspunkt.minusDays(7),
            LocalDate.now().plusMonths(5));

        var oppdateringBuilder = new VilkårBuilder(vilkår).medKantIKantVurderer(new DefaultKantIKantVurderer());
        var nyPeriode = LocalDate.now().plusMonths(5).plusDays(1);
        oppdateringBuilder.leggTil(oppdateringBuilder.hentBuilderFor(nyPeriode, nyPeriode.plusDays(3))
            .medUtfall(Utfall.IKKE_VURDERT));

        var oppdatertVilkår = oppdateringBuilder.build();
        assertThat(oppdatertVilkår).isNotNull();
        assertThat(oppdatertVilkår.getPerioder()).hasSize(2);
    }

    @Test
    public void skal_nullstille_ved_nulltimer() {
        var vilkårBuilder = new VilkårBuilder(VilkårType.OPPTJENINGSVILKÅRET)
            .medKantIKantVurderer(new DefaultKantIKantVurderer())
            .medMaksMellomliggendePeriodeAvstand(7);

        var førsteSkjæringstidspunkt = LocalDate.now();
        var sluttFørstePeriode = LocalDate.now().plusMonths(3);
        var førstePeriode = vilkårBuilder.hentBuilderFor(førsteSkjæringstidspunkt, sluttFørstePeriode)
            .medUtfall(Utfall.IKKE_OPPFYLT)
            .medUtfallOverstyrt(Utfall.OPPFYLT);
        var andreSkjæringstidspunkt = LocalDate.now().plusMonths(3).plusDays(7);
        var andrePeriode = vilkårBuilder.hentBuilderFor(andreSkjæringstidspunkt, LocalDate.now().plusMonths(5))
            .medUtfall(Utfall.OPPFYLT);

        vilkårBuilder.leggTil(førstePeriode)
            .leggTil(andrePeriode);

        var vilkår = vilkårBuilder.build();
        assertThat(vilkår).isNotNull();
        assertThat(vilkår.getPerioder()).hasSize(2);
        assertThat(vilkår.getPerioder().stream().map(VilkårPeriode::getPeriode).map(DatoIntervallEntitet::getFomDato)).containsExactly(førsteSkjæringstidspunkt, andreSkjæringstidspunkt);
        assertThat(vilkår.getPerioder().stream().map(VilkårPeriode::getPeriode).map(DatoIntervallEntitet::getTomDato)).containsExactly(andreSkjæringstidspunkt.minusDays(7),
            LocalDate.now().plusMonths(5));

        var oppdateringBuilder = new VilkårBuilder(vilkår).medKantIKantVurderer(new DefaultKantIKantVurderer());
        oppdateringBuilder.tilbakestill(DatoIntervallEntitet.fraOgMedTilOgMed(andreSkjæringstidspunkt.plusDays(7), andreSkjæringstidspunkt.plusDays(10)));

        var oppdatertVilkår = oppdateringBuilder.build();
        assertThat(oppdatertVilkår).isNotNull();
        assertThat(oppdatertVilkår.getPerioder()).hasSize(3);
        assertThat(oppdatertVilkår.getPerioder().stream().filter(it -> it.getUtfall().equals(Utfall.IKKE_VURDERT)).collect(Collectors.toList())).hasSize(2);
    }

    @Test
    public void name() {
        var zero = Duration.ZERO;
        var string = Duration.parse("PT0S");

        assertThat(zero.equals(string)).isTrue();
    }
}
