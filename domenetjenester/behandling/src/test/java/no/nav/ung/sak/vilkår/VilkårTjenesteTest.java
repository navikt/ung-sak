package no.nav.ung.sak.vilkår;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.k9.kodeverk.behandling.BehandlingStatus;
import no.nav.k9.kodeverk.behandling.BehandlingType;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.vilkår.Avslagsårsak;
import no.nav.k9.kodeverk.vilkår.Utfall;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.ung.sak.behandling.BehandlingReferanse;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.VilkårBuilder;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.Vilkårene;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriodeBuilder;
import no.nav.ung.sak.behandlingslager.fagsak.Fagsak;
import no.nav.ung.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.ung.sak.db.util.JpaExtension;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.typer.AktørId;
import no.nav.ung.sak.typer.Saksnummer;

@ExtendWith(CdiAwareExtension.class)
@ExtendWith(JpaExtension.class)
class VilkårTjenesteTest {


    @Inject
    private EntityManager entityManager;
    private FagsakRepository fagsakRepository;
    private BehandlingRepository behandlingRepository;
    private VilkårResultatRepository vilkårResultatRepository;
    private VilkårTjeneste vilkårTjeneste;
    private Behandling behandling;
    private Behandling revurdering;
    private Fagsak fagsak;


    @BeforeEach
    void setUp() {
        fagsakRepository = new FagsakRepository(entityManager);
        behandlingRepository = new BehandlingRepository(entityManager);
        vilkårResultatRepository = new VilkårResultatRepository(entityManager);
        vilkårTjeneste = new VilkårTjeneste(behandlingRepository, null, vilkårResultatRepository);
        fagsak = Fagsak.opprettNy(FagsakYtelseType.PLEIEPENGER_SYKT_BARN, new AktørId(123L), new Saksnummer("987"), LocalDate.now(), LocalDate.now());
        fagsakRepository.opprettNy(fagsak);
        behandling = Behandling.forFørstegangssøknad(fagsak).medBehandlingStatus(BehandlingStatus.UTREDES).build();
        behandlingRepository.lagre(behandling, behandlingRepository.taSkriveLås(behandling));
        revurdering = Behandling.fraTidligereBehandling(behandling, BehandlingType.REVURDERING).build();
        behandlingRepository.lagre(revurdering, behandlingRepository.taSkriveLås(revurdering));
    }


    @Test
    void skal_kopiere_resultat_fra_forrige_behandling() {
        // Arrange
        var vilkårBuilder = new VilkårBuilder(VilkårType.BEREGNINGSGRUNNLAGVILKÅR);
        var fom = LocalDate.now();
        var tom = LocalDate.now();

        vilkårBuilder.leggTil(lagVilkårperiode(vilkårBuilder, fom, tom, Utfall.OPPFYLT,
            "En regelinput", "En regelevaluering"));
        var vilkårResultatBuilder = Vilkårene.builder().leggTil(vilkårBuilder);
        vilkårResultatRepository.lagre(behandling.getId(), vilkårResultatBuilder.build());

        var vilkårBuilder2 = new VilkårBuilder(VilkårType.BEREGNINGSGRUNNLAGVILKÅR);
        vilkårBuilder2.leggTil(lagVilkårperiode(vilkårBuilder2, fom, tom, Utfall.IKKE_VURDERT,
            null,
            null));
        var vilkårResultatBuilder2 = Vilkårene.builder().leggTil(vilkårBuilder2);
        vilkårResultatRepository.lagre(revurdering.getId(), vilkårResultatBuilder2.build());

        // Act
        var resultat = vilkårTjeneste.gjenopprettVilkårsutfallForPerioderSomIkkeVurderes(BehandlingReferanse.fra(revurdering), VilkårType.BEREGNINGSGRUNNLAGVILKÅR, List.of(DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now().plusDays(2), LocalDate.now().plusDays(2))), false);

        // Assert
        assertThat(resultat.gjenopprettetPerioder().size()).isEqualTo(1);
        var gjenopprettetPeriode = resultat.gjenopprettetPerioder().iterator().next();
        assertThat(gjenopprettetPeriode.getFomDato()).isEqualTo(fom);
        assertThat(gjenopprettetPeriode.getTomDato()).isEqualTo(tom);

        var nyttVilkårResultat = vilkårResultatRepository.hentHvisEksisterer(revurdering.getId());
        var nyPeriode = nyttVilkårResultat.get().getVilkår(VilkårType.BEREGNINGSGRUNNLAGVILKÅR)
            .get()
            .finnPeriodeForSkjæringstidspunkt(fom);
        assertThat(nyPeriode.getUtfall()).isEqualTo(Utfall.OPPFYLT);
        assertThat(nyPeriode.getRegelInput()).isEqualTo("En regelinput");
        assertThat(nyPeriode.getRegelEvaluering()).isEqualTo("En regelevaluering");
    }

    @Test
    void skal_ikke_kopiere_resultat_fra_forrige_behandling_dersom_alle_perioder_er_til_vurdering() {
        // Arrange
        var vilkårBuilder = new VilkårBuilder(VilkårType.BEREGNINGSGRUNNLAGVILKÅR);
        var fom = LocalDate.now();
        var tom = LocalDate.now();

        vilkårBuilder.leggTil(lagVilkårperiode(vilkårBuilder, fom, tom, Utfall.OPPFYLT,
            "En regelinput", "En regelevaluering"));
        var vilkårResultatBuilder = Vilkårene.builder().leggTil(vilkårBuilder);
        vilkårResultatRepository.lagre(behandling.getId(), vilkårResultatBuilder.build());

        var vilkårBuilder2 = new VilkårBuilder(VilkårType.BEREGNINGSGRUNNLAGVILKÅR);
        vilkårBuilder2.leggTil(lagVilkårperiode(vilkårBuilder2, fom, tom, Utfall.IKKE_VURDERT,
            null,
            null));
        var vilkårResultatBuilder2 = Vilkårene.builder().leggTil(vilkårBuilder2);
        vilkårResultatRepository.lagre(revurdering.getId(), vilkårResultatBuilder2.build());

        // Act
        var resultat = vilkårTjeneste.gjenopprettVilkårsutfallForPerioderSomIkkeVurderes(
            BehandlingReferanse.fra(revurdering),
            VilkårType.BEREGNINGSGRUNNLAGVILKÅR,
            List.of(DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom)), false);

        // Assert
        assertThat(resultat.gjenopprettetPerioder().size()).isEqualTo(0);
        var nyttVilkårResultat = vilkårResultatRepository.hentHvisEksisterer(revurdering.getId());
        var nyPeriode = nyttVilkårResultat.get().getVilkår(VilkårType.BEREGNINGSGRUNNLAGVILKÅR)
            .get()
            .finnPeriodeForSkjæringstidspunkt(fom);
        assertThat(nyPeriode.getUtfall()).isEqualTo(Utfall.IKKE_VURDERT);
        assertThat(nyPeriode.getRegelInput()).isNull();
        assertThat(nyPeriode.getRegelEvaluering()).isNull();
    }

    @Test
    void skal_klippe_bort_periode_dersom_den_ikke_finnes_i_original() {
        // Arrange
        var vilkårBuilder = new VilkårBuilder(VilkårType.BEREGNINGSGRUNNLAGVILKÅR);
        var fom = LocalDate.now();
        var tom = LocalDate.now();

        vilkårBuilder.leggTil(lagVilkårperiode(vilkårBuilder, fom.minusDays(10), tom.minusDays(9), Utfall.OPPFYLT,
            "En regelinput", "En regelevaluering"));
        var vilkårResultatBuilder = Vilkårene.builder().leggTil(vilkårBuilder);
        vilkårResultatRepository.lagre(behandling.getId(), vilkårResultatBuilder.build());

        var vilkårBuilder2 = new VilkårBuilder(VilkårType.BEREGNINGSGRUNNLAGVILKÅR);
        vilkårBuilder2.leggTil(lagVilkårperiode(vilkårBuilder2, fom, tom, Utfall.IKKE_VURDERT,
            null,
            null));
        vilkårBuilder2.leggTil(lagVilkårperiode(vilkårBuilder2, fom.minusDays(10)
            , tom.minusDays(9), Utfall.IKKE_VURDERT,
            null,
            null));
        var vilkårResultatBuilder2 = Vilkårene.builder().leggTil(vilkårBuilder2);
        vilkårResultatRepository.lagre(revurdering.getId(), vilkårResultatBuilder2.build());

        // Act
        var resultat = vilkårTjeneste.gjenopprettVilkårsutfallForPerioderSomIkkeVurderes(
            BehandlingReferanse.fra(revurdering),
            VilkårType.BEREGNINGSGRUNNLAGVILKÅR,
            List.of(DatoIntervallEntitet.fraOgMedTilOgMed(fom.minusDays(10), tom.minusDays(9))), true);

        // Assert
        assertThat(resultat.gjenopprettetPerioder().size()).isEqualTo(0);
        assertThat(resultat.fjernetPerioder().size()).isEqualTo(1);
        var nyttVilkårResultat = vilkårResultatRepository.hentHvisEksisterer(revurdering.getId());
        var nyPeriode = nyttVilkårResultat.get().getVilkår(VilkårType.BEREGNINGSGRUNNLAGVILKÅR)
            .get()
            .finnPeriodeForSkjæringstidspunktHvisFinnes(fom);
        assertThat(nyPeriode.isPresent()).isFalse();
    }

    @Test
    void skal_ikke_kopiere_resultat_fra_forrige_behandling_dersom_perioden_er_inkludert_i_periode_til_vurdering() {
        // Arrange
        var vilkårBuilder = new VilkårBuilder(VilkårType.BEREGNINGSGRUNNLAGVILKÅR);
        var fom = LocalDate.now();
        var tom = LocalDate.now().plusDays(10);

        vilkårBuilder.leggTil(lagVilkårperiode(vilkårBuilder, fom, tom, Utfall.OPPFYLT,
            "En regelinput", "En regelevaluering"));
        var vilkårResultatBuilder = Vilkårene.builder().leggTil(vilkårBuilder);
        vilkårResultatRepository.lagre(behandling.getId(), vilkårResultatBuilder.build());

        var vilkårBuilder2 = new VilkårBuilder(VilkårType.BEREGNINGSGRUNNLAGVILKÅR);
        vilkårBuilder2.leggTil(lagVilkårperiode(vilkårBuilder2, fom.plusDays(5), tom, Utfall.IKKE_VURDERT,
            null,
            null));
        var vilkårResultatBuilder2 = Vilkårene.builder().leggTil(vilkårBuilder2);
        vilkårResultatRepository.lagre(revurdering.getId(), vilkårResultatBuilder2.build());

        // Act
        var resultat = vilkårTjeneste.gjenopprettVilkårsutfallForPerioderSomIkkeVurderes(
            BehandlingReferanse.fra(revurdering),
            VilkårType.BEREGNINGSGRUNNLAGVILKÅR,
            List.of(DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom)), true);

        // Assert
        assertThat(resultat.gjenopprettetPerioder().size()).isEqualTo(0);
        assertThat(resultat.fjernetPerioder().size()).isEqualTo(0);
        var nyttVilkårResultat = vilkårResultatRepository.hentHvisEksisterer(revurdering.getId());
        var nyPeriode = nyttVilkårResultat.get().getVilkår(VilkårType.BEREGNINGSGRUNNLAGVILKÅR)
            .get()
            .finnPeriodeForSkjæringstidspunkt(fom.plusDays(5));
        assertThat(nyPeriode.getUtfall()).isEqualTo(Utfall.IKKE_VURDERT);
        assertThat(nyPeriode.getRegelInput()).isNull();
        assertThat(nyPeriode.getRegelEvaluering()).isNull();
    }



    private static VilkårPeriodeBuilder lagVilkårperiode(VilkårBuilder vilkårBuilder, LocalDate fom, LocalDate tom, Utfall utfall, String regelinput, String regelevaluering) {
        var periodeBuilder = vilkårBuilder.hentBuilderFor(fom, tom);
        periodeBuilder.medUtfall(utfall);
        periodeBuilder.medAvslagsårsak(Avslagsårsak.UDEFINERT);
        periodeBuilder.medRegelInput(regelinput);
        periodeBuilder.medRegelEvaluering(regelevaluering);
        return periodeBuilder;
    }
}
