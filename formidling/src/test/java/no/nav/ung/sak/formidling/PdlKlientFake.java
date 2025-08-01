package no.nav.ung.sak.formidling;

import com.kobylynskyi.graphql.codegen.model.graphql.GraphQLOperationRequest;
import com.kobylynskyi.graphql.codegen.model.graphql.GraphQLRequest;
import com.kobylynskyi.graphql.codegen.model.graphql.GraphQLResponseProjection;
import com.kobylynskyi.graphql.codegen.model.graphql.GraphQLResult;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;
import no.nav.k9.felles.integrasjon.pdl.*;
import no.nav.ung.sak.test.util.aktør.FiktiveFnr;

import java.util.List;

@ApplicationScoped
@Alternative
@Priority(value = 1)
public class PdlKlientFake implements Pdl {

    private static final FiktiveFnr fiktiveFnr = new FiktiveFnr();
    private String fnr;

    public PdlKlientFake() {
        this.fnr = gyldigFnr();
    }

    public PdlKlientFake(String fnr) {
        this.fnr = fnr;
    }

    public static PdlKlientFake medTilfeldigFnr() {
        return new PdlKlientFake(gyldigFnr());
    }

    private static String gyldigFnr() {
        return fiktiveFnr.nesteFnr();
    }


    @Override
    public List<HentIdenterBolkResult> hentIdenterBolkResults(HentIdenterBolkQueryRequest q, HentIdenterBolkResultResponseProjection p) {
        return List.of();
    }

    @Override
    public Identliste hentIdenter(HentIdenterQueryRequest q, IdentlisteResponseProjection p) {
        return new Identliste(List.of(new IdentInformasjon(fnr, IdentGruppe.FOLKEREGISTERIDENT, false)));
    }

    @Override
    public Person hentPerson(HentPersonQueryRequest q, PersonResponseProjection p) {
        return null;
    }

    @Override
    public Person hentPerson(HentPersonQueryRequest q, PersonResponseProjection p, List<Behandlingsnummer> behandlingsnummere) {
        return null;
    }

    @Override
    public Person hentPerson(HentPersonQueryRequest q, PersonResponseProjection p, boolean ignoreNotFound) {
        return null;
    }

    @Override
    public Person hentPerson(HentPersonQueryRequest q, PersonResponseProjection p, boolean ignoreNotFound, List<Behandlingsnummer> behandlingsnummere) {
        return null;
    }

    @Override
    public GeografiskTilknytning hentGT(HentGeografiskTilknytningQueryRequest q, GeografiskTilknytningResponseProjection p) {
        return null;
    }

    @Override
    public GeografiskTilknytning hentGT(HentGeografiskTilknytningQueryRequest q, GeografiskTilknytningResponseProjection p, List<Behandlingsnummer> behandlingsnummere) {
        return null;
    }

    @Override
    public <T extends GraphQLResult<?>> T query(GraphQLRequest req, Class<T> clazz, List<Behandlingsnummer> behandlingsnummer) {
        return null;
    }

    @Override
    public <T extends GraphQLResult<?>> T query(GraphQLOperationRequest q, GraphQLResponseProjection p, Class<T> clazz) {
        return null;
    }

    public String fnr() {
        return fnr;
    }

}

