package no.nav.ung.sak.test.util.behandling;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.k9.felles.konfigurasjon.konfig.Tid;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.ung.kodeverk.behandling.BehandlingType;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.kodeverk.vilkår.Avslagsårsak;
import no.nav.ung.kodeverk.vilkår.Utfall;
import no.nav.ung.kodeverk.vilkår.VilkårType;
import no.nav.ung.kodeverk.vilkår.VilkårUtfallMerknad;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.Vilkår;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.VilkårResultatBuilder;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.Vilkårene;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriodeBuilder;
import no.nav.ung.sak.behandlingslager.fagsak.Fagsak;
import no.nav.ung.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.ung.sak.db.util.JpaExtension;
import no.nav.ung.sak.db.util.Repository;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.test.util.fagsak.FagsakBuilder;

@ExtendWith(CdiAwareExtension.class)
@ExtendWith(JpaExtension.class)
public class VilkårResultatTest {

    @Inject
    private EntityManager entityManager;

    private FagsakRepository fagsakReposiory;
    private Repository repository;
    private BehandlingRepositoryProvider repositoryProvider;
    private BehandlingRepository behandlingRepository;
    private VilkårResultatRepository vilkårResultatRepository;

    private Fagsak fagsak = FagsakBuilder.nyFagsak(FagsakYtelseType.OMSORGSPENGER).build();
    private Behandling.Builder behandlingBuilder = Behandling.forFørstegangssøknad(fagsak);
    private Behandling behandling1;

    @BeforeEach
    public void setup() {

        fagsakReposiory = new FagsakRepository(entityManager);
        repository = new Repository(entityManager);
        repositoryProvider = new BehandlingRepositoryProvider(entityManager);
        behandlingRepository = repositoryProvider.getBehandlingRepository();
        vilkårResultatRepository = repositoryProvider.getVilkårResultatRepository();

        fagsakReposiory.opprettNy(fagsak);
        behandling1 = behandlingBuilder.build();
    }

    @Test
    public void skal_gjenbruke_vilkårresultat_i_ny_behandling_når_det_ikke_er_endret() throws Exception {
        // Arrange
        lagreBehandling(behandling1);

        lagreOgGjenopphenteBehandlingsresultat(behandling1);

        // Act
        Behandling behandling2 = Behandling.fraTidligereBehandling(behandling1, BehandlingType.REVURDERING)
            .build();
        lagreBehandling(behandling2);
        vilkårResultatRepository.kopier(behandling2.getId(), behandling2.getId());

        lagreOgGjenopphenteBehandlingsresultat(behandling2);

        // Assert
        assertThat(getVilkårene(behandling2))
            .isNotSameAs(getVilkårene(behandling1));
        assertThat(getVilkårene(behandling2))
            .isEqualTo(getVilkårene(behandling1));

    }

    @Test
    public void skal_opprette_nytt_vilkårresultat_i_ny_behandling_når_det_endrer_vilkårresultat() throws Exception {
        // Arrange
        lagreBehandling(behandling1);
        lagreOgGjenopphenteBehandlingsresultat(behandling1);

        // Act
        Behandling behandling2 = Behandling.fraTidligereBehandling(behandling1, BehandlingType.REVURDERING).build();
        lagreBehandling(behandling2);
        vilkårResultatRepository.kopier(behandling1.getId(), behandling2.getId());

        // legg til et nytt vilkårsresultat
        final var vilkårResultatBuilder = Vilkårene.builderFraEksisterende(getVilkårene(behandling2));
        final var vilkårResultat = vilkårResultatBuilder.leggTil(vilkårResultatBuilder.hentBuilderFor(VilkårType.MEDLEMSKAPSVILKÅRET)
                .leggTil(new VilkårPeriodeBuilder()
                    .medUtfall(Utfall.OPPFYLT)
                    .medMerknad(VilkårUtfallMerknad.VM_1001)
                    .medPeriode(LocalDate.now(), Tid.TIDENES_ENDE)))
            .build();
        vilkårResultatRepository.lagre(behandling2.getId(), vilkårResultat);

        lagreOgGjenopphenteBehandlingsresultat(behandling2);

        // Assert
        assertThat(getVilkårene(behandling2).getId())
            .isNotEqualTo(getVilkårene(behandling1).getId());

    }

    @Test
    public void skal_lagre_og_hente_vilkår_med_avslagsårsak() {
        // Arrange
        lagreBehandling(behandling1);
        VilkårResultatBuilder vilkårResultatBuilder = Vilkårene.builder();
        vilkårResultatBuilder.leggTil(vilkårResultatBuilder.hentBuilderFor(VilkårType.OPPTJENINGSVILKÅRET)
            .leggTil(new VilkårPeriodeBuilder()
                .medUtfall(Utfall.IKKE_OPPFYLT)
                .medAvslagsårsak(Avslagsårsak.IKKE_TILSTREKKELIG_OPPTJENING)
                .medPeriode(LocalDate.now(), Tid.TIDENES_ENDE)));

        // Act
        BehandlingLås lås = behandlingRepository.taSkriveLås(behandling1);
        vilkårResultatRepository.lagre(behandling1.getId(), vilkårResultatBuilder.build());
        behandlingRepository.lagre(behandling1, lås);
        lagreBehandling(behandling1);
        Behandling lagretBehandling = repository.hent(Behandling.class, behandling1.getId());

        // Assert
        assertThat(lagretBehandling).isEqualTo(behandling1);
        final var vilkårene = vilkårResultatRepository.hent(behandling1.getId());
        assertThat(vilkårene.getVilkårene()).hasSize(1);
        Vilkår vilkår = vilkårene.getVilkårene().get(0);
        final var periode = vilkår.getPerioder().get(0);
        assertThat(periode.getAvslagsårsak()).isNotNull();
        assertThat(periode.getAvslagsårsak()).isEqualTo(Avslagsårsak.IKKE_TILSTREKKELIG_OPPTJENING);
    }

    private Vilkårene getVilkårene(Behandling behandling) {
        return repositoryProvider.getVilkårResultatRepository().hent(behandling.getId());
    }

    @Test
    public void skal_hente_vilkårresultatperioder_optimalisert_uten_regelsporing() throws Exception {
        // Arrange
        lagreBehandling(behandling1);
        Long behandlingId = behandling1.getId();

        VilkårResultatBuilder vilkårResultatBuilder = Vilkårene.builder();
        vilkårResultatBuilder.leggTil(vilkårResultatBuilder.hentBuilderFor(VilkårType.OPPTJENINGSVILKÅRET)
            .leggTil(new VilkårPeriodeBuilder()
                .medUtfall(Utfall.IKKE_OPPFYLT)
                .medAvslagsårsak(Avslagsårsak.IKKE_TILSTREKKELIG_OPPTJENING)
                .medPeriode(LocalDate.now(), Tid.TIDENES_ENDE)));

        // Act
        vilkårResultatRepository.lagre(behandlingId, vilkårResultatBuilder.build());

        var resultater = vilkårResultatRepository.hentVilkårResultater(behandlingId);

        assertThat(resultater).hasSize(1);
    }

    @Test
    public void skal_legge_til_vilkår() throws Exception {
        // Arrange
        VilkårResultatBuilder vilkårResultatBuilder = Vilkårene.builder();
        vilkårResultatBuilder.leggTil(vilkårResultatBuilder.hentBuilderFor(VilkårType.MEDLEMSKAPSVILKÅRET)
            .leggTil(new VilkårPeriodeBuilder()
                .medUtfall(Utfall.IKKE_VURDERT)
                .medPeriode(LocalDate.now(), Tid.TIDENES_ENDE)));

        final var vilkårResultat = vilkårResultatBuilder.build();

        // Act
        final var oppdatertResultatBuilder = Vilkårene.builderFraEksisterende(vilkårResultat);
        final var oppdatertVilkårResultat = oppdatertResultatBuilder.leggTil(oppdatertResultatBuilder.hentBuilderFor(VilkårType.OPPTJENINGSVILKÅRET)
                .leggTil(new VilkårPeriodeBuilder()
                    .medUtfall(Utfall.IKKE_VURDERT)
                    .medPeriode(LocalDate.now(), Tid.TIDENES_ENDE)))
            .build();

        // Assert
        assertThat(oppdatertVilkårResultat.getVilkårene()).hasSize(2);

        Vilkår vilkår1 = oppdatertVilkårResultat.getVilkårene().stream().filter(v -> VilkårType.MEDLEMSKAPSVILKÅRET.equals(v.getVilkårType())).findFirst().orElse(null);
        assertThat(vilkår1).isNotNull();
        assertThat(vilkår1.getPerioder()).hasSize(1);
        final var vilkårPeriode = vilkår1.getPerioder().get(0);
        assertThat(vilkårPeriode.getGjeldendeUtfall()).isEqualTo(Utfall.IKKE_VURDERT);

        Vilkår vilkår2 = oppdatertVilkårResultat.getVilkårene().stream().filter(v -> VilkårType.OPPTJENINGSVILKÅRET.equals(v.getVilkårType())).findFirst().orElse(null);
        assertThat(vilkår2).isNotNull();
        assertThat(vilkår2.getPerioder()).hasSize(1);
        final var vilkårPeriode1 = vilkår1.getPerioder().get(0);
        assertThat(vilkårPeriode1.getGjeldendeUtfall()).isEqualTo(Utfall.IKKE_VURDERT);
    }

    @Test
    void skal_slette_vilkårperiode() throws Exception {
        VilkårType vilkårType = VilkårType.MEDLEMSKAPSVILKÅRET;

        // Arrange
        VilkårResultatBuilder vilkårResultatBuilder = Vilkårene.builder();
        LocalDate t1 = LocalDate.now();
        LocalDate tend = Tid.TIDENES_ENDE;
        vilkårResultatBuilder.leggTil(vilkårResultatBuilder.hentBuilderFor(vilkårType)
            .leggTil(new VilkårPeriodeBuilder()
                .medUtfall(Utfall.IKKE_VURDERT)
                .medPeriode(t1, tend)));

        var vilkårResultat = vilkårResultatBuilder.build();

        var nyVrb = Vilkårene.builderFraEksisterende(vilkårResultat);

        LocalDate t2 = t1.plusDays(10);
        nyVrb.slettPerioder(vilkårType, Set.of(DatoIntervallEntitet.fraOgMedTilOgMed(t2, tend)));

        var nyvilkår = nyVrb.build();
        var nytimeline = nyvilkår.getVilkårTimeline(vilkårType);

        assertThat(nytimeline.getLocalDateIntervals()).hasSize(1);
        assertThat(nytimeline.getLocalDateIntervals()).containsOnly(new LocalDateInterval(t1, t2.minusDays(1)));

    }

    @Test
    public void skal_oppdatere_vilkår_med_nytt_utfall() {
        // Arrange
        VilkårResultatBuilder vilkårResultatBuilder = Vilkårene.builder();
        final var opprinneligVilkårResultat = vilkårResultatBuilder.leggTil(vilkårResultatBuilder.hentBuilderFor(VilkårType.OPPTJENINGSVILKÅRET)
                .leggTil(new VilkårPeriodeBuilder()
                    .medUtfall(Utfall.IKKE_OPPFYLT)
                    .medPeriode(LocalDate.now(), Tid.TIDENES_ENDE)))
            .build();

        // Act
        VilkårResultatBuilder oppdatertVilkårResultatBuilder = Vilkårene.builderFraEksisterende(opprinneligVilkårResultat);
        final var vilkårBuilder = oppdatertVilkårResultatBuilder.hentBuilderFor(VilkårType.OPPTJENINGSVILKÅRET);
        final var oppdatertVilkårResultat = oppdatertVilkårResultatBuilder.leggTil(vilkårBuilder
                .leggTil(vilkårBuilder.hentBuilderFor(LocalDate.now(), Tid.TIDENES_ENDE)
                    .medUtfall(Utfall.OPPFYLT)))
            .build();

        // Assert
        assertThat(oppdatertVilkårResultat.getVilkårene()).hasSize(1);
        Vilkår vilkår = oppdatertVilkårResultat.getVilkårene().get(0);
        assertThat(vilkår.getVilkårType()).isEqualTo(VilkårType.OPPTJENINGSVILKÅRET);
        assertThat(vilkår.getPerioder()).hasSize(1);
        final var vilkårPeriode = vilkår.getPerioder().get(0);
        assertThat(vilkårPeriode.getAvslagsårsak()).isNull();
        assertThat(vilkårPeriode.getGjeldendeUtfall()).isEqualTo(Utfall.OPPFYLT);
    }

    @Test
    public void skal_overstyre_vilkår() throws Exception {
        // Arrange
        VilkårResultatBuilder vilkårResultatBuilder = Vilkårene.builder();
        final var vilkårBuilder2 = vilkårResultatBuilder.hentBuilderFor(VilkårType.MEDLEMSKAPSVILKÅRET);
        final var opprinneligVilkårResultat = vilkårResultatBuilder.leggTil(vilkårBuilder2
                .leggTil(vilkårBuilder2.hentBuilderFor(LocalDate.now(), Tid.TIDENES_ENDE)
                    .medUtfall(Utfall.OPPFYLT)))
            .build();

        // Act 1: Ikke oppfylt (overstyrt)
        VilkårResultatBuilder oppdatertVilkårResultatBuilder = Vilkårene.builderFraEksisterende(opprinneligVilkårResultat);
        final var vilkårBuilder1 = oppdatertVilkårResultatBuilder.hentBuilderFor(VilkårType.MEDLEMSKAPSVILKÅRET);
        var oppdatertVilkårResultat = oppdatertVilkårResultatBuilder.leggTil(vilkårBuilder1
                .leggTil(vilkårBuilder1.hentBuilderFor(LocalDate.now(), Tid.TIDENES_ENDE)
                    .medUtfallOverstyrt(Utfall.IKKE_OPPFYLT)
                    .medAvslagsårsak(Avslagsårsak.SØKER_ER_UTVANDRET)))
            .build();

        // Assert
        assertThat(oppdatertVilkårResultat.getVilkårene()).hasSize(1);
        Vilkår vilkår = oppdatertVilkårResultat.getVilkårene().get(0);
        assertThat(vilkår.getVilkårType()).isEqualTo(VilkårType.MEDLEMSKAPSVILKÅRET);
        assertThat(vilkår.getPerioder()).hasSize(1);
        final var vilkårPeriode = vilkår.getPerioder().get(0);
        assertThat(vilkårPeriode.getGjeldendeUtfall()).isEqualTo(Utfall.IKKE_OPPFYLT);
        assertThat(vilkårPeriode.getAvslagsårsak()).isEqualTo(Avslagsårsak.SØKER_ER_UTVANDRET);
        assertThat(vilkårPeriode.getErOverstyrt()).isTrue();
        assertThat(vilkårPeriode.getErManueltVurdert()).isFalse();

        // Act 2: Oppfylt
        final var vilkårResultatBuilder1 = Vilkårene.builderFraEksisterende(oppdatertVilkårResultat);
        final var vilkårBuilder = vilkårResultatBuilder1.hentBuilderFor(VilkårType.MEDLEMSKAPSVILKÅRET);
        final var periodeBuilder = vilkårBuilder.hentBuilderFor(LocalDate.now(), Tid.TIDENES_ENDE)
            .medUtfallOverstyrt(Utfall.OPPFYLT);
        vilkårBuilder.leggTil(periodeBuilder);
        vilkårResultatBuilder1.leggTil(vilkårBuilder);
        final var vilkårResultat = vilkårResultatBuilder1.build();

        // Assert
        assertThat(vilkårResultat.getVilkårene()).hasSize(1);
        vilkår = vilkårResultat.getVilkårene().get(0);
        assertThat(vilkår.getVilkårType()).isEqualTo(VilkårType.MEDLEMSKAPSVILKÅRET);
        assertThat(vilkår.getPerioder()).hasSize(1);
        final var vilkårPeriode1 = vilkår.getPerioder().get(0);
        assertThat(vilkårPeriode1.getGjeldendeUtfall()).isEqualTo(Utfall.OPPFYLT);
        assertThat(vilkårPeriode1.getAvslagsårsak()).isNull();
        assertThat(vilkårPeriode1.getErOverstyrt()).isTrue();
        assertThat(vilkårPeriode1.getErManueltVurdert()).isFalse();
    }

    @Test
    public void skal_beholde_tidligere_overstyring_inkl_avslagsårsak_når_manuell_vurdering_oppdateres() throws Exception {
        // Arrange
        final var vilkårResultatBuilder = Vilkårene.builder();
        final var vilkårBuilder = vilkårResultatBuilder.hentBuilderFor(VilkårType.MEDLEMSKAPSVILKÅRET);
        vilkårBuilder.leggTil(vilkårBuilder.hentBuilderFor(LocalDate.now(), Tid.TIDENES_ENDE)
            .medUtfall(Utfall.OPPFYLT)
            .medUtfallOverstyrt(Utfall.IKKE_OPPFYLT)
            .medAvslagsårsak(Avslagsårsak.SØKER_ER_UTVANDRET));
        final var overstyrtVilkårResultat = vilkårResultatBuilder.leggTil(vilkårBuilder).build();

        // Act
        final var vilkårResultatBuilder1 = Vilkårene.builderFraEksisterende(overstyrtVilkårResultat);
        final var vilkårBuilder1 = vilkårResultatBuilder1.hentBuilderFor(VilkårType.MEDLEMSKAPSVILKÅRET);
        vilkårBuilder1.leggTil(vilkårBuilder1.hentBuilderFor(LocalDate.now(), Tid.TIDENES_ENDE).medUtfall(Utfall.OPPFYLT));
        final var oppdatertVilkårResultat = vilkårResultatBuilder1.leggTil(vilkårBuilder1).build();

        // Assert
        assertThat(oppdatertVilkårResultat.getVilkårene()).hasSize(1);
        Vilkår vilkår = oppdatertVilkårResultat.getVilkårene().get(0);
        assertThat(vilkår.getVilkårType()).isEqualTo(VilkårType.MEDLEMSKAPSVILKÅRET);
        assertThat(vilkår.getPerioder()).hasSize(1);
        final var vilkårPeriode = vilkår.getPerioder().get(0);
        assertThat(vilkårPeriode.getErOverstyrt()).isTrue();
        assertThat(vilkårPeriode.getAvslagsårsak()).isEqualTo(Avslagsårsak.SØKER_ER_UTVANDRET);
        assertThat(vilkårPeriode.getGjeldendeUtfall()).isEqualTo(Utfall.IKKE_OPPFYLT);
        assertThat(vilkårPeriode.getUtfall()).isEqualTo(Utfall.OPPFYLT);
    }

    private void lagreOgGjenopphenteBehandlingsresultat(Behandling behandling) {

        lagreBehandling(behandling);
        vilkårResultatRepository.lagre(behandling.getId(), Vilkårene.builder()
            .leggTilIkkeVurderteVilkår(List.of(DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now(), LocalDate.now().plusDays(8))), VilkårType.MEDISINSKEVILKÅR_UNDER_18_ÅR).build());
        Long id = behandling.getId();
        assertThat(id).isNotNull();

        Behandling lagretBehandling = repository.hent(Behandling.class, id);
        assertThat(lagretBehandling).isEqualTo(behandling);

    }

    private void lagreBehandling(Behandling behandling) {
        BehandlingLås lås = behandlingRepository.taSkriveLås(behandling);
        behandlingRepository.lagre(behandling, lås);
    }

}
