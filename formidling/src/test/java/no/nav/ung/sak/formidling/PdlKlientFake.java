package no.nav.ung.sak.formidling;

import java.util.Collections;
import java.util.List;

import com.kobylynskyi.graphql.codegen.model.graphql.GraphQLOperationRequest;
import com.kobylynskyi.graphql.codegen.model.graphql.GraphQLResponseProjection;
import com.kobylynskyi.graphql.codegen.model.graphql.GraphQLResult;

import no.nav.k9.felles.integrasjon.pdl.Doedsfall;
import no.nav.k9.felles.integrasjon.pdl.Foedselsdato;
import no.nav.k9.felles.integrasjon.pdl.Folkeregisterpersonstatus;
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
import no.nav.k9.felles.integrasjon.pdl.Kjoenn;
import no.nav.k9.felles.integrasjon.pdl.KjoennType;
import no.nav.k9.felles.integrasjon.pdl.Navn;
import no.nav.k9.felles.integrasjon.pdl.Pdl;
import no.nav.k9.felles.integrasjon.pdl.Person;
import no.nav.k9.felles.integrasjon.pdl.PersonResponseProjection;

public class PdlKlientFake implements Pdl {
    private final String aktørid;
    private final Person person;

    public PdlKlientFake(String fornavn, String etternavn, String aktørid) {
        this.aktørid = aktørid;
        // Create a test instance of Person
        person = lagPerson(fornavn, etternavn);

    }

    public static Person lagPerson(String fornavn, String etternavn) {
        var person = new Person();

        // Set values using setters
        Doedsfall doedsfall = new Doedsfall();
        doedsfall.setDoedsdato("2024-12-01");
        person.setDoedsfall(Collections.singletonList(doedsfall));

        Foedselsdato foedselsdato = new Foedselsdato();
        foedselsdato.setFoedselsdato("1990-01-01");
        person.setFoedselsdato(Collections.singletonList(foedselsdato));

        Folkeregisterpersonstatus folkeregisterpersonstatus = new Folkeregisterpersonstatus();
        folkeregisterpersonstatus.setStatus("BOSATT");
        person.setFolkeregisterpersonstatus(Collections.singletonList(folkeregisterpersonstatus));

        Navn navn = new Navn();
        navn.setFornavn(fornavn);
        navn.setEtternavn(etternavn);
        person.setNavn(Collections.singletonList(navn));

        person.setAdressebeskyttelse(Collections.emptyList());
        Kjoenn kjønn = new Kjoenn();
        kjønn.setKjoenn(KjoennType.KVINNE);
        person.setKjoenn(List.of(kjønn));

        return person;
    }


    @Override
    public List<HentIdenterBolkResult> hentIdenterBolkResults(HentIdenterBolkQueryRequest q, HentIdenterBolkResultResponseProjection p) {
        return List.of();
    }

    @Override
    public Identliste hentIdenter(HentIdenterQueryRequest q, IdentlisteResponseProjection p) {
        return new Identliste(List.of(new IdentInformasjon(aktørid, IdentGruppe.AKTORID, false)));
    }

    @Override
    public Person hentPerson(HentPersonQueryRequest q, PersonResponseProjection p) {
        return person;
    }

    @Override
    public Person hentPerson(HentPersonQueryRequest q, PersonResponseProjection p, boolean ignoreNotFound) {
        return person;
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
