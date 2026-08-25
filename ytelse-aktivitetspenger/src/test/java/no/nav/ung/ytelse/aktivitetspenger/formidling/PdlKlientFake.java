package no.nav.ung.ytelse.aktivitetspenger.formidling;

import com.kobylynskyi.graphql.codegen.model.graphql.GraphQLRequest;
import com.kobylynskyi.graphql.codegen.model.graphql.GraphQLResult;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;
import no.nav.k9.felles.integrasjon.pdl.*;
import no.nav.ung.sak.test.util.aktør.FiktiveFnr;

import java.net.URI;
import java.util.List;

@ApplicationScoped
@Alternative
@Priority(value = 1)
public class PdlKlientFake extends PdlKlient {

    private static final FiktiveFnr fiktiveFnr = new FiktiveFnr();
    private final String fnr;

    public PdlKlientFake() {
        super(URI.create("graphql"), null, null);
        this.fnr = fiktiveFnr.nesteFnr();
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
    public Person hentPerson(HentPersonQueryRequest q, PersonResponseProjection p, List<Behandlingsnummer> behandlingsnummere) {
        return null;
    }

    @Override
    public Person hentPerson(HentPersonQueryRequest q, PersonResponseProjection p, boolean ignoreNotFound, List<Behandlingsnummer> behandlingsnummere) {
        return null;
    }

    @Override
    public GeografiskTilknytning hentGeografiskTilknytning(HentGeografiskTilknytningQueryRequest q, GeografiskTilknytningResponseProjection p, List<Behandlingsnummer> behandlingsnummere) {
        return null;
    }

    @Override
    public <T extends GraphQLResult<?>> T query(GraphQLRequest req, Class<T> clazz, List<Behandlingsnummer> behandlingsnummer) {
        return null;
    }

    public String fnr() {
        return fnr;
    }
}


