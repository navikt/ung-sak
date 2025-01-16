package no.nav.ung.sak.tilgangskontroll.integrasjon.pdl.dto;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class PipPerson {

    private String aktoerId;
    private Person person;
    private PipIdenter identer;

    @JsonCreator
    public PipPerson(String aktoerId, Person person, PipIdenter identer) {
        this.aktoerId = aktoerId;
        this.person = person;
        this.identer = identer;
    }

    public String getAktoerId() {
        return aktoerId;
    }

    public Person getPerson() {
        return person;
    }

    public PipIdenter getIdenter() {
        return identer;
    }

    public String getAktivPersonIdent() {
        List<PipIdent> resultat = identer.identer().stream()
                .filter(ident -> !ident.historisk())
                .filter(ident -> ident.gruppe().equals("FOLKEREGISTERIDENT"))
                .toList();
        if (resultat.size() == 1) {
            return resultat.getFirst().ident();
        }
        throw new IllegalArgumentException("Forventet å finne nøyaktig en aktiv folkeregisterident ident, fant: " + resultat.size());
    }

    public Set<AdressebeskyttelseGradering> getAdressebeskyttelseGradering() {
        return person.getAdressebeskyttelse().stream()
                .map(it -> AdressebeskyttelseGradering.fraKode(it.getGradering()))
                .collect(Collectors.toSet());
    }
}
