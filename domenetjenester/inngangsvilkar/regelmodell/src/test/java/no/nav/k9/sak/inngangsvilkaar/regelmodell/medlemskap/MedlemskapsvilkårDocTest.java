package no.nav.k9.sak.inngangsvilkaar.regelmodell.medlemskap;

import org.assertj.core.api.Assertions;
import org.junit.Test;

import no.nav.fpsak.nare.doc.RuleDescriptionDigraph;
import no.nav.fpsak.nare.specification.Specification;
import no.nav.k9.sak.inngangsvilkaar.regelmodell.medlemskap.Medlemskapsvilkår;
import no.nav.k9.sak.inngangsvilkaar.regelmodell.medlemskap.MedlemskapsvilkårGrunnlag;

public class MedlemskapsvilkårDocTest {

    @Test
    public void test_documentation() throws Exception {
        Specification<MedlemskapsvilkårGrunnlag> vilkår = new Medlemskapsvilkår().getSpecification();
        RuleDescriptionDigraph digraph = new RuleDescriptionDigraph(vilkår.ruleDescription());

        String json = digraph.toJson();

        // sjekk at den inneholder noen standard felter
        Assertions.assertThat(json).isNotNull().contains("root", "nodes", "source", "target", "role", "ruleId", "id", "ruleDescription", "operator", "rule");
    }
}
