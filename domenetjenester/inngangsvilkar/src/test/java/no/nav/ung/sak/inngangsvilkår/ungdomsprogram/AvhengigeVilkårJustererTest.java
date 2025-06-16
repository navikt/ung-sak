package no.nav.ung.sak.inngangsvilkår.ungdomsprogram;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.ung.kodeverk.vilkår.Utfall;
import no.nav.ung.kodeverk.vilkår.VilkårType;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.*;
import no.nav.ung.sak.db.util.JpaExtension;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.test.util.behandling.TestScenarioBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.LocalDate;
import java.util.Set;
import java.util.TreeSet;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@ExtendWith(JpaExtension.class)
@ExtendWith(CdiAwareExtension.class)
class AvhengigeVilkårJustererTest {

    @Inject
    private EntityManager em;

    @Inject
    private VilkårResultatRepository vilkårResultatRepository;

    private AvhengigeVilkårJusterer avhengigeVilkårJusterer;
    private Behandling behandling;

    @BeforeEach
    void setUp() {

        var scenarioBuilder = TestScenarioBuilder.builderMedSøknad();
        behandling = scenarioBuilder.lagre(em);


        avhengigeVilkårJusterer = new AvhengigeVilkårJusterer(vilkårResultatRepository);

    }

    @Test
    void skal_ikke_justere_vilkår_dersom_ingen_perioder_avslått() {

        var periode = DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now(), LocalDate.now().plusDays(10));
        var resultatBuilder = new VilkårResultatBuilder();

        var ungdomsprogramVilkårBuilder = resultatBuilder.hentBuilderFor(VilkårType.UNGDOMSPROGRAMVILKÅRET);
        lagPeriodeMedUtfall(ungdomsprogramVilkårBuilder, periode, Utfall.OPPFYLT);

        var alderVilkårBuilder = resultatBuilder.hentBuilderFor(VilkårType.ALDERSVILKÅR);
        lagPeriodeMedUtfall(alderVilkårBuilder, periode, Utfall.IKKE_VURDERT);

        resultatBuilder.leggTil(alderVilkårBuilder);
        resultatBuilder.leggTil(ungdomsprogramVilkårBuilder);

        vilkårResultatRepository.lagre(behandling.getId(), resultatBuilder.build());

        avhengigeVilkårJusterer.fjernAvslåttePerioderForAvhengigeVilkår(behandling.getId(), new TreeSet<>(Set.of(periode)), Set.of(VilkårType.ALDERSVILKÅR), VilkårType.UNGDOMSPROGRAMVILKÅRET);


        var nyttResultat = vilkårResultatRepository.hent(behandling.getId());
        var avhengigVilkårResultat = nyttResultat.getVilkår(VilkårType.ALDERSVILKÅR).orElseThrow();
        var perioderAvhengigVilkår = avhengigVilkårResultat.getPerioder();
        assertThat(perioderAvhengigVilkår.size()).isEqualTo(1);
        assertThat(perioderAvhengigVilkår.get(0).getUtfall()).isEqualTo(Utfall.IKKE_VURDERT);
        assertThat(perioderAvhengigVilkår.get(0).getFom()).isEqualTo(periode.getFomDato());
        assertThat(perioderAvhengigVilkår.get(0).getTom()).isEqualTo(periode.getTomDato());
    }

    @Test
    void skal_ikke_justere_vilkår_dersom_ingen_perioder_til_vurdering() {

        var periode = DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now(), LocalDate.now().plusDays(10));
        var resultatBuilder = new VilkårResultatBuilder();

        var ungdomsprogramVilkårBuilder = resultatBuilder.hentBuilderFor(VilkårType.UNGDOMSPROGRAMVILKÅRET);
        lagPeriodeMedUtfall(ungdomsprogramVilkårBuilder, periode, Utfall.OPPFYLT);

        var alderVilkårBuilder = resultatBuilder.hentBuilderFor(VilkårType.ALDERSVILKÅR);
        lagPeriodeMedUtfall(alderVilkårBuilder, periode, Utfall.IKKE_VURDERT);

        resultatBuilder.leggTil(alderVilkårBuilder);
        resultatBuilder.leggTil(ungdomsprogramVilkårBuilder);

        vilkårResultatRepository.lagre(behandling.getId(), resultatBuilder.build());

        avhengigeVilkårJusterer.fjernAvslåttePerioderForAvhengigeVilkår(behandling.getId(), new TreeSet<>(Set.of()), Set.of(VilkårType.ALDERSVILKÅR), VilkårType.UNGDOMSPROGRAMVILKÅRET);


        var nyttResultat = vilkårResultatRepository.hent(behandling.getId());
        var avhengigVilkårResultat = nyttResultat.getVilkår(VilkårType.ALDERSVILKÅR).orElseThrow();
        var perioderAvhengigVilkår = avhengigVilkårResultat.getPerioder();
        assertThat(perioderAvhengigVilkår.size()).isEqualTo(1);
        assertThat(perioderAvhengigVilkår.get(0).getUtfall()).isEqualTo(Utfall.IKKE_VURDERT);
        assertThat(perioderAvhengigVilkår.get(0).getFom()).isEqualTo(periode.getFomDato());
        assertThat(perioderAvhengigVilkår.get(0).getTom()).isEqualTo(periode.getTomDato());
    }


    @Test
    void skal_fjerne_hele_vilkårsperiode_dersom_hele_perioden_er_avslått() {

        var periode = DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now(), LocalDate.now().plusDays(10));
        var resultatBuilder = new VilkårResultatBuilder();

        var ungdomsprogramVilkårBuilder = resultatBuilder.hentBuilderFor(VilkårType.UNGDOMSPROGRAMVILKÅRET);
        lagPeriodeMedUtfall(ungdomsprogramVilkårBuilder, periode, Utfall.IKKE_OPPFYLT);

        var alderVilkårBuilder = resultatBuilder.hentBuilderFor(VilkårType.ALDERSVILKÅR);
        lagPeriodeMedUtfall(alderVilkårBuilder, periode, Utfall.IKKE_VURDERT);

        resultatBuilder.leggTil(alderVilkårBuilder);
        resultatBuilder.leggTil(ungdomsprogramVilkårBuilder);

        vilkårResultatRepository.lagre(behandling.getId(), resultatBuilder.build());

        avhengigeVilkårJusterer.fjernAvslåttePerioderForAvhengigeVilkår(behandling.getId(), new TreeSet<>(Set.of(periode)), Set.of(VilkårType.ALDERSVILKÅR), VilkårType.UNGDOMSPROGRAMVILKÅRET);


        var nyttResultat = vilkårResultatRepository.hent(behandling.getId());
        var avhengigVilkårResultat = nyttResultat.getVilkår(VilkårType.ALDERSVILKÅR).orElseThrow();
        var perioderAvhengigVilkår = avhengigVilkårResultat.getPerioder();
        assertThat(perioderAvhengigVilkår.size()).isEqualTo(0);
    }

    @Test
    void skal_fjerne_deler_av_vilkårsperiode_dersom_deler_av_perioden_er_avslått() {

        var periode = DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now(), LocalDate.now().plusDays(10));
        var resultatBuilder = new VilkårResultatBuilder();

        var ungdomsprogramVilkårBuilder = resultatBuilder.hentBuilderFor(VilkårType.UNGDOMSPROGRAMVILKÅRET);
        var avslåttPeriode = DatoIntervallEntitet.fraOgMedTilOgMed(periode.getFomDato(), periode.getFomDato().plusDays(2));
        lagPeriodeMedUtfall(ungdomsprogramVilkårBuilder, avslåttPeriode, Utfall.IKKE_OPPFYLT);
        var innvilgetPeriode = DatoIntervallEntitet.fraOgMedTilOgMed(periode.getFomDato().plusDays(3), periode.getTomDato());
        lagPeriodeMedUtfall(ungdomsprogramVilkårBuilder, innvilgetPeriode, Utfall.OPPFYLT);

        var alderVilkårBuilder = resultatBuilder.hentBuilderFor(VilkårType.ALDERSVILKÅR);
        lagPeriodeMedUtfall(alderVilkårBuilder, periode, Utfall.IKKE_VURDERT);

        resultatBuilder.leggTil(alderVilkårBuilder);
        resultatBuilder.leggTil(ungdomsprogramVilkårBuilder);

        vilkårResultatRepository.lagre(behandling.getId(), resultatBuilder.build());

        avhengigeVilkårJusterer.fjernAvslåttePerioderForAvhengigeVilkår(behandling.getId(), new TreeSet<>(Set.of(periode)), Set.of(VilkårType.ALDERSVILKÅR), VilkårType.UNGDOMSPROGRAMVILKÅRET);


        var nyttResultat = vilkårResultatRepository.hent(behandling.getId());
        var avhengigVilkårResultat = nyttResultat.getVilkår(VilkårType.ALDERSVILKÅR).orElseThrow();
        var perioderAvhengigVilkår = avhengigVilkårResultat.getPerioder();
        assertThat(perioderAvhengigVilkår.size()).isEqualTo(1);
        assertThat(perioderAvhengigVilkår.get(0).getUtfall()).isEqualTo(Utfall.IKKE_VURDERT);
        assertThat(perioderAvhengigVilkår.get(0).getFom()).isEqualTo(innvilgetPeriode.getFomDato());
        assertThat(perioderAvhengigVilkår.get(0).getTom()).isEqualTo(innvilgetPeriode.getTomDato());
    }

    private static void lagPeriodeMedUtfall(VilkårBuilder ungdomsprogramVilkårBuilder, DatoIntervallEntitet periode, Utfall utfall) {
        var periodeBuilder = ungdomsprogramVilkårBuilder.hentBuilderFor(periode);
        periodeBuilder.medUtfall(utfall);
        ungdomsprogramVilkårBuilder.leggTil(periodeBuilder);
    }
}
