package no.nav.ung.sak.formidling;

import java.util.List;

import com.kobylynskyi.graphql.codegen.model.graphql.GraphQLOperationRequest;
import com.kobylynskyi.graphql.codegen.model.graphql.GraphQLResponseProjection;
import com.kobylynskyi.graphql.codegen.model.graphql.GraphQLResult;

import no.nav.k9.felles.integrasjon.pdl.GeografiskTilknytning;
import no.nav.k9.felles.integrasjon.pdl.GeografiskTilknytningResponseProjection;
import no.nav.k9.felles.integrasjon.pdl.HentGeografiskTilknytningQueryRequest;
import no.nav.k9.felles.integrasjon.pdl.HentIdenterBolkQueryRequest;
import no.nav.k9.felles.integrasjon.pdl.HentIdenterBolkResult;
import no.nav.k9.felles.integrasjon.pdl.HentIdenterBolkResultResponseProjection;
import no.nav.k9.felles.integrasjon.pdl.HentIdenterQueryRequest;
import no.nav.k9.felles.integrasjon.pdl.HentPersonQueryRequest;
import no.nav.k9.felles.integrasjon.pdl.IdentGruppe;
import no.nav.k9.felles.integrasjon.pdl.IdentInformasjon;
import no.nav.k9.felles.integrasjon.pdl.Identliste;
import no.nav.k9.felles.integrasjon.pdl.IdentlisteResponseProjection;
import no.nav.k9.felles.integrasjon.pdl.Pdl;
import no.nav.k9.felles.integrasjon.pdl.Person;
import no.nav.k9.felles.integrasjon.pdl.PersonResponseProjection;
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
    public Person hentPerson(HentPersonQueryRequest q, PersonResponseProjection p, boolean ignoreNotFound) {
        return null;
    }

    @Override
    public GeografiskTilknytning hentGT(HentGeografiskTilknytningQueryRequest q, GeografiskTilknytningResponseProjection p) {
        return null;
    }

    @Override
    public <T extends GraphQLResult<?>> T query(GraphQLOperationRequest q, GraphQLResponseProjection p, Class<T> clazz) {
        return null;
    }
}
