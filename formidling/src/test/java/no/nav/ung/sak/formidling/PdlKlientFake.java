package no.nav.ung.sak.formidling;

import java.util.List;

import com.kobylynskyi.graphql.codegen.model.graphql.GraphQLOperationRequest;
import com.kobylynskyi.graphql.codegen.model.graphql.GraphQLRequest;
import com.kobylynskyi.graphql.codegen.model.graphql.GraphQLResponseProjection;
import com.kobylynskyi.graphql.codegen.model.graphql.GraphQLResult;

import no.nav.k9.felles.integrasjon.pdl.*;
import no.nav.ung.sak.test.util.aktør.FiktiveFnr;

public record PdlKlientFake(String fnr) implements Pdl {
    private static final FiktiveFnr fiktiveFnr = new FiktiveFnr();

    public static PdlKlientFake medTilfeldigFnr() {
        return new PdlKlientFake(gyldigFnr());
    }

    public static String gyldigFnr() {
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
}
