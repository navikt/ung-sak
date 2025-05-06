package no.nav.ung.sak.test.util.diff;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import no.nav.ung.kodeverk.api.Kodeverdi;
import no.nav.ung.kodeverk.person.NavBrukerKjønn;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.personopplysning.PersonInformasjonBuilder;
import no.nav.ung.sak.behandlingslager.behandling.personopplysning.PersonopplysningVersjonType;
import no.nav.ung.sak.behandlingslager.diff.*;
import no.nav.ung.sak.behandlingslager.diff.TraverseGraph.TraverseResult;
import no.nav.ung.sak.test.util.behandling.TestScenarioBuilder;
import no.nav.ung.sak.typer.AktørId;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

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
        var personInformasjonBuilder = new PersonInformasjonBuilder(PersonopplysningVersjonType.REGISTRERT);

        var olaAktørId = AktørId.dummy();
        var testAktørId = AktørId.dummy();

        var personopplysningBuilder1 = personInformasjonBuilder.getPersonopplysningBuilder(olaAktørId);
        var personopplysningBuilder2 = personInformasjonBuilder.getPersonopplysningBuilder(testAktørId);

        var olaNordmann = personopplysningBuilder1
            .medNavn("Ola Nordmann")
            .medFødselsdato(LocalDate.now())
            .medKjønn(NavBrukerKjønn.MANN)
            .build();
        var testTestesen = personopplysningBuilder2
            .medNavn("Test Testesen")
            .medFødselsdato(LocalDate.now())
            .medKjønn(NavBrukerKjønn.MANN)
            .build();

        DiffEntity differ = new DiffEntity(lagTraverserForTrackedFields());

        // Act
        DiffResult diffResult = differ.diff(olaNordmann, testTestesen);

        // Assert
        assertThat(diffResult.getLeafDifferences()).hasSize(1);
    }

    @Test
    public void skal_oppdage_diff_når_det_kommer_ny_entry() {
        // Arrange
        var personInformasjonBuilder = new PersonInformasjonBuilder(PersonopplysningVersjonType.REGISTRERT);
        var olaAktørId = AktørId.dummy();

        var personopplysningBuilder1 = personInformasjonBuilder.getPersonopplysningBuilder(olaAktørId);
        var personopplysningBuilder2 = personInformasjonBuilder.getPersonopplysningBuilder(olaAktørId);

        var olaNordmann = personopplysningBuilder1
            .medNavn("Ola Nordmann")
            .medFødselsdato(LocalDate.now())
            .medKjønn(NavBrukerKjønn.MANN)
            .build();

        var testTestesen = personopplysningBuilder2
            .medNavn("Test Testesen")
            .medFødselsdato(LocalDate.now())
            .medKjønn(NavBrukerKjønn.MANN)
            .build();

        DiffEntity differ = new DiffEntity(lagTraverserForTrackedFields());

        // Act
        DiffResult diffResult = differ.diff(Arrays.asList(olaNordmann), Arrays.asList(olaNordmann, testTestesen));

        // Assert
        assertThat(diffResult.getLeafDifferences()).hasSize(1);
    }

    @Test
    public void skal_oppdage_diff_i_kodeverk() throws Exception {
        // Arrange
        var personInformasjonBuilder = new PersonInformasjonBuilder(PersonopplysningVersjonType.REGISTRERT);
        var olaAktørId = AktørId.dummy();
        var testAktørId = AktørId.dummy();

        var personopplysningBuilder1 = personInformasjonBuilder.getPersonopplysningBuilder(olaAktørId);
        var personopplysningBuilder2 = personInformasjonBuilder.getPersonopplysningBuilder(testAktørId);

        var olaNordmann = personopplysningBuilder1
            .medNavn("Ola Nordmann")
            .medFødselsdato(LocalDate.now())
            .medKjønn(NavBrukerKjønn.MANN)
            .build();

        var testTestesen = personopplysningBuilder2
            .medNavn("Test Testesen")
            .medFødselsdato(LocalDate.now())
            .medKjønn(NavBrukerKjønn.MANN)
            .build();

        DiffEntity differ = new DiffEntity(lagTraverser());

        // Act
        DiffResult diffResult = differ.diff(olaNordmann, testTestesen);

        // Assert
        Map<Node, Pair> leafDiffs = diffResult.getLeafDifferences();
        assertThat(leafDiffs).hasSize(1);


        // diff mot seg selv
        DiffResult diffResultNy = differ.diff(olaNordmann, olaNordmann);
        assertThat(diffResultNy.getLeafDifferences()).isEmpty();

        // diff mot kopi
        var build = personopplysningBuilder1.build();
        DiffResult diffResultNy2 = differ.diff(olaNordmann, build);
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
