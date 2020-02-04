package no.nav.foreldrepenger.behandlingslager.diff;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.OneToMany;

import org.junit.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapPerioderBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapPerioderEntitet;
import no.nav.foreldrepenger.behandlingslager.diff.TraverseGraph.TraverseResult;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.TestScenarioBuilder;
import no.nav.k9.kodeverk.api.Kodeverdi;
import no.nav.k9.kodeverk.medlemskap.MedlemskapKildeType;

public class TraverseEntityGraphTest {

    @Test
    public void skal_traverse_entity_graph() {
        var scenario = lagTestScenario();

        Behandling behandling = scenario.lagMocked();

        TraverseGraph traverser = lagTraverser();

        TraverseResult result = traverser.traverse(behandling);
        assertThat(result).isNotNull();
        assertThat(result.getValues()).isEmpty();
    }

    @Test
    public void skal_ikke_ha_diff_for_seg_selv() {

        var scenario = lagTestScenario();
        Behandling target = scenario.lagMocked();

        DiffEntity differ = new DiffEntity(lagTraverser());

        DiffResult diffResult = differ.diff(target, target);

        assertThat(diffResult.getLeafDifferences()).isEmpty();
    }

    @Test
    public void skal_sammenligne_Lists_med_forskjellig_rekkefølge() {

        DiffEntity differ = new DiffEntity(lagTraverser());

        DummyEntitetMedListe en = new DummyEntitetMedListe();

        en.leggTil(new DummyEntitet("a"));
        en.leggTil(new DummyEntitet("b"));

        // sjekk med annen rekkefølge
        DummyEntitetMedListe to = new DummyEntitetMedListe();
        to.leggTil(new DummyEntitet("b"));
        to.leggTil(new DummyEntitet("a"));
        assertThat(differ.diff(en, to).getLeafDifferences()).isEmpty();

        // sjekk også med kopi av seg selv
        DummyEntitetMedListe tre = new DummyEntitetMedListe();
        tre.leggTil(new DummyEntitet("a"));
        tre.leggTil(new DummyEntitet("b"));
        assertThat(differ.diff(en, tre).getLeafDifferences()).isEmpty();

        // sjekk med noe annerledes
        DummyEntitetMedListe fem = new DummyEntitetMedListe();
        fem.leggTil(new DummyEntitet("a"));
        fem.leggTil(new DummyEntitet("c"));
        assertThat(differ.diff(en, fem).getLeafDifferences()).hasSize(2);

    }

    @Test
    public void skal_sammenligne_Lists_med_forskjellig_størrelse() {
        DiffEntity differ = new DiffEntity(lagTraverser());

        DummyEntitetMedListe en = new DummyEntitetMedListe();

        en.leggTil(new DummyEntitet("a"));
        en.leggTil(new DummyEntitet("b"));

        // sjekk med noe mer
        DummyEntitetMedListe fire = new DummyEntitetMedListe();
        fire.leggTil(new DummyEntitet("a"));
        fire.leggTil(new DummyEntitet("b"));
        fire.leggTil(new DummyEntitet("c"));
        assertThat(differ.diff(en, fire).getLeafDifferences()).hasSize(1);

    }

    @Test
    public void skal_diffe_fødselsdato() {

        var scenario = lagTestScenario();
        Behandling target1 = scenario.lagMocked();
        var scenario1 = lagTestScenario();
        final Behandling target2 = scenario1.lagMocked();

        DiffEntity differ = new DiffEntity(lagTraverser());

        DiffResult diffResult = differ.diff(target1, target2);

        Map<Node, Pair> leafDifferences = diffResult.getLeafDifferences();
        assertThat(leafDifferences.size()).isGreaterThanOrEqualTo(0);
        assertThat(containsKey(leafDifferences, "Behandlingsgrunnlag.søknad.familieHendelse.barna.[0].fødselsdato")).isFalse();

        // System.out.println(diffResult.getLeafDifferences());
    }

    @Test
    public void skal_kun_diffe_på_markerte_felt() {
        // Arrange
        MedlemskapPerioderEntitet medlemskap1 = new MedlemskapPerioderBuilder()
            .medMedlId(1L) // MedlId er ikke markert
            .medErMedlem(true)
            .build();

        MedlemskapPerioderEntitet medlemskap2 = new MedlemskapPerioderBuilder()
            .medMedlId(2L) // MedlId er ikke markert
            .medErMedlem(false)
            .build();

        DiffEntity differ = new DiffEntity(lagTraverserForTrackedFields());

        // Act
        DiffResult diffResult = differ.diff(medlemskap1, medlemskap2);

        // Assert
        assertThat(diffResult.getLeafDifferences()).hasSize(1);
    }

    @Test
    public void skal_oppdage_diff_når_det_kommer_ny_entry() {
        // Arrange
        MedlemskapPerioderEntitet periode1 = new MedlemskapPerioderBuilder().medErMedlem(true).build();
        MedlemskapPerioderEntitet periode2 = new MedlemskapPerioderBuilder().medErMedlem(false).build();

        DiffEntity differ = new DiffEntity(lagTraverserForTrackedFields());

        // Act
        DiffResult diffResult = differ.diff(Arrays.asList(periode1), Arrays.asList(periode1, periode2));

        // Assert
        assertThat(diffResult.getLeafDifferences()).hasSize(1);
    }

    @Test
    public void skal_oppdage_diff_i_kodeverk() throws Exception {

        // Arrange
        MedlemskapPerioderEntitet periode1 = new MedlemskapPerioderBuilder().medKildeType(MedlemskapKildeType.ANNEN).build();
        MedlemskapPerioderEntitet periode2 = new MedlemskapPerioderBuilder().medKildeType(MedlemskapKildeType.TPS).build();

        DiffEntity differ = new DiffEntity(lagTraverser());

        // Act
        DiffResult diffResult = differ.diff(periode1, periode2);

        // Assert
        Map<Node, Pair> leafDiffs = diffResult.getLeafDifferences();
        assertThat(leafDiffs).hasSize(1);


        // diff mot seg selv
        DiffResult diffResultNy = differ.diff(periode1, periode1);
        assertThat(diffResultNy.getLeafDifferences()).isEmpty();
        
        // diff mot kopi
        MedlemskapPerioderEntitet nyPeriode1 = new MedlemskapPerioderBuilder().medKildeType(MedlemskapKildeType.ANNEN).build();
        DiffResult diffResultNy2 = differ.diff(periode1, nyPeriode1);
        assertThat(diffResultNy2.getLeafDifferences()).isEmpty();

    }

    private boolean containsKey(Map<Node, Pair> leafDifferences, String key) {
        for (Node node : leafDifferences.keySet()) {
            if (node.toString().equals(key)) {
                return true;
            }
        }
        return false;
    }

    private TraverseGraph lagTraverserForTrackedFields() {
        var config = new TraverseJpaEntityGraphConfig();
        config.setIgnoreNulls(true);
        config.addRootClasses(Behandling.class);
        config.addLeafClasses(Kodeverdi.class);
        config.setOnlyCheckTrackedFields(true);
        return new TraverseGraph(config);
    }

    private TraverseGraph lagTraverser() {
        var config = new TraverseJpaEntityGraphConfig();
        config.setIgnoreNulls(true);
        config.addRootClasses(Behandling.class);
        config.addLeafClasses(Kodeverdi.class);
        return new TraverseGraph(config);
    }

    private TestScenarioBuilder lagTestScenario() {
        var scenario = TestScenarioBuilder
            .builderMedSøknad();
        return scenario;
    }

    @Entity
    static class DummyEntitetMedListe {

        @ChangeTracked
        @OneToMany
        private List<DummyEntitet> entiteter = new ArrayList<>();

        public void leggTil(DummyEntitet dummyEntitet) {
            entiteter.add(dummyEntitet);
        }
    }

    @Entity
    static class DummyEntitet {
        @Column(name = "kode")
        String kode;

        public DummyEntitet(String kode) {
            this.kode = kode;
        }

        @Override
        public int hashCode() {
            return Objects.hash(kode);
        }

        @Override
        public boolean equals(Object obj) {
            return Objects.equals(kode, ((DummyEntitet) obj).kode);
        }

        @Override
        public String toString() {
            return "Dummy<" + kode + ">";
        }

    }
}
